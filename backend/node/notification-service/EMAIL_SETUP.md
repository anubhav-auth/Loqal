# Email Provider Setup Guide

## ✅ Implementation Complete

The email provider has been successfully implemented with production-ready features:

### 📁 `src/providers/emailProvider.js`

✅ **Uses `resend` npm package** - Direct integration with Resend API
✅ **Reads from `.env`** - API key and sender email from environment variables
✅ **Exposes `send(to, data)` function** - Clean, simple interface
✅ **Data structure** - `subject`, `body` (HTML), optional `templateId`
✅ **Winston logging** - Comprehensive success/failure logging

## 🔧 Configuration

### Environment Variables (`.env`)
```env
# Required
RESEND_API_KEY=re_your_api_key_here
EMAIL_FROM=noreply@yourdomain.com

# Optional
NODE_ENV=production
```

### Domain Verification

**Current Issue**: Resend requires domain verification before sending emails.

**Solution Options**:

1. **Verify Your Domain** (Recommended for Production)
   - Go to [https://resend.com/domains](https://resend.com/domains)
   - Add your domain (e.g., `yourdomain.com`)
   - Follow DNS verification steps
   - Update `.env`: `EMAIL_FROM=noreply@yourdomain.com`

2. **Use Resend's Default Domain** (For Testing)
   - Check your Resend dashboard for available domains
   - Use a pre-verified domain provided by Resend
   - Update `.env`: `EMAIL_FROM=noreply@resend-provided-domain.com`

3. **Use a Verified Domain** (If Available)
   - If you have access to a verified domain in your account
   - Update `.env`: `EMAIL_FROM=noreply@verifieddomain.com`

## 🚀 Usage Examples

### Basic Email Sending
```javascript
const emailProvider = require('./src/providers/emailProvider');

// Simple email
await emailProvider.send("user@example.com", {
  subject: "Order Confirmed",
  body: "<h2>Your order has been placed!</h2>"
});

// Email with template
await emailProvider.send("user@example.com", {
  subject: "Welcome!",
  body: "<h2>Welcome to our platform!</h2>",
  templateId: "template_123"
});
```

### P0 Priority Notifications (OTP, Critical Alerts)
```bash
# Generate JWT token
curl -X POST http://localhost:3003/notifications/generate-token \
  -H "Content-Type: application/json" \
  -d '{"id": "test-user", "email": "user@example.com"}'

# Send P0 priority notification
curl -X POST http://localhost:3003/notifications/test-p0 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "to": "user@example.com",
    "subject": "Your OTP Code",
    "content": "Your verification code is: 123456"
  }'
```

### Regular Notifications
```bash
curl -X POST http://localhost:3003/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "to": "user@example.com",
    "subject": "Welcome!",
    "content": "Welcome to our platform!",
    "priority": "p0"
  }'
```

## 🧪 Testing

### Test Email Provider
```bash
node test-email.js
```

### Test P0 Endpoint
```bash
# Start server
PORT=3003 node index.js

# In another terminal
curl -X POST http://localhost:3003/notifications/test-p0 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"to": "user@example.com", "subject": "Test", "content": "Test content"}'
```

## 📊 Features

✅ **Real Email Delivery** - Uses Resend API for actual email sending
✅ **Priority Queues** - P0, P1, P2 priority handling with BullMQ
✅ **Error Handling** - Comprehensive error handling and logging
✅ **Validation** - Email format and required field validation
✅ **Template Support** - Optional template ID support
✅ **Production Ready** - Winston logging, proper error handling
✅ **Queue Integration** - Seamless integration with BullMQ queues
✅ **WebSocket Integration** - Real-time + email delivery

## 🔍 Troubleshooting

### Common Issues

1. **Domain Not Verified Error**
   ```
   Error: The gmail.com domain is not verified
   ```
   **Solution**: Verify your domain in Resend dashboard

2. **API Key Not Configured**
   ```
   Error: RESEND_API_KEY not configured
   ```
   **Solution**: Add `RESEND_API_KEY=re_your_key` to `.env`

3. **From Email Not Configured**
   ```
   Error: EMAIL_FROM not configured
   ```
   **Solution**: Add `EMAIL_FROM=noreply@yourdomain.com` to `.env`

### Logs
Check logs for detailed error information:
```bash
tail -f logs/combined.log
```

## 🎯 Next Steps

1. **Verify your domain** in Resend dashboard
2. **Update `EMAIL_FROM`** in `.env` with verified domain
3. **Test email sending** with `node test-email.js`
4. **Test P0 notifications** via API endpoints
5. **Monitor logs** for email delivery status

The email provider is now fully production-ready and will send real emails once domain verification is complete!