const { Worker } = require('bullmq');
const { getRedisClient } = require('../config/redis');
const emailProvider = require('../providers/emailProvider');
const fallbackProvider = require('../providers/fallbackProvider');
const logger = require('../utils/logger');

// Lazy load redis client
let redis;
const getRedis = () => {
  if (!redis) {
    redis = getRedisClient();
  }
  return redis;
};

let emailWorker;

/**
 * Initialize email worker
 */
const initializeEmailWorker = () => {
  try {
    // Create the email worker
    emailWorker = new Worker('email-queue', async (job) => {
      const { to, subject, content, html, text, priority, metadata, realTimeSent } = job.data;

      logger.info('Processing email job', {
        jobId: job.id,
        to,
        subject,
        priority: priority || 'normal',
        realTimeSent: realTimeSent || false
      });

      try {
        // Try primary email provider
        const result = await emailProvider.send(to, {
          subject,
          body: content || html || text,
          ...(metadata.templateId && { templateId: metadata.templateId })
        });

        logger.info('Email sent successfully', {
          jobId: job.id,
          to,
          messageId: result.messageId
        });

        return {
          success: true,
          messageId: result.messageId,
          provider: 'primary'
        };

      } catch (error) {
        logger.error('Primary email provider failed', {
          jobId: job.id,
          to,
          error: error.message
        });

        // Try fallback provider
        try {
          const fallbackResult = await fallbackProvider.sendEmail({
            to,
            subject,
            content: content || html || text,
            metadata
          });

          logger.info('Email sent via fallback provider', {
            jobId: job.id,
            to,
            messageId: fallbackResult.messageId
          });

          return {
            success: true,
            messageId: fallbackResult.messageId,
            provider: 'fallback'
          };

        } catch (fallbackError) {
          logger.error('Fallback email provider also failed', {
            jobId: job.id,
            to,
            error: fallbackError.message
          });

          throw new Error(`Both email providers failed: ${error.message}, ${fallbackError.message}`);
        }
      }
    }, {
      connection: getRedis(),
      concurrency: 5,
      removeOnComplete: 100,
      removeOnFail: 50
    });

    // Set up worker event listeners
    setupWorkerEventListeners();

    logger.info('Email worker initialized successfully');
  } catch (error) {
    logger.error('Error initializing email worker:', error);
    throw error;
  }
};

/**
 * Set up worker event listeners
 */
const setupWorkerEventListeners = () => {
  emailWorker.on('completed', (job, result) => {
    logger.info('Email worker job completed', {
      jobId: job.id,
      to: job.data.to,
      processingTime: result.processingTime,
      provider: result.provider
    });
  });

  emailWorker.on('failed', (job, err) => {
    logger.error('Email worker job failed', {
      jobId: job.id,
      to: job.data.to,
      error: err.message,
      attempts: job.attemptsMade
    });
  });

  emailWorker.on('error', (error) => {
    logger.error('Email worker error:', error);
  });

  emailWorker.on('stalled', (jobId) => {
    logger.warn('Email worker job stalled', { jobId });
  });

  emailWorker.on('closing', () => {
    logger.info('Email worker closing');
  });

  emailWorker.on('closed', () => {
    logger.info('Email worker closed');
  });
};

/**
 * Get worker status
 * @returns {Object} Worker status
 */
const getWorkerStatus = () => {
  if (!emailWorker) {
    return { status: 'not_initialized' };
  }

  return {
    status: 'running',
    isRunning: emailWorker.isRunning(),
    concurrency: emailWorker.concurrency,
    timestamp: new Date().toISOString()
  };
};

/**
 * Pause worker
 */
const pauseWorker = async () => {
  try {
    if (emailWorker) {
      await emailWorker.pause();
      logger.info('Email worker paused');
    }
  } catch (error) {
    logger.error('Error pausing email worker:', error);
    throw error;
  }
};

/**
 * Resume worker
 */
const resumeWorker = async () => {
  try {
    if (emailWorker) {
      await emailWorker.resume();
      logger.info('Email worker resumed');
    }
  } catch (error) {
    logger.error('Error resuming email worker:', error);
    throw error;
  }
};

/**
 * Close worker
 */
const closeWorker = async () => {
  try {
    if (emailWorker) {
      await emailWorker.close();
      logger.info('Email worker closed');
    }
  } catch (error) {
    logger.error('Error closing email worker:', error);
    throw error;
  }
};

/**
 * Get worker statistics
 * @returns {Object} Worker statistics
 */
const getWorkerStats = async () => {
  try {
    if (!emailWorker) {
      return { error: 'Worker not initialized' };
    }

    // This would typically get more detailed stats
    // For now, return basic info
    return {
      status: getWorkerStatus(),
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error('Error getting worker stats:', error);
    throw error;
  }
};

module.exports = {
  initializeEmailWorker,
  getWorkerStatus,
  pauseWorker,
  resumeWorker,
  closeWorker,
  getWorkerStats
};