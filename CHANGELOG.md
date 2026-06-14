# Crew Portal Changelog

## 2.2.1
- Fixed gate/stand availability timing by using airport-local time consistently between notification logic and in-app display.
- Added qualification-aware roster planning for Profile validity items: simulator recurrent, medical commission, SEP Land/Water and line check.
- Simulator recurrent now reserves three 10:00–16:00 days and counts into monthly planned duty/norm.
- Medical commission and SEP Land/Water reserve paid ground-duty days without counting into monthly flight norm.
- Line check is applied to a suitable flight and adds a line instructor to Flight Details.
- Limited augmented/double crew to flights longer than 10 hours; medium-haul flights like TAS remain standard two-pilot crew.
- Fixed Messages Inbox scrolling and kept read-message multi-select/delete behavior.
- Restyled METAR Refresh and Settings action buttons to the corporate blue/graphite style.
- Removed Update Center from More; update checking remains in Settings.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.2.1.apk.

## 2.2.0
- Refreshed Airbus fleet database only: A320, A321neo, A330-300 and A350-900.
- Moved HS-TXH to A320, moved HS-TXI to A350, added TO-series A321neo aircraft and excluded parked HS-TEW from the active pool.
- Tightened aircraft assignment so a registration cannot be assigned to the wrong aircraft family.
- Added route aircraft weighting: Phuket/domestic narrow-body only, KUL/SIN mixed narrow/wide, India any Airbus, long-haul A330/A350.
- Added more turnaround time-of-day variety and kept minimum 12h rest between separate duties.
- Cleaned Messages UI buttons, forced English date formatting, added read multi-select and persistent deletion state.
- Hidden 5-tap next-month test flow can now generate the next-month draft, then clear it with another 5 taps without touching the active roster.
- Bumped app metadata, update metadata, workflows and APK naming to CrewPortal-2.2.0.apk.
