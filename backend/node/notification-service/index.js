console.log("🚀 Starting index.js...");

try {
  require('dotenv').config();
  console.log("✅ dotenv loaded");
} catch (error) {
  console.error("❌ Failed to load dotenv:", error);
  process.exit(1);
}

try {
  const { createServer } = require('http');
  console.log("✅ http module loaded");
} catch (error) {
  console.error("❌ Failed to load http module:", error);
  process.exit(1);
}

try {
  const app = require('./src/app');
  console.log("✅ Express app loaded");
} catch (error) {
  console.error("❌ Failed to load Express app:", error);
  process.exit(1);
}

try {
  const logger = require('./src/utils/logger');
  console.log("✅ Logger loaded");
} catch (error) {
  console.error("❌ Failed to load logger:", error);
  process.exit(1);
}

try {
  const { initializeRedis } = require('./src/config/redis');
  console.log("✅ Redis config loaded");
} catch (error) {
  console.error("❌ Failed to load Redis config:", error);
  process.exit(1);
}

try {
  const { initializeQueues } = require('./src/queues/queueManager');
  console.log("✅ Queue manager loaded");
} catch (error) {
  console.error("❌ Failed to load queue manager:", error);
  process.exit(1);
}

try {
  const { startDigestJob } = require('./src/digest/digestJob');
  console.log("✅ Digest job loaded");
} catch (error) {
  console.error("❌ Failed to load digest job:", error);
  process.exit(1);
}

try {
  const { initializeSocketServer } = require('./src/sockets/socketServer');
  console.log("✅ Socket server loaded");
} catch (error) {
  console.error("❌ Failed to load socket server:", error);
  process.exit(1);
}

const { createServer } = require('http');
const app = require('./src/app');
const logger = require('./src/utils/logger');
const { initializeRedis } = require('./src/config/redis');
const { initializeQueues } = require('./src/queues/queueManager');
const { initializeEmailWorker } = require('./src/queues/emailWorker');
const { startDigestJob } = require('./src/digest/digestJob');
const { initializeSocketServer } = require('./src/sockets/socketServer');

const PORT = process.env.PORT || 3000;

console.log("🧠 Starting Notification Service...");
console.log("🔑 Loaded environment variables");

// Initialize everything asynchronously
const startServer = async () => {
  try {
    // Initialize Redis first
    console.log("🔴 Initializing Redis connection...");
    await initializeRedis();
    console.log("✅ Redis connected");

    // Create HTTP server
    const server = createServer(app);

    // Initialize BullMQ queues
    console.log("📬 Initializing BullMQ queues...");
    initializeQueues();
    console.log("✅ Queues initialized");

    // Initialize Email Worker
    console.log("📧 Initializing Email Worker...");
    initializeEmailWorker();
    console.log("✅ Email Worker initialized");

    // Initialize Socket.IO server
    console.log("🔌 Initializing Socket.IO server...");
    initializeSocketServer(server);
    console.log("✅ Socket.IO initialized");

    // Start digest job
    console.log("📧 Starting digest job...");
    startDigestJob();
    console.log("✅ Digest job started");

    // Start the server
    server.listen(PORT, () => {
      console.log(`🚀 Notification Service running at http://localhost:${PORT}`);
      logger.info(`Notification service running on port ${PORT}`);
      logger.info(`Environment: ${process.env.NODE_ENV}`);
      logger.info(`Email provider: ${process.env.EMAIL_PROVIDER}`);
    });

    // Graceful shutdown
    process.on('SIGTERM', () => {
      logger.info('SIGTERM received, shutting down gracefully');
      server.close(() => {
        logger.info('Process terminated');
        process.exit(0);
      });
    });

    process.on('SIGINT', () => {
      logger.info('SIGINT received, shutting down gracefully');
      server.close(() => {
        logger.info('Process terminated');
        process.exit(0);
      });
    });

  } catch (error) {
    console.error('❌ Failed to start server:', error);
    logger.error('Failed to start server:', error);
    process.exit(1);
  }
};

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
  console.error('❌ Uncaught Exception:', error);
  logger.error('Uncaught Exception:', error);
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('❌ Unhandled Rejection at:', promise, 'reason:', reason);
  logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
  process.exit(1);
});

console.log("🎯 About to start server...");
// Start the server
startServer();