const { getEmailQueue } = require('../queues/queueManager');
const { isUserOnline, addToBuffer } = require('../digest/bufferStore');
const { emitNotification } = require('../sockets/socketServer');
const logger = require('../utils/logger'); // Fixed import
const { getProviderStatus, validateEmail, send } = require('../providers/emailProvider');

/**
 * Send a notification
 * @param {Object} notificationData - Notification data
 * @returns {Object} Result with jobId and estimated delivery
 */
const sendNotification = async (notificationData) => {
  try {
    const {
      to,
      subject,
      content,
      type,
      priority,
      metadata,
      userId,
      timestamp
    } = notificationData;

    // Determine priority based on type if not explicitly set
    let finalPriority = priority;
    if (type === 'transactional' && priority === 'p1') {
      finalPriority = 'p0'; // Transactional emails get higher priority
    } else if (type === 'promotional' && priority === 'p1') {
      finalPriority = 'p2'; // Promotional emails get lower priority
    }

    // Check if user is online
    const userOnline = await isUserOnline(userId);

    // ALWAYS send real-time notification via WebSocket (if user is online)
    let realTimeSent = false;
    if (userOnline) {
      try {
        await emitNotification(userId, {
          type: 'email',
          subject,
          content,
          timestamp,
          metadata
        });

        realTimeSent = true;
        logger.info('Real-time notification sent via WebSocket', {
          userId,
          to,
          type
        });
      } catch (websocketError) {
        logger.warn('Failed to send WebSocket notification, continuing with email', {
          userId,
          to,
          error: websocketError.message
        });
      }
    } else {
      logger.info('User offline, skipping WebSocket notification', {
        userId,
        to,
        type
      });
    }
    logger.info("Sending message via email SMTP")
    logger.info("Provider status: ",getProviderStatus())
    logger.info("Validating email: ",validateEmail(to));
    logger.info("Sending mail: ",send(to,{
      subject:subject,
      body: content
    }));
    // ALWAYS queue email for delivery (regardless of WebSocket status)
    // Add to BullMQ queue for processing
    const job = await getEmailQueue().add(
      'send-email',
      {
        to,
        subject,
        content,
        type,
        metadata,
        userId,
        timestamp,
        realTimeSent
      },
      {
        priority: getPriorityValue(finalPriority),
        attempts: 3,
        backoff: {
          type: 'exponential',
          delay: 2000
        },
        removeOnComplete: 100,
        removeOnFail: 50
      }
    );

    // Calculate estimated delivery time
    const estimatedDelivery = calculateEstimatedDelivery(finalPriority);

    logger.info('Notification queued for email delivery', {
      jobId: job.id,
      userId,
      to,
      type,
      priority: finalPriority,
      realTimeSent,
      estimatedDelivery
    });

    return {
      jobId: job.id,
      estimatedDelivery,
      priority: finalPriority,
      realTimeSent,
      emailQueued: true
    };

  } catch (error) {
    logger.error('Error in sendNotification:', error);
    throw error;
  }
};

/**
 * Get job status
 * @param {string} jobId - Job ID
 * @param {string} userId - User ID
 * @returns {Object} Job status
 */
const getJobStatus = async (jobId, userId) => {
  try {
    const job = await getEmailQueue().getJob(jobId);

    if (!job) {
      throw new Error('Job not found');
    }

    const jobData = job.data;

    // Verify job belongs to user
    if (jobData.userId !== userId) {
      throw new Error('Unauthorized access to job');
    }

    const state = await job.getState();
    const progress = await job.progress();
    const failedReason = job.failedReason;

    return {
      id: job.id,
      state,
      progress,
      failedReason,
      data: {
        to: jobData.to,
        subject: jobData.subject,
        type: jobData.type,
        timestamp: jobData.timestamp
      },
      createdAt: job.timestamp,
      processedAt: job.processedOn,
      finishedAt: job.finishedOn
    };

  } catch (error) {
    logger.error('Error getting job status:', error);
    throw error;
  }
};

/**
 * Get notification history
 * @param {string} userId - User ID
 * @param {number} page - Page number
 * @param {number} limit - Items per page
 * @param {string} type - Filter by type
 * @returns {Object} Notification history with pagination
 */
const getNotificationHistory = async (userId, page = 1, limit = 20, type = null) => {
  try {
    // This would typically query a database
    // For now, we'll return a mock response
    const notifications = [];
    const total = 0;

    // TODO: Implement actual database query
    // const notifications = await NotificationModel.find({
    //   userId,
    //   ...(type && { type })
    // })
    // .sort({ timestamp: -1 })
    // .skip((page - 1) * limit)
    // .limit(limit);
    //
    // const total = await NotificationModel.countDocuments({
    //   userId,
    //   ...(type && { type })
    // });

    return {
      notifications,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit),
        hasNext: page * limit < total,
        hasPrev: page > 1
      }
    };

  } catch (error) {
    logger.error('Error getting notification history:', error);
    throw error;
  }
};

/**
 * Get service statistics
 * @param {string} userId - User ID
 * @returns {Object} Service statistics
 */
const getStats = async (userId) => {
  try {
    // This would typically query a database and Redis
    // For now, we'll return a mock response

    const stats = {
      totalSent: 0,
      totalDelivered: 0,
      totalFailed: 0,
      byType: {
        transactional: 0,
        promotional: 0
      },
      byPriority: {
        p0: 0,
        p1: 0,
        p2: 0
      },
      averageDeliveryTime: 0,
      successRate: 100
    };

    // TODO: Implement actual statistics calculation
    // const stats = await calculateUserStats(userId);

    return stats;

  } catch (error) {
    logger.error('Error getting stats:', error);
    throw error;
  }
};

/**
 * Get priority value for BullMQ
 * @param {string} priority - Priority string
 * @returns {number} Priority value
 */
const getPriorityValue = (priority) => {
  const priorityMap = {
    'p0': 1,    // Highest priority
    'p1': 2,    // Medium priority
    'p2': 3     // Lowest priority
  };
  return priorityMap[priority] || 2;
};

/**
 * Calculate estimated delivery time
 * @param {string} priority - Priority level
 * @returns {Date} Estimated delivery time
 */
const calculateEstimatedDelivery = (priority) => {
  const now = new Date();
  const deliveryDelays = {
    'p0': 1 * 60 * 1000,    // 1 minute
    'p1': 5 * 60 * 1000,    // 5 minutes
    'p2': 15 * 60 * 1000    // 15 minutes
  };

  const delay = deliveryDelays[priority] || deliveryDelays['p1'];
  return new Date(now.getTime() + delay);
};

module.exports = {
  sendNotification,
  getJobStatus,
  getNotificationHistory,
  getStats
};