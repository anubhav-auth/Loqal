const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'changeme';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '24h';

function generateToken(payload, expiresIn = JWT_EXPIRES_IN) {
  return jwt.sign(payload, JWT_SECRET, {
    expiresIn,
    issuer: 'loqal-notification-service',
    audience: 'loqal-users'
  });
}

function verifyToken(token) {
  return jwt.verify(token, JWT_SECRET, {
    issuer: 'loqal-notification-service',
    audience: 'loqal-users'
  });
}

function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'No token provided' });
  try {
    req.user = verifyToken(token);
    next();
  } catch (err) {
    return res.status(403).json({ error: 'Invalid or expired token' });
  }
}

module.exports = {
  generateToken,
  verifyToken,
  authenticateToken
};