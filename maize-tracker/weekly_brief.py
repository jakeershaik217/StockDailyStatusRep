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
import urllib.error
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

# (when_en, when_te, what_en, what_te, why_en, why_te)
TRIGGERS = [
    ("Sep 2026", "2026 సెప్టెంబర్",
     "IMD end-of-season rainfall; DES 1st Advance Estimates",
     "IMD సీజన్ ముగింపు వర్షపాత లెక్కలు; DES మొదటి ముందస్తు అంచనాలు",
     "Deficit >15% or maize output down >8% shifts the base case to the bull case",
     "వర్షపాత లోటు 15% దాటినా, మొక్కజొన్న దిగుబడి 8% పైగా తగ్గినా ధరలు పెరిగే అంచనాకు మారాలి"),
    ("1 Nov 2026", "2026 నవంబర్ 1",
     "Nagarjuna Sagar / Srisailam storage",
     "నాగార్జునసాగర్ / శ్రీశైలం నీటి నిల్వలు",
     "Below 40% cuts AP rabi maize area - worth about +Rs 200 on the Jun-Jul 2027 call",
     "40% కంటే తక్కువైతే ఏపీలో రబీ మొక్కజొన్న విస్తీర్ణం తగ్గుతుంది - 2027 జూన్-జూలై అంచనాకు సుమారు ₹200 అదనం"),
    ("Nov-Dec 2026", "2026 నవంబర్-డిసెంబర్",
     "Kharif arrival prices at Nizamabad / Kurnool",
     "నిజామాబాద్ / కర్నూలు మార్కెట్లలో ఖరీఫ్ పంట ఆగమన ధరలు",
     "Harvest prices holding above Rs 2,400 lift the whole 2027 curve by Rs 150-250",
     "పంట కాలంలోనే ధర ₹2,400 పైన నిలిస్తే 2027 ధరల అంచనా మొత్తం ₹150-250 పెరుగుతుంది"),
    ("Dec 2026-Feb 2027", "2026 డిసెంబర్-2027 ఫిబ్రవరి",
     "Any DGFT notification on duty-free maize / corn TRQ for ethanol",
     "ఇథనాల్ కోసం సుంకం లేని మొక్కజొన్న దిగుమతులు / TRQ పై DGFT ప్రకటన",
     "The single biggest bear trigger - caps the market immediately",
     "ధరలు తగ్గించే అతిపెద్ద కారణం - మార్కెట్‌కు వెంటనే పరిమితి విధిస్తుంది"),
    ("Jan 2027", "2027 జనవరి",
     "OMC ethanol procurement price revision for maize ethanol",
     "మొక్కజొన్న ఇథనాల్‌కు OMC సేకరణ ధర సవరణ",
     "A higher ethanol price raises the maize floor roughly one-for-one",
     "ఇథనాల్ ధర పెరిగితే మొక్కజొన్న కనీస ధర కూడా దాదాపు అంతే స్థాయిలో పెరుగుతుంది"),
    ("Mar 2027", "2027 మార్చి",
     "DES 2nd Advance Estimates, rabi maize",
     "DES రెండో ముందస్తు అంచనాలు, రబీ మొక్కజొన్న",
     "Confirms or kills the tight-stocks story",
     "నిల్వలు తక్కువగా ఉన్నాయా లేదా అనేది స్పష్టమవుతుంది"),
    ("May 2027", "2027 మే",
     "MSP announcement for KMS 2027-28",
     "KMS 2027-28 కనీస మద్దతు ధర (MSP) ప్రకటన",
     "Expect Rs 2,500-2,570; sets the psychological floor",
     "₹2,500-2,570 ఉండవచ్చు; మార్కెట్‌కు మానసిక కనీస స్థాయిని నిర్ణయిస్తుంది"),
]

