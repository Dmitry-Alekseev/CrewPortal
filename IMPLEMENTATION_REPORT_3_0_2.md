# Crew Portal 3.0.2 — implementation report

## Implemented

- Tashkent is no longer evaluated before all other routes. Its Thursday/Sunday operational templates remain in `TashkentRotationFactory`, but selection now occurs inside the ordinary layover/turnaround candidate pools.
- Operational Roster Change can create an `OFF` row and optionally replace the existing assignment on that date. Linked qualification/training rows remain protected.
- The complete historical Thai Airways A380 fleet (`HS-TUA`…`HS-TUF`) is seeded as `A380-800` / Airbus A380-841.
- The A380 rating is date-gated after the final 31 October 2026 check. A380 flight time has a dedicated DataStore counter.
- LAX, SFO, SEA, JFK, IAD, ORD, DFW, BOS, MIA and ATL are available in Airport Info, Company Routes, manual selection, maps and monthly generation.
- `RouteDefinition.a380Eligible` requires a documented compatible airport and a great-circle distance no greater than 8,000 NM. SEA is deliberately excluded by airport policy; MIA is excluded by range.
- October 2026 company leave remains 1-8 October and the pilot-requested extension is 9-14 October. These are separate leave types and cards.
- `A380TransitionProgram` adds BKK-IST-TLS passenger positioning on 15 October, weekday training with weekends OFF, theory on 29 October, simulator training on 30 October and final simulator examination on 31 October.
- TLS-IST-BKK passenger return is scheduled on 1-2 November, followed by recovery through 3 November. Ordinary November generation cannot occupy these dates.
- Generated A380 sectors between 1 November and 31 December 2026 show Dmitrii Alekseev as First Officer, hide the electronic logbook and add total/A380 time without PIC. From 1 January 2027 the normal Captain, PIC and logbook behavior resumes.

## Persistence and migration

No Room schema change is required. The fixed personal leave uses the existing `leave_periods` table. The A380 flight-time counter is a backward-compatible DataStore key whose default is zero. The existing signing certificate remains unchanged.

## External data basis

- Thai A380 registrations/configuration: Planespotters/Plane Finder fleet records and archived CAAT registration data.
- US A380 handling: FAA Airbus A380 modification-of-standards destination list.
- A380 range limit: Airbus published 8,000 NM passenger range.
- Toulouse positioning: BKK-TLS has no direct service; the programme uses a supported one-stop connection through Istanbul.

## Verification targets

`A380TransitionProgramTest`, `A380RouteCompatibilityTest`, `AircraftPoolCurrentFleetTest` and `AircraftTypeCatalogTest` cover the new transition, route and fleet invariants. GitHub Android CI remains the authoritative `testDebugUnitTest`, `lintDebug` and `assembleDebug` environment.
