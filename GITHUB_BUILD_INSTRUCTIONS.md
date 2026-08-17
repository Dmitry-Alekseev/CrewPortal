# GitHub build instructions — Crew Portal 3.0

1. Create an empty GitHub repository and upload the contents of this archive root.
2. Push to `main` or `master`, or open the **Actions** tab and run **Android CI** manually.
3. The workflow runs `testDebugUnitTest`, `lintDebug`, `assembleDebug` with JDK 17 / Gradle 8.7 and uploads `CrewPortal-3.0.apk` as an artifact.

No GitHub signing secrets or `KEYSTORE_BASE64` are required. The repository includes the legacy Crew Portal test keystore and Gradle uses it automatically. Installing 3.0 over a previous Crew Portal build with the same certificate preserves the local Room/DataStore database; the explicit Room 4-to-5 migration preserves existing roster rows.

To create a GitHub Release, run **Release APK**, leave version `3.0`, and verify the generated release. This workflow intentionally publishes the installable debug APK signed with the legacy Crew Portal certificate. It is suitable for testing and direct installation, but it is not a production Play Store signing setup.

Next-month generation is gated to the 27th calendar day. WorkManager loads persisted Leave first and is idempotent, so repeated background runs do not duplicate next-month rows.

Legacy certificate SHA-256 fingerprint: `47:08:4C:2B:0B:7C:D6:99:95:7A:DF:A9:26:43:4E:A7:85:0D:E0:F6:86:74:4A:D3:F3:99:59:FE:36:BB:82:5F`.

The GitHub Actions run is the authoritative Android compile/test/lint result and publishes the requested APK artifact.
