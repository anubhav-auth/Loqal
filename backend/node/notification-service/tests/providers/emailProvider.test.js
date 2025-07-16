const emailProvider = require('../../src/providers/emailProvider');

// Mock Resend
jest.mock('resend', () => {
  return jest.fn().mockImplementation(() => ({
    emails: {
      send: jest.fn()
    }
  }));
});

describe('EmailProvider', () => {
  const mockEmailData = {
    to: 'test@example.com',
    subject: 'Test Email',
    content: 'This is a test email content',
    from: 'sender@example.com',
    metadata: { test: true }
  };

  beforeEach(() => {
    jest.clearAllMocks();
    process.env.RESEND_API_KEY = 'test-api-key';
    process.env.FROM_EMAIL = 'sender@example.com';
  });

  describe('sendEmail', () => {
    it('should send email successfully', async () => {
      const mockResendResponse = {
        data: { id: 'email123' },
        error: null
      };

      const { Resend } = require('resend');
      const mockResendInstance = new Resend();
      mockResendInstance.emails.send.mockResolvedValue(mockResendResponse);

      const result = await emailProvider.sendEmail(mockEmailData);

      expect(mockResendInstance.emails.send).toHaveBeenCalledWith({
        from: 'sender@example.com',
        to: ['test@example.com'],
        subject: 'Test Email',
        html: expect.stringContaining('Test Email'),
        text: expect.stringContaining('This is a test email content'),
        headers: { test: true }
      });

      expect(result).toEqual({
        success: true,
        messageId: 'email123',
        provider: 'resend',
        timestamp: expect.any(String)
      });
    });

    it('should throw error for missing required fields', async () => {
      const invalidData = {
        to: 'test@example.com',
        // Missing subject and content
      };

      await expect(emailProvider.sendEmail(invalidData))
        .rejects.toThrow('Missing required fields: to, subject, content');
    });

    it('should handle Resend API errors', async () => {
      const mockResendResponse = {
        data: null,
        error: { message: 'Invalid API key' }
      };

      const { Resend } = require('resend');
      const mockResendInstance = new Resend();
      mockResendInstance.emails.send.mockResolvedValue(mockResendResponse);

      await expect(emailProvider.sendEmail(mockEmailData))
        .rejects.toThrow('Failed to send email via Resend: Resend API error: Invalid API key');
    });

    it('should handle network errors', async () => {
      const { Resend } = require('resend');
      const mockResendInstance = new Resend();
      mockResendInstance.emails.send.mockRejectedValue(new Error('Network error'));

      await expect(emailProvider.sendEmail(mockEmailData))
        .rejects.toThrow('Failed to send email via Resend: Network error');
    });

    it('should use custom HTML and text if provided', async () => {
      const customData = {
        ...mockEmailData,
        html: '<h1>Custom HTML</h1>',
        text: 'Custom text content'
      };

      const mockResendResponse = {
        data: { id: 'email123' },
        error: null
      };

      const { Resend } = require('resend');
      const mockResendInstance = new Resend();
      mockResendInstance.emails.send.mockResolvedValue(mockResendResponse);

      await emailProvider.sendEmail(customData);

      expect(mockResendInstance.emails.send).toHaveBeenCalledWith(
        expect.objectContaining({
          html: '<h1>Custom HTML</h1>',
          text: 'Custom text content'
        })
      );
    });
  });

  describe('sendBulkEmails', () => {
    it('should send bulk emails successfully', async () => {
      const bulkEmails = [
        { to: 'user1@example.com', subject: 'Email 1', content: 'Content 1' },
        { to: 'user2@example.com', subject: 'Email 2', content: 'Content 2' }
      ];

      const mockResendResponse = {
        data: { id: 'email123' },
        error: null
      };

      const { Resend } = require('resend');
      const mockResendInstance = new Resend();
      mockResendInstance.emails.send.mockResolvedValue(mockResendResponse);

      const results = await emailProvider.sendBulkEmails(bulkEmails);

      expect(results).toHaveLength(2);
      expect(results[0].success).toBe(true);
      expect(results[1].success).toBe(true);
      expect(mockResendInstance.emails.send).toHaveBeenCalledTimes(2);
    });

    it('should handle mixed success and failure in bulk emails', async () => {
      const bulkEmails = [
        { to: 'user1@example.com', subject: 'Email 1', content: 'Content 1' },
        { to: 'user2@example.com', subject: 'Email 2', content: 'Content 2' }
      ];

      const { Resend } = require('resend');
      const mockResendInstance = new Resend();

      // First email succeeds, second fails
      mockResendInstance.emails.send
        .mockResolvedValueOnce({ data: { id: 'email123' }, error: null })
        .mockRejectedValueOnce(new Error('Network error'));

      const results = await emailProvider.sendBulkEmails(bulkEmails);

      expect(results).toHaveLength(2);
      expect(results[0].success).toBe(true);
      expect(results[1].success).toBe(false);
      expect(results[1].error).toBe('Failed to send email via Resend: Network error');
    });
  });

  describe('generateHTMLContent', () => {
    it('should generate proper HTML content', () => {
      const content = 'Test content\nwith newlines';
      const subject = 'Test Subject';

      const html = emailProvider.generateHTMLContent(content, subject);

      expect(html).toContain('<title>Test Subject</title>');
      expect(html).toContain('<h1>Test Subject</h1>');
      expect(html).toContain('Test content<br>with newlines');
      expect(html).toContain('Loqal Notification Service');
    });
  });

  describe('generateTextContent', () => {
    it('should convert HTML to plain text', () => {
      const htmlContent = '<h1>Title</h1><p>Content with <strong>bold</strong> text</p><br>New line';

      const text = emailProvider.generateTextContent(htmlContent);

      expect(text).toBe('TitleContent with bold textNew line');
    });
  });

  describe('validateEmail', () => {
    it('should validate correct email addresses', () => {
      expect(emailProvider.validateEmail('test@example.com')).toBe(true);
      expect(emailProvider.validateEmail('user.name+tag@domain.co.uk')).toBe(true);
    });

    it('should reject invalid email addresses', () => {
      expect(emailProvider.validateEmail('invalid-email')).toBe(false);
      expect(emailProvider.validateEmail('test@')).toBe(false);
      expect(emailProvider.validateEmail('@example.com')).toBe(false);
      expect(emailProvider.validateEmail('')).toBe(false);
    });
  });

  describe('isRetryableError', () => {
    it('should identify retryable errors', () => {
      const retryableErrors = [
        'rate_limit_exceeded',
        'quota_exceeded',
        'temporary_failure',
        'server_error',
        'timeout',
        'network_error'
      ];

      retryableErrors.forEach(errorType => {
        const error = new Error(`Some ${errorType} occurred`);
        expect(emailProvider.isRetryableError(error)).toBe(true);
      });
    });

    it('should identify non-retryable errors', () => {
      const nonRetryableErrors = [
        'invalid_email',
        'authentication_failed',
        'invalid_api_key'
      ];

      nonRetryableErrors.forEach(errorType => {
        const error = new Error(`Some ${errorType} occurred`);
        expect(emailProvider.isRetryableError(error)).toBe(false);
      });
    });
  });

  describe('getProviderStatus', () => {
    it('should return provider status', async () => {
      const status = await emailProvider.getProviderStatus();

      expect(status).toEqual({
        provider: 'resend',
        status: 'healthy',
        timestamp: expect.any(String)
      });
    });
  });
});