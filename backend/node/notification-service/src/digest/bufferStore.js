const { getRedisClient } = require('../config/redis');
const logger = require('../utils/logger');

const BUFFER_KEY_PREFIX = 'notif_buffer_';
const PRESENCE_KEY_PREFIX = 'presence_';
const BUFFER_EXPIRY = 24 * 60 * 60;

let redis;
const getRedis = () => {
  if (!redis) redis = getRedisClient();
  return redis;
};

const addToBuffer = async (userId, notification) => {
  try {
    const redisClient = getRedis();
    const key = `${BUFFER_KEY_PREFIX}${userId}`;
    const notificationData = {
      ...notification,
      timestamp: Date.now(),
      id: `${userId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    };
    logger.info('addToBuffer: before lPush');
    await redisClient.lPush(key, JSON.stringify(notificationData));
    logger.info('addToBuffer: after lPush');
    await redisClient.expire(key, BUFFER_EXPIRY);
    logger.info('addToBuffer: after expire');
    logger.debug('Notification added to buffer', {
      userId,
      notificationId: notificationData.id,
      bufferSize: await redisClient.lLen(key)
    });
    return true;
  } catch (error) {
    logger.error('Error adding notification to buffer:', error);
    return false;
  }
};

const getBuffer = async (userId) => {
  try {
    const redisClient = getRedis();
    const key = `${BUFFER_KEY_PREFIX}${userId}`;
    logger.info('getBuffer: before lRange');
    const notifications = await redisClient.lRange(key, 0, -1);
    logger.info('getBuffer: after lRange');
    return notifications.map(notification => JSON.parse(notification));
  } catch (error) {
    logger.error('Error getting notification buffer:', error);
    return [];
  }
};

const clearBuffer = async (userId) => {
  try {
    const redisClient = getRedis();
    const key = `${BUFFER_KEY_PREFIX}${userId}`;
    await redisClient.del(key);
    logger.info('Notification buffer cleared', { userId });
    return true;
  } catch (error) {
    logger.error('Error clearing notification buffer:', error);
    return false;
  }
};

const getBufferSize = async (userId) => {
  try {
    const redisClient = getRedis();
    const key = `${BUFFER_KEY_PREFIX}${userId}`;
    logger.info('getBufferSize: before lLen');
    const size = await redisClient.lLen(key);
    logger.info('getBufferSize: after lLen');
    return size;
  } catch (error) {
    logger.error('Error getting buffer size:', error);
    return 0;
  }
};

const getBufferStats = async () => {
  try {
    const redisClient = getRedis();
    const keys = await redisClient.keys(`${BUFFER_KEY_PREFIX}*`);
    let total = 0;
    for (const key of keys) {
      total += await redisClient.lLen(key);
    }
    return { totalBuffers: keys.length, totalNotifications: total };
  } catch (error) {
    logger.error('Error getting buffer statistics:', error);
    return { totalBuffers: 0, totalNotifications: 0 };
  }
};

const cleanupPresence = async () => {
  try {
    const redisClient = getRedis();
    const keys = await redisClient.keys(`${PRESENCE_KEY_PREFIX}*`);
    let cleanedCount = 0;
    for (const key of keys) {
      const ttl = await redisClient.ttl(key);
      if (ttl === -1) { // No expiry set
        await redisClient.del(key);
        cleanedCount++;
      }
    }
    if (cleanedCount > 0) {
      logger.info('Cleaned up expired presence data', { cleanedCount });
    }
    return cleanedCount;
  } catch (error) {
    logger.error('Error cleaning up presence data:', error);
    return 0;
  }
};

const isUserOnline = async (userId) => {
  try {
    const redisClient = getRedis();
    const key = `${PRESENCE_KEY_PREFIX}${userId}`;
    const presenceData = await redisClient.get(key);
    if (presenceData) {
      const presence = JSON.parse(presenceData);
      return presence.isOnline;
    }
    return false;
  } catch (error) {
    logger.error('Error checking user online status:', error);
    return false;
  }
};

const setUserPresence = async (userId, isOnline, socketId = null) => {
  try {
    const redisClient = getRedis();
    const key = `${PRESENCE_KEY_PREFIX}${userId}`;
    const presenceData = {
      userId,
      isOnline,
      socketId,
      lastUpdated: Date.now()
    };

    if (isOnline) {
      // Set presence with expiry (5 minutes)
      await redisClient.setEx(key, 300, JSON.stringify(presenceData));
    } else {
      // Remove presence data when user goes offline
      await redisClient.del(key);
    }

    logger.debug('User presence updated', {
      userId,
      isOnline,
      socketId
    });

    return true;
  } catch (error) {
    logger.error('Error setting user presence:', error);
    return false;
  }
};

const removeOfflineMessages = async (userId, messageIds) => {
  try {
    const redisClient = getRedis();
    const key = `${BUFFER_KEY_PREFIX}${userId}`;

    // Get all messages in buffer
    const messages = await redisClient.lRange(key, 0, -1);
    const parsedMessages = messages.map(msg => JSON.parse(msg));

    // Filter out the processed messages
    const remainingMessages = parsedMessages.filter(msg => !messageIds.includes(msg.id));

    // Clear the buffer and add back remaining messages
    await redisClient.del(key);

    if (remainingMessages.length > 0) {
      for (const message of remainingMessages) {
        await redisClient.lPush(key, JSON.stringify(message));
      }
      await redisClient.expire(key, BUFFER_EXPIRY);
    }

    logger.info('Removed processed messages from buffer', {
      userId,
      removedCount: messageIds.length,
      remainingCount: remainingMessages.length
    });

    return true;
  } catch (error) {
    logger.error('Error removing offline messages:', error);
    return false;
  }
};

module.exports = {
  addToBuffer,
  getBuffer,
  clearBuffer,
  getBufferSize,
  getBufferStats,
  cleanupPresence,
  isUserOnline,
  setUserPresence,
  removeOfflineMessages
};