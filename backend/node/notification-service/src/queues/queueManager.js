const { Queue, Worker } = require('bullmq');
const { getRedisClient } = require('../config/redis');
const logger = require('../../tests/services/utils/logger');

// Lazy load redis client
let redis;
const getRedis = () => {
  if (!redis) {
    redis = getRedisClient();
  }
  return redis;
};

// Queue instances
let emailQueue;
let digestQueue;

/**
 * Initialize BullMQ queues
 */
const initializeQueues = () => {
  try {
    const redisClient = getRedis();

    // Email queue for sending notifications
    emailQueue = new Queue('email-queue', {
      connection: redisClient,
      defaultJobOptions: {
        removeOnComplete: 100,
        removeOnFail: 50,
        attempts: 3,
        backoff: {
          type: 'exponential',
          delay: 2000
        }
      }
    });

    // Digest queue for batch processing
    digestQueue = new Queue('digest-queue', {
      connection: redisClient,
      defaultJobOptions: {
        removeOnComplete: 50,
        removeOnFail: 25,
        attempts: 2,
        backoff: {
          type: 'exponential',
          delay: 5000
        }
      }
    });

    logger.info('BullMQ queues initialized successfully');

    // Set up queue event listeners
    setupQueueEventListeners();

  } catch (error) {
    logger.error('Error initializing BullMQ queues:', error);
    throw error;
  }
};

/**
 * Set up queue event listeners
 */
const setupQueueEventListeners = () => {
  // Email queue events
  emailQueue.on('waiting', (job) => {
    logger.debug('Email job waiting', { jobId: job.id });
  });

  emailQueue.on('active', (job) => {
    logger.info('Email job started processing', {
      jobId: job.id,
      to: job.data.to,
      subject: job.data.subject
    });
  });

  emailQueue.on('completed', (job, result) => {
    logger.info('Email job completed successfully', {
      jobId: job.id,
      to: job.data.to,
      result: result
    });
  });

  emailQueue.on('failed', (job, err) => {
    logger.error('Email job failed', {
      jobId: job.id,
      to: job.data.to,
      error: err.message,
      attempts: job.attemptsMade
    });
  });

  emailQueue.on('stalled', (job) => {
    logger.warn('Email job stalled', { jobId: job.id });
  });

  // Digest queue events
  digestQueue.on('waiting', (job) => {
    logger.debug('Digest job waiting', { jobId: job.id });
  });

  digestQueue.on('active', (job) => {
    logger.info('Digest job started processing', {
      jobId: job.id,
      userId: job.data.userId
    });
  });

  digestQueue.on('completed', (job, result) => {
    logger.info('Digest job completed successfully', {
      jobId: job.id,
      userId: job.data.userId,
      emailsSent: result?.emailsSent || 0
    });
  });

  digestQueue.on('failed', (job, err) => {
    logger.error('Digest job failed', {
      jobId: job.id,
      userId: job.data.userId,
      error: err.message
    });
  });

  // Global queue events
  emailQueue.on('error', (error) => {
    logger.error('Email queue error:', error);
  });

  digestQueue.on('error', (error) => {
    logger.error('Digest queue error:', error);
  });
};

/**
 * Add job to email queue
 * @param {string} name - Job name
 * @param {Object} data - Job data
 * @param {Object} options - Job options
 * @returns {Object} Job instance
 */
const addEmailJob = async (name, data, options = {}) => {
  try {
    const job = await emailQueue.add(name, data, {
      priority: options.priority || 2,
      delay: options.delay || 0,
      attempts: options.attempts || 3,
      backoff: options.backoff || {
        type: 'exponential',
        delay: 2000
      },
      ...options
    });

    logger.info('Email job added to queue', {
      jobId: job.id,
      name,
      to: data.to,
      priority: options.priority || 2
    });

    return job;
  } catch (error) {
    logger.error('Error adding email job to queue:', error);
    throw error;
  }
};

