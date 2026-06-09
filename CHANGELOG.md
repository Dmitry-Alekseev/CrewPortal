# Crew Portal Changelog

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

## 2.1.9
- Hide completed/past non-flight roster items from the active Roster list without deleting DB records.
- Registered button is green after successful registration.
- Same-duty return/continuation sectors inherit the outbound aircraft registration.
