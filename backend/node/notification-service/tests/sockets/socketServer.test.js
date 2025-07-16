const socketServer = require('../../src/sockets/socketServer');
const { verifyToken } = require('../../src/utils/jwtUtils');
const { setUserOnline, setUserOffline } = require('../../src/digest/bufferStore');

// Mock dependencies
jest.mock('../../src/utils/jwtUtils');
jest.mock('../../src/digest/bufferStore');
jest.mock('socket.io');

describe('SocketServer', () => {
  let mockServer;
  let mockIO;
  let mockSocket;

  beforeEach(() => {
    jest.clearAllMocks();

    // Mock Socket.IO
    const { Server } = require('socket.io');
    mockIO = {
      use: jest.fn(),
      on: jest.fn(),
      to: jest.fn().mockReturnThis(),
      emit: jest.fn(),
      engine: {
        clientsCount: 5
      },
      sockets: {
        adapter: {
          rooms: {
            get: jest.fn().mockReturnValue(new Set(['socket1', 'socket2']))
        }
      }
    };
    Server.mockImplementation(() => mockIO);

    // Mock server
    mockServer = {
      on: jest.fn()
    };

    // Mock socket
    mockSocket = {
      id: 'socket123',
      handshake: {
        query: { token: 'valid-token' }
      },
      userId: 'user123',
      userEmail: 'user@example.com',
      join: jest.fn(),
      on: jest.fn(),
      emit: jest.fn()
    };
  });

  describe('initializeSocketServer', () => {
    it('should initialize Socket.IO server successfully', () => {
      const { Server } = require('socket.io');

      socketServer.initializeSocketServer(mockServer);

      expect(Server).toHaveBeenCalledWith(mockServer, {
        cors: {
          origin: "*",
          methods: ["GET", "POST"]
        },
        transports: ['websocket', 'polling']
      });
      expect(mockIO.use).toHaveBeenCalled();
      expect(mockIO.on).toHaveBeenCalledWith('connection', expect.any(Function));
    });
  });

  describe('authentication middleware', () => {
    it('should authenticate valid token', async () => {
      const mockDecoded = { id: 'user123', email: 'user@example.com' };
      verifyToken.mockResolvedValue(mockDecoded);

      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      // Get the authentication middleware
      const authMiddleware = mockIO.use.mock.calls[0][0];
      const next = jest.fn();

      await authMiddleware(mockSocket, next);

      expect(verifyToken).toHaveBeenCalledWith('valid-token');
      expect(mockSocket.userId).toBe('user123');
      expect(mockSocket.userEmail).toBe('user@example.com');
      expect(next).toHaveBeenCalled();
    });

    it('should reject missing token', async () => {
      mockSocket.handshake.query = {};

      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const authMiddleware = mockIO.use.mock.calls[0][0];
      const next = jest.fn();

      await authMiddleware(mockSocket, next);

      expect(next).toHaveBeenCalledWith(new Error('Authentication token required'));
    });

    it('should reject invalid token', async () => {
      verifyToken.mockRejectedValue(new Error('Invalid token'));

      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const authMiddleware = mockIO.use.mock.calls[0][0];
      const next = jest.fn();

      await authMiddleware(mockSocket, next);

      expect(next).toHaveBeenCalledWith(new Error('Authentication failed'));
    });
  });

  describe('connection handling', () => {
    it('should handle socket connection', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      // Get the connection handler
      const connectionHandler = mockIO.on.mock.calls[0][1];

      await connectionHandler(mockSocket);

      expect(setUserOnline).toHaveBeenCalledWith('user123', 'socket123');
      expect(mockSocket.join).toHaveBeenCalledWith('user:user123');
    });

    it('should handle socket disconnect', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const connectionHandler = mockIO.on.mock.calls[0][1];
      await connectionHandler(mockSocket);

      // Get the disconnect handler
      const disconnectHandler = mockSocket.on.mock.calls.find(call => call[0] === 'disconnect')[1];

      await disconnectHandler();

      expect(setUserOffline).toHaveBeenCalledWith('user123');
    });

    it('should handle notification acknowledgment', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const connectionHandler = mockIO.on.mock.calls[0][1];
      await connectionHandler(mockSocket);

      // Get the notification:acknowledge handler
      const ackHandler = mockSocket.on.mock.calls.find(call => call[0] === 'notification:acknowledge')[1];

      await ackHandler({ notificationId: 'notif123' });

      // Should log the acknowledgment (implementation would handle this)
    });

    it('should handle notification dismissal', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const connectionHandler = mockIO.on.mock.calls[0][1];
      await connectionHandler(mockSocket);

      // Get the notification:dismiss handler
      const dismissHandler = mockSocket.on.mock.calls.find(call => call[0] === 'notification:dismiss')[1];

      await dismissHandler({ notificationId: 'notif123' });

      // Should log the dismissal (implementation would handle this)
    });

    it('should handle socket errors', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const connectionHandler = mockIO.on.mock.calls[0][1];
      await connectionHandler(mockSocket);

      // Get the error handler
      const errorHandler = mockSocket.on.mock.calls.find(call => call[0] === 'error')[1];

      const error = new Error('Socket error');
      errorHandler(error);

      // Should log the error (implementation would handle this)
    });
  });

  describe('emitNotification', () => {
    it('should emit notification to specific user', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const notification = {
        type: 'email',
        subject: 'Test Notification',
        content: 'Test content'
      };

      const result = await socketServer.emitNotification('user123', notification);

      expect(mockIO.to).toHaveBeenCalledWith('user:user123');
      expect(mockIO.emit).toHaveBeenCalledWith('notification', expect.objectContaining({
        id: expect.stringMatching(/^notif_\d+_[a-z0-9]+$/),
        type: 'email',
        subject: 'Test Notification',
        content: 'Test content',
        timestamp: expect.any(String)
      }));
      expect(result).toEqual(expect.objectContaining({
        id: expect.any(String),
        type: 'email',
        subject: 'Test Notification'
      }));
    });

    it('should throw error when Socket.IO not initialized', async () => {
      const notification = {
        type: 'email',
        subject: 'Test Notification',
        content: 'Test content'
      };

      await expect(socketServer.emitNotification('user123', notification))
        .rejects.toThrow('Socket.IO server not initialized');
    });
  });

  describe('emitBroadcast', () => {
    it('should emit notification to all users', async () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const notification = {
        type: 'email',
        subject: 'Broadcast Notification',
        content: 'Broadcast content'
      };

      const result = await socketServer.emitBroadcast(notification);

      expect(mockIO.emit).toHaveBeenCalledWith('notification', expect.objectContaining({
        id: expect.stringMatching(/^notif_\d+_[a-z0-9]+$/),
        type: 'email',
        subject: 'Broadcast Notification',
        content: 'Broadcast content',
        timestamp: expect.any(String)
      }));
      expect(result).toEqual(expect.objectContaining({
        id: expect.any(String),
        type: 'email',
        subject: 'Broadcast Notification'
      }));
    });

    it('should throw error when Socket.IO not initialized', async () => {
      const notification = {
        type: 'email',
        subject: 'Broadcast Notification',
        content: 'Broadcast content'
      };

      await expect(socketServer.emitBroadcast(notification))
        .rejects.toThrow('Socket.IO server not initialized');
    });
  });

  describe('getConnectedUsersCount', () => {
    it('should return connected users count', () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const count = socketServer.getConnectedUsersCount();

      expect(count).toBe(5);
    });

    it('should return 0 when Socket.IO not initialized', () => {
      const count = socketServer.getConnectedUsersCount();

      expect(count).toBe(0);
    });
  });

  describe('getUserSocketIds', () => {
    it('should return user socket IDs', () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const socketIds = socketServer.getUserSocketIds('user123');

      expect(socketIds).toEqual(['socket1', 'socket2']);
    });

    it('should return empty array when Socket.IO not initialized', () => {
      const socketIds = socketServer.getUserSocketIds('user123');

      expect(socketIds).toEqual([]);
    });

    it('should return empty array when user has no sockets', () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      mockIO.sockets.adapter.rooms.get.mockReturnValue(null);

      const socketIds = socketServer.getUserSocketIds('user123');

      expect(socketIds).toEqual([]);
    });
  });

  describe('generateNotificationId', () => {
    it('should generate unique notification ID', () => {
      const id1 = socketServer.generateNotificationId();
      const id2 = socketServer.generateNotificationId();

      expect(id1).toMatch(/^notif_\d+_[a-z0-9]+$/);
      expect(id2).toMatch(/^notif_\d+_[a-z0-9]+$/);
      expect(id1).not.toBe(id2);
    });
  });

  describe('getIO', () => {
    it('should return Socket.IO instance when initialized', () => {
      const { Server } = require('socket.io');
      socketServer.initializeSocketServer(mockServer);

      const io = socketServer.getIO();

      expect(io).toBe(mockIO);
    });

    it('should return undefined when not initialized', () => {
      const io = socketServer.getIO();

      expect(io).toBeUndefined();
    });
  });
});