# Firebase Setup Guide for HeartGuard

## Overview
Your app temporarily has a placeholder `google-services.json` file so it can build. To use Firebase features properly, you need to download the real configuration from Firebase Console.

## 🚀 Steps to Get Real google-services.json

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Enter project name: `heartbeat-app` (or your preferred name)
4. Accept the terms and click "Create project"
5. Wait for project to initialize

### 2. Register Android App
1. In Firebase Console, click the Android icon (or "+ Add app")
2. Fill in Android app details:
   - **Package name**: `com.example.myapplication` (matches your AndroidManifest.xml)
   - **App nickname**: `HeartGuard` (optional)
   - **Debug signing certificate SHA-1**: Get this from Android Studio or below

### 3. Get Your SHA-1 Certificate

#### Option A: From Android Studio
1. Open your project in Android Studio
2. Go to **Gradle** panel on right
3. Expand: `heartbeat` → `Tasks` → `android`
4. Double-click `signingReport`
5. Find your SHA-1 in the output

#### Option B: From Command Line
```powershell
# On Windows PowerShell
cd C:\Users\raksh\Desktop\heartbeat
.\gradlew.bat signingReport
```

Look for `SHA-1:` in the output (for debug build)

### 4. Download google-services.json
1. After entering SHA-1, click "Register app"
2. Click "Download google-services.json"
3. Place the file here: **`app/google-services.json`** ✅

**The file location is critical!** Must be in `app/google-services.json`, NOT in any subdirectories.

### 5. Verify Setup
1. Rebuild your project:
```powershell
cd C:\Users\raksh\Desktop\heartbeat
.\gradlew.bat clean build
```

2. If successful, you'll see:
```
BUILD SUCCESSFUL
```

## 📁 File Location
```
heartbeat/
  app/
    google-services.json  ← Place file HERE
    src/
    build.gradle.kts
```

## 🔧 Firebase Features Enabled

Once you add the real `google-services.json`, these features work:

✅ **Google Sign-In** (Gmail login)
- Users can sign in with Gmail/Google account

✅ **Cloud Firestore**
- Stores user profiles, alerts, and health data

✅ **Firebase Authentication**
- Secure user authentication

✅ **Cloud Messaging**
- Push notifications to devices

## ⚠️ Current Placeholder File
The placeholder `google-services.json` in your app folder is a stub that allows the build to pass without errors. Some Firebase features will show warnings or not work fully until you replace it with the real config.

## 🧪 Testing Firebase

### Test if Firebase is working:
1. Run the app
2. Sign in with Gmail
3. Check if Guardian Setup screen appears
4. Enter nominee phone number
5. Check Firestore Console to verify data was saved

### Verify in Firebase Console:
1. Go to [Firestore Database](https://console.firebase.google.com/project/YOUR-PROJECT/firestore)
2. Look for `users` collection
3. Should see user documents with `nomineePhone` and `guardianEmail`

## 🔑 Security Note
**Never commit real google-services.json to public repositories!** 
- The file contains sensitive configuration
- Add to `.gitignore`: `google-services.json`

```
# .gitignore
google-services.json
*.keystore
*.jks
```

## 📞 Troubleshooting

### Issue: "FirebaseApp not initialized"
- Make sure real `google-services.json` is in `app/` folder
- Rebuild project: `.\gradlew.bat clean build`

### Issue: "Google Sign-In failed"
- Check SHA-1 fingerprint matches Firebase Console
- Restart the app
- Check internet connection

### Issue: "Still getting build error"
1. Delete old placeholder: `rm app/google-services.json`
2. Download fresh one from Firebase Console
3. Place in `app/google-services.json`
4. Clean build: `.\gradlew.bat clean build`

## 📚 Reference Files
- `google-services.json` - Firebase project configuration (created from Firebase Console)
- `app/build.gradle.kts` - Already has Firebase dependencies
- `app/src/main/AndroidManifest.xml` - Has internet permission for Firebase

## ✅ Checklist

- [ ] Create Firebase project
- [ ] Register Android app with correct package name
- [ ] Get SHA-1 certificate fingerprint
- [ ] Download google-services.json from Firebase Console
- [ ] Place file in `app/google-services.json` (exact path)
- [ ] Rebuild project: `.\gradlew.bat clean build`
- [ ] Run app and test sign-in
- [ ] Verify data in Firestore Console

---

**Once you complete these steps, your app will have full Firebase integration!** 🎉

Need help? Check:
- [Firebase Setup Guide](https://firebase.google.com/docs/android/setup)
- [Google Sign-In Setup](https://developers.google.com/identity/sign-in/android/start)
- [Firestore Database Guide](https://firebase.google.com/docs/firestore/quickstart)