# (key, name_en, name_te, probability, low, high, centre)
SCENARIOS = [
    ("base", "Base - mild deficit, ethanol demand grinds prices up",
     "సాధారణ అంచనా - కొద్దిపాటి కొరత, ఇథనాల్ డిమాండ్‌తో ధరలు నెమ్మదిగా పైకి",
     55, 2550, 2850, 2700),
    ("bull", "Bull - El Nino damage carries into rabi",
     "ధరలు పెరిగే అంచనా - ఎల్ నినో నష్టం రబీ వరకు కొనసాగితే",
     25, 3000, 3400, 3150),
    ("bear", "Bear - monsoon recovers, big rabi, liberal imports",
     "ధరలు తగ్గే అంచనా - రుతుపవనాలు కోలుకుని, రబీ దిగుబడి పెరిగి, దిగుమతులు సడలిస్తే",
     20, 2200, 2450, 2350),
]

# Market, district and variety names arrive from the API in English. Transliterate
# the ones that actually show up in the southern maize feed; anything unmapped is
# left as it came, which is better than a wrong guess at a place name.
TRANSLIT_TE = {
    # AP markets
    "Kurnool": "కర్నూలు", "Panyam": "పాణ్యం", "Nandyal": "నంద్యాల",
    "Bapatla": "బాపట్ల", "Parchur": "పర్చూరు", "Ipur": "ఇపూరు",
    "Mylavaram": "మైలవరం", "Rajanagaram": "రాజానగరం", "Bhimunipatnam": "భీమునిపట్నం",
    "Ponduru": "పొందూరు", "Simhadhripuram": "సింహాద్రిపురం", "Guntur": "గుంటూరు",
    "Chittoor": "చిత్తూరు", "Tadepalligudem": "తాడేపల్లిగూడెం", "Palakonda": "పాలకొండ",
    "Vinukonda": "వినుకొండ", "Duggirala": "దుగ్గిరాల", "Podili": "పొదిలి",
    # Telangana markets
    "Nizamabad": "నిజామాబాద్", "Jagtial": "జగిత్యాల", "Karimnagar": "కరీంనగర్",
    "Warangal": "వరంగల్", "Siddipet": "సిద్దిపేట", "Bejjenki": "బెజ్జంకి",
    # Districts
    "Visakhapatnam": "విశాఖపట్నం", "NTR": "ఎన్టీఆర్", "YSR": "వైఎస్సార్",
    "Kadapa": "కడప", "Krishna": "కృష్ణా", "Prakasam": "ప్రకాశం",
    "Srikakulam": "శ్రీకాకుళం", "Anantapur": "అనంతపురం", "Vizianagaram": "విజయనగరం",
    "Palnadu": "పల్నాడు", "Eluru": "ఏలూరు", "Kakinada": "కాకినాడ",
    "Konaseema": "కోనసీమ", "Annamayya": "అన్నమయ్య", "Tirupati": "తిరుపతి",
    "Nellore": "నెల్లూరు", "Anakapalli": "అనకాపల్లి", "Manyam": "మన్యం",
    "Parvathipuram": "పార్వతీపురం", "Godavari": "గోదావరి", "East": "తూర్పు",
    "West": "పశ్చిమ", "Sri": "శ్రీ", "Sathya": "సత్యసాయి", "Sai": "",
    # Varieties
    "Hybrid": "హైబ్రిడ్", "Local": "లోకల్", "Yellow": "పసుపు", "White": "తెలుపు",
    "Red": "ఎరుపు", "Deshi": "దేశీ", "Cattle": "పశువుల", "Feed": "దాణా",
    "Medium": "మధ్యస్థం", "Other": "ఇతర", "Maize": "మొక్కజొన్న",
    "APMC": "మార్కెట్",
}
_TRANSLIT_RE = re.compile(r"\b(" + "|".join(sorted(TRANSLIT_TE, key=len, reverse=True)) + r")\b")


def te_name(text: str) -> str:
    """Transliterate the English tokens in a market, district or variety name."""
    return " ".join(_TRANSLIT_RE.sub(lambda m: TRANSLIT_TE[m.group(1)], text).split())


MONTHS_TE = ["జనవరి", "ఫిబ్రవరి", "మార్చి", "ఏప్రిల్", "మే", "జూన్",
             "జూలై", "ఆగస్టు", "సెప్టెంబర్", "అక్టోబర్", "నవంబర్", "డిసెంబర్"]

