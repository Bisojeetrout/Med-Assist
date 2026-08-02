Med Assist (formerly Swasthya) 🩺
Med Assist is a comprehensive, AI-powered health tracking and medical assistance Android application built with Kotlin and Jetpack Compose. It seamlessly tracks daily vitals, manages medical reports, schedules medicine reminders, and integrates advanced AI capabilities to provide actionable insights on your health.

🌟 Key Features
AI Health Insights (Powered by Google Gemini): Automatically generates summaries for uploaded medical reports and analyzes your recent vitals, diet, and sleep to offer concise, personalized health advice.
SOS Emergency Alert: A fully customizable, one-tap emergency button that can instantly text or dial your emergency contact (or 911) and share a distress message featuring your blood group and medical conditions.
One-Tap Physician Sharing: Instantly generates and shares a comprehensive summary of your health data, including recent vitals, medications, and AI-summarized medical reports with your doctor via SMS, WhatsApp, or Email.
Health Connect Integration: Securely aggregates and syncs daily step counts, heart rate, and calories burned directly from the Android Health Connect API.
Cloud Medical Records: Upload medical documents securely. Files are hosted on Cloudinary and synchronized seamlessly with Firebase Firestore.
Medication & Diet Tracking: Log daily foods and track medication schedules with offline support.
Local-First Architecture: Utilizes Room Database for lightning-fast offline access, automatically backing up to Firebase when online.
🛠️ Tech Stack
Language: Kotlin
UI Framework: Jetpack Compose (Material 3)
AI Integration: Google Generative AI SDK (Gemini 3.5 Flash)
Local Database: Room Persistence Library
Cloud Database & Auth: Firebase (Authentication, Firestore)
Media Storage: Cloudinary (Unsigned Uploads)
Health APIs: Android Health Connect
Concurrency: Kotlin Coroutines & Flows
🚀 Getting Started
Prerequisites
Android Studio Iguana (or newer)
Android device or emulator running API 26+
Setup Instructions
Clone the repository:
bash

git clone https://github.com/yourusername/med-assist.git
Firebase Setup:
Create a new project in the Firebase Console.
Download the google-services.json file and place it in the app/ directory.
Gemini API Setup:
Obtain a Gemini API Key from Google AI Studio.
Create a local.properties file in the root directory (if it doesn't exist) and add your key:
properties

GEMINI_API_KEY=your_api_key_here
Sync the Gradle project and hit Run!
Testing With Your Own Credentials
If you want to test this code locally, you will need to set up your own Firebase and Gemini backend resources since the API keys are kept private:

Firebase: Make sure to enable Firestore Database and Authentication (Email/Password) in your Firebase console. Download the updated google-services.json and replace the one in the app/ folder.
Cloudinary (Optional): The app is pre-configured with a public unsigned upload preset (swasthya_preset), which will work out of the box. However, if you wish to use your own Cloudinary bucket, you can update the cloudName and uploadPreset variables inside the uploadFileToCloudinary function located in app/src/main/java/com/example/swasthya/ui/screens/Screens.kt.
Gemini: A valid Gemini API key is strictly required for the AI Health Insights to function. If you don't provide one in local.properties, the app will crash when attempting to load the AI features.
🔒 Security & Privacy
This app handles sensitive medical data. Medical records and vital logs are stored securely. API Keys (local.properties) and Firebase Configs (google-services.json) are strictly ignored from version control to prevent credential leaks.

Disclaimer: The AI features in this app are designed to assist and summarize data, and should not be used as a replacement for professional medical diagnosis.
