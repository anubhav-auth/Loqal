const express = require('express');
const notificationController = require('../controllers/notificationController');

const router = express.Router();

/**
 * @route POST /notifications/send
 * @desc Send a notification (adds to BullMQ queue)
 * @access Private
 */
router.post('/send', notificationController.sendNotification);

/**
 * @route POST /notifications/test
 * @desc Send a test notification
 * @access Private
 */
router.post('/test', notificationController.sendTestNotification);

/**
 * @route POST /notifications/test-p0
 * @desc Send a P0 priority test notification (for OTP, critical alerts, etc.)
 * @access Private
 */
router.post('/test-p0', notificationController.sendP0TestNotification);

/**
 * @route POST /notifications/test-dual-delivery
 * @desc Send a dual delivery test notification (WebSocket + Email)
 * @access Private
 */
router.post('/test-dual-delivery', notificationController.sendDualDeliveryTest);

/**
 * @route GET /notifications/status/:jobId
 * @desc Get notification status by job ID
 * @access Private
 */
router.get('/status/:jobId', notificationController.getJobStatus);

/**
 * @route GET /notifications/history
 * @desc Get notification history
 * @access Private
 */
router.get('/history', notificationController.getNotificationHistory);

/**
 * @route POST /notifications/generate-token
 * @desc Generate a test JWT token
 * @access Public
 */
router.post('/generate-token', notificationController.generateTestToken);

/**
 * @route GET /notifications/stats
 * @desc Get notification service statistics
 * @access Private
 */
router.get('/stats', notificationController.getStats);

module.exports = router;