# Crew Portal 3.0 — implementation report

## Scope

Crew Portal 3.0 refreshes the application shell without removing existing modules, replaces the single-sector delivery shortcut with a complete Aircraft Delivery plan, and adds a permanently one-time early next-month QA generator.

## UI and icon

- `ui/theme/Color.kt` and `Theme.kt` define the 3.0 navy/sky/graphite/gold palette, rounded Material 3 shapes and stronger typography hierarchy.
- `MainNavigation.kt` uses a surfaced navigation bar, tonal selection indicator and consistent app-bar colors.
- `ScheduleScreen.kt` has a modern roster header while retaining pull-to-refresh, current/next-month switching and the hidden Operational Roster Change gesture.
- `LoginScreen.kt` uses the shared background and new `drawable-nodpi/app_icon_v3.png` launcher artwork.
- `AndroidManifest.xml` points both standard and round icons at the new asset.

## Aircraft Delivery

Primary source: `data/delivery/AircraftDeliveryPlanner.kt`.

The form accepts only delivery date, delivery flight number, one supported new-aircraft type and the suffix following the fixed `HS-` prefix. The planner then creates:

1. passenger positioning BKK-DXB on EK375;
2. passenger positioning DXB-HAM on EK61;
3. no separate EDDH-EDHI row;
4. an operating delivery leg from XFW/EDHI to GYD, DWC, DOH or MCT;
5. a midpoint rest or technical-stop row;
6. a final delivery leg to BKK.

The plan is deterministic for the same request. A320neo/A321neo deliveries use two pilots and receive 8-12 hours of midpoint rest. A330/A350 deliveries use four pilots, receive a 2-4 hour fuel stop and alternate the user's `OPERATING PILOT` versus `PASSENGER / IN-FLIGHT REST` role between legs. Only operating legs have flight-time credit.

`FlightRepository.addAircraftDeliveryPlan` applies the rows atomically and protects linked qualification groups. `refreshCompletedFlights` activates the new registration in persistent Fleet only when the final delivery row has arrived at BKK. Intermediate arrival cannot add the aircraft early.

Supported distinct delivery types are A330-800neo, A330-900neo, A350-900, A320neo and A321neo. Adding a delivered registration still uses the existing Room `fleet_aircraft` upsert and therefore requires no schema change.

## One-time next-month generation

Five taps on `Crew Portal <version>` in Settings open a confirmation dialog. `FlightRepository.generateNextMonthRosterOnce` bypasses only the normal day-27 date gate and calls the existing `RosterGenerator` for the next month. It refuses to overwrite an existing target month. The DataStore flag is written only after successful persistence and is not reset by an app update, process restart or draft deletion.

Installed code cannot physically delete itself from an APK. The persisted flag makes the code path unreachable after success; the source handler can be removed in a later release.

## Persistence and migration

No Room schema migration is required for 3.0. Existing `FlightEntity` fields store delivery source, role credit and activation state, while new duty values remain strings in the established format. Version 3.0 continues using schema 5 and the existing explicit 3→4 and 4→5 migrations.

## Version and build

- Android `versionName`: `3.0`
- Android `versionCode`: `3000`
- GitHub artifact: `CrewPortal-3.0.apk`
- Gradle wrapper: 8.7
- Android Gradle Plugin: 8.5.2
- Kotlin: 1.9.24
- Signing: existing Crew Portal debug certificate; no GitHub keystore secret is required.

## Known limitation

The BKK-DXB-HAM passenger path is an offline timetable reference based on the public Emirates route listing. The roster explicitly says to verify the live booking because commercial schedules can change. A future live supplier may replace the catalog behind the planner, but roster creation must retain the offline fallback.
