# Sideglance 🗺️👀

**Sideglance** is a premium, minimal, and modern Jetpack Compose Android application designed for partners to share their presence, custom statuses, and relative distance in real-time. It keeps you connected with your partner through instant dashboard visual indicators, local notifications, and customizable home-screen widgets.

---

## Key Features 🚀

### 1. Google Sign-In & Offline Gating
- **Modern Authentication**: Uses the modern Android **Credential Manager API** combined with `GetGoogleIdOption` to handle secure OAuth sign-in flow.
- **Mock Mode Fallback**: Provides a robust, offline-first developer fallback mode that lets you test login flows and database queries using local mock structures without requiring active Firebase credentials.

### 2. CameraX & ZXing Partner Pairing
- **Scan-to-Connect**: Seamlessly link with your partner by generating a custom pairing link and QR code.
- **Real-Time Scanner**: Decodes partner QR codes instantly using a CameraX analyzer backed by the **ZXing barcode decoding library**. Includes scanning locks to prevent double-pairing requests.
- **Deep Linking**: Supports deep linking (`sideglance://pair?code=XYZ`) so partners can click a shared link to open the app and pair automatically.

### 3. Location Sharing, Fallbacks & Opt-Out
- **Relative Distance Calculation**: Calculates the geographical distance in meters/kilometers using the Haversine formula based on coordinates.
- **Location Fallback (Infinity)**: If location permission is denied, distance is cleanly rendered as `infinity (∞)` for both partners, and location coordinates are updated to `null` in the database to preserve privacy.
- **Manual Toggle Opt-Out**: Includes a physical location opt-out switch right in the distance display card. Toggle location sharing OFF to stop updating location coordinates at any time, even if system location permission remains granted.
- **App Settings Navigation**: Provides a direct settings navigation button to open the system App Info details screen so permissions can be easily restored, rechecking state automatically on return via a Compose lifecycle listener.

### 4. Presence & Custom Status Sheet
- **Custom Activity Types**: Select from common presets (Work 💼, Cook 🍳, Play 🎮, Rest 😴, Travel 🚗) or set a fully custom activity and emoji.
- **Detailed Descriptions**: Add extra context under the "Add some details" input field (e.g. "Pasta", "Coding", "Gym").
- **Flexible Expiration**: Set status duration (1 Hour, 4 Hours, 8 Hours, 1 Day, or Forever). Defaults to **1 Hour** to encourage fresh updates. Statuses automatically expire and clear.

### 5. Home-Screen Glance Widgets
- **Jetpack Glance Layout**: A beautiful, minimal warm-themed widget that updates instantly when your partner's status, details, or distance changes.
- **Status Resetting**: Correctly handles resetting status detail texts to empty spaces so they don't persist on the widget when cleared.
- **Expiry Warnings**: Appends formatted expiration alerts directly on the widget layout (e.g., `• forever` or `• until 4:30 PM`).

### 6. Foreground & Background Notifications
- **Android 13 to Android 16 Support**: Declares and requests the runtime `POST_NOTIFICATIONS` permission, delivering alerts using high-priority channel paths and app icon drawables.
- **Partner Status Alerts**: Fires local notifications in the foreground and background when your partner updates their status.
- **Background Work Optimization**: The background `WidgetUpdateWorker` uses direct document fetches (`get()`) instead of open gRPC snapshot listeners, avoiding connection freezes and conserving battery.
- **Own Expiration Workers**: Schedules a One-Off `StatusExpiredWorker` using WorkManager delays, sending you a notification warning you to update your status when your current one runs out.

---

## Tech Stack 🛠️

- **UI Framework**: Jetpack Compose (Kotlin)
- **Design System**: Material Design 3 (Vanilla CSS styling equivalent spacing, warm colors)
- **Database & Sync**: Firebase Auth & Cloud Firestore
- **Local Widgets**: Jetpack Glance (PreferencesDataStore-backed state)
- **Background Execution**: WorkManager API
- **QR & Codecs**: ZXing (Zebra Crossing) & CameraX
- **Navigation**: Jetpack Compose Navigation (Safe-args and single-task launchMode task alignment)

---

## Getting Started 💻

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or JDK 20
- A Firebase project with Auth (Google Sign-In) and Firestore database enabled

### Installation
1. Clone the repository.
2. Place your `google-services.json` inside the `app/` directory.
3. Build and run the app on an emulator or physical device.

---

## Firestore Security Rules 🔒

To secure your Cloud Firestore database, apply the following security rules in your Firebase console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null;
    }
    match /statuses/{userId} {
      allow read, write: if request.auth != null;
    }
  }
}
```
