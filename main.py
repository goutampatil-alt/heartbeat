from fastapi import FastAPI
import random
import math
import time
import threading
from pydantic import BaseModel
from datetime import datetime
import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# For SMS notifications (optional - comment out if not using Twilio)
# Install: pip install twilio
try:
    from twilio.rest import Client
    TWILIO_AVAILABLE = True
except ImportError:
    TWILIO_AVAILABLE = False
    print("⚠️  Twilio not installed. SMS features will be simulated.")

app = FastAPI()

latest_data = {
    "heart_rate": 80,
    "spo2": 98,
    "status": "SAFE",
    "confidence": 0.2,
    "ecg": []
}

# ==========================================
# Twilio Configuration (Optional)
# ==========================================
# Set these environment variables:
# TWILIO_ACCOUNT_SID="your_account_sid"
# TWILIO_AUTH_TOKEN="your_auth_token"
# TWILIO_PHONE_NUMBER="+1234567890"
TWILIO_ACCOUNT_SID = os.getenv("TWILIO_ACCOUNT_SID", "")
TWILIO_AUTH_TOKEN = os.getenv("TWILIO_AUTH_TOKEN", "")
TWILIO_PHONE_NUMBER = os.getenv("TWILIO_PHONE_NUMBER", "+1234567890")

if TWILIO_AVAILABLE and TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN:
    twilio_client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
else:
    twilio_client = None


# ==========================================
# Email Configuration
# ==========================================
# Set these environment variables to enable email:
# EMAIL_SENDER="your_email@gmail.com"
# EMAIL_PASSWORD="your_app_password"  (Gmail app password, not regular password)
# SMTP_SERVER="smtp.gmail.com"
# SMTP_PORT=587
#
# For Gmail: https://myaccount.google.com/apppasswords
# Generate an app-specific password for this app
EMAIL_SENDER = os.getenv("EMAIL_SENDER", "")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD", "")
SMTP_SERVER = os.getenv("SMTP_SERVER", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))

EMAIL_AVAILABLE = bool(EMAIL_SENDER and EMAIL_PASSWORD)

if EMAIL_AVAILABLE:
    print("✅ Email notifications enabled")
else:
    print("⚠️  Email notifications disabled. Set EMAIL_SENDER and EMAIL_PASSWORD to enable.")


# ==========================================
# Pydantic Models
# ==========================================
class SMSAlertRequest(BaseModel):
    phone: str
    risk_level: str
    heart_rate: int
    spo2: int
    patient_name: str
    patient_email: str
    timestamp: int


class TestSMSRequest(BaseModel):
    phone: str
    message: str | None = None


class EmailAlertRequest(BaseModel):
    guardian_email: str
    patient_name: str
    patient_email: str
    risk_level: str
    heart_rate: int
    spo2: int
    timestamp: int


class TestEmailRequest(BaseModel):
    email: str
    subject: str | None = None

# Simulated ECG waveform generator (PQRST pattern)
def generate_ecg_beat(heart_rate: int) -> list:
    """Generate a single PQRST complex waveform."""
    samples = []
    beat_duration = 60.0 / heart_rate  # seconds per beat
    num_samples = 50  # samples per beat

    for i in range(num_samples):
        t = i / num_samples

        # P wave (small bump at ~10-20% of cycle)
        p_wave = 0.15 * math.exp(-((t - 0.12) ** 2) / (2 * 0.01 ** 2))

        # Q wave (small dip before R)
        q_wave = -0.1 * math.exp(-((t - 0.28) ** 2) / (2 * 0.005 ** 2))

        # R wave (tall spike at ~30% of cycle)
        r_wave = 1.0 * math.exp(-((t - 0.32) ** 2) / (2 * 0.008 ** 2))

        # S wave (dip after R)
        s_wave = -0.2 * math.exp(-((t - 0.36) ** 2) / (2 * 0.008 ** 2))

        # T wave (broad bump at ~55% of cycle)
        t_wave = 0.25 * math.exp(-((t - 0.55) ** 2) / (2 * 0.025 ** 2))

        value = p_wave + q_wave + r_wave + s_wave + t_wave

        # Add subtle noise for realism
        noise = random.gauss(0, 0.02)
        samples.append(round(value + noise, 4))

    return samples


