# Crew Portal

Version: 3.0

Crew Portal is a Kotlin / Jetpack Compose Android app for a Thai Airways Airbus pilot workflow.

## Version 3.0 highlights

- Modern Material 3 corporate design system, refreshed navigation and a new aviation launcher icon.
- Simplified Aircraft Delivery form: delivery date, flight number, aircraft type and manual `HS-` suffix only.
- Automatic BKK-DXB-HAM passenger positioning followed by EDHI/XFW-intermediate-BKK ferry planning; EDDH-EDHI is intentionally omitted from roster rows.
- Delivery technical stops use GYD, DWC, DOH or MCT. Two-pilot crews receive deterministic 8-12h rest; four-pilot crews receive a 2-4h stop and an explicit operating/passenger role on each leg.
- Deliverable types are distinct: A330-800neo, A330-900neo, A350-900, A320neo and A321neo. The aircraft joins persistent Fleet only after final BKK arrival.
- Five taps on the application version expose a confirmed, permanently one-time early next-month generation command for QA.

## Existing platform capabilities

- Route maps use an interactive OpenFreeMap vector basemap through MapLibre; no API key is required.
- Qualifications have one shared source of truth: completed and next-due dates appear identically in Profile and Documents & Qualifications.
- Medical (2 days), simulator (3 days), SEP and instructor line checks are reserved before ordinary roster generation and recur every six months.
- Route block time is selected once in a five-minute step inside each directional min/max range and persisted on the roster leg.
- Unknown supported destinations use a distance-based block estimate instead of a fixed 2h30 fallback.
- STAY headings always resolve the airport city; legacy crew-hotel text cannot become the location title.

- Fillable EASA-style electronic pilot logbook is embedded in Flight Details with draft and certify/lock states.
- Aircraft Delivery / Ferry supports manual `HS-` registration and persistent fleet enrollment.
- Room migration 3→4 preserves roster data and adds persistent fleet, leave, logbook and UTC instant state.
- Roster replacement is transactional, generation deterministic, and next-month publication runs through WorkManager.
- Payroll policy and common route metadata have dedicated non-UI sources of truth.
- GitHub builds require no signing secrets and reuse the legacy certificate, allowing an in-place update that preserves local data.
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

APK artifact name: `CrewPortal-3.0.apk`