# Telugu labels for the news categories defined in NEWS_QUERIES.
NEWS_LABELS_TE = {
    "Monsoon / El Nino": "రుతుపవనాలు / ఎల్ నినో",
    "Sowing & acreage": "విత్తనాలు & సాగు విస్తీర్ణం",
    "Reservoirs / AP water": "జలాశయాలు / ఏపీ నీటి పరిస్థితి",
    "Imports & duty (bear trigger)": "దిగుమతులు & సుంకం (ధర తగ్గే ముప్పు)",
    "Ethanol policy & pricing": "ఇథనాల్ విధానం & ధరలు",
    "MSP & procurement": "కనీస మద్దతు ధర & సేకరణ",
    "Feed & poultry demand": "దాణా & కోళ్ల పరిశ్రమ డిమాండ్",
    "AP maize market": "ఏపీ మొక్కజొన్న మార్కెట్",
}


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
        except urllib.error.HTTPError as exc:
            last = exc
            # data.gov.in throttles hard; back off well past its window.
            time.sleep((30 if exc.code == 429 else 3) * (attempt + 1))
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

def arrow(change, lang="en"):
    if change is None:
        return "n/a" if lang == "en" else "అందుబాటులో లేదు"
    sign = "+" if change >= 0 else ""
    if lang == "en":
        marker = "UP" if change > 0.5 else ("DOWN" if change < -0.5 else "FLAT")
    else:
        marker = "పెరుగుదల" if change > 0.5 else ("తగ్గుదల" if change < -0.5 else "స్థిరం")
    return f"{sign}{change:.1f}% {marker}"


def _msp_line(ctx, lang):
    gap, state = ctx["msp_gap"], ctx["msp_state"]
    if lang == "en":
        if state == "above":
            return f"Rs {gap:,.0f} above MSP - the ethanol bid is doing the work"
        if state == "parity":
            return (f"Rs {abs(gap):,.0f} {'above' if gap >= 0 else 'below'} MSP - "
                    f"trading at parity")
        return (f"Rs {abs(gap):,.0f} below MSP - distress territory, watch for "
                f"procurement demands from AP and Telangana")
    if state == "above":
        return f"కనీస మద్దతు ధర కంటే ₹{gap:,.0f} ఎక్కువ - ఇథనాల్ కొనుగోళ్లే ధరను నిలబెడుతున్నాయి"
    if state == "parity":
        return (f"కనీస మద్దతు ధర కంటే ₹{abs(gap):,.0f} {'ఎక్కువ' if gap >= 0 else 'తక్కువ'} - "
                f"దాదాపు సమాన స్థాయిలో")
    return (f"కనీస మద్దతు ధర కంటే ₹{abs(gap):,.0f} తక్కువ - రైతులకు నష్టదాయక స్థాయి, ఏపీ మరియు "
            f"తెలంగాణ నుంచి సేకరణ డిమాండ్లు రావచ్చు")


def _forecast_note(ctx, lang):
    state = ctx["note_state"]
    if lang == "en":
        return {
            "bull": "Prices are already inside the bull range - consider raising the base case.",
            "bear": "Prices are soft; if this persists into the kharif harvest the bear case "
                    "gains weight.",
            "base": "Today's print is consistent with the base case.",
        }[state]
    return {
        "bull": "ధరలు ఇప్పటికే ఎక్కువ అంచనా పరిధిలో ఉన్నాయి - సాధారణ అంచనాను పెంచడం పరిశీలించాలి.",
        "bear": "ధరలు బలహీనంగా ఉన్నాయి; ఖరీఫ్ కోత వరకు ఇలాగే కొనసాగితే తక్కువ ధరల అంచనాకు బలం చేకూరుతుంది.",
        "base": "నేటి ధర సాధారణ అంచనాకు అనుగుణంగానే ఉంది.",
    }[state]


STATUS_TE = {"on track": "అనుకున్నట్టే", "off track": "పక్కదారి",
             "live": "అమల్లోకి వచ్చింది", "not yet": "ఇంకా కాదు"}


