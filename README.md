# Crew Portal 1.6.3

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

## Version 1.6.3 additions
- Updated version references across the project.
- Corrected Airbus A320 and A321neo engine types for THAI fleet simulation.
- Removed Back button from top-level screens and Settings.
- Improved UTC toggle spacing and UTC time display.
- Replaced WebView route map with a native offline route map renderer so it works without uninstalling or relying on map tiles.
- Added automatic built-in roster refresh on app version update, so APK updates can be installed over the previous version.
