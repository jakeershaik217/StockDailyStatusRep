#!/usr/bin/env python3
"""Weekly Andhra Pradesh maize price brief.

Pulls daily mandi prices from the Government of India open data platform
(Agmarknet feed), appends them to a local history file, computes week-on-week
and month-on-month moves, scans news feeds for the events that drive the AP
maize market, and renders a brief in Markdown and HTML.

Optionally emails the brief when SMTP settings are supplied via environment
variables (see send_email).

Usage:
    python3 weekly_brief.py [--outdir DIR] [--no-news] [--email]
"""

import argparse
import csv
import datetime as dt
import html
import json
import os
import re
import smtplib
import ssl
import statistics
import sys
import time
import urllib.parse
import urllib.request
from email.message import EmailMessage
from pathlib import Path

AGMARKNET_RESOURCE = "9ef84268-d588-465a-a308-a864a43d0070"
AGMARKNET_URL = f"https://api.data.gov.in/resource/{AGMARKNET_RESOURCE}"
# Public sample key published by data.gov.in for open datasets.
DEFAULT_API_KEY = "579b464db66ec23bdd000001cdd3946e44ce4aad7209ff7b23ac571b"

REPO_ROOT = Path(__file__).resolve().parent
HISTORY_FILE = REPO_ROOT / "history" / "maize_prices.csv"

# Markets that actually set the price for an Andhra Pradesh seller.
AP_BENCHMARK_MARKETS = [
    "Kurnool", "Panyam", "Bapatla", "Parchur", "Ipur", "Mylavaram",
    "Rajanagaram", "Bhimunipatnam", "Ponduru", "Chittoor", "Guntur",
]
# Telangana leads southern price direction; AP follows it.
TG_BENCHMARK_MARKETS = ["Nizamabad", "Jagtial", "Karimnagar", "Warangal", "Siddipet"]

MSP_BY_SEASON = {"2024-25": 2225, "2025-26": 2400, "2026-27": 2410}
CURRENT_MSP_SEASON = "2026-27"

# Thin-arrival quotes wreck the median; ignore anything outside this band.
SANE_MIN, SANE_MAX = 1200, 4000

# The Agmarknet feed fills through the Indian working day. Fewer than this many
# Andhra Pradesh quotes means the day is incomplete, not that prices moved.
MIN_AP_QUOTES = 8

NEWS_QUERIES = [
    ("Monsoon / El Nino", "IMD+monsoon+rainfall+deficit+El+Nino+India"),
    ("Sowing & acreage", "maize+sowing+acreage+kharif+rabi+India+lakh+hectares"),
    ("Reservoirs / AP water", "Nagarjuna+Sagar+Srisailam+reservoir+storage+Krishna+delta"),
    ("Imports & duty (bear trigger)", "maize+corn+import+duty+TRQ+India+DGFT+ethanol"),
    ("Ethanol policy & pricing", "ethanol+maize+feedstock+OMC+procurement+price+blending+India"),
    ("MSP & procurement", "maize+MSP+procurement+PSS+Andhra+Pradesh+Telangana"),
    ("Feed & poultry demand", "poultry+feed+maize+soymeal+price+India+egg"),
    ("AP maize market", "Andhra+Pradesh+maize+farmers+price+mandi"),
]

TRIGGERS = [
    ("Sep 2026", "IMD end-of-season rainfall; DES 1st Advance Estimates",
     "Deficit >15% or maize output down >8% shifts the base case to the bull case"),
    ("1 Nov 2026", "Nagarjuna Sagar / Srisailam storage",
     "Below 40% cuts AP rabi maize area - worth about +Rs 200 on the Jun-Jul 2027 call"),
    ("Nov-Dec 2026", "Kharif arrival prices at Nizamabad / Kurnool",
     "Harvest prices holding above Rs 2,400 lift the whole 2027 curve by Rs 150-250"),
    ("Dec 2026-Feb 2027", "Any DGFT notification on duty-free maize / corn TRQ for ethanol",
     "The single biggest bear trigger - caps the market immediately"),
    ("Jan 2027", "OMC ethanol procurement price revision for maize ethanol",
     "A higher ethanol price raises the maize floor roughly one-for-one"),
    ("Mar 2027", "DES 2nd Advance Estimates, rabi maize",
     "Confirms or kills the tight-stocks story"),
    ("May 2027", "MSP announcement for KMS 2027-28",
     "Expect Rs 2,500-2,570; sets the psychological floor"),
]

