# Postman Testing Guide for Notification Service

## 🚀 Service Status
✅ **Service is running on port 3003**
✅ **Health endpoint responding**
✅ **JWT token generation working**
✅ **Redis connection established**
✅ **Queue system operational**

## 📋 Quick Start for Postman

### 1. Generate JWT Token
**Endpoint:** `POST http://localhost:3003/notifications/generate-token`

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "id": "test-user-123",
  "email": "test@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "payload": {
    "id": "test-user-123",
    "email": "test@example.com",
    "role": "user",
    "permissions": ["read", "write"]
  }
}
```

### 2. Test Health Endpoint
**Endpoint:** `GET http://localhost:3003/health`

**No authentication required**

**Response:**
```json
{
  "status": "OK",
  "timestamp": "2025-07-12T16:15:30.988Z",
  "service": "notification-service",
  "version": "1.0.0"
}
```

### 3. Send Test Notification
**Endpoint:** `POST http://localhost:3003/notifications/test`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

**Body (raw JSON):**
```json
{
  "to": "test@example.com",
  "subject": "Test Notification",
  "content": "This is a test notification for offline user testing"
}
```

### 4. Send P0 Priority Notification (OTP/Critical)
**Endpoint:** `POST http://localhost:3003/notifications/test-p0`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

**Body (raw JSON):**
```json
{
  "to": "test@example.com",
  "subject": "Critical Alert - OTP Verification",
  "content": "Your OTP code is: 123456"
}
```

### 5. Send Custom Notification
**Endpoint:** `POST http://localhost:3003/notifications/send`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

**Body (raw JSON):**
```json
{
  "to": "test@example.com",
  "subject": "Welcome to Loqal",
  "content": "Thank you for joining our platform!",
  "type": "transactional",
  "priority": "p1",
  "metadata": {
    "category": "welcome",
    "userId": "user-123"
  }
}
```

## 🔍 Monitoring & Debugging

### Check Service Logs
```bash
tail -f logs/combined.log
```

### Check Redis Status
```bash
redis-cli ping
```

### Check Queue Status
```bash
redis-cli llen email_queue
```

## 📧 Email Configuration

### Current Setup
- **Provider:** Resend
- **API Key:** `re_your_api_key_here` (needs to be updated)
- **From Email:** `noreply@yourdomain.com` (needs domain verification)

### To Enable Real Email Sending
1. Get a Resend API key from [resend.com](https://resend.com)
2. Update `.env` file:
   ```env
   RESEND_API_KEY=re_your_actual_api_key
   EMAIL_FROM=noreply@yourdomain.com
   ```
3. Verify your domain in Resend dashboard

### Testing Without Real Email
The service will still work and queue messages even without a valid email configuration. Messages will be processed and logged, but won't be sent via email until proper configuration is set up.

## 🎯 Testing Offline User Scenarios

### Scenario 1: User Offline - Message Queued
1. Send notification to offline user
2. Message gets stored in digest buffer
3. Check logs: `Message stored for digest (user offline)`
4. Digest job processes every 30 seconds

### Scenario 2: User Online - Immediate Delivery
1. User connects via WebSocket
2. Pending messages are delivered immediately
3. Check logs for delivery confirmation

### Scenario 3: Digest Processing
1. Offline messages accumulate
2. Digest job runs every 30 seconds
3. Messages are batched and processed
4. Check logs: `Processing digest for user`

## 🔧 Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Check JWT token is valid
   - Ensure Authorization header format: `Bearer <token>`

2. **500 Internal Server Error**
   - Check service logs: `tail -f logs/combined.log`
   - Verify Redis is running: `redis-cli ping`

3. **No Response from Service**
   - Verify service is running: `curl http://localhost:3003/health`
   - Check port 3003 is not blocked

4. **Email Not Sending**
   - Check email configuration in `.env`
   - Verify Resend API key and domain
   - Check email provider logs

### Service Commands
```bash
# Start service
npm start

# Start in development mode
npm run dev

# Check service status
curl http://localhost:3003/health

# View logs
tail -f logs/combined.log
```

## 📊 Expected Responses

### Successful Notification
```json
{
  "success": true,
  "message": "Test notification sent successfully",
  "jobId": "job_123456789"
}
```

### P0 Priority Notification
```json
{
  "success": true,
  "message": "P0 priority notification queued successfully",
  "jobId": "job_123456789",
  "priority": "p0",
  "estimatedDelivery": "immediate"
}
```

### Error Response
```json
{
  "error": "Failed to send notification",
  "details": "Error message here"
}
```

## 🎉 Success Indicators

✅ **Service responds to health check**
✅ **JWT token generation works**
✅ **Notifications are queued successfully**
✅ **Job IDs are returned**
✅ **Logs show message processing**
✅ **Redis queues are working**

Your notification service is now fully operational and ready for testing with Postman!