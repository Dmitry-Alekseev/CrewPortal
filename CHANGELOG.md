# Crew Portal Changelog

## 3.0.2

- Added the complete historical Thai Airways A380 fleet and a dedicated A380 type/catalog entry.
- Added the fixed October 2026 Toulouse type-rating programme with company leave through 8 October, personal leave on 9-14 October, passenger positioning on 15 October, weekday training/weekends off and final examinations on 29-31 October.
- Added passenger return `TLS-IST-BKK` on 1-2 November plus recovery through 3 November, preventing an immediate generated operating flight.
- The A380 rating becomes visible after the final check. A380 sectors use the First Officer role through 31 December 2026, accrue total/type time without PIC time and hide the electronic logbook; Captain policy resumes on 1 January 2027.
- Added LAX, SFO, SEA, JFK, IAD, ORD, DFW, BOS, MIA and ATL to Airport Info, Company Routes, maps, hotels and generation.
- Added a shared A380 airport-compatibility and 8,000 NM range policy.
- Moved Tashkent into ordinary randomized route selection while preserving its two operational patterns.
- Added manual OFF to Operational Roster Change.
- Updated Android/release metadata to 3.0.2 / versionCode 3002 without changing the Room schema or signing certificate.

## 3.0.1

- Approved personal leave now schedules a durable five-minute WorkManager reconciliation that removes conflicting future roster rows and immediately updates monthly target/payroll sources.
- Next-month generation continues to load persisted approved leave before placing flights, reserve, qualifications or other duties.
- Removed the unavailable extra-next-month button and special Tashkent rotation labels.
- Added distinct regular Captain, operating Captain Instructor and third-seat Instructor Observer assignments for manual and generated duties.
- Instructor sectors no longer show the electronic logbook; active Captain Instructor block time remains credited.
- Reconciled the bundled active Thai Airways Airbus fleet with open registers, including all nine listed A321neo registrations.
- Updated Android and release metadata to 3.0.1 / versionCode 3001.

## 3.0

- Introduced a modern Material 3 corporate design system, updated navigation surfaces and a new aviation launcher icon.
- Reduced Aircraft Delivery input to date, delivery flight number, supported new-aircraft type and manual `HS-` registration suffix.
- Added deterministic BKK-DXB-HAM passenger positioning based on a public Emirates timetable reference and EDHI/XFW-intermediate-BKK ferry planning without an EDDH-EDHI roster row.
- Added GYD, DWC, DOH and MCT delivery stops, correct airport metadata/time zones and route-map coordinates.
- Added 8-12h midpoint rest for two-pilot delivery crews and 2-4h fuel stops with explicit per-leg operating/passenger roles for four-pilot crews.
- Split A330-800neo, A330-900neo, A350-900, A320neo and A321neo into distinct deliverable fleet types; activation occurs only after final BKK arrival.
- Made the five-tap next-month QA generator truly one-time: confirmation is required, the flag is written only after success and app updates/draft deletion cannot reset it.
- Updated Android, update and GitHub workflow metadata to 3.0 / versionCode 3000 and retained the legacy update certificate.

## 2.2.10

- Replaced the placeholder gray route diagram with OpenFreeMap's public Liberty vector basemap through MapLibre; no API key or signing secret is required.
- Added a shared qualification schedule for Profile, Documents & Qualifications and roster generation.
- Recorded the completed August medical and line check and July simulator session; each next-due date is six months later.
- Added linked three-day simulator, two-day medical, SEP and instructor line-check events before normal flight generation, including cross-month group metadata and protection from partial roster replacement.
- Excluded simulator, medical and instructor-observer line-check duties from monthly operating flight time and operating block pay.
- Added persisted roster source, linked-event and flight-time-credit columns through Room migration 4 to 5.
- Made generated and manual block times deterministic five-minute selections within directional min/max route ranges.
- Changed automatic next-month generation to the 27th and loaded persisted leave in the background worker before generation.
- Preserved the already-published current month during application and generator-rule updates.
- Bumped Android, update and GitHub workflow metadata to 2.2.10 / versionCode 2210.

## 2.2.9

- Replaced the online osmdroid tile map with a bundled, offline Compose route map; no map API key or tile-server access is required.
- Added LED to the offline airport-coordinate source and to generated long-haul layovers.
- Confirmed authoritative block times: BKK-LED 10h50 / LED-BKK 10h25 and BKK-LHR 12h40 / LHR-BKK 11h45.
- Replaced the fixed 2h30 unknown-route fallback with a distance-based scheduled-block estimate.
- Made STAY headings derive the city from airport metadata, preventing legacy `crew hotel` text from becoming the displayed location.
- Added unit coverage for LED/LHR block time, LED offline coordinates and non-150-minute route fallback.
- Bumped Android, update and GitHub workflow metadata to 2.2.9 / versionCode 2209.

## 2.2.8

- Added a fillable, certifiable EASA-style electronic pilot logbook to Flight Details.
- Added Aircraft Delivery / Ferry with manual `HS-` registration and automatic persistent fleet enrollment after arrival, including A330neo.
- Added safe Room 3→4 migration, persisted fleet/leave/logbook records, UTC instant backfill, atomic roster replacement and typed duty values.
- Made roster generation deterministic, connected next-month WorkManager scheduling and extracted payroll policy from Compose UI.
- GitHub release workflow reuses the legacy certificate without signing secrets, allowing an in-place update that preserves local app data.

- Corrected TAS timezone handling and isolated the two TAS rotation patterns behind tested business logic.
- Thursday TAS rotations now explicitly contain Friday/Saturday STAY and Sunday operating return; Sunday arrivals use a same-Sunday deadhead departure.
- Preserved existing published current-month roster data during app updates; new generator rules seed only an empty database or future manual drafts.
- Centralized monthly block metrics and crew-hotel data.
- Replaced remaining purple UI tokens with corporate blue/graphite/neutral styling and normalized roster action labels.
- Added Gradle wrapper 8.7, GitHub tests/lint/assemble workflow and a complete developer guide.
- Fixed Leave Management approval flow: approved personal leave is now added to shared leave state instead of staying only as a local screen preview.
- Roster, Calendar and Payroll leave calculations can now see newly approved personal leave immediately.
- Updated Profile role from Captain to Line Pilot Instructor.
- Added Line Pilot Instructor license record issued 19 June 2026.
- Added "operate as line pilot instructor / observer" checkbox to Operational Roster Change.
- Manual instructor duties keep Captain and First Officer generated by the crew system and show Dmitrii Alekseev as third crew member / line pilot instructor.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.2.8.apk.

## 2.2.5

- Operational Roster Change destination starts empty, with no Denpasar prefill.
- Destination suggestions are sorted by ICAO and scrollable.
- Manual route airport display now resolves city and airport names from shared airport database, fixing repeated HKG labels.
