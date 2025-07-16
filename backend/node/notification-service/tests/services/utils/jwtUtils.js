const jwt = require('jsonwebtoken');
const logger = require('./logger');

const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '24h';

/**
 * Generate JWT token
 * @param {Object} payload - Token payload
 * @param {string} expiresIn - Token expiration time
 * @returns {string} JWT token
 */
const generateToken = (payload, expiresIn = JWT_EXPIRES_IN) => {
  try {
    if (!JWT_SECRET) {
      throw new Error('JWT_SECRET is not configured');
    }

    const token = jwt.sign(payload, JWT_SECRET, {
      expiresIn,
      issuer: 'loqal-notification-service',
      audience: 'loqal-users'
    });

    logger.debug('JWT token generated', {
      userId: payload.id,
      expiresIn
    });

    return token;
  } catch (error) {
    logger.error('Error generating JWT token:', error);
    throw error;
  }
};

/**
 * Verify JWT token
 * @param {string} token - JWT token
 * @returns {Object} Decoded token payload
 */
const verifyToken = async (token) => {
  try {
    if (!JWT_SECRET) {
      throw new Error('JWT_SECRET is not configured');
    }

    if (!token) {
      throw new Error('Token is required');
    }

    // Remove 'Bearer ' prefix if present
    const cleanToken = token.replace(/^Bearer\s+/, '');

    const decoded = jwt.verify(cleanToken, JWT_SECRET, {
      issuer: 'loqal-notification-service',
      audience: 'loqal-users'
    });

    logger.debug('JWT token verified', {
      userId: decoded.id,
      exp: decoded.exp
    });

    return decoded;
  } catch (error) {
    logger.error('Error verifying JWT token:', error);

    if (error.name === 'TokenExpiredError') {
      throw new Error('Token has expired');
    } else if (error.name === 'JsonWebTokenError') {
      throw new Error('Invalid token');
    } else if (error.name === 'NotBeforeError') {
      throw new Error('Token not active yet');
    }

    throw error;
  }
};

/**
 * Decode JWT token without verification (for debugging)
 * @param {string} token - JWT token
 * @returns {Object} Decoded token payload
 */
const decodeToken = (token) => {
  try {
    if (!token) {
      throw new Error('Token is required');
    }

    const cleanToken = token.replace(/^Bearer\s+/, '');
    const decoded = jwt.decode(cleanToken);

    if (!decoded) {
      throw new Error('Invalid token format');
    }

    return decoded;
  } catch (error) {
    logger.error('Error decoding JWT token:', error);
    throw error;
  }
};

/**
 * Extract token from request headers or query parameters
 * @param {Object} req - Express request object
 * @returns {string|null} Token or null
 */
const extractToken = (req) => {
  // Check Authorization header
  if (req.headers.authorization) {
    return req.headers.authorization;
  }

  // Check query parameter
  if (req.query.token) {
    return req.query.token;
  }

  // Check cookies
  if (req.cookies && req.cookies.token) {
    return req.cookies.token;
  }

  return null;
};

/**
 * JWT authentication middleware
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const authenticateToken = async (req, res, next) => {
  try {
    const token = extractToken(req);

    if (!token) {
      return res.status(401).json({
        error: 'Access token required',
        message: 'Please provide a valid authentication token'
      });
    }

    const decoded = await verifyToken(token);

    // Add user info to request
    req.user = {
      id: decoded.id,
      email: decoded.email,
      role: decoded.role || 'user',
      permissions: decoded.permissions || []
    };

    logger.debug('User authenticated', {
      userId: req.user.id,
      email: req.user.email,
      role: req.user.role
    });

    next();
  } catch (error) {
    logger.error('Authentication failed:', error);

    return res.status(401).json({
      error: 'Authentication failed',
      message: error.message
    });
  }
};

/**
 * Optional JWT authentication middleware
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const optionalAuth = async (req, res, next) => {
  try {
    const token = extractToken(req);

    if (token) {
      const decoded = await verifyToken(token);
      req.user = {
        id: decoded.id,
        email: decoded.email,
        role: decoded.role || 'user',
        permissions: decoded.permissions || []
      };
    }

    next();
  } catch (error) {
    // Continue without authentication
    logger.debug('Optional authentication failed, continuing without user:', error.message);
    next();
  }
};

/**
 * Role-based authorization middleware
 * @param {string|Array} roles - Required role(s)
 * @returns {Function} Express middleware function
 */
const authorizeRoles = (roles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        error: 'Authentication required',
        message: 'User must be authenticated'
      });
    }

    const userRole = req.user.role;
    const requiredRoles = Array.isArray(roles) ? roles : [roles];

    if (!requiredRoles.includes(userRole)) {
      logger.warn('Access denied - insufficient role', {
        userId: req.user.id,
        userRole,
        requiredRoles
      });

      return res.status(403).json({
        error: 'Access denied',
        message: 'Insufficient permissions'
      });
    }

    next();
  };
};

/**
 * Permission-based authorization middleware
 * @param {string|Array} permissions - Required permission(s)
 * @returns {Function} Express middleware function
 */
const authorizePermissions = (permissions) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        error: 'Authentication required',
        message: 'User must be authenticated'
      });
    }

    const userPermissions = req.user.permissions || [];
    const requiredPermissions = Array.isArray(permissions) ? permissions : [permissions];

    const hasAllPermissions = requiredPermissions.every(permission =>
      userPermissions.includes(permission)
    );

    if (!hasAllPermissions) {
      logger.warn('Access denied - insufficient permissions', {
        userId: req.user.id,
        userPermissions,
        requiredPermissions
      });

      return res.status(403).json({
        error: 'Access denied',
        message: 'Insufficient permissions'
      });
    }

    next();
  };
};

/**
 * Generate test token for development
 * @param {Object} userData - User data for token
 * @returns {string} Test JWT token
 */
const generateTestToken = (userData = {}) => {
  const defaultUser = {
    id: 'test-user-123',
    email: 'test@example.com',
    role: 'user',
    permissions: ['notifications:read', 'notifications:send']
  };

  const payload = { ...defaultUser, ...userData };
  return generateToken(payload, '24h');
};

module.exports = {
  generateToken,
  verifyToken,
  decodeToken,
  extractToken,
  authenticateToken,
  optionalAuth,
  authorizeRoles,
  authorizePermissions,
  generateTestToken
};