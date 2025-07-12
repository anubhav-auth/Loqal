const { getRedisClient } = require('../config/redis');
const logger = require('./logger');

// Lazy load redis client
let redis;
const getRedis = () => {
  if (!redis) {
    redis = getRedisClient();
  }
  return redis;
};

const RATE_LIMIT_WINDOW = 15 * 60 * 1000; // 15 minutes
const RATE_LIMIT_MAX_REQUESTS = 100; // 100 requests per window
const RATE_LIMIT_BURST = 10; // Allow burst of 10 requests

/**
 * Rate limiter middleware
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const rateLimiter = async (req, res, next) => {
  try {
    const clientId = getClientIdentifier(req);
    const key = `rate_limit:${clientId}`;

    const currentTime = Date.now();
    const windowStart = currentTime - RATE_LIMIT_WINDOW;

    // Get current request count
    const requests = await getRequestCount(key, windowStart);

    if (requests >= RATE_LIMIT_MAX_REQUESTS) {
      logger.warn('Rate limit exceeded', {
        clientId,
        requests,
        limit: RATE_LIMIT_MAX_REQUESTS,
        window: RATE_LIMIT_WINDOW
      });

      return res.status(429).json({
        error: 'Rate limit exceeded',
        message: 'Too many requests. Please try again later.',
        retryAfter: Math.ceil(RATE_LIMIT_WINDOW / 1000)
      });
    }

    // Add current request
    await addRequest(key, currentTime);

    // Set response headers
    res.set({
      'X-RateLimit-Limit': RATE_LIMIT_MAX_REQUESTS,
      'X-RateLimit-Remaining': RATE_LIMIT_MAX_REQUESTS - requests - 1,
      'X-RateLimit-Reset': new Date(currentTime + RATE_LIMIT_WINDOW).toISOString()
    });

    next();
  } catch (error) {
    logger.error('Rate limiter error:', error);
    // Continue without rate limiting on error
    next();
  }
};

/**
 * Get client identifier for rate limiting
 * @param {Object} req - Express request object
 * @returns {string} Client identifier
 */
const getClientIdentifier = (req) => {
  // Use IP address as primary identifier
  const ip = req.ip || req.connection.remoteAddress || req.socket.remoteAddress;

  // If user is authenticated, include user ID for more granular limiting
  if (req.user && req.user.id) {
    return `${ip}:${req.user.id}`;
  }

  return ip;
};

/**
 * Get request count for a client within the time window
 * @param {string} key - Redis key
 * @param {number} windowStart - Window start timestamp
 * @returns {number} Request count
 */
const getRequestCount = async (key, windowStart) => {
  try {
    const redisClient = getRedis();
    const requests = await redisClient.zRangeByScore(key, windowStart, '+inf');
    return requests.length;
  } catch (error) {
    logger.error('Error getting request count:', error);
    return 0;
  }
};

/**
 * Add request to rate limit tracking
 * @param {string} key - Redis key
 * @param {number} timestamp - Request timestamp
 */
const addRequest = async (key, timestamp) => {
  try {
    const redisClient = getRedis();
    await redisClient.zAdd(key, [{ score: timestamp, value: timestamp.toString() }]);
    await redisClient.expire(key, Math.ceil(RATE_LIMIT_WINDOW / 1000));
  } catch (error) {
    logger.error('Error adding request to rate limit:', error);
  }
};

/**
 * Strict rate limiter for sensitive endpoints
 * @param {number} maxRequests - Maximum requests per window
 * @param {number} windowMs - Time window in milliseconds
 * @returns {Function} Express middleware function
 */
const strictRateLimiter = (maxRequests = 10, windowMs = 60 * 1000) => {
  return async (req, res, next) => {
    try {
      const clientId = getClientIdentifier(req);
      const key = `strict_rate_limit:${clientId}`;

      const currentTime = Date.now();
      const windowStart = currentTime - windowMs;

      const requests = await getRequestCount(key, windowStart);

      if (requests >= maxRequests) {
        logger.warn('Strict rate limit exceeded', {
          clientId,
          requests,
          limit: maxRequests,
          window: windowMs
        });

        return res.status(429).json({
          error: 'Rate limit exceeded',
          message: 'Too many requests to this endpoint. Please try again later.',
          retryAfter: Math.ceil(windowMs / 1000)
        });
      }

      await addRequest(key, currentTime);

      res.set({
        'X-RateLimit-Limit': maxRequests,
        'X-RateLimit-Remaining': maxRequests - requests - 1,
        'X-RateLimit-Reset': new Date(currentTime + windowMs).toISOString()
      });

      next();
    } catch (error) {
      logger.error('Strict rate limiter error:', error);
      next();
    }
  };
};