def generate_data():
    global latest_data

    cycle = 0
    while True:
        cycle += 1

        # Create more realistic status transitions
        # Mostly SAFE, occasionally WARNING, rarely DANGER
        prob = random.uniform(0, 1)

        if prob > 0.90:
            status = "DANGER"
            heart_rate = random.randint(120, 180)
            spo2 = random.randint(80, 90)
        elif prob > 0.70:
            status = "WARNING"
            heart_rate = random.randint(100, 130)
            spo2 = random.randint(90, 95)
        else:
            status = "SAFE"
            heart_rate = random.randint(65, 95)
            spo2 = random.randint(95, 100)

        # Generate 2 beats worth of ECG data
        ecg_data = generate_ecg_beat(heart_rate) + generate_ecg_beat(heart_rate)

        # Add anomalies in DANGER mode
        if status == "DANGER":
            for i in range(len(ecg_data)):
                ecg_data[i] += random.gauss(0, 0.1)  # more noise
                ecg_data[i] = round(ecg_data[i], 4)

        latest_data = {
            "heart_rate": heart_rate,
            "spo2": spo2,
            "status": status,
            "confidence": round(prob, 3),
            "ecg": ecg_data
        }

        print(f"[{status}] HR: {heart_rate} | SpO2: {spo2}% | Confidence: {round(prob, 3)}")
        time.sleep(2)


@app.get("/latest-data")
def get_data():
    return latest_data


# ==========================================
# Email Helper Function
# ==========================================
def send_email(recipient: str, subject: str, html_body: str) -> dict:
    """Send an email using Gmail SMTP."""
    if not EMAIL_AVAILABLE:
        print(f"⚠️  Email disabled. Simulating email to {recipient}")
        return {
            "success": True,
            "message": f"Email simulated to {recipient} (email not configured)",
            "email_id": f"sim_{int(time.time())}",
            "simulated": True
        }
    
    try:
        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT)
        server.starttls()
        server.login(EMAIL_SENDER, EMAIL_PASSWORD)
        
        msg = MIMEMultipart("alternative")
        msg["Subject"] = subject
        msg["From"] = EMAIL_SENDER
        msg["To"] = recipient
        
        # Attach HTML content
        msg.attach(MIMEText(html_body, "html"))
        
        # Send the email
        server.sendmail(EMAIL_SENDER, recipient, msg.as_string())
        server.quit()
        
        print(f"✅ Email sent successfully to {recipient}")
        return {
            "success": True,
            "message": f"Email sent to {recipient}",
            "recipient": recipient
        }
    except Exception as e:
        print(f"❌ Email failed: {str(e)}")
        return {
            "success": False,
            "error": str(e),
            "message": f"Failed to send email to {recipient}"
        }


# ==========================================
# SMS Alert Endpoints
# ==========================================
@app.post("/send-sms-alert")
def send_sms_alert(request: SMSAlertRequest):
    """
    Send SMS alert to nominee/guardian when patient has health risk.
    
    Risk Levels:
    - LOW RISK: Heart rate or SpO2 slightly abnormal
    - HIGH RISK: Significant abnormality detected
    - CRITICAL: Immediate medical attention needed
    """
    phone = request.phone
    risk_level = request.risk_level
    heart_rate = request.heart_rate
    spo2 = request.spo2
    patient_name = request.patient_name
    
    # Create alert message based on risk level
    emoji = {
        "LOW RISK": "⚠️",
        "HIGH RISK": "🔴",
        "CRITICAL": "🆘"
    }.get(risk_level, "📢")
    
    message = f"""
{emoji} HeartGuard Alert - {risk_level}

Patient: {patient_name}
Heart Rate: {heart_rate} bpm
SpO2: {spo2}%

Please check on the patient immediately.
Access app for more details.
""".strip()
    
    print(f"\n{'='*50}")
    print(f"📱 SMS ALERT")
    print(f"{'='*50}")
    print(f"To: {phone}")
    print(f"Risk: {risk_level}")
    print(f"Message:\n{message}")
    print(f"{'='*50}\n")
    
    try:
        if twilio_client:
            # Send via Twilio (real SMS)
            msg = twilio_client.messages.create(
                body=message,
                from_=TWILIO_PHONE_NUMBER,
                to=phone
            )
            print(f"✅ SMS sent successfully (Twilio SID: {msg.sid})")
            return {
                "success": True,
                "message": f"SMS sent to {phone}",
                "sms_id": msg.sid,
                "risk_level": risk_level
            }
        else:
            # Simulate SMS (development mode)
            print(f"✅ SMS simulated (demo mode)")
            return {
                "success": True,
                "message": f"SMS simulated to {phone} (demo mode)",
                "sms_id": f"sim_{int(time.time())}",
                "risk_level": risk_level
            }
    except Exception as e:
        print(f"❌ SMS failed: {str(e)}")
        return {
            "success": False,
            "error": str(e),
            "message": f"Failed to send SMS to {phone}"
        }


