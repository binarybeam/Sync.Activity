# Notification Sync App for Android

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Overview

Notification Sync is an Android application that captures notifications from your device and synchronizes them across multiple devices or services. It is designed for users who want to mirror notifications in real-time, ensuring no alert is missed.

The app is built in **Kotlin** and follows modern Android development practices.

---

## Features

* **Real-time notification capture** using `NotificationListenerService`.
* **Sync to multiple targets** (other devices, servers, or cloud endpoints).
* **Selective app filtering** – choose which apps' notifications get synced.
* **Background sync** with optimized battery usage.
* **Lightweight UI** with Material 3 design.

---

## Architecture

* **Language:** Kotlin
* **UI:** XML + Kotlin
* **Permissions:** Notification access, Internet, Foreground service

---

## Requirements

* **Minimum Android Version:** Android 6.0 (API 23)
* **Target Android Version:** Latest stable
* **Build Tool:** Android Studio Koala | Gradle 8+

---

## Getting Started / Setup Guide

1. **Clone the Repository**

   ```bash
   git clone https://github.com/<your-username>/notification-sync.git
   cd notification-sync
   ```

2. **Open in Android Studio**

   * Launch Android Studio.
   * Select `Open an existing project` and choose the cloned folder.

3. **Grant Notification Access**

   * Go to `Settings > Notifications > Notification Access`.
   * Enable access for Notification Sync.

---

## Configuration

| Setting          | Description                                          |
| ---------------- | ---------------------------------------------------- |
| `SYNC_ENDPOINT`  | The API or device endpoint to send notifications to. |
| `ALLOWED_APPS`   | List of apps whose notifications will be synced.     |

---

## Running Locally

1. Connect an Android device or start an emulator.
2. Click the **Run ▶** button in Android Studio.
3. Grant all required permissions when prompted.

---

## Building the APK

### Debug APK
```bash
./gradlew assembleDebug
```

APK will be located in:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (signed)

1. Generate a Keystore (if you don’t already have one):
   ```bash
   keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release-key
   ```
2. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```
3. The signed APK will be in:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```
