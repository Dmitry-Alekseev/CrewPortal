# GitHub build instructions — Crew Portal 2.2.8

1. Create an empty GitHub repository and upload the contents of this archive root.
2. Push to `main` or `master`, or open the **Actions** tab and run **Android CI** manually.
3. The workflow runs `testDebugUnitTest`, `lintDebug`, `assembleDebug` with JDK 17 / Gradle 8.7 and uploads `CrewPortal-2.2.8.apk` as an artifact.

No GitHub signing secrets, `KEYSTORE_BASE64`, passwords or keystore files are required. Android Gradle Plugin creates the standard temporary debug key on the GitHub runner automatically.

To create a GitHub Release, run **Release APK**, leave version `2.2.8`, and verify the generated release. This workflow intentionally publishes the installable debug APK produced by the project. It is suitable for testing and direct installation, but it is not a production Play Store signing setup.

The first GitHub run is the authoritative Android compile/test/lint result because this archive was prepared without a local Android SDK at the user's request.