@app.post("/send-test-sms")
def send_test_sms(request: TestSMSRequest):
    """
    Send test SMS to verify phone number configuration.
    Useful for testing before patient health alerts.
    """
    phone = request.phone
    message = request.message or "HeartGuard: This is a test message to verify your phone number for health alerts."
    
    print(f"\n{'='*50}")
    print(f"📱 TEST SMS")
    print(f"{'='*50}")
    print(f"To: {phone}")
    print(f"Message:\n{message}")
    print(f"{'='*50}\n")
    
    try:
        if twilio_client:
            # Send via Twilio (real SMS)
            msg = twilio_client.messages.create(
                body=message,
                from_=TWILIO_PHONE_NUMBER,
                to=phone
            )
            print(f"✅ Test SMS sent successfully (Twilio SID: {msg.sid})")
            return {
                "success": True,
                "message": f"Test SMS sent to {phone}",
                "sms_id": msg.sid
            }
        else:
            # Simulate SMS (development mode)
            print(f"✅ Test SMS simulated (demo mode)")
            return {
                "success": True,
                "message": f"Test SMS simulated to {phone} (demo mode)",
                "sms_id": f"sim_test_{int(time.time())}"
            }
    except Exception as e:
        print(f"❌ Test SMS failed: {str(e)}")
        return {
            "success": False,
            "error": str(e),
            "message": f"Failed to send test SMS to {phone}"
        }


@app.get("/health")
def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "sms_enabled": bool(twilio_client),
        "email_enabled": EMAIL_AVAILABLE,
        "latest_data": latest_data
    }


