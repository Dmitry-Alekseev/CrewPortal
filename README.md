# Crew Portal 1.6.7

Android/Kotlin Jetpack Compose crew portal demo.

## Login
- Corporate ID: CPD9842
- Password: Airbus1998

## New in 1.2
- Mandatory login/biometric screen after full app close.
- Roster calendar view.
- Duty day / FDP calculations.
- Rest status block.
- Flight briefing page with distance, alternate, cruise level and ETOPS status.
- Virtual crew database: Thai cabin crew plus Thai/foreign flight crew.
- Fleet database for THAI Airbus aircraft.
- Smarter aircraft assignment 24h before departure and paired return-sector tail matching.
- Fleet screen.
- Notifications center.
- Documents & Qualifications dashboard.
- Layover / hotel reserve cards.

## Build
Use GitHub Actions workflow `Build Debug APK`, or run locally:

```bash
gradle assembleDebug
```

APK path:

```text
app/build/outputs/apk/debug/*.apk
```


## Version 1.3 additions
- Flight status timeline
- Route map schematic
- Pre-flight checklist
- Airport database with ICAO and UTC offsets
- Local/UTC schedule toggle
- Monthly flight-time progress
- Estimated fuel briefing
- NOTAM summary placeholder
- Company messages
- Export logbook CSV
- System light/dark appearance
- Disruption simulation action


## Version 1.4 additions
- Airport assignment module
- Gate / Stand appears about 3 hours before departure
- Terminal assignment by departure airport
- Gate/stand notifications and notifications center entries
- Status timeline now includes Gate / Stand Assigned


## Version 1.6 additions
- Removed Logbook from the bottom navigation.
- Bottom navigation now uses Roster / Calendar / Weather / Fleet / More.
- More screen contains Alerts, Profile and Settings.

## Version 1.6.1 additions
- Added contextual back navigation only for nested screens.

## Version 1.6.2 additions
- Fleet cards now show engine type instead of aircraft rotation / assigned sectors.

## Version 1.6.5 additions
- Replaced Google Maps with OpenStreetMap via osmdroid.
- No Google API key or Google Cloud setup is required.
- Route map now shows live map tiles, departure/arrival markers and a route line.
- Kept 1.6.4 UTC layout improvements: local time stays primary, UTC is shown inline next to it.
- Added automatic built-in roster refresh on app version update, so APK updates can be installed over the previous version.

## Version 1.6.7 additions
- Added THAI logo badge to flight cards.
- OFF duty blocks are display-only and no longer open a detail screen.
- Route map labels A departure / B arrival and keeps +/− zoom buttons hidden.
- Improved dark theme contrast across roster, calendar, fleet, profile and weather screens.
- METAR / TAF cards use theme-aware colors.
- Added Refresh controls for roster sync and weather refresh.
- Expanded pilot license records with IR(A), MEP(L), MCC, UPRT, ICAO English and Radio Telephony.
- Updated all app metadata and built-in roster version to 1.6.7.
