# Crew Portal Changelog

## 2.1.2
- Reworked roster generation: June/next-month roster is now produced from randomized route pools instead of a fixed June template.
- Generator targets a realistic ~78-82 block-hour month, mixes turnarounds, layovers, reserve and OFF days, and avoids long fixed OFF chains.
- Replaced existing generated next-month roster when the hidden 5-tap generator is used again after update.
- Fixed rest indicator: no more tiny rest cards between connected turnaround sectors such as BKK-SIN and SIN-BKK.
- Removed FO/Capt. prefixes from crew names; roles remain shown by row labels.
- Updated release metadata to 2.1.2.

# Changelog

## 2.1.1
- Fixed roster review flow: Calendar/Roster stay on the current month until hidden generation is triggered.
- Disabled automatic next-month roster preparation on app start/update.
- Balanced generated roster close to 80 block hours and removed long OFF blocks.
- Roster list now shows the whole selected month, including past/current/future duties.
- Kept 2.1.0 UI, MEL and Payroll updates.

# Crew Portal Changelog

## 2.1.0
- Removed JSON roster as an active source; roster is local/generated.
- Added one-time hidden next-month roster generation from Settings version label.
- Added version label on splash screen.
- Reworked default purple buttons/cards toward a cleaner corporate style.
- Improved airport-name layout for Kuala Lumpur/KUL.
- Expanded MEL / Technical Remarks with dynamic aircraft-type based deferred items.
- Payroll now waits until monthly closing date and shows monthly payslip cards.
- Added rest-period cards in Roster between operational duties, including OFF/leave/layover days in the rest calculation.
