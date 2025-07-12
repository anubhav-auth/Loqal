const jwtUtils = require('../../src/utils/jwtUtils');
const jwt = require('jsonwebtoken');

// Mock jsonwebtoken
jest.mock('jsonwebtoken');

describe('JWT Utils', () => {
  const mockPayload = {
    id: 'user123',
    email: 'user@example.com',
    role: 'user',
    permissions: ['notifications:read', 'notifications:send']
  };

  beforeEach(() => {
    jest.clearAllMocks();
    process.env.JWT_SECRET = 'test-secret';
    process.env.JWT_EXPIRES_IN = '24h';
  });

  describe('generateToken', () => {
    it('should generate JWT token successfully', () => {
      const mockToken = 'mock.jwt.token';
      jwt.sign.mockReturnValue(mockToken);

      const result = jwtUtils.generateToken(mockPayload);

      expect(jwt.sign).toHaveBeenCalledWith(mockPayload, 'test-secret', {
        expiresIn: '24h',
        issuer: 'loqal-notification-service',
        audience: 'loqal-users'
      });
      expect(result).toBe(mockToken);
    });

    it('should generate token with custom expiration', () => {
      const mockToken = 'mock.jwt.token';
      jwt.sign.mockReturnValue(mockToken);

      const result = jwtUtils.generateToken(mockPayload, '1h');

      expect(jwt.sign).toHaveBeenCalledWith(mockPayload, 'test-secret', {
        expiresIn: '1h',
        issuer: 'loqal-notification-service',
        audience: 'loqal-users'
      });
      expect(result).toBe(mockToken);
    });

    it('should throw error when JWT_SECRET is not configured', () => {
      delete process.env.JWT_SECRET;

      expect(() => jwtUtils.generateToken(mockPayload))
        .toThrow('JWT_SECRET is not configured');
    });

    it('should handle JWT signing errors', () => {
      jwt.sign.mockImplementation(() => {
        throw new Error('JWT signing failed');
      });

      expect(() => jwtUtils.generateToken(mockPayload))
        .toThrow('JWT signing failed');
    });
  });

  describe('verifyToken', () => {
    it('should verify valid JWT token', async () => {
      const mockToken = 'valid.jwt.token';
      const mockDecoded = { ...mockPayload, exp: Date.now() / 1000 + 3600 };
      jwt.verify.mockReturnValue(mockDecoded);

      const result = await jwtUtils.verifyToken(mockToken);

      expect(jwt.verify).toHaveBeenCalledWith(mockToken, 'test-secret', {
        issuer: 'loqal-notification-service',
        audience: 'loqal-users'
      });
      expect(result).toEqual(mockDecoded);
    });

    it('should handle Bearer token prefix', async () => {
      const mockToken = 'Bearer valid.jwt.token';
      const mockDecoded = { ...mockPayload, exp: Date.now() / 1000 + 3600 };
      jwt.verify.mockReturnValue(mockDecoded);

      const result = await jwtUtils.verifyToken(mockToken);

      expect(jwt.verify).toHaveBeenCalledWith('valid.jwt.token', 'test-secret', {
        issuer: 'loqal-notification-service',
        audience: 'loqal-users'
      });
      expect(result).toEqual(mockDecoded);
    });

    it('should throw error when JWT_SECRET is not configured', async () => {
      delete process.env.JWT_SECRET;

      await expect(jwtUtils.verifyToken('token'))
        .rejects.toThrow('JWT_SECRET is not configured');
    });

    it('should throw error when token is missing', async () => {
      await expect(jwtUtils.verifyToken())
        .rejects.toThrow('Token is required');
    });

    it('should throw error when token is empty', async () => {
      await expect(jwtUtils.verifyToken(''))
        .rejects.toThrow('Token is required');
    });

    it('should handle expired token error', async () => {
      const error = new Error('jwt expired');
      error.name = 'TokenExpiredError';
      jwt.verify.mockImplementation(() => {
        throw error;
      });

      await expect(jwtUtils.verifyToken('expired.token'))
        .rejects.toThrow('Token has expired');
    });

    it('should handle invalid token error', async () => {
      const error = new Error('invalid signature');
      error.name = 'JsonWebTokenError';
      jwt.verify.mockImplementation(() => {
        throw error;
      });

      await expect(jwtUtils.verifyToken('invalid.token'))
        .rejects.toThrow('Invalid token');
    });

    it('should handle token not active error', async () => {
      const error = new Error('jwt not active');
      error.name = 'NotBeforeError';
      jwt.verify.mockImplementation(() => {
        throw error;
      });

      await expect(jwtUtils.verifyToken('notactive.token'))
        .rejects.toThrow('Token not active yet');
    });

    it('should handle other JWT errors', async () => {
      const error = new Error('Unknown JWT error');
      jwt.verify.mockImplementation(() => {
        throw error;
      });

      await expect(jwtUtils.verifyToken('unknown.token'))
        .rejects.toThrow('Unknown JWT error');
    });
  });

  describe('decodeToken', () => {
    it('should decode JWT token without verification', () => {
      const mockToken = 'mock.jwt.token';
      const mockDecoded = { ...mockPayload, exp: Date.now() / 1000 + 3600 };
      jwt.decode.mockReturnValue(mockDecoded);

      const result = jwtUtils.decodeToken(mockToken);

      expect(jwt.decode).toHaveBeenCalledWith(mockToken);
      expect(result).toEqual(mockDecoded);
    });

    it('should handle Bearer token prefix', () => {
      const mockToken = 'Bearer mock.jwt.token';
      const mockDecoded = { ...mockPayload, exp: Date.now() / 1000 + 3600 };
      jwt.decode.mockReturnValue(mockDecoded);

      const result = jwtUtils.decodeToken(mockToken);

      expect(jwt.decode).toHaveBeenCalledWith('mock.jwt.token');
      expect(result).toEqual(mockDecoded);
    });

    it('should throw error when token is missing', () => {
      expect(() => jwtUtils.decodeToken())
        .toThrow('Token is required');
    });

    it('should throw error when token is empty', () => {
      expect(() => jwtUtils.decodeToken(''))
        .toThrow('Token is required');
    });

    it('should throw error when token format is invalid', () => {
      jwt.decode.mockReturnValue(null);

      expect(() => jwtUtils.decodeToken('invalid.token'))
        .toThrow('Invalid token format');
    });
  });

  describe('extractToken', () => {
    it('should extract token from Authorization header', () => {
      const req = {
        headers: {
          authorization: 'Bearer test.token.here'
        }
      };

      const result = jwtUtils.extractToken(req);

      expect(result).toBe('Bearer test.token.here');
    });

    it('should extract token from query parameter', () => {
      const req = {
        query: {
          token: 'test.token.here'
        }
      };

      const result = jwtUtils.extractToken(req);

      expect(result).toBe('test.token.here');
    });

    it('should extract token from cookies', () => {
      const req = {
        cookies: {
          token: 'test.token.here'
        }
      };

      const result = jwtUtils.extractToken(req);

      expect(result).toBe('test.token.here');
    });

    it('should return null when no token found', () => {
      const req = {
        headers: {},
        query: {},
        cookies: {}
      };

      const result = jwtUtils.extractToken(req);

      expect(result).toBeNull();
    });

    it('should prioritize Authorization header over query parameter', () => {
      const req = {
        headers: {
          authorization: 'Bearer header.token'
        },
        query: {
          token: 'query.token'
        }
      };

      const result = jwtUtils.extractToken(req);

      expect(result).toBe('Bearer header.token');
    });
  });

  describe('authenticateToken', () => {
    it('should authenticate valid token', async () => {
      const req = {
        headers: {
          authorization: 'Bearer valid.token'
        }
      };
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      const mockDecoded = { ...mockPayload, exp: Date.now() / 1000 + 3600 };
      jwt.verify.mockReturnValue(mockDecoded);

      await jwtUtils.authenticateToken(req, res, next);

      expect(req.user).toEqual({
        id: 'user123',
        email: 'user@example.com',
        role: 'user',
        permissions: ['notifications:read', 'notifications:send']
      });
      expect(next).toHaveBeenCalled();
    });

    it('should return 401 when no token provided', async () => {
      const req = {
        headers: {}
      };
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      await jwtUtils.authenticateToken(req, res, next);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.json).toHaveBeenCalledWith({
        error: 'Access token required',
        message: 'Please provide a valid authentication token'
      });
      expect(next).not.toHaveBeenCalled();
    });

    it('should return 401 when token verification fails', async () => {
      const req = {
        headers: {
          authorization: 'Bearer invalid.token'
        }
      };
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      jwt.verify.mockImplementation(() => {
        throw new Error('Invalid token');
      });

      await jwtUtils.authenticateToken(req, res, next);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.json).toHaveBeenCalledWith({
        error: 'Authentication failed',
        message: 'Invalid token'
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  describe('optionalAuth', () => {
    it('should set user when valid token provided', async () => {
      const req = {
        headers: {
          authorization: 'Bearer valid.token'
        }
      };
      const res = {};
      const next = jest.fn();

      const mockDecoded = { ...mockPayload, exp: Date.now() / 1000 + 3600 };
      jwt.verify.mockReturnValue(mockDecoded);

      await jwtUtils.optionalAuth(req, res, next);

      expect(req.user).toEqual({
        id: 'user123',
        email: 'user@example.com',
        role: 'user',
        permissions: ['notifications:read', 'notifications:send']
      });
      expect(next).toHaveBeenCalled();
    });

    it('should continue without user when no token provided', async () => {
      const req = {
        headers: {}
      };
      const res = {};
      const next = jest.fn();

      await jwtUtils.optionalAuth(req, res, next);

      expect(req.user).toBeUndefined();
      expect(next).toHaveBeenCalled();
    });

    it('should continue without user when token verification fails', async () => {
      const req = {
        headers: {
          authorization: 'Bearer invalid.token'
        }
      };
      const res = {};
      const next = jest.fn();

      jwt.verify.mockImplementation(() => {
        throw new Error('Invalid token');
      });

      await jwtUtils.optionalAuth(req, res, next);

      expect(req.user).toBeUndefined();
      expect(next).toHaveBeenCalled();
    });
  });

  describe('authorizeRoles', () => {
    it('should allow access for authorized role', () => {
      const req = {
        user: {
          id: 'user123',
          role: 'admin'
        }
      };
      const res = {};
      const next = jest.fn();

      const middleware = jwtUtils.authorizeRoles('admin');
      middleware(req, res, next);

      expect(next).toHaveBeenCalled();
    });

    it('should allow access for multiple authorized roles', () => {
      const req = {
        user: {
          id: 'user123',
          role: 'moderator'
        }
      };
      const res = {};
      const next = jest.fn();

      const middleware = jwtUtils.authorizeRoles(['admin', 'moderator']);
      middleware(req, res, next);

      expect(next).toHaveBeenCalled();
    });

    it('should deny access for unauthorized role', () => {
      const req = {
        user: {
          id: 'user123',
          role: 'user'
        }
      };
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      const middleware = jwtUtils.authorizeRoles('admin');
      middleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({
        error: 'Access denied',
        message: 'Insufficient permissions'
      });
      expect(next).not.toHaveBeenCalled();
    });

    it('should require authentication', () => {
      const req = {};
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      const middleware = jwtUtils.authorizeRoles('admin');
      middleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.json).toHaveBeenCalledWith({
        error: 'Authentication required',
        message: 'User must be authenticated'
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  describe('authorizePermissions', () => {
    it('should allow access for authorized permissions', () => {
      const req = {
        user: {
          id: 'user123',
          permissions: ['notifications:read', 'notifications:send']
        }
      };
      const res = {};
      const next = jest.fn();

      const middleware = jwtUtils.authorizePermissions('notifications:read');
      middleware(req, res, next);

      expect(next).toHaveBeenCalled();
    });

    it('should allow access for multiple authorized permissions', () => {
      const req = {
        user: {
          id: 'user123',
          permissions: ['notifications:read', 'notifications:send']
        }
      };
      const res = {};
      const next = jest.fn();

      const middleware = jwtUtils.authorizePermissions(['notifications:read', 'notifications:send']);
      middleware(req, res, next);

      expect(next).toHaveBeenCalled();
    });

    it('should deny access for missing permissions', () => {
      const req = {
        user: {
          id: 'user123',
          permissions: ['notifications:read']
        }
      };
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      const middleware = jwtUtils.authorizePermissions(['notifications:read', 'notifications:delete']);
      middleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith({
        error: 'Access denied',
        message: 'Insufficient permissions'
      });
      expect(next).not.toHaveBeenCalled();
    });

    it('should require authentication', () => {
      const req = {};
      const res = {
        status: jest.fn().mockReturnThis(),
        json: jest.fn()
      };
      const next = jest.fn();

      const middleware = jwtUtils.authorizePermissions('notifications:read');
      middleware(req, res, next);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.json).toHaveBeenCalledWith({
        error: 'Authentication required',
        message: 'User must be authenticated'
      });
      expect(next).not.toHaveBeenCalled();
    });
  });

  describe('generateTestToken', () => {
    it('should generate test token with default user data', () => {
      const mockToken = 'test.jwt.token';
      jwt.sign.mockReturnValue(mockToken);

      const result = jwtUtils.generateTestToken();

      expect(jwt.sign).toHaveBeenCalledWith({
        id: 'test-user-123',
        email: 'test@example.com',
        role: 'user',
        permissions: ['notifications:read', 'notifications:send']
      }, 'test-secret', {
        expiresIn: '24h',
        issuer: 'loqal-notification-service',
        audience: 'loqal-users'
      });
      expect(result).toBe(mockToken);
    });

    it('should generate test token with custom user data', () => {
      const mockToken = 'test.jwt.token';
      jwt.sign.mockReturnValue(mockToken);

      const customUserData = {
        id: 'custom-user',
        email: 'custom@example.com',
        role: 'admin'
      };

      const result = jwtUtils.generateTestToken(customUserData);

      expect(jwt.sign).toHaveBeenCalledWith({
        id: 'custom-user',
        email: 'custom@example.com',
        role: 'admin',
        permissions: ['notifications:read', 'notifications:send']
      }, 'test-secret', {
        expiresIn: '24h',
        issuer: 'loqal-notification-service',
        audience: 'loqal-users'
      });
      expect(result).toBe(mockToken);
    });
  });
});