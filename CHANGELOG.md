# Changelog

## 2.1.9

- Hide past non-flight roster items from active Roster after their end time, including OFF, RESERVE, STAY and leave.
- Keep historical roster records in the local database for monthly totals, logbook and payroll.
- Show registered flight action button in green after successful registration.
- Propagate aircraft registration across same-duty turnaround legs, so return sectors immediately show the same aircraft registration as the outbound sector.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.1.9.apk.

# Crew Portal Changelog

## 2.1.8
- Hide completed flight duties from the active Roster list after completion.
- Completed flights remain stored and continue to count toward monthly completed flight time/logbook data.
- Bumped update metadata to CrewPortal-2.1.8.apk.


## 2.1.7
- Fixed active month selection after month boundary: confirmed June roster no longer jumps to empty July.
- Kept active roster/calendar visible while Today’s Duty is In flight.
- Made startup roster refresh silent so old due notifications are not replayed on app launch.
- Added departure-airport timezone checks for registration window logic.
- Suppressed gate/stand assignment and notifications on same-duty turnaround return sectors.
- Replaced Calendar List/Month default purple controls with a compact blue/graphite segmented control.
- Moved TAS/Tashkent out of normal random layovers and added future Thursday/Sunday special handling.
- Refreshed login screen styling.

## 2.1.6
- Added Calendar month grid view with a small List/Month toggle.
- Month grid shows flight, stay, reserve, OFF and leave indicators.
- Tapping a duty date opens the existing Roster/Flight details screen instead of creating a separate calendar details page.
- Does not regenerate or modify the current local roster.

## 2.1.5
- Cleaned airport display names across roster cards; TAS now shows Tashkent / Islam Karimov.
- Replaced layover labels like “Stay at TAS” with city names such as “Stay in Tashkent”.
- Removed UTC/local time toggle from Roster and removed UTC timing text from flight details.
- Fixed monthly progress wording so Leave is displayed as “Leave: 0 days” on one line.

# Crew Portal Changelog

## 2.1.4
- Reordered Leave screen: calendar first, personal leave confirmation below.
- Reworked Leave action buttons to avoid default purple styling.
- Fixed flight card date format: year removed, weekday kept readable.
- Fixed route maps so each flight uses its actual departure and arrival airports.
- Bonus is now hidden until the monthly payslip is available.
- Removed Leave block from Profile because Leave has its own section.
- Kept existing generated roster data unchanged.

## 2.1.2
- Reworked roster generation: no fixed June template; next month is generated from route pools.
- Generator targets about 78–82 block hours and mixes turnarounds, layovers, reserve and OFF days.
- Fixed rest card logic so connected turnaround sectors do not show 5-minute crew rest.
- Removed FO/Capt. prefixes from displayed crew names.
- Kept JSON roster removed as an active schedule source.