def render_markdown(ctx: dict, lang: str = "en") -> str:
    """Render the brief in English (lang="en") or Telugu (lang="te")."""
    te = lang == "te"
    L = []
    a = L.append
    rs = "₹" if te else "Rs "

    if te:
        a(f"# ఏపీ మొక్కజొన్న వారపు నివేదిక - {ctx['pretty_date_te']}")
        a("")
        a(f"**ముఖ్యాంశం: ఏపీ సగటు (మధ్యస్థ) మార్కెట్ ధర ₹{ctx['ap_median']:,.0f}/క్వింటాల్** "
          f"(వారంతో పోలిస్తే {arrow(ctx['wow'], 'te')}, నాలుగు వారాలతో పోలిస్తే "
          f"{arrow(ctx['mom'], 'te')}). KMS {CURRENT_MSP_SEASON} కనీస మద్దతు ధర "
          f"₹{ctx['msp']:,}.")
    else:
        a(f"# AP Maize Weekly Brief - {ctx['pretty_date']}")
        a("")
        a(f"**Headline: AP median modal price Rs {ctx['ap_median']:,.0f}/qtl** "
          f"({arrow(ctx['wow'])} w/w, {arrow(ctx['mom'])} vs 4 weeks ago) "
          f"against an MSP of Rs {ctx['msp']:,} for KMS {CURRENT_MSP_SEASON}.")
    a("")

    if ctx.get("stale_from"):
        if te:
            a(f"> **గమనిక:** ఈ నివేదిక తయారుచేసే సమయానికి అగ్రిమార్క్‌నెట్‌లో ఆంధ్రప్రదేశ్ "
              f"ధరలు చాలా తక్కువ మార్కెట్ల నుంచే వచ్చాయి (మార్కెట్ సెలవు, లేదా ఆ రోజు "
              f"సమాచారం ఇంకా నమోదు కాలేదు). అందుకే ప్రధాన సగటు ధరలు {ctx['stale_from']} "
              f"నాటివి. కింది పట్టికలో ఈ రోజు ఇప్పటివరకు నమోదైన ధరలే ఉన్నాయి.")
        else:
            a(f"> **Note:** the Agmarknet feed had only a handful of Andhra Pradesh quotes "
              f"at run time (mandi holiday, or the feed had not filled for the day), so the "
              f"headline medians carry over the {ctx['stale_from']} reading. The market "
              f"table below shows whatever has reported so far today.")
        a("")

    if te:
        a(f"కనీస మద్దతు ధరతో పోలిక: **{_msp_line(ctx, 'te')}**. "
          f"తెలంగాణ సగటు ₹{ctx['tg_median']:,.0f} (దక్షిణాదిలో ధరను నిర్ణయించేది ఇదే). "
          f"జాతీయ సగటు ₹{ctx['national_median']:,.0f} - అంటే దక్షిణాదిలో "
          f"₹{ctx['south_premium']:,.0f} అధిక ధర.")
    else:
        a(f"Position vs MSP: **{_msp_line(ctx, 'en')}**. "
          f"Telangana median Rs {ctx['tg_median']:,.0f} (the southern price setter). "
          f"National median Rs {ctx['national_median']:,.0f}, so the South carries a "
          f"Rs {ctx['south_premium']:,.0f} premium.")
    a("")

    a("## 1. " + ("ఆంధ్రప్రదేశ్ ప్రధాన మార్కెట్లు" if te else "Andhra Pradesh benchmark markets"))
    a("")
    if ctx["ap_bench"]:
        a("| మార్కెట్ | జిల్లా | రకం | ధర ₹/క్వింటాల్ |" if te
          else "| Market | District | Variety | Modal Rs/qtl |")
        a("|---|---|---|---|")
        name = te_name if te else (lambda s: s)
        for r in ctx["ap_bench"][:12]:
            a(f"| {name(r['market'])} | {name(r['district'])} | {name(r['variety'])} "
              f"| {r['modal']:,.0f} |")
    else:
        a("ఈ రోజు ఇప్పటివరకు ఏ ఏపీ మార్కెట్ ధరలను నమోదు చేయలేదు." if te
          else "No AP market had reported a quote at run time.")
    a("")
    if ctx["tg_bench"]:
        label = "**తెలంగాణ మార్కెట్లు:** " if te else "**Telangana reference:** "
        a(label + ", ".join(f"{te_name(r['market']) if te else r['market']} "
                            f"{rs}{r['modal']:,.0f}" for r in ctx["tg_bench"][:5]))
        a("")

    a("## 2. " + ("ఈ వారం మార్పు" if te else "What moved"))
    a("")
    if te:
        a("- గత వారంతో పోలిస్తే: **" + arrow(ctx["wow"], "te") + "**"
          + (f" (అప్పుడు ₹{ctx['prev_week']:,.0f})" if ctx["prev_week"]
             else " (గత వారం సమాచారం ఇంకా లేదు)"))
        a("- నాలుగు వారాలతో పోలిస్తే: **" + arrow(ctx["mom"], "te") + "**"
          + (f" (అప్పుడు ₹{ctx['prev_month']:,.0f})" if ctx["prev_month"]
             else " (సమాచారం ఇంకా సేకరిస్తున్నాం)"))
        a(f"- ఏపీ మార్కెట్లలో ధరల వ్యత్యాసం: ₹{ctx['ap_min']:,.0f} నుంచి "
          f"₹{ctx['ap_max']:,.0f} వరకు, మొత్తం {ctx['ap_count']} మార్కెట్లలో")
    else:
        a(f"- Week on week: **{arrow(ctx['wow'])}**"
          + (f" (from Rs {ctx['prev_week']:,.0f})" if ctx["prev_week"]
             else " (no prior week on file yet)"))
        a(f"- Four weeks: **{arrow(ctx['mom'])}**"
          + (f" (from Rs {ctx['prev_month']:,.0f})" if ctx["prev_month"]
             else " (history still building)"))
        a(f"- Spread across AP markets: Rs {ctx['ap_min']:,.0f} to Rs {ctx['ap_max']:,.0f} "
          f"across {ctx['ap_count']} reporting mandis")
    a("")

    a("## 3. " + ("2027 జూన్-జూలై అంచనాల పట్టిక" if te
                 else "Scenario tracker for June-July 2027"))
    a("")
    a("| అంచనా | సంభావ్యత | పరిధి ₹/క్వి. | సగటు | ప్రస్తుత స్థితి |" if te
      else "| Scenario | Prob. | Range Rs/qtl | Centre | On track? |")
    a("|---|---|---|---|---|")
    for key, name_en, name_te, prob, lo, hi, mid in SCENARIOS:
        status = ctx["scenario_status"].get(key, "monitoring")
        if te:
            status = STATUS_TE.get(status, status)
        a(f"| {name_te if te else name_en} | {prob}% | {lo:,} - {hi:,} | {mid:,} | {status} |")
    a("")
    if te:
        a(f"**ప్రస్తుత అంచనాలో మార్పు లేదు: 2027 జూన్‌లో ₹2,700/క్వింటాల్, జూలైలో ₹2,750 "
          f"(70% విశ్వాస పరిధి ₹2,450-3,000).** {_forecast_note(ctx, 'te')}")
    else:
        a(f"**Working forecast unchanged: Rs 2,700/qtl in June 2027, Rs 2,750 in July 2027 "
          f"(70% band Rs 2,450-3,000).** {_forecast_note(ctx, 'en')}")
    a("")

    a("## 4. " + ("గమనించవలసిన ముఖ్య తేదీలు" if te else "Trigger calendar"))
    a("")
    a("| ఎప్పుడు | ఏమిటి | ఎందుకు ముఖ్యం |" if te else "| When | What | Why it matters |")
    a("|---|---|---|")
    for when_en, when_te, what_en, what_te, why_en, why_te in TRIGGERS:
        a(f"| {when_te if te else when_en} | {what_te if te else what_en} "
          f"| {why_te if te else why_en} |")
    a("")

    if ctx["news"]:
        a("## 5. " + ("వార్తల సమీక్ష (గత 8 రోజులు)" if te
                     else "News scan (last 8 days)"))
        a("")
        if te:
            a("*శీర్షికలు ప్రచురణకర్తలు ఇచ్చిన ఆంగ్ల రూపంలోనే ఉంచాము.*")
            a("")
        for label, items in ctx["news"].items():
            if not items:
                continue
            a(f"**{NEWS_LABELS_TE.get(label, label) if te else label}**")
            for item in items:
                a(f"- {item['title']} ({item['date']})")
            a("")

    a("---")
    a("")
    if te:
        a("*సమాచారం: data.gov.in ద్వారా అగ్రిమార్క్‌నెట్ రోజువారీ మార్కెట్ ధరలు. "
          "₹1,200-4,000 పరిధి దాటిన ధరలను సగటు లెక్కల నుంచి తొలగించాము. "
          "ఇది స్వయంచాలకంగా తయారైన నివేదిక; అంచనాలు ఒక అభిప్రాయం మాత్రమే, ఖచ్చితమైన నిజం కాదు.*")
    else:
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
    # Noto Sans Telugu first so the Telugu half renders with proper conjuncts.
    return ("<html><head><meta charset=\"utf-8\"></head>"
            "<body style=\"font-family:'Noto Sans Telugu','Gautami',Segoe UI,"
            "Helvetica,Arial,sans-serif;font-size:14px;color:#222;max-width:900px\">"
            + body + "</body></html>")


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
    msp_state = "above" if gap > 150 else ("parity" if gap >= -50 else "below")

    # Which scenario does today's print support?
    status = {
        "base": "on track" if 2350 <= ap_median <= 2700 else "off track",
        "bull": "live" if ap_median > 2700 else "not yet",
        "bear": "live" if ap_median < 2350 else "not yet",
    }
    note_state = "bull" if ap_median > 2700 else ("bear" if ap_median < 2300 else "base")

    day = dt.date.fromisoformat(run_date)
    return {
        "run_date": run_date,
        "pretty_date": day.strftime("%d %B %Y"),
        "pretty_date_te": f"{day.year} {MONTHS_TE[day.month - 1]} {day.day}",
        "ap_median": ap_median,
        "tg_median": tg_median,
        "national_median": national_median,
        "south_premium": ap_median - national_median,
        "msp": msp,
        "msp_gap": gap,
        "msp_state": msp_state,
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
        "note_state": note_state,
        "news": {} if no_news else fetch_news(),
    }


