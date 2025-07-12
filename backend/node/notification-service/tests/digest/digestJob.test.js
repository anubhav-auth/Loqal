const digestJob = require('../../src/digest/digestJob');
const { getOfflineMessages, removeOfflineMessages, getUsersWithOfflineMessages, cleanExpiredMessages } = require('../../src/digest/bufferStore');
const emailProvider = require('../../src/providers/emailProvider');
const { addDigestJob } = require('../../src/queues/queueManager');

// Mock dependencies
jest.mock('../../src/digest/bufferStore');
jest.mock('../../src/providers/emailProvider');
jest.mock('../../src/queues/queueManager');
jest.mock('node-cron');

describe('DigestJob', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    process.env.DIGEST_INTERVAL = '30';
  });

  describe('startDigestJob', () => {
    it('should start digest job scheduler', () => {
      const mockCron = require('node-cron');
      mockCron.schedule.mockReturnValue({
        start: jest.fn(),
        stop: jest.fn()
      });

      digestJob.startDigestJob();

      expect(mockCron.schedule).toHaveBeenCalledWith(
        '*/30 * * * * *',
        expect.any(Function),
        {
          scheduled: true,
          timezone: 'UTC'
        }
      );
    });

    it('should handle errors when starting scheduler', () => {
      const mockCron = require('node-cron');
      mockCron.schedule.mockImplementation(() => {
        throw new Error('Cron error');
      });

      expect(() => digestJob.startDigestJob()).toThrow('Cron error');
    });
  });

  describe('processDigestJob', () => {
    it('should process digest for users with offline messages', async () => {
      const mockUsers = ['user1', 'user2'];
      const mockMessages = [
        {
          id: 'msg1',
          to: 'user1@example.com',
          subject: 'Test Message 1',
          content: 'Content 1',
          type: 'transactional',
          timestamp: Date.now()
        },
        {
          id: 'msg2',
          to: 'user1@example.com',
          subject: 'Test Message 2',
          content: 'Content 2',
          type: 'promotional',
          timestamp: Date.now()
        }
      ];

      getUsersWithOfflineMessages.mockResolvedValue(mockUsers);
      getOfflineMessages.mockResolvedValue(mockMessages);
      addDigestJob.mockResolvedValue({ id: 'job123' });

      await digestJob.processDigestJob();

      expect(cleanExpiredMessages).toHaveBeenCalled();
      expect(getUsersWithOfflineMessages).toHaveBeenCalled();
      expect(getOfflineMessages).toHaveBeenCalledWith('user1', 50);
      expect(getOfflineMessages).toHaveBeenCalledWith('user2', 50);
      expect(addDigestJob).toHaveBeenCalledTimes(2);
    });

    it('should handle empty users list', async () => {
      getUsersWithOfflineMessages.mockResolvedValue([]);

      await digestJob.processDigestJob();

      expect(cleanExpiredMessages).toHaveBeenCalled();
      expect(getUsersWithOfflineMessages).toHaveBeenCalled();
      expect(addDigestJob).not.toHaveBeenCalled();
    });

    it('should handle errors for individual users', async () => {
      const mockUsers = ['user1', 'user2'];

      getUsersWithOfflineMessages.mockResolvedValue(mockUsers);
      getOfflineMessages.mockRejectedValueOnce(new Error('Redis error'));

      await digestJob.processDigestJob();

      expect(cleanExpiredMessages).toHaveBeenCalled();
      expect(getUsersWithOfflineMessages).toHaveBeenCalled();
      // Should continue processing other users even if one fails
    });
  });

  describe('processUserDigest', () => {
    it('should process digest for a specific user', async () => {
      const userId = 'user123';
      const mockMessages = [
        {
          id: 'msg1',
          to: 'user@example.com',
          subject: 'Test Message 1',
          content: 'Content 1',
          type: 'transactional',
          timestamp: Date.now()
        },
        {
          id: 'msg2',
          to: 'user@example.com',
          subject: 'Test Message 2',
          content: 'Content 2',
          type: 'promotional',
          timestamp: Date.now()
        }
      ];

      getOfflineMessages.mockResolvedValue(mockMessages);
      addDigestJob.mockResolvedValue({ id: 'job123' });

      await digestJob.processUserDigest(userId);

      expect(getOfflineMessages).toHaveBeenCalledWith(userId, 50);
      expect(addDigestJob).toHaveBeenCalledWith(
        'send-digest',
        {
          userId,
          emailData: expect.objectContaining({
            to: 'user@example.com',
            subject: expect.stringContaining('2 new messages'),
            type: 'transactional'
          }),
          messageIds: ['msg1', 'msg2']
        },
        {
          priority: 2,
          delay: 0
        }
      );
    });

    it('should handle empty messages for user', async () => {
      const userId = 'user123';
      getOfflineMessages.mockResolvedValue([]);

      await digestJob.processUserDigest(userId);

      expect(getOfflineMessages).toHaveBeenCalledWith(userId, 50);
      expect(addDigestJob).not.toHaveBeenCalled();
    });

    it('should handle errors during processing', async () => {
      const userId = 'user123';
      getOfflineMessages.mockRejectedValue(new Error('Processing error'));

      await expect(digestJob.processUserDigest(userId))
        .rejects.toThrow('Processing error');
    });
  });

  describe('groupMessagesByType', () => {
    it('should group messages by type correctly', () => {
      const messages = [
        { type: 'transactional', subject: 'Trans 1' },
        { type: 'promotional', subject: 'Promo 1' },
        { type: 'transactional', subject: 'Trans 2' },
        { type: 'promotional', subject: 'Promo 2' }
      ];

      const result = digestJob.groupMessagesByType(messages);

      expect(result.transactional).toHaveLength(2);
      expect(result.promotional).toHaveLength(2);
      expect(result.transactional[0].subject).toBe('Trans 1');
      expect(result.promotional[0].subject).toBe('Promo 1');
    });

    it('should default to transactional for unknown types', () => {
      const messages = [
        { type: 'unknown', subject: 'Unknown 1' },
        { type: 'transactional', subject: 'Trans 1' }
      ];

      const result = digestJob.groupMessagesByType(messages);

      expect(result.transactional).toHaveLength(2);
      expect(result.promotional).toHaveLength(0);
    });
  });

  describe('createDigestContent', () => {
    it('should create digest content with both message types', () => {
      const groupedMessages = {
        transactional: [
          { subject: 'Trans 1', content: 'Content 1', timestamp: Date.now() }
        ],
        promotional: [
          { subject: 'Promo 1', content: 'Content 2', timestamp: Date.now() }
        ]
      };

      const content = digestJob.createDigestContent(groupedMessages);

      expect(content).toContain('Your Notification Digest');
      expect(content).toContain('2 new notifications');
      expect(content).toContain('Important Notifications');
      expect(content).toContain('Updates & Offers');
      expect(content).toContain('Trans 1');
      expect(content).toContain('Promo 1');
    });

    it('should create digest content with only transactional messages', () => {
      const groupedMessages = {
        transactional: [
          { subject: 'Trans 1', content: 'Content 1', timestamp: Date.now() }
        ],
        promotional: []
      };

      const content = digestJob.createDigestContent(groupedMessages);

      expect(content).toContain('1 new notification');
      expect(content).toContain('Important Notifications');
      expect(content).not.toContain('Updates & Offers');
    });

    it('should create digest content with only promotional messages', () => {
      const groupedMessages = {
        transactional: [],
        promotional: [
          { subject: 'Promo 1', content: 'Content 1', timestamp: Date.now() }
        ]
      };

      const content = digestJob.createDigestContent(groupedMessages);

      expect(content).toContain('1 new notification');
      expect(content).not.toContain('Important Notifications');
      expect(content).toContain('Updates & Offers');
    });
  });

  describe('sendDigestEmail', () => {
    it('should send digest email and remove messages', async () => {
      const userId = 'user123';
      const emailData = {
        to: 'user@example.com',
        subject: 'Digest',
        content: 'Content'
      };
      const messageIds = ['msg1', 'msg2'];

      emailProvider.sendEmail.mockResolvedValue({
        success: true,
        messageId: 'email123'
      });

      const result = await digestJob.sendDigestEmail(userId, emailData, messageIds);

      expect(emailProvider.sendEmail).toHaveBeenCalledWith(emailData);
      expect(removeOfflineMessages).toHaveBeenCalledWith(userId, messageIds);
      expect(result).toEqual({
        success: true,
        messageId: 'email123'
      });
    });

    it('should handle email sending failure', async () => {
      const userId = 'user123';
      const emailData = { to: 'user@example.com', subject: 'Digest', content: 'Content' };
      const messageIds = ['msg1'];

      emailProvider.sendEmail.mockRejectedValue(new Error('Email failed'));

      await expect(digestJob.sendDigestEmail(userId, emailData, messageIds))
        .rejects.toThrow('Email failed');

      expect(emailProvider.sendEmail).toHaveBeenCalledWith(emailData);
      expect(removeOfflineMessages).not.toHaveBeenCalled();
    });
  });

  describe('stopDigestJob', () => {
    it('should stop digest job scheduler', () => {
      const mockCron = require('node-cron');
      const mockScheduler = {
        start: jest.fn(),
        stop: jest.fn()
      };
      mockCron.schedule.mockReturnValue(mockScheduler);

      // Start the job first
      digestJob.startDigestJob();

      // Then stop it
      digestJob.stopDigestJob();

      expect(mockScheduler.stop).toHaveBeenCalled();
    });
  });

  describe('getDigestJobStatus', () => {
    it('should return digest job status', () => {
      const mockCron = require('node-cron');
      const mockScheduler = {
        running: true,
        scheduled: true
      };
      mockCron.schedule.mockReturnValue(mockScheduler);

      digestJob.startDigestJob();
      const status = digestJob.getDigestJobStatus();

      expect(status).toEqual({
        running: true,
        scheduled: true,
        interval: '30',
        timestamp: expect.any(String)
      });
    });
  });

  describe('triggerDigestProcessing', () => {
    it('should manually trigger digest processing', async () => {
      const mockUsers = ['user1'];
      getUsersWithOfflineMessages.mockResolvedValue(mockUsers);
      getOfflineMessages.mockResolvedValue([]);

      await digestJob.triggerDigestProcessing();

      expect(cleanExpiredMessages).toHaveBeenCalled();
      expect(getUsersWithOfflineMessages).toHaveBeenCalled();
    });

    it('should handle errors during manual trigger', async () => {
      getUsersWithOfflineMessages.mockRejectedValue(new Error('Trigger error'));

      await expect(digestJob.triggerDigestProcessing())
        .rejects.toThrow('Trigger error');
    });
  });
});