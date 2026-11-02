# Task Manager

A native Android application designed for advanced task management, featuring location-based reminders, recurring scheduling, and multimedia attachments.

## Features
* Task Management: Full CRUD operations with advanced sorting, filtering, and priority-based planning.
* Advanced Notifications: Implements both time-based alarms and geofence-triggered (location-based) reminders.
* Multimedia Attachments: Supports attaching photos, PDF documents, and voice recordings directly to tasks.
* Recurring Tasks: Automated scheduling logic for daily, weekly, monthly, or custom recurring intervals.
* Modern UX: Includes dark mode support, swipe gestures for quick actions, and home screen widgets.

## Tech Stack
* Language: Kotlin
* UI Toolkit: Jetpack Compose
* Architecture: MVVM (Model-View-ViewModel)
* Local Database: Room
* Background Services: Location Services (Geofencing), AlarmManager / WorkManager for notifications

## Setup Instructions
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Ensure location permissions are granted on the target device to test geofencing features.
5. Build and run the application on an emulator or a physical device.