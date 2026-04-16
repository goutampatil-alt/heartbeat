# ❤️ HeartGuard - Cardiac Health Monitoring App

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Backend](https://img.shields.io/badge/Backend-Firebase-orange)
![Status](https://img.shields.io/badge/Status-Active-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

**Real-time cardiac monitoring with automated emergency alerts**

</div>

---

## 📌 Overview

**HeartGuard** is an Android application that monitors vital signs such as heart rate and SpO₂ in real time.
When abnormal readings are detected, the system automatically sends email alerts to a guardian using Firebase Cloud Functions.

> ⚡ Built as a full-stack mobile + cloud project using Firebase

---


---

## ✨ Features

### 🏥 Health Monitoring

* Real-time Heart Rate (BPM) tracking
* SpO₂ (oxygen saturation) monitoring
* Live status updates (SAFE / LOW / HIGH / CRITICAL)

### 🚨 Alert System

* Rule-based risk detection
* Automatic alert creation in Firestore
* Email notifications sent within seconds

### 👥 Guardian Alerts

* Guardian email setup
* Formatted alert emails with patient vitals
* Quick emergency awareness

### 🔐 Authentication & Security

* Google Sign-In (Firebase Auth)
* Secure user-based Firestore access
* No sensitive credentials stored in app

---

## 🏗️ Architecture

```
Android App
   ↓
Cloud Firestore (alerts collection)
   ↓
Firebase Cloud Function (trigger)
   ↓
Gmail SMTP (nodemailer)
   ↓
Guardian receives email
```

---

## ⚙️ Tech Stack

### 📱 Frontend

* Android (Kotlin)
* Firebase SDK
* Material Design

### ☁️ Backend

* Firebase Firestore
* Firebase Authentication
* Firebase Cloud Functions (Node.js)

### 📧 Services

* Gmail SMTP (Nodemailer)
* Firebase Secret Manager

---

## 🚀 Setup

### 1. Clone Repository

```bash
git clone https://github.com/goutampatil-alt/heartguard.git
cd heartguard
```

---

### 2. Firebase Setup

* Create a Firebase project
* Enable **Firestore** and **Authentication (Google Sign-In)**
* Download `google-services.json` → place in `app/`

---

### 3. Cloud Functions Setup

```bash
npm install -g firebase-tools
firebase login
firebase init functions
```

Set email credentials:

```bash
firebase functions:secrets:set EMAIL_SENDER
firebase functions:secrets:set EMAIL_PASSWORD
```

Deploy:

```bash
firebase deploy --only functions
```

---

### 4. Run the App

Open in Android Studio:

```
Build → Run App
```

---

## 📖 Usage

1. Sign in with Google
2. Add guardian email
3. App monitors vitals automatically
4. If abnormal:

   * Alert stored in Firestore
   * Email sent to guardian within seconds

---

## 📊 Database Structure

**alerts collection:**

```json
{
  "fromUid": "...",
  "guardianEmail": "guardian@gmail.com",
  "heartRate": 120,
  "spo2": 92,
  "riskLevel": "HIGH",
  "timestamp": 1234567890
}
```

---

## 🔍 Monitoring

View logs:

```bash
firebase functions:log
```

Check alerts:

```
Firebase Console → Firestore → alerts
```

---

## 🐛 Troubleshooting

**Email not sending**

* Check Gmail app password
* Verify guardian email exists
* Check logs: `firebase functions:log`

**Function deploy fails**

* Ensure Blaze plan enabled
* Run `npm install` in functions folder

---

## 📈 Future Improvements

* SMS alerts (Twilio)
* Push notifications (FCM)
* Multiple guardians
* Health history dashboard

---

## 📄 License

MIT License

---

## 🙌 Acknowledgments

Built using Firebase, Android, and Nodemailer.

---

<div align="center">

**Made for real-time health alert systems ❤️**

</div>
