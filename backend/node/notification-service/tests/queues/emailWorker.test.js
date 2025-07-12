const emailWorker = require('../../src/queues/emailWorker');
const { redis } = require('../../src/config/redis');
const emailProvider = require('../../src/providers/emailProvider');
const fallbackProvider = require('../../src/providers/fallbackProvider');

// Mock dependencies
jest.mock('../../src/config/redis');
jest.mock('../../src/providers/emailProvider');
jest.mock('../../src/providers/fallbackProvider');
jest.mock('bullmq');

describe('EmailWorker', () => {
  const mockJob = {
    id: 'job123',
    data: {
      to: 'test@example.com',
      subject: 'Test Email',
      content: 'Test content',
      type: 'transactional',
      metadata: { test: true },
      userId: 'user123',
      timestamp: new Date(),
      realTimeSent: false
    },
    opts: {
      priority: 1,
      attempts: 3
    },
    attemptsMade: 0,
    updateProgress: jest.fn(),
    timestamp: Date.now(),
    processedOn: Date.now(),
    finishedOn: Date.now()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('initializeEmailWorker', () => {
    it('should initialize email worker successfully', () => {
      const { Worker } = require('bullmq');
      Worker.mockImplementation(() => ({
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5
      }));

      emailWorker.initializeEmailWorker();

      expect(Worker).toHaveBeenCalledWith(
        'email-queue',
        expect.any(Function),
        {
          connection: redis,
          concurrency: 5,
          removeOnComplete: 100,
          removeOnFail: 50
        }
      );
    });

    it('should handle initialization errors', () => {
      const { Worker } = require('bullmq');
      Worker.mockImplementation(() => {
        throw new Error('Worker initialization failed');
      });

      expect(() => emailWorker.initializeEmailWorker())
        .toThrow('Worker initialization failed');
    });
  });

  describe('processEmailJob', () => {
    beforeEach(() => {
      // Mock Worker instance
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5
      };
      Worker.mockImplementation(() => mockWorkerInstance);
    });

    it('should process email job successfully with primary provider', async () => {
      const mockResult = {
        success: true,
        messageId: 'email123',
        provider: 'resend'
      };

      emailProvider.sendEmail.mockResolvedValue(mockResult);

      const result = await emailWorker.processEmailJob(mockJob);

      expect(mockJob.updateProgress).toHaveBeenCalledWith(25);
      expect(mockJob.updateProgress).toHaveBeenCalledWith(50);
      expect(mockJob.updateProgress).toHaveBeenCalledWith(100);
      expect(emailProvider.sendEmail).toHaveBeenCalledWith({
        to: 'test@example.com',
        subject: 'Test Email',
        content: 'Test content',
        type: 'transactional',
        metadata: {
          test: true,
          jobId: 'job123',
          userId: 'user123',
          timestamp: mockJob.data.timestamp,
          realTimeSent: false
        }
      });
      expect(result).toEqual({
        success: true,
        messageId: 'email123',
        provider: 'resend',
        processingTime: expect.any(Number),
        timestamp: expect.any(String)
      });
    });

    it('should use fallback provider when primary fails', async () => {
      const primaryError = new Error('Primary provider failed');
      const fallbackResult = {
        success: true,
        messageId: 'fallback123',
        provider: 'fallback'
      };

      emailProvider.sendEmail.mockRejectedValue(primaryError);
      fallbackProvider.shouldUseFallback.mockReturnValue(true);
      fallbackProvider.sendEmail.mockResolvedValue(fallbackResult);

      const result = await emailWorker.processEmailJob(mockJob);

      expect(emailProvider.sendEmail).toHaveBeenCalled();
      expect(fallbackProvider.shouldUseFallback).toHaveBeenCalledWith(primaryError);
      expect(fallbackProvider.sendEmail).toHaveBeenCalledWith({
        to: 'test@example.com',
        subject: 'Test Email',
        content: 'Test content',
        type: 'transactional',
        metadata: {
          test: true,
          jobId: 'job123',
          userId: 'user123',
          timestamp: mockJob.data.timestamp,
          realTimeSent: false
        },
        originalError: primaryError
      });
      expect(mockJob.updateProgress).toHaveBeenCalledWith(75);
      expect(result.provider).toBe('fallback');
    });

    it('should not use fallback for non-fallback errors', async () => {
      const primaryError = new Error('Invalid email address');

      emailProvider.sendEmail.mockRejectedValue(primaryError);
      fallbackProvider.shouldUseFallback.mockReturnValue(false);

      await expect(emailWorker.processEmailJob(mockJob))
        .rejects.toThrow('Invalid email address');

      expect(emailProvider.sendEmail).toHaveBeenCalled();
      expect(fallbackProvider.sendEmail).not.toHaveBeenCalled();
    });

    it('should handle job processing failure', async () => {
      const processingError = new Error('Processing failed');
      emailProvider.sendEmail.mockRejectedValue(processingError);
      fallbackProvider.shouldUseFallback.mockReturnValue(false);

      await expect(emailWorker.processEmailJob(mockJob))
        .rejects.toThrow('Processing failed');
    });

    it('should store failed email on final attempt', async () => {
      const processingError = new Error('Processing failed');
      mockJob.attemptsMade = 3; // Final attempt

      emailProvider.sendEmail.mockRejectedValue(processingError);
      fallbackProvider.shouldUseFallback.mockReturnValue(false);
      fallbackProvider.storeFailedEmail.mockResolvedValue({ id: 'failed123' });

      await expect(emailWorker.processEmailJob(mockJob))
        .rejects.toThrow('Processing failed');

      expect(fallbackProvider.storeFailedEmail).toHaveBeenCalledWith(
        mockJob.data,
        processingError
      );
    });

    it('should not store failed email on non-final attempts', async () => {
      const processingError = new Error('Processing failed');
      mockJob.attemptsMade = 1; // Not final attempt

      emailProvider.sendEmail.mockRejectedValue(processingError);
      fallbackProvider.shouldUseFallback.mockReturnValue(false);

      await expect(emailWorker.processEmailJob(mockJob))
        .rejects.toThrow('Processing failed');

      expect(fallbackProvider.storeFailedEmail).not.toHaveBeenCalled();
    });
  });

  describe('setupWorkerEventListeners', () => {
    it('should set up all worker event listeners', () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();

      expect(mockWorkerInstance.on).toHaveBeenCalledWith('completed', expect.any(Function));
      expect(mockWorkerInstance.on).toHaveBeenCalledWith('failed', expect.any(Function));
      expect(mockWorkerInstance.on).toHaveBeenCalledWith('error', expect.any(Function));
      expect(mockWorkerInstance.on).toHaveBeenCalledWith('stalled', expect.any(Function));
      expect(mockWorkerInstance.on).toHaveBeenCalledWith('closing', expect.any(Function));
      expect(mockWorkerInstance.on).toHaveBeenCalledWith('closed', expect.any(Function));
    });
  });

  describe('getWorkerStatus', () => {
    it('should return worker status when initialized', () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();
      const status = emailWorker.getWorkerStatus();

      expect(status).toEqual({
        status: 'running',
        isRunning: true,
        concurrency: 5,
        timestamp: expect.any(String)
      });
    });

    it('should return not initialized status when worker is not initialized', () => {
      const status = emailWorker.getWorkerStatus();

      expect(status).toEqual({
        status: 'not_initialized'
      });
    });
  });

  describe('pauseWorker', () => {
    it('should pause worker successfully', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5,
        pause: jest.fn()
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();
      await emailWorker.pauseWorker();

      expect(mockWorkerInstance.pause).toHaveBeenCalled();
    });

    it('should handle pause errors', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5,
        pause: jest.fn().mockRejectedValue(new Error('Pause failed'))
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();

      await expect(emailWorker.pauseWorker())
        .rejects.toThrow('Pause failed');
    });
  });

  describe('resumeWorker', () => {
    it('should resume worker successfully', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5,
        resume: jest.fn()
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();
      await emailWorker.resumeWorker();

      expect(mockWorkerInstance.resume).toHaveBeenCalled();
    });

    it('should handle resume errors', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5,
        resume: jest.fn().mockRejectedValue(new Error('Resume failed'))
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();

      await expect(emailWorker.resumeWorker())
        .rejects.toThrow('Resume failed');
    });
  });

  describe('closeWorker', () => {
    it('should close worker successfully', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5,
        close: jest.fn()
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();
      await emailWorker.closeWorker();

      expect(mockWorkerInstance.close).toHaveBeenCalled();
    });

    it('should handle close errors', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5,
        close: jest.fn().mockRejectedValue(new Error('Close failed'))
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();

      await expect(emailWorker.closeWorker())
        .rejects.toThrow('Close failed');
    });
  });

  describe('getWorkerStats', () => {
    it('should return worker statistics', async () => {
      const { Worker } = require('bullmq');
      const mockWorkerInstance = {
        on: jest.fn(),
        isRunning: jest.fn().mockReturnValue(true),
        concurrency: 5
      };
      Worker.mockImplementation(() => mockWorkerInstance);

      emailWorker.initializeEmailWorker();
      const stats = await emailWorker.getWorkerStats();

      expect(stats).toEqual({
        status: {
          status: 'running',
          isRunning: true,
          concurrency: 5,
          timestamp: expect.any(String)
        },
        timestamp: expect.any(String)
      });
    });

    it('should return error when worker not initialized', async () => {
      const stats = await emailWorker.getWorkerStats();

      expect(stats).toEqual({
        error: 'Worker not initialized'
      });
    });
  });
});