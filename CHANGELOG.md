# Crew Portal Changelog

## 2.0.6a
- Kept airport-name lines under IATA codes in Roster flight cards, but removed awkward ellipsis display.
- Airport labels now use compact clean names such as Suvarnabhumi and Sheremetyevo to avoid broken one-letter/syllable wrapping.
- Added automatic operational roster changes: deterministic 1-5 same-day flight replacements per month.
- Roster changes reset registration, aircraft assignment, gate/stand and terminal data for affected sectors.
- Roster changes create a Crew Portal notification and appear in the Notifications screen.
- Kept the 2.0.6 build fix for Compose pull-to-refresh.

# Crew Portal Change Log

## 2.0.6
- Fixed GitHub Actions build failure caused by missing `androidx.compose.material:material` dependency.
- Roster pull-to-refresh now compiles with `ExperimentalMaterialApi`, `rememberPullRefreshState`, `pullRefresh`, and `PullRefreshIndicator`.

## 2.0.5
- Fixed Roster pull-to-refresh by switching to the native Compose pull refresh indicator.
- Monthly Flight Time now changes when viewing another roster month.
- Removed the hidden 5-tap manual roster generation test.
- Added automatic next-month roster preparation 7 days before month end.
- Calendar review and 80h/90h target confirmation remain required before the next roster appears in Roster.
- App updates no longer overwrite the active roster with built-in assets.

## 2.0.4
- Generated roster review flow improvements.
- Payroll biometric access fixes.
- Roster generator validation updates.

## 1.8.2
- Hotfix: splash loader cleanup, Weather BKK default, localization fixes, sick leave demo cleanup, update check state, turnaround registration logic.
