const { Server } = require('socket.io');
const { verifyToken } = require('../utils/jwtUtils');
const { setUserPresence } = require('../digest/bufferStore');
const logger = require('../utils/logger');

let io;

/**
 * Initialize Socket.IO server
 * @param {Object} server - HTTP server instance
 */
const initializeSocketServer = (server) => {
  io = new Server(server, {
    cors: {
      origin: "*",
      methods: ["GET", "POST"]
    },
    transports: ['websocket', 'polling']
  });

  // Authentication middleware
  io.use(async (socket, next) => {
    try {
      const token = socket.handshake.query.token;
      logger.info('Socket.IO authentication attempt', { token: token ? token.slice(0, 20) + '...' : undefined });

      if (!token) {
        logger.error('Socket.IO authentication failed: No token provided');
        return next(new Error('Authentication token required'));
      }

      const decoded = await verifyToken(token);
      socket.userId = decoded.id;
      socket.userEmail = decoded.email;
      logger.info('Socket.IO authentication success', { userId: decoded.id, email: decoded.email });
      next();
    } catch (error) {
      logger.error('Socket.IO authentication failed:', error);
      next(new Error('Authentication failed: ' + error.message));
    }
  });

  // Connection handler
  io.on('connection', async (socket) => {
    const userId = socket.userId;
    const userEmail = socket.userEmail;

    logger.info('User connected via WebSocket', {
      userId,
      userEmail,
      socketId: socket.id
    });

    // Set user as online
    await setUserPresence(userId, true, socket.id);

    // Join user-specific room
    socket.join(`user:${userId}`);

    // Handle disconnect
    socket.on('disconnect', async () => {
      logger.info('User disconnected from WebSocket', {
        userId,
        userEmail,
        socketId: socket.id
      });

      // Set user as offline
      await setUserPresence(userId, false);
    });

    // Handle custom events
    socket.on('notification:acknowledge', async (data) => {
      logger.info('Notification acknowledged', {
        userId,
        notificationId: data.notificationId
      });

      // TODO: Update notification status in database
    });

    socket.on('notification:dismiss', async (data) => {
      logger.info('Notification dismissed', {
        userId,
        notificationId: data.notificationId
      });

      // TODO: Update notification status in database
    });

    // Error handling
    socket.on('error', (error) => {
      logger.error('Socket error:', error);
    });
  });

  // Global error handler for connection errors
  io.engine.on('connection_error', (err) => {
    logger.error('Socket.IO connection error:', err);
  });

  logger.info('Socket.IO server initialized');
};

/**
 * Emit notification to specific user
 * @param {string} userId - User ID
 * @param {Object} notification - Notification data
 */
const emitNotification = async (userId, notification) => {
  try {
    if (!io) {
      throw new Error('Socket.IO server not initialized');
    }

    const notificationData = {
      id: generateNotificationId(),
      ...notification,
      timestamp: new Date().toISOString()
    };

    io.to(`user:${userId}`).emit('notification', notificationData);

    logger.info('Notification emitted via WebSocket', {
      userId,
      notificationId: notificationData.id,
      type: notification.type
    });

    return notificationData;
  } catch (error) {
    logger.error('Error emitting notification:', error);
    throw error;
  }
};

/**
 * Emit notification to all connected users
 * @param {Object} notification - Notification data
 */
const emitBroadcast = async (notification) => {
  try {
    if (!io) {
      throw new Error('Socket.IO server not initialized');
    }

    const notificationData = {
      id: generateNotificationId(),
      ...notification,
      timestamp: new Date().toISOString()
    };

    io.emit('notification', notificationData);

    logger.info('Broadcast notification emitted', {
      notificationId: notificationData.id,
      type: notification.type
    });

    return notificationData;
  } catch (error) {
    logger.error('Error emitting broadcast notification:', error);
    throw error;
  }
};

/**
 * Get connected users count
 * @returns {number} Number of connected users
 */
const getConnectedUsersCount = () => {
  if (!io) {
    return 0;
  }
  return io.engine.clientsCount;
};

/**
 * Get user's socket IDs
 * @param {string} userId - User ID
 * @returns {Array} Array of socket IDs
 */
const getUserSocketIds = (userId) => {
  if (!io) {
    return [];
  }

  const room = io.sockets.adapter.rooms.get(`user:${userId}`);
  return room ? Array.from(room) : [];
};

/**
 * Generate unique notification ID
 * @returns {string} Unique notification ID
 */
const generateNotificationId = () => {
  return `notif_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Get Socket.IO server instance
 * @returns {Object} Socket.IO server instance
 */
const getIO = () => {
  return io;
};

module.exports = {
  initializeSocketServer,
  emitNotification,
  emitBroadcast,
  getConnectedUsersCount,
  getUserSocketIds,
  getIO
};