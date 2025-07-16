const notificationService = require('../../src/services/notificationService');
const { emailQueue } = require('../../src/queues/queueManager');
const { isUserOnline, storeOfflineMessage } = require('../../src/digest/bufferStore');
const { emitNotification } = require('../../src/sockets/socketServer');

// Mock dependencies
jest.mock('../../src/queues/queueManager');
jest.mock('../../src/digest/bufferStore');
jest.mock('../../src/sockets/socketServer');

describe('NotificationService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('sendNotification', () => {
    const mockNotificationData = {
      to: 'test@example.com',
      subject: 'Test Notification',
      content: 'This is a test notification',
      type: 'transactional',
      priority: 'p1',
      metadata: { test: true },
      userId: 'user123',
      timestamp: new Date()
    };

    it('should send notification when user is online', async () => {
      // Mock user online
      isUserOnline.mockResolvedValue(true);
      emitNotification.mockResolvedValue({ id: 'notif123' });
      emailQueue.add.mockResolvedValue({ id: 'job123' });

      const result = await notificationService.sendNotification(mockNotificationData);

      expect(isUserOnline).toHaveBeenCalledWith('user123');
      expect(emitNotification).toHaveBeenCalledWith('user123', {
        type: 'email',
        subject: 'Test Notification',
        content: 'This is a test notification',
        timestamp: mockNotificationData.timestamp,
        metadata: { test: true }
      });
      expect(emailQueue.add).toHaveBeenCalledWith(
        'send-email',
        expect.objectContaining({
          to: 'test@example.com',
          subject: 'Test Notification',
          realTimeSent: true
        }),
        expect.objectContaining({
          priority: 1, // p0 for transactional
          attempts: 3
        })
      );
      expect(result.jobId).toBe('job123');
      expect(result.realTimeSent).toBe(true);
    });

    it('should store offline message when user is offline', async () => {
      // Mock user offline
      isUserOnline.mockResolvedValue(false);
      storeOfflineMessage.mockResolvedValue({ id: 'msg123' });
      emailQueue.add.mockResolvedValue({ id: 'job123' });

      const result = await notificationService.sendNotification(mockNotificationData);

      expect(isUserOnline).toHaveBeenCalledWith('user123');
      expect(storeOfflineMessage).toHaveBeenCalledWith('user123', {
        to: 'test@example.com',
        subject: 'Test Notification',
        content: 'This is a test notification',
        type: 'transactional',
        metadata: { test: true },
        timestamp: mockNotificationData.timestamp
      });
      expect(emailQueue.add).toHaveBeenCalledWith(
        'send-email',
        expect.objectContaining({
          realTimeSent: false
        }),
        expect.any(Object)
      );
      expect(result.realTimeSent).toBe(false);
    });

    it('should assign correct priority based on type', async () => {
      isUserOnline.mockResolvedValue(true);
      emitNotification.mockResolvedValue({ id: 'notif123' });
      emailQueue.add.mockResolvedValue({ id: 'job123' });

      // Test promotional type
      const promotionalData = { ...mockNotificationData, type: 'promotional' };
      await notificationService.sendNotification(promotionalData);

      expect(emailQueue.add).toHaveBeenCalledWith(
        'send-email',
        expect.any(Object),
        expect.objectContaining({
          priority: 3 // p2 for promotional
        })
      );
    });

    it('should handle errors gracefully', async () => {
      isUserOnline.mockRejectedValue(new Error('Redis error'));

      await expect(notificationService.sendNotification(mockNotificationData))
        .rejects.toThrow('Redis error');
    });
  });

  describe('getJobStatus', () => {
    it('should return job status for valid job', async () => {
      const mockJob = {
        id: 'job123',
        data: {
          to: 'test@example.com',
          subject: 'Test',
          type: 'transactional',
          timestamp: new Date(),
          userId: 'user123'
        },
        getState: jest.fn().mockResolvedValue('completed'),
        progress: jest.fn().mockResolvedValue(100),
        failedReason: null,
        timestamp: Date.now(),
        processedOn: Date.now(),
        finishedOn: Date.now()
      };

      emailQueue.getJob.mockResolvedValue(mockJob);

      const result = await notificationService.getJobStatus('job123', 'user123');

      expect(result).toEqual({
        id: 'job123',
        state: 'completed',
        progress: 100,
        failedReason: null,
        data: {
          to: 'test@example.com',
          subject: 'Test',
          type: 'transactional',
          timestamp: mockJob.data.timestamp
        },
        createdAt: mockJob.timestamp,
        processedAt: mockJob.processedOn,
        finishedAt: mockJob.finishedOn
      });
    });

    it('should throw error for non-existent job', async () => {
      emailQueue.getJob.mockResolvedValue(null);

      await expect(notificationService.getJobStatus('invalid', 'user123'))
        .rejects.toThrow('Job not found');
    });

    it('should throw error for unauthorized access', async () => {
      const mockJob = {
        data: { userId: 'other-user' }
      };

      emailQueue.getJob.mockResolvedValue(mockJob);

      await expect(notificationService.getJobStatus('job123', 'user123'))
        .rejects.toThrow('Unauthorized access to job');
    });
  });

  describe('getNotificationHistory', () => {
    it('should return notification history with pagination', async () => {
      const result = await notificationService.getNotificationHistory('user123', 1, 20, 'transactional');

      expect(result).toEqual({
        notifications: [],
        pagination: {
          page: 1,
          limit: 20,
          total: 0,
          pages: 0,
          hasNext: false,
          hasPrev: false
        }
      });
    });
  });

  describe('getStats', () => {
    it('should return user statistics', async () => {
      const result = await notificationService.getStats('user123');

      expect(result).toEqual({
        totalSent: 0,
        totalDelivered: 0,
        totalFailed: 0,
        byType: {
          transactional: 0,
          promotional: 0
        },
        byPriority: {
          p0: 0,
          p1: 0,
          p2: 0
        },
        averageDeliveryTime: 0,
        successRate: 100
      });
    });
  });
});