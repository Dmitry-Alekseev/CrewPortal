# Crew Portal Changelog

## 2.2.4
- Improved Operational Roster Change form controls: date picker calendar, time picker clock, ICAO/airport autocomplete, aircraft type dropdown, and registration dropdown filtered by selected aircraft type.
- Added hidden Operational Roster Change form from Roster sync line 5-tap action.
- Manual form now collects outbound skeleton first, then reveals Turnaround or Layover return/rest fields.
- Manual operational changes create normal Roster/Calendar entries and show a company notification.
- Added 90h extra-duty publication from the company Messages acknowledgement flow.
- Changed DPS/Bali to same-day turnaround-capable route and ensured layover STAY items are visible.
- Increased MEL / Technical Remarks variability across aircraft.
- Added Messages unread red dot in bottom navigation and highlighted unread cards.
- Restyled next-month preview/review buttons and removed the large post-review info card.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.2.4.apk.

## 2.2.2
- Fixed next-month draft state: selecting 80h/90h no longer switches the active roster away from the current month.
- Added explicit next-month preview controls in Roster and Calendar when a draft exists.
- Added company Messages item when a generated roster is ready for review.
- Updated roster generation seed so test-generated drafts are fresh/random rather than near-identical copies.
