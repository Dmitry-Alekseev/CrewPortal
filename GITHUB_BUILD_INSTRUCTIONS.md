# GitHub build instructions — Crew Portal 2.2.8

1. Create an empty GitHub repository and upload the contents of this archive root.
2. Push to `main` or `master`, or open the **Actions** tab and run **Android CI** manually.
3. The workflow runs `testDebugUnitTest`, `lintDebug`, `assembleDebug` with JDK 17 / Gradle 8.7 and uploads `CrewPortal-2.2.8.apk` as an artifact.

No GitHub signing secrets or `KEYSTORE_BASE64` are required. The archive includes the same legacy Crew Portal test keystore that signed the previous installed build, and Gradle uses it automatically. This is required so Android can install 2.2.8 over the existing package without deleting its local database.

To create a GitHub Release, run **Release APK**, leave version `2.2.8`, and verify the generated release. This workflow intentionally publishes the installable debug APK signed with the legacy Crew Portal certificate. It is suitable for testing, direct installation and upgrading the previous project build, but it is not a production Play Store signing setup.

Legacy certificate SHA-256 fingerprint: `47:08:4C:2B:0B:7C:D6:99:95:7A:DF:A9:26:43:4E:A7:85:0D:E0:F6:86:74:4A:D3:F3:99:59:FE:36:BB:82:5F`.

The first GitHub run is the authoritative Android compile/test/lint result because this archive was prepared without a local Android SDK at the user's request.
