const axios = require('axios');

const BASE_URL = 'http://localhost:3003';

async function testNotificationService() {
  console.log('🧪 Testing Notification Service for Postman...\n');

  try {
    // 1. Generate a test JWT token
    console.log('1️⃣ Generating JWT token...');
    const tokenResponse = await axios.post(`${BASE_URL}/notifications/generate-token`, {
      id: 'test-user-123',
      email: 'test@example.com'
    });

    const token = tokenResponse.data.token;
    console.log('✅ Token generated successfully');
    console.log(`🔑 Token: ${token}\n`);

    // 2. Test health endpoint
    console.log('2️⃣ Testing health endpoint...');
    const healthResponse = await axios.get(`${BASE_URL}/health`);
    console.log('✅ Health check passed:', healthResponse.data);

    // 3. Test sending a notification
    console.log('\n3️⃣ Testing notification sending...');
    const notificationResponse = await axios.post(`${BASE_URL}/notifications/test`, {
      to: 'soham7857@gmail.com',
      subject: 'Test Notification from Postman',
      content: 'This is a test notification to verify the service is working correctly.'
    }, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    console.log('✅ Notification sent successfully:', notificationResponse.data);

    // 4. Test P0 priority notification (for OTP/critical alerts)
    console.log('\n4️⃣ Testing P0 priority notification...');
    const p0Response = await axios.post(`${BASE_URL}/notifications/test-p0`, {
      to: 'test@example.com',
      subject: 'Critical Alert - OTP Verification',
      content: 'Your OTP code is: 123456. This is a high-priority notification.'
    }, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    console.log('✅ P0 notification sent successfully:', p0Response.data);

    console.log('\n🎉 All tests passed! Your notification service is working correctly.');
    console.log('\n📋 Postman Test Instructions:');
    console.log('1. Use the token above in your Authorization header: Bearer <token>');
    console.log('2. Test endpoints:');
    console.log('   - POST /notifications/test');
    console.log('   - POST /notifications/test-p0');
    console.log('   - POST /notifications/send');
    console.log('3. Check logs for email processing: tail -f logs/combined.log');

  } catch (error) {
    console.error('❌ Test failed:', error.response?.data || error.message);
  }
}

// Run the test
testNotificationService();