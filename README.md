# Crew Portal

Version: 2.2.5

Android crew operations portal built with Kotlin and Jetpack Compose.

## Version 2.2.5 highlights

- Operational Roster Change destination picker no longer pre-fills Denpasar.
- Destination dropdown is ICAO-sorted and scrollable.
- Manual route airport names are resolved from the shared airport database, preventing repeated code/code/code display for destinations such as HKG.
- Hidden Operational Roster Change form for manual add/replace duty testing, now with date/time pickers and ICAO autocomplete.
- Ordered manual flow: outbound details → Turnaround/Layover → generated return/rest details.
- Manual changes are inserted consistently into Roster and Calendar and trigger a company notification.
- 90h selected target can publish an additional duty from Messages after acknowledgement.
- DPS/Bali can be planned as same-day turnaround when timing allows; layovers show STAY clearly.
- MEL / Technical Remarks variability improved so different aircraft are not almost always empty.
- Messages red-dot unread indicator and unread card highlight added.
- Next-month preview buttons cleaned up and large reviewed info card removed.

APK artifact name: `CrewPortal-2.2.5.apk`