/**
 * Add job to digest queue
 * @param {string} name - Job name
 * @param {Object} data - Job data
 * @param {Object} options - Job options
 * @returns {Object} Job instance
 */
const addDigestJob = async (name, data, options = {}) => {
  try {
    const job = await digestQueue.add(name, data, {
      priority: options.priority || 3,
      delay: options.delay || 0,
      attempts: options.attempts || 2,
      backoff: options.backoff || {
        type: 'exponential',
        delay: 5000
      },
      ...options
    });

    logger.info('Digest job added to queue', {
      jobId: job.id,
      name,
      userId: data.userId
    });

    return job;
  } catch (error) {
    logger.error('Error adding digest job to queue:', error);
    throw error;
  }
};

/**
 * Get queue statistics
 * @returns {Object} Queue statistics
 */
const getQueueStats = async () => {
  try {
    const emailStats = await emailQueue.getJobCounts();
    const digestStats = await digestQueue.getJobCounts();

    return {
      email: {
        waiting: emailStats.waiting,
        active: emailStats.active,
        completed: emailStats.completed,
        failed: emailStats.failed,
        delayed: emailStats.delayed,
        paused: emailStats.paused
      },
      digest: {
        waiting: digestStats.waiting,
        active: digestStats.active,
        completed: digestStats.completed,
        failed: digestStats.failed,
        delayed: digestStats.delayed,
        paused: digestStats.paused
      },
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error('Error getting queue stats:', error);
    throw error;
  }
};

/**
 * Clean completed jobs
 * @param {number} maxAge - Maximum age in milliseconds
 */
const cleanCompletedJobs = async (maxAge = 24 * 60 * 60 * 1000) => {
  try {
    const emailCleaned = await emailQueue.clean(maxAge, 'completed');
    const digestCleaned = await digestQueue.clean(maxAge, 'completed');

    logger.info('Cleaned completed jobs', {
      emailCleaned,
      digestCleaned,
      maxAge
    });

    return { emailCleaned, digestCleaned };
  } catch (error) {
    logger.error('Error cleaning completed jobs:', error);
    throw error;
  }
};

/**
 * Pause queue
 * @param {string} queueName - Queue name ('email' or 'digest')
 */
const pauseQueue = async (queueName) => {
  try {
    const queue = queueName === 'email' ? emailQueue : digestQueue;
    await queue.pause();

    logger.info(`Queue paused: ${queueName}`);
  } catch (error) {
    logger.error(`Error pausing queue ${queueName}:`, error);
    throw error;
  }
};

/**
 * Resume queue
 * @param {string} queueName - Queue name ('email' or 'digest')
 */
const resumeQueue = async (queueName) => {
  try {
    const queue = queueName === 'email' ? emailQueue : digestQueue;
    await queue.resume();

    logger.info(`Queue resumed: ${queueName}`);
  } catch (error) {
    logger.error(`Error resuming queue ${queueName}:`, error);
    throw error;
  }
};

/**
 * Get email queue instance
 * @returns {Object} Email queue instance
 */
const getEmailQueue = () => {
  if (!emailQueue) {
    throw new Error('Email queue not initialized. Call initializeQueues() first.');
  }
  return emailQueue;
};

/**
 * Get digest queue instance
 * @returns {Object} Digest queue instance
 */
const getDigestQueue = () => {
  if (!digestQueue) {
    throw new Error('Digest queue not initialized. Call initializeQueues() first.');
  }
  return digestQueue;
};

/**
 * Close all queues
 */
const closeQueues = async () => {
  try {
    if (emailQueue) {
      await emailQueue.close();
    }
    if (digestQueue) {
      await digestQueue.close();
    }

    logger.info('All queues closed');
  } catch (error) {
    logger.error('Error closing queues:', error);
    throw error;
  }
};

module.exports = {
  initializeQueues,
  emailQueue,
  digestQueue,
  getEmailQueue,
  getDigestQueue,
  addEmailJob,
  addDigestJob,
  getQueueStats,
  cleanCompletedJobs,
  pauseQueue,
  resumeQueue,
  closeQueues
};