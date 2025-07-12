const winston = require('winston');
const path = require('path');

// Define log levels
const levels = {
  error: 0,
  warn: 1,
  info: 2,
  http: 3,
  debug: 4
};

// Define colors for each level
const colors = {
  error: 'red',
  warn: 'yellow',
  info: 'green',
  http: 'magenta',
  debug: 'white'
};

// Tell winston that you want to link the colors
winston.addColors(colors);

// Define which level to log based on environment
const level = () => {
  const env = process.env.NODE_ENV || 'development';
  const isDevelopment = env === 'development';
  return isDevelopment ? 'debug' : 'info';
};

// Define format for logs
const format = winston.format.combine(
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss:ms' }),
  winston.format.colorize({ all: true }),
  winston.format.printf(
    (info) => `${info.timestamp} ${info.level}: ${info.message}`
  )
);

// Define format for file logs (without colors)
const fileFormat = winston.format.combine(
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss:ms' }),
  winston.format.errors({ stack: true }),
  winston.format.json()
);

// Define transports
const transports = [
  // Console transport
  new winston.transports.Console({
    format: winston.format.combine(
      winston.format.colorize(),
      winston.format.simple()
    )
  }),

  // Error log file
  new winston.transports.File({
    filename: path.join(__dirname, '../../logs/error.log'),
    level: 'error',
    format: fileFormat,
    maxsize: 5242880, // 5MB
    maxFiles: 5
  }),

  // Combined log file
  new winston.transports.File({
    filename: path.join(__dirname, '../../logs/combined.log'),
    format: fileFormat,
    maxsize: 5242880, // 5MB
    maxFiles: 5
  })
];

// Create the logger
const logger = winston.createLogger({
  level: level(),
  levels,
  format,
  transports,
  exitOnError: false
});

// Create a stream object for Morgan HTTP logging
logger.stream = {
  write: (message) => {
    logger.http(message.trim());
  }
};

// Add custom methods for structured logging
logger.logWithContext = (level, message, context = {}) => {
  const logData = {
    message,
    ...context,
    timestamp: new Date().toISOString(),
    service: 'notification-service'
  };

  logger.log(level, JSON.stringify(logData));
};

logger.infoWithContext = (message, context = {}) => {
  logger.logWithContext('info', message, context);
};

logger.errorWithContext = (message, context = {}) => {
  logger.logWithContext('error', message, context);
};

logger.warnWithContext = (message, context = {}) => {
  logger.logWithContext('warn', message, context);
};

logger.debugWithContext = (message, context = {}) => {
  logger.logWithContext('debug', message, context);
};

// Add performance logging
logger.performance = (operation, duration, context = {}) => {
  logger.infoWithContext(`Performance: ${operation}`, {
    ...context,
    operation,
    duration: `${duration}ms`,
    performance: true
  });
};

// Add API request logging
logger.apiRequest = (method, url, statusCode, duration, context = {}) => {
  const level = statusCode >= 400 ? 'warn' : 'info';
  logger.logWithContext(level, `API Request: ${method} ${url}`, {
    ...context,
    method,
    url,
    statusCode,
    duration: `${duration}ms`,
    type: 'api_request'
  });
};

// Add notification logging
logger.notification = (action, notificationData, context = {}) => {
  logger.infoWithContext(`Notification: ${action}`, {
    ...context,
    action,
    notificationId: notificationData.id,
    type: notificationData.type,
    to: notificationData.to,
    type: 'notification'
  });
};

// Add queue logging
logger.queue = (action, jobData, context = {}) => {
  logger.infoWithContext(`Queue: ${action}`, {
    ...context,
    action,
    jobId: jobData.id,
    queue: jobData.queue,
    type: 'queue'
  });
};

// Add WebSocket logging
logger.websocket = (action, socketData, context = {}) => {
  logger.infoWithContext(`WebSocket: ${action}`, {
    ...context,
    action,
    socketId: socketData.socketId,
    userId: socketData.userId,
    type: 'websocket'
  });
};

// Add database logging
logger.database = (action, queryData, context = {}) => {
  logger.infoWithContext(`Database: ${action}`, {
    ...context,
    action,
    collection: queryData.collection,
    operation: queryData.operation,
    type: 'database'
  });
};

// Add Redis logging
logger.redis = (action, redisData, context = {}) => {
  logger.infoWithContext(`Redis: ${action}`, {
    ...context,
    action,
    key: redisData.key,
    operation: redisData.operation,
    type: 'redis'
  });
};

// Handle uncaught exceptions
logger.exceptions.handle(
  new winston.transports.File({
    filename: path.join(__dirname, '../../logs/exceptions.log'),
    format: fileFormat
  })
);

// Handle unhandled promise rejections
logger.rejections.handle(
  new winston.transports.File({
    filename: path.join(__dirname, '../../logs/rejections.log'),
    format: fileFormat
  })
);

module.exports = logger;