# ==========================================
# Email Alert Endpoints
# ==========================================
@app.post("/send-email-alert")
def send_email_alert(request: EmailAlertRequest):
    """
    Send email alert to guardian when patient has health risk.
    
    Risk Levels:
    - LOW RISK: Heart rate or SpO2 slightly abnormal
    - HIGH RISK: Significant abnormality detected
    - CRITICAL: Immediate medical attention needed
    """
    guardian_email = request.guardian_email
    patient_name = request.patient_name
    risk_level = request.risk_level
    heart_rate = request.heart_rate
    spo2 = request.spo2
    
    # Determine email styling based on risk level
    risk_colors = {
        "LOW RISK": "#FFB300",
        "HIGH RISK": "#FF6B35",
        "CRITICAL": "#FF3B3B"
    }
    color = risk_colors.get(risk_level, "#FF9800")
    
    emoji_map = {
        "LOW RISK": "⚠️",
        "HIGH RISK": "🔴",
        "CRITICAL": "🆘"
    }
    emoji = emoji_map.get(risk_level, "📢")
    
    subject = f"{emoji} HeartGuard Alert - {risk_level}"
    
    html_body = f"""
    <html>
        <head>
            <style>
                body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
                .container {{ max-width: 500px; margin: 0 auto; padding: 20px; border-radius: 8px; background-color: #f9f9f9; }}
                .header {{ background-color: {color}; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }}
                .header h1 {{ margin: 0; font-size: 24px; }}
                .content {{ background-color: white; padding: 20px; }}
                .alert-box {{ background-color: {color}; color: white; padding: 15px; border-radius: 5px; margin: 15px 0; }}
                .vital {{ margin: 10px 0; }}
                .vital-label {{ font-weight: bold; color: {color}; }}
                .footer {{ background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 8px 8px; }}
                .button {{ background-color: {color}; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; display: inline-block; margin-top: 15px; }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>{emoji} HeartGuard Alert</h1>
                    <p style="margin: 10px 0 0 0; font-size: 16px;">{risk_level}</p>
                </div>
                
                <div class="content">
                    <p>Dear Guardian,</p>
                    
                    <p>We are writing to alert you about a potential health concern with <strong>{patient_name}</strong>.</p>
                    
                    <div class="alert-box">
                        <p style="margin: 0; font-weight: bold;">⚠️ {risk_level} Alert Detected</p>
                    </div>
                    
                    <h3 style="color: {color}; margin-top: 20px;">Patient Vitals:</h3>
                    <div class="vital">
                        <span class="vital-label">Heart Rate:</span> {heart_rate} bpm
                    </div>
                    <div class="vital">
                        <span class="vital-label">SpO2 Level:</span> {spo2}%
                    </div>
                    
                    <p style="margin-top: 20px; color: #666;">
                        <strong>Risk Level Details:</strong>
                    </p>
                    <ul style="color: #666;">
                        <li><strong>LOW RISK:</strong> Minor abnormality detected. Continue monitoring.</li>
                        <li><strong>HIGH RISK:</strong> Significant abnormality. Medical consultation recommended.</li>
                        <li><strong>CRITICAL:</strong> Severe condition. Seek immediate medical attention.</li>
                    </ul>
                    
                    <p style="margin-top: 20px; padding: 15px; background-color: #fff3cd; border-left: 4px solid {color}; border-radius: 3px;">
                        <strong>Recommended Action:</strong><br>
                        Please contact the patient immediately and consider seeking medical assistance if symptoms persist.
                    </p>
                    
                    <p style="text-align: center; margin-top: 20px;">
                        <a href="https://heartguard.app" class="button">View Full Details in App</a>
                    </p>
                </div>
                
                <div class="footer">
                    <p style="margin: 0;">HeartGuard - Automated Health Monitoring System</p>
                    <p style="margin: 5px 0 0 0;">This is an automated alert. Patient: {patient_name}</p>
                </div>
            </div>
        </body>
    </html>
    """
    
    print(f"\n{'='*50}")
    print(f"📧 EMAIL ALERT")
    print(f"{'='*50}")
    print(f"To: {guardian_email}")
    print(f"Patient: {patient_name}")
    print(f"Risk: {risk_level}")
    print(f"HR: {heart_rate} bpm, SpO2: {spo2}%")
    print(f"{'='*50}\n")
    
    result = send_email(guardian_email, subject, html_body)
    result["risk_level"] = risk_level
    return result


@app.post("/send-test-email")
def send_test_email(request: TestEmailRequest):
    """
    Send test email to verify email configuration.
    Useful for testing before patient health alerts.
    """
    recipient = request.email
    subject = request.subject or "HeartGuard: Test Email"
    
    html_body = f"""
    <html>
        <head>
            <style>
                body {{ font-family: Arial, sans-serif; }}
                .container {{ max-width: 400px; margin: 0 auto; padding: 20px; }}
                .box {{ background-color: #f0f8ff; padding: 20px; border-radius: 8px; border-left: 4px solid #2196F3; }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="box">
                    <h2>✅ HeartGuard Test Email</h2>
                    <p>This is a test email to verify your email configuration is working correctly.</p>
                    <p><strong>Status:</strong> Email system is ready for health alerts.</p>
                    <p style="color: #666; font-size: 12px; margin-top: 20px;">
                        Sent on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
                    </p>
                </div>
            </div>
        </body>
    </html>
    """
    
    print(f"\n{'='*50}")
    print(f"📧 TEST EMAIL")
    print(f"{'='*50}")
    print(f"To: {recipient}")
    print(f"Subject: {subject}")
    print(f"{'='*50}\n")
    
    return send_email(recipient, subject, html_body)


# ==========================================
# Test/Health Endpoints
# ==========================================


threading.Thread(target=generate_data, daemon=True).start()