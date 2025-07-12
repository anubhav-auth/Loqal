require('dotenv').config();
const emailProvider = require('./src/providers/emailProvider');

async function testEmailSending() {
  try {
    console.log('🧪 Testing email sending with SMTP...');
    console.log('📧 From:', process.env.FROM_EMAIL);
    console.log('🔑 SMTP Host:', process.env.SMTP_HOST);

    // Check provider status
    const status = emailProvider.getProviderStatus();
    console.log('📊 Provider Status:', status);

    const result = await emailProvider.send('soham7857@gmail.com', {
      subject: 'Test Email from Notification Service',
      body: `
        <h2>Hello from Loqal Notification Service!</h2>
        <p>This is a test email sent using SMTP.</p>
        <p><strong>Timestamp:</strong> ${new Date().toISOString()}</p>
        <p>If you received this email, the notification service is working correctly!</p>
      `
    });

    console.log('✅ Email sent successfully!');
    console.log('📨 Message ID:', result.messageId);
    console.log('⏰ Timestamp:', result.timestamp);

  } catch (error) {
    console.error('❌ Failed to send email:', error.message);
    if (error.originalError) {
      console.error('🔍 Original error:', error.originalError.message);
    }
  }
}

testEmailSending();