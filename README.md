# Crew Portal

Android MVP for a local crew personal cabinet.

## Stack

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Retrofit / OkHttp
- Android BiometricPrompt
- Local JSON schedule from `app/src/main/assets/schedule.json`

## Login

- Corporate ID: `CPD9842`
- Password: `Airbus1998`

## Profile

- Dmitrii Alekseev
- Date of Birth: 14 July 1998
- Position: Captain
- Airline: Thai Airways
- Home Base: BKK
- Total Flight Time starts from 4000h 00m
- PIC Time starts from 1500h 00m

## Build APK

Open this folder in Android Studio, wait for Gradle sync, then use:

`Build -> Build Bundle(s) / APK(s) -> Build APK(s)`

If Android Studio asks to install missing SDK components, accept it.

## Schedule updates

Replace `app/src/main/assets/schedule.json`, rebuild the app, then open:

`Settings -> Reload Schedule from JSON`

This reloads the local schedule into Room.
