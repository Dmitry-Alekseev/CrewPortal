# Crew Portal Changelog

## 2.2.0
- Refreshed Airbus fleet database only: A320, A321neo, A330-300 and A350-900.
- Moved HS-TXH to A320, moved HS-TXI to A350, added TO-series A321neo aircraft and excluded parked HS-TEW from the active pool.
- Tightened aircraft assignment so a registration cannot be assigned to the wrong aircraft family.
- Added route aircraft weighting: Phuket/domestic narrow-body only, KUL/SIN mixed narrow/wide, India any Airbus, long-haul A330/A350.
- Added more turnaround time-of-day variety and kept minimum 12h rest between separate duties.
- Cleaned Messages UI buttons, forced English date formatting, added read multi-select and persistent deletion state.
- Hidden 5-tap next-month test flow can now generate the next-month draft, then clear it with another 5 taps without touching the active roster.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.2.0.apk.

## 2.1.10
- Added reserve/standby recognition in Today’s Duty, including upcoming and active reserve windows.
- Added variable reserve generation: day reserve and night reserve windows, including cross-midnight standby.
- Reworked Flight Details timeline order and removed the redundant Roster Published step.
- Restyled Open MEL to match the corporate blue/graphite UI and removed the Pre-flight Checklist section.
- Improved Weather invalid ICAO handling with short snackbar messages instead of raw API/Retrofit errors.
- Moved Fleet from bottom navigation to More and added Messages as the main bottom navigation item.
- Added a first Messages center for important company items: roster changes, payslip and validity reminders.
- Replaced Monthly Flight Time “Limit 90h” with selected workload target display.
- Added GitHub Actions release workflow to build and publish APK releases automatically.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.1.10.apk.
