# AP Maize Weekly Tracker

Weekly monitoring for the Andhra Pradesh maize market, built to keep the
June–July 2027 price forecast honest as the facts change.

## What it does

`weekly_brief.py` pulls the day's mandi prices for maize from the Government of
India open data platform (the Agmarknet daily price feed), splits them by state,
and produces a brief covering:

1. **Andhra Pradesh benchmark markets** — Kurnool, Panyam, Bapatla, Parchur,
   Ipur, Mylavaram, Rajanagaram, Bhimunipatnam, Ponduru, with a Telangana
   reference block (Nizamabad and Karimnagar lead southern price direction).
2. **What moved** — week-on-week and four-week change in the AP median, computed
   from `history/maize_prices.csv`, which the script appends to on every run.
3. **Scenario tracker** — whether the current price supports the base, bull or
   bear case for June–July 2027.
4. **Trigger calendar** — the dated events that would change the forecast.
5. **News scan** — the last eight days of headlines across monsoon/El Niño,
   sowing and acreage, reservoirs, import duty and TRQ, ethanol policy, MSP and
   procurement, feed demand, and the AP market itself.

Output is written to `briefs/` as both Markdown and HTML, and can be emailed.

## Languages

The brief is bilingual by default: the English version, then the same analysis in
Telugu below it. News headlines stay in the publisher's original English rather
than being machine-translated.

## Running it

```bash
python3 weekly_brief.py                 # bilingual brief in briefs/
python3 weekly_brief.py --lang te       # Telugu only
python3 weekly_brief.py --no-news       # prices only, much faster
python3 weekly_brief.py --email         # also email it
```

No third-party dependencies — standard library only, Python 3.8+.

Run it **after about 15:00 IST**. The Agmarknet feed fills through the Indian
working day; earlier than that only a handful of mandis have reported. If fewer
than eight Andhra Pradesh quotes are available the script carries forward the
last complete reading, flags this at the top of the brief, and leaves the
history file untouched rather than recording a bad median.

## Email configuration

Credentials are read from the environment; nothing sensitive is stored here.

| Variable | Purpose |
|---|---|
| `MAIZE_SMTP_HOST` | SMTP host (default `smtp.gmail.com`) |
| `MAIZE_SMTP_PORT` | SMTP port (default `587`; `465` switches to SSL) |
| `MAIZE_SMTP_USER` | Sending account |
| `MAIZE_SMTP_PASSWORD` | App password for that account |
| `MAIZE_EMAIL_TO` | Comma-separated recipients |
| `MAIZE_EMAIL_FROM` | Optional; defaults to `MAIZE_SMTP_USER` |
| `DATA_GOV_IN_API_KEY` | Optional; overrides the public data.gov.in sample key |

For Gmail, use an App Password (Google Account → Security → 2-Step Verification
→ App passwords), not the account password.

## Data notes

- Quotes outside ₹1,200–4,000 per quintal are excluded from medians. Thin-arrival
  mandis occasionally print absurd values — one AP market quoted ₹4,800 on
  27 July 2026 on a single lot.
- The upstream API caps every response at ten records regardless of the `limit`
  parameter and applies state filters inconsistently, so the script pages the
  commodity-only filter and splits by state locally.
- MSP for maize, KMS 2026-27, is ₹2,410 per quintal. Update `MSP_BY_SEASON` and
  `CURRENT_MSP_SEASON` when the next announcement lands (expected May 2027).
- The public data.gov.in sample key is rate-limited and returns HTTP 429 under
  repeated runs; the script backs off 30 seconds per retry. Set
  `DATA_GOV_IN_API_KEY` to your own key to avoid this.

## Adding Telugu text

The Telugu strings live alongside their English counterparts rather than in a
separate catalogue: `TRIGGERS` rows are
`(when_en, when_te, what_en, what_te, why_en, why_te)`, `SCENARIOS` rows are
`(key, name_en, name_te, prob, low, high, centre)`, and `NEWS_LABELS_TE` maps the
category names in `NEWS_QUERIES`. Sentences that depend on the day's numbers are
built in `_msp_line()` and `_forecast_note()`, which switch on a state key
(`msp_state`, `note_state`) set in `_assemble()` — so the analysis logic is
decided once and only the wording differs by language.
