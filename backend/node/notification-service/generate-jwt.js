const jwt = require('jsonwebtoken');

const payload = {
  userId: 'user123',
};

const token = jwt.sign(payload, '1958eb6563fff9e2f9df5b023869bc79ed0ab567c9725674bb447d7f1845464882ce515d64884d04d2a1b3e026a6131562c3fa52a848cea36126e12345e50319', {
  expiresIn: '12h',            
});

console.log("🔐 JWT Token:\n", token);