# GitHub build instructions — Crew Portal 2.2.9

1. Create an empty GitHub repository and upload the contents of this archive root.
2. Push to `main` or `master`, or open the **Actions** tab and run **Android CI** manually.
3. The workflow runs `testDebugUnitTest`, `lintDebug`, `assembleDebug` with JDK 17 / Gradle 8.7 and uploads `CrewPortal-2.2.9.apk` as an artifact.

No GitHub signing secrets or `KEYSTORE_BASE64` are required. The archive includes the legacy Crew Portal test keystore and Gradle uses it automatically. The requested 2.2.9 test starts with a clean database: uninstall 2.2.8 from Android, then install 2.2.9. Android will erase the old app-local database during uninstall and Crew Portal will seed a new roster for the current Bangkok month on first launch.

To create a GitHub Release, run **Release APK**, leave version `2.2.9`, and verify the generated release. This workflow intentionally publishes the installable debug APK signed with the legacy Crew Portal certificate. It is suitable for testing and direct installation, but it is not a production Play Store signing setup.

Next-month generation is gated to the seventh calendar day counting from month end (for September 2026: 25 August 2026). WorkManager is idempotent, so repeated background runs do not duplicate September rows.

Legacy certificate SHA-256 fingerprint: `47:08:4C:2B:0B:7C:D6:99:95:7A:DF:A9:26:43:4E:A7:85:0D:E0:F6:86:74:4A:D3:F3:99:59:FE:36:BB:82:5F`.

The first GitHub run is the authoritative Android compile/test/lint result because this archive was prepared without a local Android SDK at the user's request.
