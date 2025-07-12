const Redis = require('redis');
const logger = require('../utils/logger');

let redis;

/**
 * Initialize Redis connection
 */
const initializeRedis = async () => {
  try {
    const redisUrl = process.env.REDIS_URL || 'redis://localhost:6379';

    redis = Redis.createClient({
      url: redisUrl,
      socket: {
        reconnectStrategy: (retries) => {
          if (retries > 10) {
            logger.error('Redis reconnection failed after 10 attempts');
            return new Error('Redis reconnection limit exceeded');
          }
          return Math.min(retries * 100, 3000);
        }
      }
    });

    // Set up event listeners
    setupRedisEventListeners();

    // Connect to Redis
    await redis.connect();

    logger.info('Redis connection established', {
      url: redisUrl.replace(/\/\/.*@/, '//***:***@') // Hide credentials in logs
    });

  } catch (error) {
    logger.error('Error initializing Redis connection:', error);
    throw error;
  }
};

/**
 * Set up Redis event listeners
 */
const setupRedisEventListeners = () => {
  redis.on('connect', () => {
    logger.info('Redis client connected');
  });

  redis.on('ready', () => {
    logger.info('Redis client ready');
  });

  redis.on('error', (error) => {
    logger.error('Redis client error:', error);
  });

  redis.on('end', () => {
    logger.warn('Redis client connection ended');
  });

  redis.on('reconnecting', (params) => {
    logger.info('Redis client reconnecting', { params });
  });

  redis.on('warning', (warning) => {
    logger.warn('Redis client warning:', warning);
  });
};

/**
 * Test Redis connection
 * @returns {boolean} Connection status
 */
const testRedisConnection = async () => {
  try {
    await redis.ping();
    return true;
  } catch (error) {
    logger.error('Redis connection test failed:', error);
    return false;
  }
};

/**
 * Get Redis info
 * @returns {Object} Redis information
 */
const getRedisInfo = async () => {
  try {
    const info = await redis.info();
    const memory = await redis.memory('USAGE');

    return {
      connected: redis.isReady,
      memory: memory,
      timestamp: new Date().toISOString()
    };
  } catch (error) {
    logger.error('Error getting Redis info:', error);
    return {
      connected: false,
      error: error.message,
      timestamp: new Date().toISOString()
    };
  }
};

/**
 * Close Redis connection
 */
const closeRedis = async () => {
  try {
    if (redis && redis.isReady) {
      await redis.quit();
      logger.info('Redis connection closed');
    }
  } catch (error) {
    logger.error('Error closing Redis connection:', error);
    throw error;
  }
};

/**
 * Get Redis client instance
 * @returns {Object} Redis client
 */
const getRedisClient = () => {
  return redis;
};

module.exports = {
  initializeRedis,
  testRedisConnection,
  getRedisInfo,
  closeRedis,
  getRedisClient,
  redis
};