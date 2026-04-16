# HeartGuard: Email Alert Setup Guide

## Overview
The app now sends **email alerts to guardians** when a patient experiences low risk, high risk, or critical health situations.

## How It Works

1. **Patient App Monitors Health**: The Android app continuously monitors heart rate and SpO2
2. **Risk Detected**: When abnormal vitals are detected (LOW RISK, HIGH RISK, or CRITICAL)
3. **Alert Triggered**: The app records the alert in Firestore and calls the backend API
4. **Email Sent**: The backend sends a formatted email to the guardian's email address
5. **Guardian Notified**: Guardian receives an email with patient vitals and risk details

## Setup Requirements

### 1. **Backend Setup (Python)**

#### Configure Email Credentials
Set these environment variables on your backend server:

```bash
# Using Gmail (recommended)
export EMAIL_SENDER="your_email@gmail.com"
export EMAIL_PASSWORD="your_app_password"
export SMTP_SERVER="smtp.gmail.com"
export SMTP_PORT="587"
```

#### Get Gmail App Password
Since Gmail requires an app-specific password (not your regular password):

1. Go to [Google Account Settings](https://myaccount.google.com)
2. Enable **2-Step Verification** (if not already enabled)
3. Go to **App passwords** section
4. Select "Mail" and "Windows Computer" (or your device)
5. Generate an app password (Gmail will give you a 16-character password)
6. Copy this password to `EMAIL_PASSWORD`

**Example:**
```bash
export EMAIL_SENDER="heartguard.alerts@gmail.com"
export EMAIL_PASSWORD="abcd efgh ijkl mnop"  # App password
export SMTP_SERVER="smtp.gmail.com"
export SMTP_PORT="587"
```

#### Run Backend with Email
```bash
# Install email dependency (if not already installed)
pip install python-dotenv

# Create .env file
echo 'EMAIL_SENDER=your_email@gmail.com' > .env
echo 'EMAIL_PASSWORD=your_app_password' >> .env
echo 'SMTP_SERVER=smtp.gmail.com' >> .env
echo 'SMTP_PORT=587' >> .env

# Run the backend
python main.py
# or with .env file
python -m uvicorn main:app --reload
```

### 2. **Android App Setup**

#### Guardian Email Configuration
1. Open the app and **Sign in with Gmail**
2. On the **Guardian Setup** screen, enter your guardian's email address
3. Tap **Save Guardian**
4. Guardian email is now saved to Firestore

#### Verify Backend Connection
- The app automatically connects to `http://10.41.74.246:8000/` (edit in [RetrofitClient.kt](app/src/main/java/com/example/myapplication/RetrofitClient.kt) if different)
- When a health alert is triggered, the app sends the email request to the backend

### 3. **Test Email Alerts**

#### Option A: Send Test Email via Backend
```bash
curl -X POST "http://localhost:8000/send-test-email" \
  -H "Content-Type: application/json" \
  -d '{"email": "guardian@example.com", "subject": "HeartGuard Test Email"}'
```

#### Option B: Trigger Test Alert in App
1. Open the app
2. The app cycles through health statuses: SAFE → LOW RISK → HIGH RISK → CRITICAL
3. When a risk level is detected, an email is automatically sent to the guardian

### 4. **Monitor Email Logs**

Check the backend console output:
```
==================================================
📧 EMAIL ALERT
==================================================
To: guardian@example.com
Patient: John Doe
Risk: HIGH RISK
HR: 125 bpm, SpO2: 91%
==================================================
```

---

## Email Alert Formats

### LOW RISK Alert
- **Color**: Orange (#FFB300)
- **Icon**: ⚠️
- **Use Case**: Minor abnormality (HR slightly elevated, SpO2 slightly low)

### HIGH RISK Alert
- **Color**: Red-Orange (#FF6B35)
- **Icon**: 🔴
- **Use Case**: Significant abnormality (HR highly elevated, SpO2 below 94%)

### CRITICAL Alert
- **Color**: Red (#FF3B3B)
- **Icon**: 🆘
- **Use Case**: Dangerous condition (HR >140 bpm, SpO2 <90%)

---

## Troubleshooting

### "Email notifications disabled" Message
**Problem**: Backend shows `⚠️ Email notifications disabled`

**Solution**:
1. Check that `EMAIL_SENDER` and `EMAIL_PASSWORD` environment variables are set
2. Verify Gmail app password is correct (not regular Gmail password)
3. Ensure SMTP server is reachable (Gmail uses `smtp.gmail.com:587`)

### Email Not Sent
**Problem**: Risk alert detected but guardian didn't receive email

**Checklist**:
1. ✅ Guardian email is set in app (Guardian Setup screen)
2. ✅ Backend has EMAIL_SENDER and EMAIL_PASSWORD configured
3. ✅ Internet connection available on backend and app
4. ✅ Check backend logs for error messages
5. ✅ Check spam/junk folder in guardian's email

### "Failed to send SMS" or "Failed to send email"
**Problem**: Backend error when sending

**Solution**:
1. Verify email credentials are correct:
   ```bash
   echo $EMAIL_SENDER
   echo $EMAIL_PASSWORD
   ```
2. Try sending a test email:
   ```bash
   curl -X POST "http://localhost:8000/send-test-email" \
     -H "Content-Type: application/json" \
     -d '{"email": "test@gmail.com"}'
   ```
3. Check Gmail "Less secure apps" settings (if using regular account)
4. Ensure Gmail 2-Step Verification is enabled before generating app password

---

## API Reference

### POST `/send-email-alert`
Sends an email alert to a guardian.

**Request Body**:
```json
{
  "guardian_email": "guardian@example.com",
  "patient_name": "John Doe",
  "patient_email": "patient@example.com",
  "risk_level": "HIGH RISK",
  "heart_rate": 125,
  "spo2": 91,
  "timestamp": 1681234567890
}
```

**Response**:
```json
{
  "success": true,
  "message": "Email sent to guardian@example.com",
  "recipient": "guardian@example.com",
  "risk_level": "HIGH RISK"
}
```

### POST `/send-test-email`
Sends a test email to verify email configuration.

**Request Body**:
```json
{
  "email": "test@example.com",
  "subject": "HeartGuard: Test Email"
}
```

---

## Alternative Email Providers

If you can't use Gmail, you can configure other providers:

### **SendGrid** (Recommended for production)
```bash
export SMTP_SERVER="smtp.sendgrid.net"
export SMTP_PORT="587"
export EMAIL_SENDER="apikey"
export EMAIL_PASSWORD="SG.your_sendgrid_api_key"
```

### **Outlook/Hotmail**
```bash
export SMTP_SERVER="smtp-mail.outlook.com"
export SMTP_PORT="587"
export EMAIL_SENDER="your_email@outlook.com"
export EMAIL_PASSWORD="your_app_password"
```

### **Custom SMTP Server**
Just update `SMTP_SERVER` and `SMTP_PORT` with your server details.

---

## Files Modified

- **Backend**: `main.py` - Added email configuration and endpoints
- **Android App**:
  - `NotificationHelper.kt` - Updated to send email to backend API
  - `HealthApiService.kt` - Added `sendEmailAlert()` endpoint
  - `RetrofitClient.kt` - Already configured (no changes needed)

---

## What's Next

- ✅ Email alerts on health risk detection
- 🔄 SMS alerts (via Twilio) - Configure `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN`
- 🔄 Push notifications (FCM) - Already partially implemented
- 🔄 Guardian dashboard to view alert history
- 🔄 Alert acknowledgment tracking

---

## Questions or Issues?

Check the logs in your backend console for detailed error messages. All email sends are logged with the recipient, subject, and status (success/failure).
