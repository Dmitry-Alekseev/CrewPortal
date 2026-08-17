# Crew Portal 2.2.10 — implementation report

## Outcome

Version 2.2.10 replaces the gray route diagram with a real public vector basemap, synchronizes six-month qualification validity across Profile and Documents & Qualifications, and makes Medical/Simulator/SEP/Line Check first-class inputs to month generation. The already-published current month remains frozen during an app/ruleset update.

## Changed files

- Release/project: `app/build.gradle.kts`, `.github/workflows/android-build.yml`, `.github/workflows/release-apk.yml`, `update/app_update.json`, `README.md`, `CHANGELOG.md`.
- Database/state: `FlightEntity.kt`, `AppDatabase.kt`, `DatabaseMigrations.kt`, `PilotQualificationSchedule.kt`, `RosterConflictValidator.kt`.
- Generator/repositories: `RosterGenerator.kt`, `NextRosterWorker.kt`, `FlightRepository.kt`, `RouteCatalog.kt`, `RosterMetrics.kt`, `PayrollCalculator.kt`.
- Airport/timezone/map: `AirportDatabase.kt`, `AirportGeoDirectory.kt`, `AirportTime.kt`, `TimeUtils.kt`, `RosterNotificationScheduler.kt`, `FlightDetailsScreen.kt`.
- UI consumers: `ProfileScreen.kt`, `NotificationsScreen.kt`, `ScheduleScreen.kt`, `CalendarScreen.kt`, `CompanyRoutesScreen.kt`, `CrewPool.kt`.
- Test sources (added/updated, not executed): `QualificationRosterTest.kt`, `AirportTimeTest.kt`.
- Documentation: `CREW_PORTAL_DEVELOPER_GUIDE.md`, this report.

## Qualifications and month generation

`PilotQualificationSchedule` is now the common source of truth for validity and recurring duties:

- Medical completed 11 August 2026; next due 11 February 2027. Generator event: two linked days beginning 10 August/February.
- Simulator completed 18 July 2026; next due 18 January 2027. Generator event: three linked days beginning 16 July/January.
- Line Check completed 6 August 2026; next due 6 February 2027. It is assigned to a real HKG operating pairing with the user as Line Pilot Instructor/observer.
- SEP Land/Water retain their records and recur on the common six-month schedule.

Generation priority is Leave, linked Medical/Simulator/SEP groups, Line Check, ordinary pairings, Reserve, then OFF. Multi-day events use a stable `eventGroupId`, day index and total; a whole free window is selected so the middle day cannot be independently replaced. Boundary-month pieces keep the same group metadata. A final `RosterConflictValidator` checks duplicate IDs/days, continuity, Leave overlap, Flight/Reserve inside training, minimum rest and inappropriate block credit before the list is returned for persistence.

The next-month worker runs daily but `prepareNextMonthRosterIfDue` opens only on the 27th. It first restores persisted personal Leave into the shared leave source. Existence checks keep generation idempotent. App version/ruleset changes still do not regenerate a non-empty published current month.

## Migration

Room schema is 5. `MIGRATION_4_5` adds backward-compatible columns to `flights`:

- `rosterSource` (`AUTO_GENERATED`, `OPERATIONAL_CHANGE`, `COMPANY_EXTRA_DUTY`, `DELIVERY`);
- `eventGroupId`, `eventDayIndex`, `eventTotalDays`;
- `lineCheckRole`;
- `flightTimeCreditEligible`.

Defaults preserve old rows. Existing delivery and manual-change rows are backfilled by SQL. No destructive migration or persistence-format reset was introduced.

## Route and timezone logic

`RouteCatalog` stores separate outbound/inbound min/max ranges. A concrete leg receives a deterministic five-minute value within its directional range; that value is written to `FlightEntity.durationMinutes` and is not rerolled when a screen opens. MNL is 3:15–3:30 outbound; DPS remains approximately 4:20/4:25; LED and LHR use long-haul ranges instead of the former 2h30 fallback. Unknown known-airport routes use a distance-based estimate and are manual-only unless explicitly enabled for auto generation.

Each `AirportInfo` now exposes a region `zoneId`. `arrivalLocalDateTime` converts departure local time to an `Instant`, adds persisted block minutes and converts the result to the arrival airport zone. London/European/Australian DST is therefore date-sensitive. Registration windows, completion state, UTC display and alarm scheduling also use airport ZoneId/Instant rather than the device timezone or a fixed offset.

## Route Map

`FlightDetailsScreen.PublicRouteMap` hosts a native MapLibre `MapView`. It loads OpenFreeMap Liberty (`https://tiles.openfreemap.org/styles/liberty`), draws the two airport markers and corporate-blue route overlay, and frames both endpoints. No API key, Google account, WebView or signing secret is required. The application already has INTERNET/NETWORK_STATE permissions.

## Monthly flight time and payroll

Monthly Flight Time is recomputed from the canonical Room roster and includes only `FLIGHT` segments with `flightTimeCreditEligible=true`. Simulator, Medical, SEP, Stay, Reserve, OFF, Deadhead and instructor-observer Line Check do not add operating block. Completion adds profile flight time only for an eligible segment. Payroll uses the same eligibility flag for operating block pay while retaining its existing ground-duty policy.

## Operational Roster Change and 90-hour flow

Manual turnaround/layover duties use the shared directional route policy and persisted block value. A change is rejected if it would replace any day of a linked Medical/Simulator/SEP group or a Line Check pairing. Instructor/observer manual duties persist the role and do not receive operating credit.

Selecting the 90-hour target still does not rebuild the month. Extra duty is added only after the existing acknowledgement/publication flow, replaces only an OFF/Reserve row, rejects Leave/overlap, verifies 12-hour rest before and after, and stores source `COMPANY_EXTRA_DUTY`.

## Aircraft Delivery

The existing 2.2.8 delivery implementation remains intact: one-way ferry duty, fixed `HS-` prefix plus manually entered suffix, A330neo as a distinct supported type, persistent Fleet upsert after actual arrival and idempotent `deliveryProcessed`. Delivery rows are marked with source `DELIVERY` by new data and migration backfill.

## Static verification and build status

- Active version metadata: `2.2.10`, versionCode `2210`; GitHub artifact: `CrewPortal-2.2.10.apk`.
- Gradle wrapper remains 8.7; AGP 8.5.2; Kotlin 1.9.24; no Gradle 9.x change.
- Runtime `Rooster` spelling and old purple roster controls were searched; no roster misspelling was introduced.
- MapLibre dependency/style/lifecycle were compared with the official MapLibre Android and OpenFreeMap mobile quick starts.
- Unit-test source was added for linked simulator/medical/line-check credit and London seasonal ZoneId behavior.
- Per the user's explicit instruction, Gradle sync, tests, lint and `assembleDebug` were not run locally. The GitHub workflows remain configured to run tests, lint and assemble the APK.

## Known limitations

- The OpenFreeMap basemap needs network access to `tiles.openfreemap.org`; route coordinates and the rest of Flight Details remain local, but an offline device cannot download new tiles.
- OpenFreeMap is a public third-party service. For guaranteed production availability, self-host the same MapLibre-compatible style/tiles and change the single style URI.
- Final compiler/runtime confirmation will come from the first GitHub Actions run because local build execution was deliberately skipped.
