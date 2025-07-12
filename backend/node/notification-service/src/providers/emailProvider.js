const nodemailer = require('nodemailer');
const logger = require('../../tests/services/utils/logger');

// Read SMTP config from environment
const smtpHost = process.env.SMTP_HOST;
const smtpPort = process.env.SMTP_PORT;
const smtpUser = process.env.SMTP_USER;
const smtpPass = process.env.SMTP_PASS;
const fromEmail = process.env.FROM_EMAIL;

// Create Nodemailer transporter
const transporter = nodemailer.createTransport({
  host: smtpHost,
  port: Number(smtpPort),
  secure: Number(smtpPort) === 465, // true for 465, false for other ports
  auth: {
    user: smtpUser,
    pass: smtpPass
  },
  logger: true,
  debug: true
});

/**
 * Send email using SMTP
 * @param {string} to - Recipient email address
 * @param {Object} data - Email data
 * @param {string} data.subject - Email subject
 * @param {string} data.body - HTML body content
 * @returns {Promise<Object>} Send result
 */
const send = async (to, data) => {
  try {
    const { subject, body } = data;

    if (!to || !subject || !body) {
      throw new Error('Missing required fields: to, subject, body');
    }
    if (!smtpHost || !smtpPort || !smtpUser || !smtpPass || !fromEmail) {
      throw new Error('SMTP configuration missing in environment variables');
    }

    const mailOptions = {
      from: fromEmail,
      to,
      subject,
      html: body
    };

    logger.info('Sending email via SMTP', { to, subject, from: fromEmail });
    const info = await transporter.sendMail(mailOptions);
    logger.info('Email sent successfully via SMTP', { to, subject, messageId: info.messageId });

    return {
      success: true,
      messageId: info.messageId,
      provider: 'smtp',
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error('Error sending email via SMTP', {
      to,
      subject: data?.subject,
      error: error.message,
      stack: error.stack
    });
    throw {
      message: `Failed to send email via SMTP: ${error.message}`,
      originalError: error,
      provider: 'smtp'
    };
  }
};

const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

const getProviderStatus = () => {
  return {
    provider: 'smtp',
    smtpConfigured: !!(smtpHost && smtpPort && smtpUser && smtpPass && fromEmail),
    fromEmail,
    timestamp: new Date().toISOString()
  };
};

module.exports = {
  send,
  validateEmail,
  getProviderStatus
};