SCENARIOS = [
    ("Base - mild deficit, ethanol demand grinds prices up", 55, 2550, 2850, 2700),
    ("Bull - El Nino damage carries into rabi", 25, 3000, 3400, 3150),
    ("Bear - monsoon recovers, big rabi, liberal imports", 20, 2200, 2450, 2350),
]


# --------------------------------------------------------------------------
# Data collection
# --------------------------------------------------------------------------

def _get_json(url: str, timeout: int = 45, attempts: int = 3) -> dict:
    """GET with retries - the data.gov.in endpoint times out fairly often."""
    last = None
    for attempt in range(attempts):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return json.loads(resp.read().decode("utf-8", "ignore"))
        except Exception as exc:
            last = exc
            time.sleep(3 * (attempt + 1))
    raise last


def fetch_mandi_prices(api_key: str, max_records: int = 600) -> list:
    """Page through today's maize quotes across every reporting mandi.

    The API caps each response at ten records regardless of the limit
    parameter, and applies state filters inconsistently, so page the
    commodity-only filter and split by state locally.
    """
    records, offset, seen, empty_pages = [], 0, set(), 0
    while offset < max_records:
        params = urllib.parse.urlencode({
            "api-key": api_key,
            "format": "json",
            "offset": offset,
            "filters[commodity]": "Maize",
        })
        try:
            payload = _get_json(f"{AGMARKNET_URL}?{params}")
        except Exception as exc:  # network hiccup - keep what we have
            print(f"  warning: mandi fetch failed at offset {offset}: {exc}", file=sys.stderr)
            break
        batch = payload.get("records", [])
        if not batch:
            # A short page can mean the end of the feed or a transient gap;
            # probe one page further before giving up.
            empty_pages += 1
            if empty_pages > 1:
                break
            offset += 10
            continue
        empty_pages = 0
        for row in batch:
            key = (row.get("state"), row.get("market"), row.get("variety"), row.get("modal_price"))
            if key not in seen:
                seen.add(key)
                records.append(row)
        offset += len(batch)
    return records


def clean(records: list, state: str) -> list:
    out = []
    for row in records:
        if row.get("state") != state:
            continue
        try:
            modal = float(row.get("modal_price"))
        except (TypeError, ValueError):
            continue
        if not (SANE_MIN <= modal <= SANE_MAX):
            continue  # thin-arrival outlier
        out.append({
            "district": row.get("district", ""),
            "market": row.get("market", ""),
            "variety": row.get("variety", ""),
            "modal": modal,
            "date": row.get("arrival_date", ""),
        })
    return sorted(out, key=lambda r: -r["modal"])


def median_of(rows: list):
    return statistics.median([r["modal"] for r in rows]) if rows else None


def benchmark_rows(rows: list, names: list) -> list:
    return [r for r in rows if any(n.lower() in r["market"].lower() for n in names)]


# --------------------------------------------------------------------------
# History
# --------------------------------------------------------------------------

HISTORY_FIELDS = ["run_date", "ap_median", "tg_median", "national_median",
                  "ap_min", "ap_max", "ap_markets_reporting"]


