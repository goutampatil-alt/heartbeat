# HeartGuard: Gmail Login & SMS Alerts Setup Guide

## 📱 Features Added

### 1. Gmail Login (Google Firebase Authentication) ✅
- Already configured in your app via Firebase
- Users sign in with their Google/Gmail account
- Secure authentication through Firebase

### 2. SMS Alerts for Nominee/Guardian 🆘
- SMS notifications sent when health risks are detected:
  - **LOW RISK**: Minor irregularities (slightly elevated HR or low SpO2)
  - **HIGH RISK**: Significant anomalies requiring attention
  - **CRITICAL**: Immediate danger requiring emergency help

## 📋 Setup Instructions

### Part 1: Android App Configuration

#### Step 1.1: Update Your `build.gradle.kts` Dependencies ✅
Already added:
```gradle
// Twilio SMS Service
implementation("com.twilio.sdk:twilio:9.0.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")
```

#### Step 1.2: Update AndroidManifest.xml ✅
Already added SMS permissions:
```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

#### Step 1.3: Configure SMS Service URL
Edit `SMSService.kt` line 14:
```kotlin
private const val BACKEND_URL = "http://10.0.2.2:8000" // For emulator
// For real device, replace with: "http://<YOUR_SERVER_IP>:8000"
```

**For Real Device:**
- Replace `10.0.2.2` with your backend server's actual IP
- Example: `"http://192.168.1.100:8000"`

### Part 2: Backend Setup (FastAPI)

#### Step 2.1: Install Python Dependencies
```bash
pip install python-multipart
pip install twilio  # Optional, for real SMS
pip install python-dotenv  # For environment variables
```

#### Step 2.2: Configure Twilio (Optional - for Real SMS)

**Option A: Demo Mode (No Twilio needed)**
- SMS will be simulated in console output
- Perfect for testing without costs

**Option B: Real SMS via Twilio**
1. Create a Twilio account: https://www.twilio.com
2. Get your credentials:
   - Account SID
   - Auth Token
   - Phone Number (purchased from Twilio)

3. Set Environment Variables:
   ```bash
   # On Windows (PowerShell)
   $env:TWILIO_ACCOUNT_SID="your_account_sid"
   $env:TWILIO_AUTH_TOKEN="your_auth_token"
   $env:TWILIO_PHONE_NUMBER="+1234567890"
   
   # On Linux/Mac (.env file)
   export TWILIO_ACCOUNT_SID="your_account_sid"
   export TWILIO_AUTH_TOKEN="your_auth_token"
   export TWILIO_PHONE_NUMBER="+1234567890"
   ```

4. Or create a `.env` file in your project root:
   ```
   TWILIO_ACCOUNT_SID=your_account_sid
   TWILIO_AUTH_TOKEN=your_auth_token
   TWILIO_PHONE_NUMBER=+1234567890
   ```

#### Step 2.3: Run the Backend
```bash
cd c:\Users\raksh\Desktop\heartbeat
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at: `http://localhost:8000`

### Part 3: Firebase Configuration

Your app already uses Firebase for:
- Authentication (Google Sign-In)
- Firestore (storing alerts)
- Cloud Messaging

The SMS feature is a **supplement** to Firebase alerts.

### Part 4: User Setup Flow

#### For Patients:
1. **Login** → Sign in with Gmail (Google account)
2. **Guardian Setup** → 
   - Enter guardian's email (optional)
   - Enter **nominee's phone number** for SMS alerts (required)
   - Click "Test SMS" to verify phone number works
   - Click "Save"

#### For Guardians:
- They receive Firebase notifications + SMS alerts when patient has LOW RISK, HIGH RISK, or CRITICAL status

## 📱 How SMS Alerts Work

```
Patient Health Status Changes
    ↓
MainActivity calls NotificationHelper.sendRiskAlert()
    ↓
Alert written to Firestore (for Firebase)
    ↓
SMSService.sendRiskAlertSMS() sends HTTP request to backend
    ↓
Backend receives request
    ↓
[Demo Mode] → Print to console
[Twilio Mode] → Send real SMS via Twilio
    ↓
SMS delivered to nominee's phone ✅
```

## 🔍 API Endpoints

### 1. Send Risk Alert SMS
**POST** `/send-sms-alert`

Request:
```json
{
  "phone": "+1234567890",
  "risk_level": "HIGH RISK",
  "heart_rate": 125,
  "spo2": 92,
  "patient_name": "John Doe",
  "patient_email": "john@example.com"
}
```

