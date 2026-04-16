# HeartGuard: Firebase Cloud Functions Setup

## Overview
Instead of running a backend on your laptop, we're using **Firebase Cloud Functions** to automatically send emails. When the app saves an alert to Firestore, the Cloud Function triggers and sends the email.

**Benefits:**
- ✅ No backend on laptop needed
- ✅ Automatic email sending
- ✅ Secure (credentials in Firebase, not in app)
- ✅ Works 24/7

---

## Prerequisites

1. **Firebase CLI installed**: https://firebase.google.com/docs/cli
2. **Node.js installed**: https://nodejs.org (version 18 or higher)
3. **Gmail app password** (from earlier setup)

---

## Step 1: Install Firebase CLI

```powershell
npm install -g firebase-tools
```

Verify installation:
```powershell
firebase --version
```

---

## Step 2: Log in to Firebase

```powershell
firebase login
```

This opens a browser to sign in with your Google account.

---

## Step 3: Initialize Cloud Functions in Your Project

```powershell
cd c:\Users\raksh\Desktop\heartbeat

firebase init functions
```

**Choose these options:**
- Project: **heartguard-ce711** (or your Firebase project)
- Runtime: **Node.js**
- ESLint: **No**
- Install dependencies: **Yes**

This creates a `functions/` folder.

---

## Step 4: Set Up Email Environment Variables

In Firebase Console:

1. Go to: https://console.firebase.google.com
2. Select **heartguard-ce711** project
3. Go to **Functions** (left sidebar)
4. Click **Runtime settings** (gear icon)
5. Set environment variables:

```
EMAIL_SENDER = goutampatil2007@gmail.com
EMAIL_PASSWORD = rbwj qcyh vbjy ixms
```

(Use your actual Gmail app password)

**OR** use Firebase CLI:

```powershell
firebase functions:config:set email.sender="goutampatil2007@gmail.com" email.password="rbwj qcyh vbjy ixms"

firebase functions:config:set smtp.server="smtp.gmail.com" smtp.port="587"
```

---

## Step 5: Replace Cloud Function Files

The `functions/` folder was created. Now replace the files:

```powershell
# Copy the new function files to the functions folder
# The function code is in: c:\Users\raksh\Desktop\heartbeat\functions\index.js
# The package.json is in: c:\Users\raksh\Desktop\heartbeat\functions\package.json
```

---

## Step 6: Deploy to Firebase

```powershell
cd c:\Users\raksh\Desktop\heartbeat

firebase deploy --only functions
```

**You should see:**
```
✔  Deploy complete!

Function URL (sendEmailAlert): https://us-central1-heartguard-ce711.cloudfunctions.net/sendEmailAlert
Function URL (sendTestEmail): https://us-central1-heartguard-ce711.cloudfunctions.net/sendTestEmail
```

**Copy the `sendTestEmail` URL** - you'll need it next.

---

## Step 7: Test the Cloud Function

```powershell
$body = @{
    email = "goutampatil2007@gmail.com"
} | ConvertTo-Json

Invoke-WebRequest -Uri "https://us-central1-heartguard-ce711.cloudfunctions.net/sendTestEmail" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body -UseBasicParsing
```

**Check your email!** You should receive a test email.

---

## Step 8: Update Android App

The app is already updated! It just saves alerts to Firestore.

**No need to change:**
- ✅ RetrofitClient.kt - URL doesn't matter anymore
- ✅ NotificationHelper.kt - Already updated

---

## How It Works Now

```
1. App detects risk
   ↓
2. App saves alert to Firestore
   ↓
3. Cloud Function automatically triggers
   ↓
4. Cloud Function sends email to guardian
   ↓
5. Cloud Function updates alert with "emailSent: true"
   ↓
6. Guardian receives email (no laptop needed!)
```

---

## Testing on Your Phone

1. **Keep laptop off** (Cloud Functions run in Google's servers)
2. Open app
3. Sign in with Gmail
4. Go to Guardian Setup → Enter a guardian email → Save
5. Wait for app to cycle through health statuses
6. When LOW RISK/HIGH RISK/CRITICAL is detected → Email sent automatically!

---

## Troubleshooting

### "Deploy failed"
- Check: `firebase login` successful?
- Check: Project ID correct?
- Check: Firebase CLI installed?

```powershell
firebase projects:list
```

### "Email not sending"
- Check environment variables set in Firebase Console
- Check Gmail app password is correct
- View logs:
  ```powershell
  firebase functions:log
  ```

### "Function not found"
- Redeploy:
  ```powershell
  firebase deploy --only functions --force
  ```

---

## Viewing Logs

See what the Cloud Function is doing:

```powershell
firebase functions:log
```

Or in Firebase Console:
1. Go to **Functions** → **Logs**
2. Select `sendEmailAlert` function
3. View all email sends in real-time

---

## Monitoring Alerts

In Firebase Console:

1. Go to **Firestore Database**
2. Open **alerts** collection
3. See all alerts with their status:
   - `emailSent: false` → Pending
   - `emailSent: true` → Sent successfully

---

## Files Modified

- `functions/index.js` - Cloud Functions code
- `functions/package.json` - Dependencies
- `app/.../NotificationHelper.kt` - Now just saves to Firestore

---

## What's Next

- ✅ Email alerts via Cloud Functions
- 🔄 SMS alerts (optional - needs Twilio)
- 🔄 Guardian dashboard to view alerts
- 🔄 Alert history and statistics

---

## Questions?

Check the logs:
```powershell
firebase functions:log
```

Or view in Firebase Console → Functions → Logs
