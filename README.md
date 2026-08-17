# Crew Portal

Version: 2.2.9

Crew Portal is a Kotlin / Jetpack Compose Android app for a Thai Airways Airbus pilot workflow.

## Version 2.2.9 highlights

- Route maps are rendered from bundled airport coordinates and need no tile server, API key or network access.
- LED is supported by the offline map and generated layover rules.
- BKK-LED is 10h50 outbound / 10h25 inbound; BKK-LHR is 12h40 outbound / 11h45 inbound.
- Unknown supported destinations use a distance-based block estimate instead of a fixed 2h30 fallback.
- STAY headings always resolve the airport city; legacy crew-hotel text cannot become the location title.

- Fillable EASA-style electronic pilot logbook is embedded in Flight Details with draft and certify/lock states.
- Aircraft Delivery / Ferry supports A330neo and manual `HS-` registration, then adds the aircraft to the persistent fleet after arrival.
- Room migration 3→4 preserves roster data and adds persistent fleet, leave, logbook and UTC instant state.
- Roster replacement is transactional, generation deterministic, and next-month publication runs through WorkManager.
- Payroll policy and common route metadata have dedicated non-UI sources of truth.
- GitHub builds require no signing secrets and reuse the legacy certificate. For the requested clean test, uninstall 2.2.8 first and install 2.2.9 to create a new current-month database.
- Leave Management now applies approved personal leave to shared leave state.
- Profile shows Dmitrii Alekseev as Line Pilot Instructor.
- Line Pilot Instructor license added with issue date 19 June 2026.
- Operational Roster Change supports instructor/observer duty checkbox.
- Instructor manual duties display Dmitrii Alekseev as third crew member while operating Captain and First Officer remain system-generated.
- TAS rotations use airport-local timezone conversion: Thursday outbound, Friday/Saturday stay, Sunday operating return; a Sunday arrival uses same-Sunday deadhead departure.
- Published current-month roster rows are preserved across app updates and generator-rule changes.
- Corporate UI uses shared blue, graphite and neutral tokens; roster action labels use consistent `Roster` spelling.
- `CREW_PORTAL_DEVELOPER_GUIDE.md` documents architecture, state, business rules and safe extension points.
- GitHub Android CI uses the checked-in Gradle 8.7 wrapper and runs unit tests, lint and `assembleDebug`.

APK artifact name: `CrewPortal-2.2.9.apk`