def append_history(row: dict) -> None:
    HISTORY_FILE.parent.mkdir(parents=True, exist_ok=True)
    exists = HISTORY_FILE.exists()
    rows = read_history()
    # Re-running on the same day replaces that day's entry rather than duplicating it.
    rows = [r for r in rows if r.get("run_date") != row["run_date"]]
    rows.append({k: row.get(k, "") for k in HISTORY_FIELDS})
    rows.sort(key=lambda r: r["run_date"])
    with HISTORY_FILE.open("w", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=HISTORY_FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    if not exists:
        print(f"  created {HISTORY_FILE}")


def read_history() -> list:
    if not HISTORY_FILE.exists():
        return []
    with HISTORY_FILE.open(newline="") as fh:
        return [r for r in csv.DictReader(fh)]


def lookback(history: list, run_date: str, weeks: int):
    """Closest history entry to `weeks` weeks before run_date, within +/- 10 days.

    Entries less than four days old are ignored so a mid-week rerun is never
    compared against itself.
    """
    today = dt.date.fromisoformat(run_date)
    target = today - dt.timedelta(weeks=weeks)
    best, best_gap = None, dt.timedelta(days=11)
    for row in history:
        if not row.get("ap_median"):
            continue
        when = dt.date.fromisoformat(row["run_date"])
        if (today - when).days < 4:
            continue
        gap = abs(when - target)
        if gap < best_gap:
            best, best_gap = row, gap
    return best


def pct_change(new, old):
    if new is None or old in (None, "", 0):
        return None
    return (float(new) - float(old)) / float(old) * 100.0


# --------------------------------------------------------------------------
# News
# --------------------------------------------------------------------------

def fetch_news(days: int = 8) -> dict:
    cutoff = dt.datetime.now(dt.timezone.utc) - dt.timedelta(days=days)
    results = {}
    for label, query in NEWS_QUERIES:
        url = (f"https://news.google.com/rss/search?q={query}"
               f"&hl=en-IN&gl=IN&ceid=IN:en")
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            body = urllib.request.urlopen(req, timeout=30).read().decode("utf-8", "ignore")
        except Exception as exc:
            print(f"  warning: news fetch failed for {label}: {exc}", file=sys.stderr)
            results[label] = []
            continue
        items = []
        for chunk in re.findall(r"<item>(.*?)</item>", body, re.S):
            title_m = re.search(r"<title>(.*?)</title>", chunk, re.S)
            date_m = re.search(r"<pubDate>(.*?)</pubDate>", chunk)
            if not title_m or not date_m:
                continue
            try:
                pub = dt.datetime.strptime(date_m.group(1).strip(),
                                           "%a, %d %b %Y %H:%M:%S %Z")
                pub = pub.replace(tzinfo=dt.timezone.utc)
            except ValueError:
                continue
            if pub < cutoff:
                continue
            title = html.unescape(re.sub(r"<[^>]+>", "", title_m.group(1))).strip()
            items.append({"title": title, "date": pub.strftime("%d %b")})
            if len(items) >= 5:
                break
        results[label] = items
    return results


# --------------------------------------------------------------------------
# Rendering
# --------------------------------------------------------------------------

def arrow(change):
    if change is None:
        return "n/a"
    sign = "+" if change >= 0 else ""
    marker = "UP" if change > 0.5 else ("DOWN" if change < -0.5 else "FLAT")
    return f"{sign}{change:.1f}% {marker}"


def render_markdown(ctx: dict) -> str:
    L = []
    a = L.append
    a(f"# AP Maize Weekly Brief - {ctx['pretty_date']}")
    a("")
    a(f"**Headline: AP median modal price Rs {ctx['ap_median']:,.0f}/qtl** "
      f"({arrow(ctx['wow'])} w/w, {arrow(ctx['mom'])} vs 4 weeks ago) "
      f"against an MSP of Rs {ctx['msp']:,} for KMS {CURRENT_MSP_SEASON}.")
    a("")
    if ctx.get("stale_from"):
        a(f"> **Note:** the Agmarknet feed had only a handful of Andhra Pradesh quotes "
          f"at run time (mandi holiday, or the feed had not filled for the day), so the "
          f"headline medians carry over the {ctx['stale_from']} reading. The market "
          f"table below shows whatever has reported so far today.")
        a("")
    a(f"Position vs MSP: **{ctx['vs_msp']}**. "
      f"Telangana median Rs {ctx['tg_median']:,.0f} (the southern price setter). "
      f"National median Rs {ctx['national_median']:,.0f}, so the South carries a "
      f"Rs {ctx['south_premium']:,.0f} premium.")
    a("")

    a("## 1. Andhra Pradesh benchmark markets")
    a("")
    a("| Market | District | Variety | Modal Rs/qtl |")
    a("|---|---|---|---|")
    for r in ctx["ap_bench"][:12]:
        a(f"| {r['market']} | {r['district']} | {r['variety']} | {r['modal']:,.0f} |")
    a("")
    if ctx["tg_bench"]:
        a("**Telangana reference:** " + ", ".join(
            f"{r['market']} Rs {r['modal']:,.0f}" for r in ctx["tg_bench"][:5]))
        a("")

    a("## 2. What moved")
    a("")
    a(f"- Week on week: **{arrow(ctx['wow'])}**"
      + (f" (from Rs {ctx['prev_week']:,.0f})" if ctx["prev_week"] else " (no prior week on file yet)"))
    a(f"- Four weeks: **{arrow(ctx['mom'])}**"
      + (f" (from Rs {ctx['prev_month']:,.0f})" if ctx["prev_month"] else " (history still building)"))
    a(f"- Spread across AP markets: Rs {ctx['ap_min']:,.0f} to Rs {ctx['ap_max']:,.0f} "
      f"across {ctx['ap_count']} reporting mandis")
    a("")

    a("## 3. Scenario tracker for June-July 2027")
    a("")
    a("| Scenario | Prob. | Range Rs/qtl | Centre | On track? |")
    a("|---|---|---|---|---|")
    for name, prob, lo, hi, mid in SCENARIOS:
        a(f"| {name} | {prob}% | {lo:,} - {hi:,} | {mid:,} | {ctx['scenario_status'].get(name, 'monitoring')} |")
    a("")
    a(f"**Working forecast unchanged: Rs 2,700/qtl in June 2027, Rs 2,750 in July 2027 "
      f"(70% band Rs 2,450-3,000).** {ctx['forecast_note']}")
    a("")

    a("## 4. Trigger calendar")
    a("")
    a("| When | What | Why it matters |")
    a("|---|---|---|")
    for when, what, why in TRIGGERS:
        a(f"| {when} | {what} | {why} |")
    a("")

    if ctx["news"]:
        a("## 5. News scan (last 8 days)")
        a("")
        for label, items in ctx["news"].items():
            if not items:
                continue
            a(f"**{label}**")
            for item in items:
                a(f"- {item['title']} ({item['date']})")
            a("")

    a("---")
    a("")
    a("*Data: Agmarknet daily mandi prices via data.gov.in. "
      "Thin-arrival quotes outside Rs 1,200-4,000 are excluded from medians. "
      "Generated automatically; the scenario view is a judgement call, not a fact.*")
    return "\n".join(L)


def markdown_to_html(md: str) -> str:
    """Small Markdown-to-HTML converter covering the subset used above."""
    out, in_table, in_list = [], False, False

    def inline(text):
        text = html.escape(text)
        text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
        text = re.sub(r"\*(.+?)\*", r"<em>\1</em>", text)
        return text

    for line in md.split("\n"):
        stripped = line.strip()
        is_row = stripped.startswith("|") and stripped.endswith("|")
        if not is_row and in_table:
            out.append("</tbody></table>")
            in_table = False
        if not stripped.startswith("- ") and in_list:
            out.append("</ul>")
            in_list = False

        if is_row:
            cells = [c.strip() for c in stripped.strip("|").split("|")]
            if set("".join(cells)) <= set("-: "):
                continue  # separator row
            if not in_table:
                out.append('<table cellpadding="6" cellspacing="0" '
                           'style="border-collapse:collapse;border:1px solid #ccc">')
                out.append("<thead><tr>" + "".join(
                    f'<th style="border:1px solid #ccc;background:#f4f4f4;text-align:left">{inline(c)}</th>'
                    for c in cells) + "</tr></thead><tbody>")
                in_table = True
            else:
                out.append("<tr>" + "".join(
                    f'<td style="border:1px solid #ccc">{inline(c)}</td>' for c in cells) + "</tr>")
        elif stripped.startswith("### "):
            out.append(f"<h3>{inline(stripped[4:])}</h3>")
        elif stripped.startswith("## "):
            out.append(f"<h2>{inline(stripped[3:])}</h2>")
        elif stripped.startswith("# "):
            out.append(f"<h1>{inline(stripped[2:])}</h1>")
        elif stripped == "---":
            out.append("<hr>")
        elif stripped.startswith("> "):
            out.append(f'<blockquote style="border-left:4px solid #d0a020;margin:8px 0;'
                       f'padding:6px 12px;background:#fffbe6">{inline(stripped[2:])}</blockquote>')
        elif stripped.startswith("- "):
            if not in_list:
                out.append("<ul>")
                in_list = True
            out.append(f"<li>{inline(stripped[2:])}</li>")
        elif stripped:
            out.append(f"<p>{inline(stripped)}</p>")
    if in_table:
        out.append("</tbody></table>")
    if in_list:
        out.append("</ul>")
    body = "\n".join(out)
    return ("<html><body style=\"font-family:Segoe UI,Helvetica,Arial,sans-serif;"
            "font-size:14px;color:#222;max-width:900px\">" + body + "</body></html>")


# --------------------------------------------------------------------------
# Email
# --------------------------------------------------------------------------

def send_email(subject: str, html_body: str, text_body: str) -> bool:
    """Send the brief over SMTP.

    Reads configuration from the environment so no credential is ever stored
    in the repository:
        MAIZE_SMTP_HOST      default smtp.gmail.com
        MAIZE_SMTP_PORT      default 587
        MAIZE_SMTP_USER      sending account
        MAIZE_SMTP_PASSWORD  app password
        MAIZE_EMAIL_TO       comma separated recipients
        MAIZE_EMAIL_FROM     optional, defaults to MAIZE_SMTP_USER
    """
    host = os.environ.get("MAIZE_SMTP_HOST", "smtp.gmail.com")
    port = int(os.environ.get("MAIZE_SMTP_PORT", "587"))
    user = os.environ.get("MAIZE_SMTP_USER")
    # Google displays app passwords in four-character groups; strip the spaces.
    password = (os.environ.get("MAIZE_SMTP_PASSWORD") or "").replace(" ", "") or None
    to = os.environ.get("MAIZE_EMAIL_TO")
    sender = os.environ.get("MAIZE_EMAIL_FROM", user)

    if not (user and password and to):
        print("  email skipped: MAIZE_SMTP_USER / MAIZE_SMTP_PASSWORD / MAIZE_EMAIL_TO not set",
              file=sys.stderr)
        return False

    msg = EmailMessage()
    msg["Subject"] = subject
    msg["From"] = sender
    msg["To"] = to
    msg.set_content(text_body)
    msg.add_alternative(html_body, subtype="html")

    context = ssl.create_default_context()
    if port == 465:
        with smtplib.SMTP_SSL(host, port, context=context, timeout=60) as server:
            server.login(user, password)
            server.send_message(msg)
    else:
        with smtplib.SMTP(host, port, timeout=60) as server:
            server.starttls(context=context)
            server.login(user, password)
            server.send_message(msg)
    print(f"  email sent to {to}")
    return True


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------

def build_context(no_news: bool = False) -> dict:
    api_key = os.environ.get("DATA_GOV_IN_API_KEY", DEFAULT_API_KEY)
    print("Fetching mandi prices...")
    raw = fetch_mandi_prices(api_key)
    print(f"  {len(raw)} maize quotes across all states")

    ap = clean(raw, "Andhra Pradesh")
    tg = clean(raw, "Telangana")
    national = [float(r["modal_price"]) for r in raw
                if str(r.get("modal_price", "")).replace(".", "", 1).isdigit()
                and SANE_MIN <= float(r["modal_price"]) <= SANE_MAX]

    history = read_history()
    if len(ap) < MIN_AP_QUOTES:
        # Incomplete day (early morning, Sunday or a mandi holiday). Carry the
        # last complete reading rather than publishing a median built on a
        # handful of quotes, and leave the history file untouched.
        if not history:
            raise SystemExit(
                "Only %d Andhra Pradesh maize quotes available and no history on "
                "file - rerun later in the Indian working day (after ~15:00 IST)."
                % len(ap))
        last = history[-1]
        print(f"  only {len(ap)} AP quotes so far today; reusing the {last['run_date']} snapshot",
              file=sys.stderr)
        return _assemble(
            run_date=dt.date.today().isoformat(),
            ap_median=float(last["ap_median"]),
            tg_median=float(last["tg_median"]),
            national_median=float(last["national_median"]),
            ap_bench=ap, tg_bench=tg,
            ap_min=float(last["ap_min"]), ap_max=float(last["ap_max"]),
            ap_count=int(last["ap_markets_reporting"]),
            history=history, no_news=no_news,
            stale_from=last["run_date"],
        )

    ap_median = median_of(ap)
    tg_median = median_of(tg) or ap_median
    national_median = statistics.median(national) if national else ap_median
    run_date = dt.date.today().isoformat()

    append_history({
        "run_date": run_date,
        "ap_median": f"{ap_median:.0f}",
        "tg_median": f"{tg_median:.0f}",
        "national_median": f"{national_median:.0f}",
        "ap_min": f"{min(r['modal'] for r in ap):.0f}",
        "ap_max": f"{max(r['modal'] for r in ap):.0f}",
        "ap_markets_reporting": str(len(ap)),
    })
    return _assemble(
        run_date=run_date,
        ap_median=ap_median, tg_median=tg_median, national_median=national_median,
        ap_bench=benchmark_rows(ap, AP_BENCHMARK_MARKETS) or ap,
        tg_bench=benchmark_rows(tg, TG_BENCHMARK_MARKETS) or tg,
        ap_min=min(r["modal"] for r in ap), ap_max=max(r["modal"] for r in ap),
        ap_count=len(ap), history=read_history(), no_news=no_news,
    )


def _assemble(run_date, ap_median, tg_median, national_median, ap_bench, tg_bench,
              ap_min, ap_max, ap_count, history, no_news, stale_from=None) -> dict:
    prev_week = lookback(history, run_date, 1)
    prev_month = lookback(history, run_date, 4)
    wow = pct_change(ap_median, prev_week["ap_median"]) if prev_week else None
    mom = pct_change(ap_median, prev_month["ap_median"]) if prev_month else None

    msp = MSP_BY_SEASON[CURRENT_MSP_SEASON]
    gap = ap_median - msp
    if gap > 150:
        vs_msp = f"Rs {gap:,.0f} above MSP - the ethanol bid is doing the work"
    elif gap >= -50:
        vs_msp = f"Rs {abs(gap):,.0f} {'above' if gap >= 0 else 'below'} MSP - trading at parity"
    else:
        vs_msp = (f"Rs {abs(gap):,.0f} below MSP - distress territory, watch for "
                  f"procurement demands from AP and Telangana")

    # Which scenario does today's print support?
    status = {}
    for name, _prob, lo, hi, _mid in SCENARIOS:
        if name.startswith("Base"):
            status[name] = "on track" if 2350 <= ap_median <= 2700 else "off track"
        elif name.startswith("Bull"):
            status[name] = "live" if ap_median > 2700 else "not yet"
        else:
            status[name] = "live" if ap_median < 2350 else "not yet"

    if ap_median > 2700:
        note = "Prices are already inside the bull range - consider raising the base case."
    elif ap_median < 2300:
        note = "Prices are soft; if this persists into the kharif harvest the bear case gains weight."
    else:
        note = "Today's print is consistent with the base case."

    return {
        "run_date": run_date,
        "pretty_date": dt.date.fromisoformat(run_date).strftime("%d %B %Y"),
        "ap_median": ap_median,
        "tg_median": tg_median,
        "national_median": national_median,
        "south_premium": ap_median - national_median,
        "msp": msp,
        "vs_msp": vs_msp,
        "ap_bench": ap_bench,
        "tg_bench": tg_bench,
        "ap_min": ap_min,
        "ap_max": ap_max,
        "ap_count": ap_count,
        "stale_from": stale_from,
        "wow": wow,
        "mom": mom,
        "prev_week": float(prev_week["ap_median"]) if prev_week else None,
        "prev_month": float(prev_month["ap_median"]) if prev_month else None,
        "scenario_status": status,
        "forecast_note": note,
        "news": {} if no_news else fetch_news(),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outdir", default=str(REPO_ROOT / "briefs"))
    parser.add_argument("--no-news", action="store_true", help="skip the news scan")
    parser.add_argument("--email", action="store_true", help="email the brief over SMTP")
    args = parser.parse_args()

    ctx = build_context(no_news=args.no_news)
    if not args.no_news:
        print("Fetched news scan.")

    md = render_markdown(ctx)
    html_doc = markdown_to_html(md)

    outdir = Path(args.outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    md_path = outdir / f"ap-maize-brief-{ctx['run_date']}.md"
    html_path = outdir / f"ap-maize-brief-{ctx['run_date']}.html"
    md_path.write_text(md)
    html_path.write_text(html_doc)
    print(f"Wrote {md_path}")
    print(f"Wrote {html_path}")

    if args.email:
        subject = (f"AP Maize Weekly Brief - {ctx['pretty_date']} - "
                   f"Rs {ctx['ap_median']:,.0f}/qtl ({arrow(ctx['wow'])} w/w)")
        send_email(subject, html_doc, md)
    return 0


if __name__ == "__main__":
    sys.exit(main())
