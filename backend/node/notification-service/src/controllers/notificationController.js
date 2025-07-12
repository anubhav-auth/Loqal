const notificationService = require('../services/notificationService');
const { generateToken } = require('../utils/jwtUtils');
const logger = require('../utils/logger');

/**
 * Send a notification
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const sendNotification = async (req, res) => {
  try {
    const {
      to,
      subject,
      content,
      type = 'transactional',
      priority = 'p1',
      metadata = {}
    } = req.body;

    // Validate required fields
    if (!to || !subject || !content) {
      return res.status(400).json({
        error: 'Missing required fields: to, subject, content'
      });
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(to)) {
      return res.status(400).json({
        error: 'Invalid email format'
      });
    }

    // Validate type
    if (!['transactional', 'promotional'].includes(type)) {
      return res.status(400).json({
        error: 'Invalid type. Must be "transactional" or "promotional"'
      });
    }

    // Validate priority
    if (!['p0', 'p1', 'p2'].includes(priority)) {
      return res.status(400).json({
        error: 'Invalid priority. Must be "p0", "p1", or "p2"'
      });
    }

    const userId = req.user.id;
    const notificationData = {
      to,
      subject,
      content,
      type,
      priority,
      metadata,
      userId,
      timestamp: new Date()
    };

    const result = await notificationService.sendNotification(notificationData);

    logger.info('Notification queued successfully', {
      jobId: result.jobId,
      to,
      type,
      priority,
      userId
    });

    res.status(201).json({
      success: true,
      message: 'Notification sent via WebSocket and queued for email delivery',
      jobId: result.jobId,
      estimatedDelivery: result.estimatedDelivery,
      realTimeSent: result.realTimeSent,
      emailQueued: result.emailQueued
    });

  } catch (error) {
    logger.error('Error sending notification:', error);
    res.status(500).json({
      error: 'Failed to queue notification',
      details: error.message
    });
  }
};

/**
 * Get job status
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getJobStatus = async (req, res) => {
  try {
    const { jobId } = req.params;
    const userId = req.user.id;

    const status = await notificationService.getJobStatus(jobId, userId);

    res.status(200).json({
      success: true,
      jobId,
      status
    });

  } catch (error) {
    logger.error('Error getting job status:', error);
    res.status(500).json({
      error: 'Failed to get job status',
      details: error.message
    });
  }
};

/**
 * Get notification history
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getNotificationHistory = async (req, res) => {
  try {
    const userId = req.user.id;
    const { page = 1, limit = 20, type } = req.query;

    const history = await notificationService.getNotificationHistory(
      userId,
      parseInt(page),
      parseInt(limit),
      type
    );

    res.status(200).json({
      success: true,
      history: history.notifications,
      pagination: history.pagination
    });

  } catch (error) {
    logger.error('Error getting notification history:', error);
    res.status(500).json({
      error: 'Failed to get notification history',
      details: error.message
    });
  }
};

/**
 * Send test notification
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const sendTestNotification = async (req, res) => {
  try {
    const { to = req.user.email } = req.body;
    const userId = req.user.id;

    const testData = {
      to,
      subject: 'Test Notification',
      content: 'This is a test notification from the notification service.',
      type: 'transactional',
      priority: 'p1',
      metadata: { test: true },
      userId,
      timestamp: new Date()
    };

    const result = await notificationService.sendNotification(testData);

    logger.info('Test notification sent', {
      jobId: result.jobId,
      to,
      userId
    });

    res.status(201).json({
      success: true,
      message: 'Test notification sent successfully',
      jobId: result.jobId
    });

  } catch (error) {
    logger.error('Error sending test notification:', error);
    res.status(500).json({
      error: 'Failed to send test notification',
      details: error.message
    });
  }
};

/**
 * Send P0 priority test notification (for OTP, critical alerts, etc.)
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const sendP0TestNotification = async (req, res) => {
  try {
    const { to = req.user.email, subject = 'Critical Alert - OTP Verification', content = 'Your OTP code is: 123456' } = req.body;
    const userId = req.user.id;

    const testData = {
      to,
      subject,
      content,
      type: 'transactional',
      priority: 'p0', // Highest priority for immediate delivery
      metadata: {
        test: true,
        category: 'otp',
        urgent: true
      },
      userId,
      timestamp: new Date()
    };

    const result = await notificationService.sendNotification(testData);

    logger.info('P0 test notification sent', {
      jobId: result.jobId,
      to,
      priority: 'p0',
      userId
    });

    res.status(201).json({
      success: true,
      message: 'P0 priority notification sent via WebSocket and queued for email delivery',
      jobId: result.jobId,
      priority: 'p0',
      estimatedDelivery: result.estimatedDelivery,
      realTimeSent: result.realTimeSent,
      emailQueued: result.emailQueued
    });

  } catch (error) {
    logger.error('Error sending P0 test notification:', error);
    res.status(500).json({
      error: 'Failed to send P0 test notification',
      details: error.message
    });
  }
};

/**
 * Generate test JWT token
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const generateTestToken = async (req, res) => {
  try {
    const { id = 'test-user', email = 'test@example.com' } = req.body;

    const payload = {
      id,
      email,
      role: 'user',
      permissions: ['read', 'write']
    };

    const token = generateToken(payload, '24h');

    logger.info('Test token generated', { userId: id, email });

    res.status(200).json({
      success: true,
      token,
      payload
    });

  } catch (error) {
    logger.error('Error generating test token:', error);
    res.status(500).json({
      error: 'Failed to generate test token',
      details: error.message
    });
  }
};

/**
 * Get service statistics
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getStats = async (req, res) => {
  try {
    const userId = req.user.id;
    const stats = await notificationService.getStats(userId);

    res.status(200).json({
      success: true,
      stats
    });

  } catch (error) {
    logger.error('Error getting stats:', error);
    res.status(500).json({
      error: 'Failed to get statistics',
      details: error.message
    });
  }
};

/**
 * Send dual delivery test notification (WebSocket + Email)
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const sendDualDeliveryTest = async (req, res) => {
  try {
    const {
      to = req.user.email,
      subject = 'Dual Delivery Test - WebSocket + Email',
      content = 'This notification was sent via both WebSocket (real-time) and email!'
    } = req.body;
    const userId = req.user.id;

    const testData = {
      to,
      subject,
      content,
      type: 'transactional',
      priority: 'p1',
      metadata: {
        test: true,
        category: 'dual-delivery-test',
        deliveryMethods: ['websocket', 'email']
      },
      userId,
      timestamp: new Date()
    };

    const result = await notificationService.sendNotification(testData);

    logger.info('Dual delivery test notification sent', {
      jobId: result.jobId,
      to,
      realTimeSent: result.realTimeSent,
      emailQueued: result.emailQueued,
      userId
    });

    res.status(201).json({
      success: true,
      message: 'Dual delivery test notification sent successfully',
      jobId: result.jobId,
      realTimeSent: result.realTimeSent,
      emailQueued: result.emailQueued,
      estimatedDelivery: result.estimatedDelivery,
      deliveryMethods: {
        websocket: result.realTimeSent ? 'sent' : 'user_offline',
        email: 'queued'
      }
    });

  } catch (error) {
    logger.error('Error sending dual delivery test notification:', error);
    res.status(500).json({
      error: 'Failed to send dual delivery test notification',
      details: error.message
    });
  }
};

module.exports = {
  sendNotification,
  getJobStatus,
  getNotificationHistory,
  sendTestNotification,
  sendP0TestNotification,
  generateTestToken,
  getStats,
  sendDualDeliveryTest
};