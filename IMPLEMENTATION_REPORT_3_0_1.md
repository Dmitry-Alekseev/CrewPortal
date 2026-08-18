# Crew Portal 3.0.1 — implementation report

## Leave and roster state

`LeaveRepository.addPersonalLeave` persists an approved request and schedules `LeaveRosterSyncWorker` with a five-minute initial delay. The worker reloads Room leave state, removes every non-completed future roster row whose local date span intersects approved leave, reschedules roster notifications, and publishes a completion notification. Monthly target, roster metrics and payroll continue to read the shared persisted leave/flight sources.

`NextRosterWorker` initializes the same `LeaveRepository` before `RosterGenerator.generateForMonth`, so approved leave dates are occupied before qualifications, flights, reserve or OFF generation.

## Instructor roles and logbook

`InstructorRole` stores three stable states in the existing `lineCheckRole` column: regular Captain, operating Captain Instructor and third-seat Instructor Observer. Manual Operational Roster Change exposes all three. Generated six-month line checks alternate deterministically between the two instructor assignments. Observer block is excluded; operating Captain Instructor block is credited. `FlightDetailsScreen` hides the electronic logbook for either instructor assignment.

## Fleet register

The static active Airbus seed was reconciled on 18 August 2026 against Thai Airways, Plane Finder and the Thailand CAAT register. Missing A321neo registrations `HS-TOG`, `HS-TOI`, `HS-TOJ`, `HS-TOL`, A330 registrations `HS-TEW`, `HS-TEX`, and A350 registrations `HS-THR`, `HS-THX` were added. Obsolete bundled seed rows are pruned while aircraft delivered and persisted by the user are preserved.

## Persistence and version

No Room schema migration is needed: instructor roles reuse `lineCheckRole`, leave uses the existing `leave_periods` table, and fleet reconciliation uses existing provenance fields. Version is `3.0.1`, versionCode `3001`, with the legacy update certificate unchanged.
