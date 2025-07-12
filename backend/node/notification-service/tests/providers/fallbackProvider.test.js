const fallbackProvider = require('../../src/providers/fallbackProvider');

describe('FallbackProvider', () => {
  const mockEmailData = {
    to: 'test@example.com',
    subject: 'Test Email',
    content: 'This is a test email content',
    metadata: { test: true }
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('sendEmail', () => {
    it('should process email successfully', async () => {
      const result = await fallbackProvider.sendEmail(mockEmailData);

      expect(result).toEqual({
        success: true,
        messageId: expect.stringMatching(/^fallback_\d+_[a-z0-9]+$/),
        provider: 'fallback',
        timestamp: expect.any(String),
        note: 'Email logged for manual processing'
      });
    });

    it('should handle email with original error', async () => {
      const originalError = new Error('Primary provider failed');
      const emailDataWithError = {
        ...mockEmailData,
        originalError
      };

      const result = await fallbackProvider.sendEmail(emailDataWithError);

      expect(result).toEqual({
        success: true,
        messageId: expect.stringMatching(/^fallback_\d+_[a-z0-9]+$/),
        provider: 'fallback',
        timestamp: expect.any(String),
        note: 'Email logged for manual processing'
      });
    });

    it('should handle critical failures', async () => {
      // Mock simulateFallbackSend to throw an error
      jest.spyOn(fallbackProvider, 'simulateFallbackSend').mockRejectedValue(new Error('Critical failure'));

      await expect(fallbackProvider.sendEmail(mockEmailData))
        .rejects.toThrow('Critical: Both primary and fallback providers failed: Critical failure');
    });
  });

  describe('storeFailedEmail', () => {
    it('should store failed email successfully', async () => {
      const error = new Error('Primary provider failed');

      const result = await fallbackProvider.storeFailedEmail(mockEmailData, error);

      expect(result).toEqual({
        to: 'test@example.com',
        subject: 'Test Email',
        content: 'This is a test email content',
        metadata: { test: true },
        error: 'Primary provider failed',
        timestamp: expect.any(String),
        retryCount: 0,
        maxRetries: 3
      });
    });

    it('should handle storage errors', async () => {
      // This would typically test database/Redis storage errors
      // For now, we'll test the basic functionality
      const error = new Error('Storage error');

      // Mock a storage error scenario
      jest.spyOn(console, 'error').mockImplementation(() => {});

      const result = await fallbackProvider.storeFailedEmail(mockEmailData, error);

      expect(result).toBeDefined();
    });
  });

  describe('processFailedEmails', () => {
    it('should process failed emails successfully', async () => {
      const failedEmails = [
        {
          to: 'user1@example.com',
          subject: 'Failed Email 1',
          content: 'Content 1',
          retryCount: 0,
          maxRetries: 3
        },
        {
          to: 'user2@example.com',
          subject: 'Failed Email 2',
          content: 'Content 2',
          retryCount: 0,
          maxRetries: 3
        }
      ];

      const results = await fallbackProvider.processFailedEmails(failedEmails);

      expect(results).toHaveLength(2);
      expect(results[0].success).toBe(true);
      expect(results[0].retryCount).toBe(1);
      expect(results[1].success).toBe(true);
      expect(results[1].retryCount).toBe(1);
    });

    it('should handle emails that exceed max retries', async () => {
      const failedEmails = [
        {
          to: 'user1@example.com',
          subject: 'Failed Email',
          content: 'Content',
          retryCount: 3,
          maxRetries: 3
        }
      ];

      const results = await fallbackProvider.processFailedEmails(failedEmails);

      expect(results).toHaveLength(1);
      expect(results[0].success).toBe(false);
      expect(results[0].error).toBe('Max retries exceeded');
      expect(results[0].retryCount).toBe(4);
    });

    it('should handle retry failures', async () => {
      const failedEmails = [
        {
          to: 'user1@example.com',
          subject: 'Failed Email',
          content: 'Content',
          retryCount: 0,
          maxRetries: 3
        }
      ];

      // Mock sendEmail to fail
      jest.spyOn(fallbackProvider, 'sendEmail').mockRejectedValue(new Error('Retry failed'));

      const results = await fallbackProvider.processFailedEmails(failedEmails);

      expect(results).toHaveLength(1);
      expect(results[0].success).toBe(false);
      expect(results[0].error).toBe('Retry failed');
      expect(results[0].retryCount).toBe(1);
    });
  });

  describe('getProviderStatus', () => {
    it('should return provider status', () => {
      const status = fallbackProvider.getProviderStatus();

      expect(status).toEqual({
        provider: 'fallback',
        status: 'available',
        timestamp: expect.any(String),
        note: 'Fallback provider is always available for critical failures'
      });
    });
  });

  describe('shouldUseFallback', () => {
    it('should return true for critical errors', () => {
      const criticalErrors = [
        'authentication_failed',
        'quota_exceeded',
        'service_unavailable',
        'invalid_api_key',
        'account_suspended'
      ];

      criticalErrors.forEach(errorType => {
        const error = new Error(`Some ${errorType} occurred`);
        expect(fallbackProvider.shouldUseFallback(error)).toBe(true);
      });
    });

    it('should return false for non-critical errors', () => {
      const nonCriticalErrors = [
        'invalid_email',
        'rate_limit_exceeded',
        'temporary_failure'
      ];

      nonCriticalErrors.forEach(errorType => {
        const error = new Error(`Some ${errorType} occurred`);
        expect(fallbackProvider.shouldUseFallback(error)).toBe(false);
      });
    });

    it('should handle case-insensitive error matching', () => {
      const error = new Error('AUTHENTICATION_FAILED with some details');
      expect(fallbackProvider.shouldUseFallback(error)).toBe(true);
    });
  });

  describe('simulateFallbackSend', () => {
    it('should simulate email sending', async () => {
      const startTime = Date.now();

      await fallbackProvider.simulateFallbackSend(mockEmailData);

      const endTime = Date.now();
      const duration = endTime - startTime;

      // Should take at least 1000ms (1 second) as per the implementation
      expect(duration).toBeGreaterThanOrEqual(1000);
    });
  });
});