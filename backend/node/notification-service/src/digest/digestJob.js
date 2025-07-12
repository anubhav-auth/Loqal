const cron = require('node-cron');
const { getBuffer, clearBuffer, getBufferStats, cleanupPresence, removeOfflineMessages } = require('./bufferStore');
const emailProvider = require('../providers/emailProvider');
const { addDigestJob } = require('../queues/queueManager');
const logger = require('../utils/logger');

let digestCronJob;

/**
 * Start digest job scheduler
 */
const startDigestJob = () => {
  try {
    const interval = process.env.DIGEST_INTERVAL || 30;

    // Schedule digest job to run every 30 seconds (or configured interval)
    digestCronJob = cron.schedule(`*/${interval} * * * * *`, async () => {
      await processDigestJob();
    }, {
      scheduled: true,
      timezone: 'UTC'
    });

    logger.info('Digest job scheduler started', {
      interval: `${interval} seconds`,
      pattern: `*/${interval} * * * * *`
    });

  } catch (error) {
    logger.error('Error starting digest job scheduler:', error);
    throw error;
  }
};

/**
 * Process digest job
 */
const processDigestJob = async () => {
  try {
    logger.debug('Starting digest job processing');

    // Clean expired presence data first
    await cleanupPresence();

    // Get buffer statistics to find users with messages
    const bufferStats = await getBufferStats();

    if (bufferStats.totalUsers === 0) {
      logger.debug('No users with buffered messages found');
      return;
    }

    // For now, we'll process all users with buffers
    // In a real implementation, you'd want to get the actual user IDs
    const usersWithMessages = ['test-user']; // Placeholder

    if (usersWithMessages.length === 0) {
      logger.debug('No users with offline messages found');
      return;
    }

    logger.info('Processing digest for users with offline messages', {
      userCount: usersWithMessages.length
    });

    // Process each user's offline messages
    for (const userId of usersWithMessages) {
      try {
        await processUserDigest(userId);
      } catch (error) {
        logger.error('Error processing digest for user:', {
          userId,
          error: error.message
        });
      }
    }

    logger.info('Digest job processing completed', {
      processedUsers: usersWithMessages.length
    });

  } catch (error) {
    logger.error('Error in digest job processing:', error);
  }
};

/**
 * Process digest for a specific user
 * @param {string} userId - User ID
 */
const processUserDigest = async (userId) => {
  try {
    // Get buffered messages for user
    const messages = await getBuffer(userId);

    if (messages.length === 0) {
      return;
    }

    logger.info('Processing digest for user', {
      userId,
      messageCount: messages.length
    });

    // Group messages by type
    const groupedMessages = groupMessagesByType(messages);

    // Create digest content
    const digestContent = createDigestContent(groupedMessages);

    // Send digest email
    const emailData = {
      to: messages[0].to, // Use the first message's recipient
      subject: `Your Notification Digest - ${messages.length} new messages`,
      content: digestContent,
      type: 'transactional',
      metadata: {
        digest: true,
        userId,
        messageCount: messages.length,
        messageIds: messages.map(msg => msg.id)
      }
    };

    await addDigestJob('send-digest', {
      userId,
      emailData,
      messageIds: messages.map(msg => msg.id)
    }, {
      priority: 2,
      delay: 0
    });

    logger.info('Digest job queued for user', {
      userId,
      messageCount: messages.length,
      messageIds: messages.map(msg => msg.id)
    });

  } catch (error) {
    logger.error('Error processing user digest:', {
      userId,
      error: error.message
    });
    throw error;
  }
};

/**
 * @param {Array} messages
 * @returns {Object}
 */
const groupMessagesByType = (messages) => {
  const grouped = {
    transactional: [],
    promotional: []
  };

  messages.forEach(message => {
    const type = message.type || 'transactional';
    if (grouped[type]) {
      grouped[type].push(message);
    } else {
      grouped.transactional.push(message);
    }
  });

  return grouped;
};

/**
 * Create digest email content
 * @param {Object} groupedMessages
 * @returns {string}
 */
const createDigestContent = (groupedMessages) => {
  let content = '<h2>Your Notification Digest</h2>\n\n';

  const totalMessages = Object.values(groupedMessages).reduce((sum, messages) => sum + messages.length, 0);
  content += `<p>You have <strong>${totalMessages}</strong> new notifications while you were away.</p>\n\n`;

  // Transactional messages
  if (groupedMessages.transactional.length > 0) {
    content += '<h3>Important Notifications</h3>\n<ul>\n';
    groupedMessages.transactional.forEach(message => {
      content += `<li><strong>${message.subject}</strong><br>`;
      content += `${message.content}<br>`;
      content += `<small>Received: ${new Date(message.timestamp).toLocaleString()}</small></li>\n`;
    });
    content += '</ul>\n\n';
  }

  // Promotional messages
  if (groupedMessages.promotional.length > 0) {
    content += '<h3>Updates & Offers</h3>\n<ul>\n';
    groupedMessages.promotional.forEach(message => {
      content += `<li><strong>${message.subject}</strong><br>`;
      content += `${message.content}<br>`;
      content += `<small>Received: ${new Date(message.timestamp).toLocaleString()}</small></li>\n`;
    });
    content += '</ul>\n\n';
  }

  content += '<hr>\n';
  content += '<p><small>This digest was automatically generated by the Loqal Notification Service.</small></p>';
  content += `<p><small>Generated at: ${new Date().toLocaleString()}</small></p>`;

  return content;
};

/**
 * Send digest email directly (for immediate processing)
 * @param {string} userId - User ID
 * @param {Object} emailData - Email data
 * @param {Array} messageIds - Message IDs to remove after sending
 */
const sendDigestEmail = async (userId, emailData, messageIds) => {
  try {
    logger.info('Sending digest email', {
      userId,
      to: emailData.to,
      messageCount: messageIds.length
    });

    // Send email
    const result = await emailProvider.sendEmail(emailData);

    if (result.success) {
      // Remove processed messages
      await removeOfflineMessages(userId, messageIds);

      logger.info('Digest email sent successfully', {
        userId,
        to: emailData.to,
        messageIds,
        messageId: result.messageId
      });
    }

    return result;

  } catch (error) {
    logger.error('Error sending digest email:', {
      userId,
      to: emailData.to,
      error: error.message
    });
    throw error;
  }
};

/**
 * Stop digest job scheduler
 */
const stopDigestJob = () => {
  try {
    if (digestCronJob) {
      digestCronJob.stop();
      logger.info('Digest job scheduler stopped');
    }
  } catch (error) {
    logger.error('Error stopping digest job scheduler:', error);
    throw error;
  }
};

/**
 * Get digest job status
 * @returns {Object} Digest job status
 */
const getDigestJobStatus = () => {
  return {
    running: digestCronJob ? digestCronJob.running : false,
    scheduled: digestCronJob ? digestCronJob.scheduled : false,
    interval: process.env.DIGEST_INTERVAL || 30,
    timestamp: new Date().toISOString()
  };
};

/**
 * Manually trigger digest processing
 */
const triggerDigestProcessing = async () => {
  try {
    logger.info('Manually triggering digest processing');
    await processDigestJob();
  } catch (error) {
    logger.error('Error in manual digest trigger:', error);
    throw error;
  }
};

module.exports = {
  startDigestJob,
  stopDigestJob,
  processDigestJob,
  processUserDigest,
  sendDigestEmail,
  getDigestJobStatus,
  triggerDigestProcessing
};