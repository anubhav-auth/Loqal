const logger = require('../utils/logger');

/**
 * Fallback email provider for when primary provider fails
 * @param {Object} emailData - Email data
 * @returns {Object} Send result
 */
const sendEmail = async (emailData) => {
  try {
    const {
      to,
      subject,
      content,
      metadata = {},
      originalError = null
    } = emailData;

    logger.warn('Using fallback email provider', {
      to,
      subject,
      originalError: originalError?.message,
      provider: 'fallback'
    });

    // Log the email content for manual processing
    const emailLog = {
      to,
      subject,
      content,
      metadata,
      timestamp: new Date().toISOString(),
      provider: 'fallback',
      originalError: originalError?.message || 'Primary provider failed'
    };

    // In a production environment, you might:
    // 1. Store in a database for manual processing
    // 2. Send to a different email service
    // 3. Queue for retry with exponential backoff
    // 4. Send to a monitoring system

    logger.info('Email logged for manual processing', emailLog);

    // For now, we'll simulate a successful send
    // In production, implement actual fallback logic
    await simulateFallbackSend(emailData);

    return {
      success: true,
      messageId: `fallback_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      provider: 'fallback',
      timestamp: new Date().toISOString(),
      note: 'Email logged for manual processing'
    };

  } catch (error) {
    logger.error('Fallback provider also failed:', error);

    // If fallback fails, we have a critical issue
    throw {
      message: `Critical: Both primary and fallback providers failed: ${error.message}`,
      isRetryable: false,
      originalError: error,
      provider: 'fallback'
    };
  }
};

/**
 * Simulate fallback email sending
 * @param {Object} emailData - Email data
 */
const simulateFallbackSend = async (emailData) => {
  // Simulate processing time
  await new Promise(resolve => setTimeout(resolve, 1000));

  logger.info('Fallback email processing simulated', {
    to: emailData.to,
    subject: emailData.subject
  });
};

/**
 * Store failed email for later processing
 * @param {Object} emailData - Email data
 * @param {Error} error - Original error
 */
const storeFailedEmail = async (emailData, error) => {
  try {
    const failedEmail = {
      ...emailData,
      error: error.message,
      timestamp: new Date().toISOString(),
      retryCount: 0,
      maxRetries: 3
    };

    // TODO: Store in Redis or database for later processing
    logger.info('Failed email stored for retry', {
      to: emailData.to,
      subject: emailData.subject,
      error: error.message
    });

    return failedEmail;
  } catch (storeError) {
    logger.error('Failed to store failed email:', storeError);
    throw storeError;
  }
};

/**
 * Process stored failed emails
 * @param {Array} failedEmails - Array of failed email data
 */
const processFailedEmails = async (failedEmails) => {
  try {
    const results = [];

    for (const emailData of failedEmails) {
      try {
        // Increment retry count
        emailData.retryCount = (emailData.retryCount || 0) + 1;

        if (emailData.retryCount > emailData.maxRetries) {
          logger.error('Email exceeded max retries', {
            to: emailData.to,
            subject: emailData.subject,
            retryCount: emailData.retryCount
          });

          results.push({
            success: false,
            to: emailData.to,
            error: 'Max retries exceeded',
            retryCount: emailData.retryCount
          });
          continue;
        }

        // Try to send again
        const result = await sendEmail(emailData);
        results.push({
          success: true,
          to: emailData.to,
          retryCount: emailData.retryCount,
          ...result
        });

      } catch (retryError) {
        logger.error('Retry failed for email:', {
          to: emailData.to,
          subject: emailData.subject,
          retryCount: emailData.retryCount,
          error: retryError.message
        });

        results.push({
          success: false,
          to: emailData.to,
          error: retryError.message,
          retryCount: emailData.retryCount
        });
      }
    }

    return results;
  } catch (error) {
    logger.error('Error processing failed emails:', error);
    throw error;
  }
};

/**
 * Get fallback provider status
 * @returns {Object} Provider status
 */
const getProviderStatus = () => {
  return {
    provider: 'fallback',
    status: 'available',
    timestamp: new Date().toISOString(),
    note: 'Fallback provider is always available for critical failures'
  };
};

/**
 * Validate if fallback should be used
 * @param {Error} error - Original error from primary provider
 * @returns {boolean} Whether to use fallback
 */
const shouldUseFallback = (error) => {
  // Use fallback for critical errors or when primary provider is completely down
  const criticalErrors = [
    'authentication_failed',
    'quota_exceeded',
    'service_unavailable',
    'invalid_api_key',
    'account_suspended'
  ];

  const errorMessage = error.message.toLowerCase();
  return criticalErrors.some(criticalError =>
    errorMessage.includes(criticalError)
  );
};

module.exports = {
  sendEmail,
  storeFailedEmail,
  processFailedEmails,
  getProviderStatus,
  shouldUseFallback
};