/**
 * User-specific rate limiter
 * @param {number} maxRequests - Maximum requests per window
 * @param {number} windowMs - Time window in milliseconds
 * @returns {Function} Express middleware function
 */
const userRateLimiter = (maxRequests = 50, windowMs = 15 * 60 * 1000) => {
  return async (req, res, next) => {
    try {
      if (!req.user || !req.user.id) {
        return res.status(401).json({
          error: 'Authentication required',
          message: 'User authentication required for rate limiting'
        });
      }

      const userId = req.user.id;
      const key = `user_rate_limit:${userId}`;

      const currentTime = Date.now();
      const windowStart = currentTime - windowMs;

      const requests = await getRequestCount(key, windowStart);

      if (requests >= maxRequests) {
        logger.warn('User rate limit exceeded', {
          userId,
          requests,
          limit: maxRequests,
          window: windowMs
        });

        return res.status(429).json({
          error: 'Rate limit exceeded',
          message: 'You have exceeded your request limit. Please try again later.',
          retryAfter: Math.ceil(windowMs / 1000)
        });
      }

      await addRequest(key, currentTime);

      res.set({
        'X-RateLimit-Limit': maxRequests,
        'X-RateLimit-Remaining': maxRequests - requests - 1,
        'X-RateLimit-Reset': new Date(currentTime + windowMs).toISOString()
      });

      next();
    } catch (error) {
      logger.error('User rate limiter error:', error);
      next();
    }
  };
};

/**
 * Get rate limit status for a client
 * @param {string} clientId - Client identifier
 * @returns {Object} Rate limit status
 */
const getRateLimitStatus = async (clientId) => {
  try {
    const redisClient = getRedis();
    const key = `rate_limit:${clientId}`;
    const currentTime = Date.now();
    const windowStart = currentTime - RATE_LIMIT_WINDOW;

    const requests = await getRequestCount(key, windowStart);
    const remaining = Math.max(0, RATE_LIMIT_MAX_REQUESTS - requests);
    const resetTime = new Date(currentTime + RATE_LIMIT_WINDOW);

    return {
      clientId,
      requests,
      limit: RATE_LIMIT_MAX_REQUESTS,
      remaining,
      resetTime: resetTime.toISOString(),
      window: RATE_LIMIT_WINDOW
    };
  } catch (error) {
    logger.error('Error getting rate limit status:', error);
    return {
      clientId,
      requests: 0,
      limit: RATE_LIMIT_MAX_REQUESTS,
      remaining: RATE_LIMIT_MAX_REQUESTS,
      resetTime: new Date().toISOString(),
      window: RATE_LIMIT_WINDOW,
      error: error.message
    };
  }
};

/**
 * Reset rate limit for a client
 * @param {string} clientId - Client identifier
 * @returns {boolean} Success status
 */
const resetRateLimit = async (clientId) => {
  try {
    const redisClient = getRedis();
    const key = `rate_limit:${clientId}`;
    await redisClient.del(key);
    logger.info('Rate limit reset for client', { clientId });
    return true;
  } catch (error) {
    logger.error('Error resetting rate limit:', error);
    return false;
  }
};

/**
 * Clean up expired rate limit entries
 * @returns {number} Number of entries cleaned up
 */
const cleanupRateLimits = async () => {
  try {
    const redisClient = getRedis();
    const currentTime = Date.now();
    const windowStart = currentTime - RATE_LIMIT_WINDOW;

    // Get all rate limit keys
    const keys = await redisClient.keys('rate_limit:*');
    let cleanedCount = 0;

    for (const key of keys) {
      const requests = await redisClient.zRangeByScore(key, '-inf', windowStart);
      if (requests.length > 0) {
        await redisClient.zRemRangeByScore(key, '-inf', windowStart);
        cleanedCount += requests.length;
      }
    }

    logger.info('Rate limit cleanup completed', { cleanedCount });
    return cleanedCount;
  } catch (error) {
    logger.error('Error cleaning up rate limits:', error);
    return 0;
  }
};

module.exports = {
  rateLimiter,
  strictRateLimiter,
  userRateLimiter,
  getRateLimitStatus,
  resetRateLimit,
  cleanupRateLimits
};