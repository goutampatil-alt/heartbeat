const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const nodemailer = require("nodemailer");

// Define secrets (v2 style)
const EMAIL_SENDER = defineSecret("EMAIL_SENDER");
const EMAIL_PASSWORD = defineSecret("EMAIL_PASSWORD");

/**
 * Cloud Function: Sends email alert when a new alert document is created
 * Trigger: Firestore "alerts/{alertId}" collection
 */
exports.sendAlertEmail = onDocumentCreated(
  {
    document: "alerts/{alertId}",
    secrets: [EMAIL_SENDER, EMAIL_PASSWORD],
  },
  async (event) => {
    try {
      // Safety check: ensure event data exists
      if (!event.data || !event.data.exists) {
        console.log("No data in event");
        return;
      }

      const alertData = event.data.data();

      // Safety check: ensure required fields exist
      if (!alertData || typeof alertData !== "object") {
        console.error("Invalid alert data");
        return;
      }

      // Extract fields safely
      const guardianEmail = alertData.guardianEmail || alertData.to;
      const patientName = alertData.fromName || alertData.patientName || "Patient";
      const riskLevel = alertData.riskLevel || "UNKNOWN";
      const heartRate = alertData.heartRate || "N/A";
      const spo2 = alertData.spo2 || "N/A";

      // Validate email
      if (!guardianEmail || typeof guardianEmail !== "string") {
        console.error("No valid guardian email found");
        return;
      }

      // Get secrets
      const senderEmail = EMAIL_SENDER.value();
      const senderPassword = EMAIL_PASSWORD.value();

      // Validate secrets exist
      if (!senderEmail || !senderPassword) {
        console.error("Email credentials not configured");
        return;
      }

      // Create transporter INSIDE the function
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
          user: senderEmail,
          pass: senderPassword,
        },
      });

      // Determine styling
      const riskColors = {
        "LOW RISK": "#FFB300",
        "HIGH RISK": "#FF6B35",
        CRITICAL: "#FF3B3B",
      };
      const color = riskColors[riskLevel] || "#FF9800";

      const emojiMap = {
        "LOW RISK": "⚠️",
        "HIGH RISK": "🔴",
        CRITICAL: "🆘",
      };
      const emoji = emojiMap[riskLevel] || "📢";

      // Build email
      const subject = `${emoji} HeartGuard Alert - ${riskLevel}`;
      const htmlBody = `
        <html>
          <head>
            <style>
              body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
              .container { max-width: 500px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px; }
              .header { background-color: ${color}; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
              .header h1 { margin: 0; font-size: 24px; }
              .content { background-color: white; padding: 20px; }
              .vital { margin: 10px 0; }
              .vital-label { font-weight: bold; color: ${color}; }
              .footer { background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 8px 8px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="header">
                <h1>${emoji} HeartGuard Alert</h1>
                <p style="margin: 10px 0 0 0; font-size: 16px;">${riskLevel}</p>
              </div>
              <div class="content">
                <p>Dear Guardian,</p>
                <p>Health alert for: <strong>${patientName}</strong></p>
                <div style="background-color: ${color}; color: white; padding: 15px; border-radius: 5px; margin: 15px 0;">
                  <p style="margin: 0; font-weight: bold;">⚠️ ${riskLevel} Detected</p>
                </div>
                <h3 style="color: ${color}; margin-top: 20px;">Vital Signs:</h3>
                <div class="vital">
                  <span class="vital-label">Heart Rate:</span> ${heartRate} bpm
                </div>
                <div class="vital">
                  <span class="vital-label">SpO2 Level:</span> ${spo2}%
                </div>
                <p style="margin-top: 20px; padding: 15px; background-color: #fff3cd; border-left: 4px solid ${color}; border-radius: 3px;">
                  <strong>Action Required:</strong> Please check on the patient immediately.
                </p>
              </div>
              <div class="footer">
                <p style="margin: 0;">HeartGuard Monitoring System</p>
              </div>
            </div>
          </body>
        </html>
      `;

      // Send email
      const info = await transporter.sendMail({
        from: senderEmail,
        to: guardianEmail,
        subject: subject,
        html: htmlBody,
      });

      console.log(`✅ Email sent to ${guardianEmail} (Message ID: ${info.messageId})`);
      console.log(`Patient: ${patientName}, Risk: ${riskLevel}, HR: ${heartRate}, SpO2: ${spo2}`);

      return {
        success: true,
        messageId: info.messageId,
        recipient: guardianEmail,
      };
    } catch (error) {
      console.error("❌ Error sending email:", error);
      console.error("Error stack:", error.stack);
      // Don't re-throw - let the function complete gracefully
      return {
        success: false,
        error: error.message,
      };
    }
  }
);