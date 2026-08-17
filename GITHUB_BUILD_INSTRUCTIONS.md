# GitHub build instructions — Crew Portal 2.2.8

1. Create an empty GitHub repository and upload the contents of this archive root.
2. Push to `main` or `master`, or open the **Actions** tab and run **Android CI** manually.
3. The workflow runs `testDebugUnitTest`, `lintDebug`, `assembleDebug` with JDK 17 / Gradle 8.7 and uploads `CrewPortal-2.2.8.apk` as an artifact.

No signing secrets are needed for the debug APK.

For a signed release, configure repository **Settings → Secrets and variables → Actions**:

- `CREWPORTAL_KEYSTORE_BASE64` — base64 of the production JKS/keystore;
- `CREWPORTAL_STORE_PASSWORD`;
- `CREWPORTAL_KEY_ALIAS`;
- `CREWPORTAL_KEY_PASSWORD`.

Then run **Release APK**, leave version `2.2.8`, and verify the generated GitHub Release. The production keystore must be backed up outside the repository; never commit it.

The first GitHub run is the authoritative Android compile/test/lint result because this archive was prepared without a local Android SDK at the user's request.