Response (Success):
```json
{
  "success": true,
  "message": "SMS sent to +1234567890",
  "sms_id": "SM1234abcd",
  "risk_level": "HIGH RISK"
}
```

### 2. Send Test SMS
**POST** `/send-test-sms`

Request:
```json
{
  "phone": "+1234567890",
  "message": "Optional custom message"
}
```

### 3. Health Check
**GET** `/health`

Returns current status and latest health data.

## 🧪 Testing

### Test 1: Test SMS from App
1. Go to Guardian Setup screen
2. Enter nominee phone: `+1234567890` (or real number)
3. Click "Test SMS"
4. Check console/phone for message

### Test 2: Trigger Risk Alerts
1. Open the app
2. Watch as status cycles: SAFE → LOW RISK → HIGH RISK → CRITICAL
3. When status changes to LOW/HIGH/CRITICAL:
   - Dialog appears on screen (for CRITICAL only)
   - SMS sent to nominee
   - Alert stored in Firestore

### Test 3: Verify Backend
```bash
curl http://localhost:8000/health
```

Should return:
```json
{
  "status": "healthy",
  "sms_enabled": true/false,
  "latest_data": {...}
}
```

## 📧 Gmail Login Notes

Your app uses **Google Firebase Authentication**:
- Users sign in with any Google account (including Gmail)
- No additional credentials needed
- Already configured in `LoginActivity.kt`
- Requires valid `google-services.json` from Firebase Console

## 🚀 Deployment

### For Production:
1. Update SMSService backend URL to your server
2. Configure real Twilio credentials
3. Test SMS sending before launch
4. Ensure HTTPS for real device communication
5. Store API keys securely (use environment variables, not hardcoded)

### Example Production URL:
```kotlin
private const val BACKEND_URL = "https://api.yourserver.com"
```

## ⚠️ Important Notes

1. **Phone Number Format**: Must include country code
   - ✅ Valid: `+1234567890`, `+91-987-654-3210`
   - ❌ Invalid: `1234567890`, `987 654 3210`

2. **SMS Costs**: Twilio charges per SMS (~$0.01 USD each)
   - Demo mode is free (simulated)
   - Enable Twilio only when ready to send real SMS

3. **Backend Timeout**: Emulator may need longer timeout
   - Add timeout in SMSService if needed

4. **Firebase Setup**: Make sure to add your Firebase configuration
   - Download `google-services.json` from Firebase Console
   - Place it in `app/` folder

## 🔐 Security Best Practices

1. ✅ Never hardcode API keys in app code
2. ✅ Use environment variables for Twilio credentials
3. ✅ Always validate phone numbers before sending SMS
4. ✅ Implement rate limiting to prevent spam
5. ✅ Encrypt sensitive data at rest

## 📞 Troubleshooting

### Issue: SMS not sending
- **Check**: Backend URL is correct in SMSService.kt
- **Check**: Backend is running (`python main.py`)
- **Check**: Phone number format is correct (with +country code)
- **Check**: Twilio credentials are valid (if using real SMS)

### Issue: "Cannot connect to backend"
- **Android Emulator**: Use `10.0.2.2` as localhost
- **Real Device**: Use your computer's IP (e.g., `192.168.x.x`)
- **Firewall**: Allow port 8000 through firewall

### Issue: Gmail login not working
- **Check**: `google-services.json` is in `app/` folder
- **Check**: Firebase project ID matches in google-services.json
- **Check**: Google Play Services installed on device

## 📚 References

- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Twilio SMS Documentation](https://www.twilio.com/docs/sms)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Android SMS Permissions](https://developer.android.com/guide/topics/permissions/overview)

## 🎯 Next Steps

1. ✅ Update gradle dependencies
2. ✅ Configure backend URL
3. ✅ Test with demo mode (no Twilio)
4. ✅ Verify SMS in console
5. ⏭️ (Optional) Get Twilio account for real SMS
6. ⏭️ Configure Twilio credentials
7. ⏭️ Deploy to production

---

**Your app is now ready for Gmail login and SMS risk alerts!** 🎉

For questions, check the implementation in:
- `SMSService.kt` - SMS API calls
- `NotificationHelper.kt` - Alert logic
- `GuardianSetupActivity.kt` - Phone number setup
- `main.py` - Backend SMS endpoints
