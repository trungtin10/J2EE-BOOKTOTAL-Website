// db.js
const mysql = require('mysql2');
require('dotenv').config();

const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '123456', 
    database: 'bookstore_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Xuất ra dưới dạng Promise để dùng async/await cho sạch code
module.exports = pool.promise();