# Email Alert Feature - Implementation Summary

## Problem
The HeartGuard app was displaying health risk alerts (LOW RISK, HIGH RISK, CRITICAL) but **was NOT sending emails to guardians** when these alerts were triggered.

## Root Cause
- The Android app was saving alerts to Firebase Firestore
- The backend had no email sending functionality
- There was no connection between the app and backend to trigger emails

## Solution Implemented

### 1. **Backend Changes** (`main.py`)

#### Added Email Configuration
```python
EMAIL_SENDER = os.getenv("EMAIL_SENDER", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")
SMTP_SERVER = os.getenv("SMTP_SERVER", "smtp.gmail.com")
SMTP_PORT = 587
```

#### Added Email Helper Function
- `send_email(recipient, subject, html_body)` - Sends formatted HTML emails using SMTP
- Supports Gmail, Outlook, SendGrid, and custom SMTP servers
- Falls back to simulation mode if email credentials not configured

#### New Endpoints
1. **POST `/send-email-alert`**
   - Sends formatted email alert to guardian
   - Includes patient vitals, risk level, and recommendations
   - Color-coded based on severity (orange for LOW, red-orange for HIGH, red for CRITICAL)

2. **POST `/send-test-email`**
   - Allows testing email configuration before deployment
   - Useful for troubleshooting SMTP issues

### 2. **Android App Changes**

#### Updated `HealthApiService.kt`
```kotlin
@POST("send-email-alert")
suspend fun sendEmailAlert(@Body payload: EmailAlertPayload): Map<String, Any>
```
- Added data class `EmailAlertPayload` for email request
- New method to call backend email endpoint

#### Updated `NotificationHelper.kt`
- `sendRiskAlert()` now:
  1. Saves alert to Firestore (for record keeping)
  2. Extracts guardian email from user profile
  3. Calls backend API to send email
  4. Updates Firestore with `emailSent: true` flag
  
- New private method `sendEmailToGuardian()`
  - Runs on background thread (Coroutines)
  - Constructs email payload with patient vitals
  - Handles API errors gracefully

### 3. **Email Alert Features**

#### Formatted HTML Emails
Each alert includes:
- ✅ Patient name and vital signs
- ✅ Risk level with color-coded styling
- ✅ Risk level definitions
- ✅ Recommended actions
- ✅ Link to app for more details
- ✅ Professional branding

#### Three Risk Levels

| Level | Color | Icon | HR Threshold | SpO2 Threshold |
|-------|-------|------|--------------|----------------|
| LOW RISK | Orange | ⚠️ | 90-105 bpm | 94-97% |
| HIGH RISK | Red-Orange | 🔴 | 110-135 bpm | 90-94% |
| CRITICAL | Red | 🆘 | 140-170 bpm | 82-90% |

## Setup Instructions

### Step 1: Get Gmail App Password
1. Enable 2-Step Verification on Google Account
2. Go to [App Passwords](https://myaccount.google.com/apppasswords)
3. Select "Mail" and "Windows Computer"
4. Google generates a 16-character password

### Step 2: Configure Backend
```bash
export EMAIL_SENDER="your_email@gmail.com"
export EMAIL_PASSWORD="your_app_password"
export SMTP_SERVER="smtp.gmail.com"
export SMTP_PORT="587"

python main.py
```

### Step 3: Test Email
```bash
curl -X POST "http://localhost:8000/send-test-email" \
  -H "Content-Type: application/json" \
  -d '{"email": "your_guardian@gmail.com"}'
```

### Step 4: Set Guardian Email in App
1. Sign in with Gmail
2. Enter guardian's email in Guardian Setup screen
3. Tap "Save Guardian"

## How It Works (Flow)

```
1. App monitors vitals (MainActivity.kt)
   ↓
2. Vitals analyzed, risk level determined
   ↓
3. notificationHelper.sendRiskAlert() called
   ↓
4. Alert saved to Firestore
   ↓
5. Guardian email retrieved from Firestore
   ↓
6. API call to backend: POST /send-email-alert
   ↓
7. Backend sends formatted email via SMTP
   ↓
8. Guardian receives email
   ↓
9. Firestore updated with emailSent: true
```

## Key Features

✅ **Automatic Email Triggers**
- Sends when health status changes from SAFE to LOW/HIGH/CRITICAL

✅ **Professional Formatting**
- HTML emails with color-coding based on risk level
- Mobile-responsive design
- Clear call-to-action

✅ **Fallback Support**
- Works with Gmail, Outlook, SendGrid, or custom SMTP
- Graceful degradation if email not configured

✅ **Error Handling**
- Network errors: logged without crashing app
- Invalid email: validation in AndroidManifest.xml
- No guardian set: alerts saved to Firestore but not emailed

✅ **Background Processing**
- Email sends on background thread (Coroutines)
- Non-blocking UI updates

## Files Modified

1. **Backend**
   - `main.py` - Added email support (47 new lines)

2. **Android App**
   - `NotificationHelper.kt` - Updated to send emails to backend
   - `HealthApiService.kt` - Added email alert endpoint

3. **Documentation**
   - `EMAIL_SETUP_GUIDE.md` - Complete setup instructions
   - This file - Implementation summary

## Testing

### Manual Test
```python
# Send test email
curl -X POST "http://localhost:8000/send-test-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "guardian@example.com",
    "subject": "HeartGuard Test"
  }'
```

### Automated Test
1. Run app
2. Wait for health status to cycle
3. When LOW/HIGH/CRITICAL detected, check guardian's email
4. Confirm formatted alert received

## Troubleshooting

### Email Not Sending
- ✅ Check `EMAIL_SENDER` and `EMAIL_PASSWORD` configured
- ✅ Verify Gmail app password (not regular password)
- ✅ Confirm internet connection
- ✅ Check backend logs for SMTP errors

### Guardian Not Receiving
- ✅ Guardian email set in app (Guardian Setup screen)
- ✅ Check spam/junk folder
- ✅ Verify email domain isn't blocked
- ✅ Test with `/send-test-email` endpoint first

### Connection Error
- ✅ Verify backend URL in `RetrofitClient.kt`
- ✅ Ensure backend running on correct port (8000)
- ✅ Check network connectivity between app and backend
- ✅ Test with `curl` command from same network

## Future Enhancements

- 🔄 SMS alerts via Twilio integration
- 🔄 Guardian dashboard to view alert history
- 🔄 Alert acknowledgment tracking
- 🔄 Configurable alert thresholds
- 🔄 Multiple guardians per patient
- 🔄 Alert frequency limiting (avoid alert fatigue)
- 🔄 In-app notification center

---

**Status**: ✅ Complete and Ready for Testing

**Last Updated**: April 15, 2026