def subject_for(ctx: dict, lang: str) -> str:
    if lang == "te":
        return (f"ఏపీ మొక్కజొన్న వారపు నివేదిక - {ctx['pretty_date_te']} - "
                f"₹{ctx['ap_median']:,.0f}/క్వింటాల్ ({arrow(ctx['wow'], 'te')})")
    return (f"AP Maize Weekly Brief - {ctx['pretty_date']} - "
            f"Rs {ctx['ap_median']:,.0f}/qtl ({arrow(ctx['wow'])} w/w)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outdir", default=str(REPO_ROOT / "briefs"))
    parser.add_argument("--no-news", action="store_true", help="skip the news scan")
    parser.add_argument("--email", action="store_true", help="email the brief over SMTP")
    parser.add_argument("--lang", default="en,te",
                        help="comma-separated languages to render: en, te, or en,te")
    args = parser.parse_args()

    ctx = build_context(no_news=args.no_news)
    if not args.no_news:
        print("Fetched news scan.")

    outdir = Path(args.outdir)
    outdir.mkdir(parents=True, exist_ok=True)

    # One self-contained document per language, and one email each - a single
    # bilingual message buries whichever language you actually read.
    for lang in [l.strip() for l in args.lang.split(",") if l.strip()]:
        md = render_markdown(ctx, lang)
        html_doc = markdown_to_html(md)
        stem = f"ap-maize-brief-{ctx['run_date']}-{lang}"
        (outdir / f"{stem}.md").write_text(md, encoding="utf-8")
        (outdir / f"{stem}.html").write_text(html_doc, encoding="utf-8")
        print(f"Wrote {outdir / stem}.md and .html")

        if args.email:
            send_email(subject_for(ctx, lang), html_doc, md)
    return 0


if __name__ == "__main__":
    sys.exit(main())
