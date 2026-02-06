// db.js
const mysql = require('mysql2');
require('dotenv').config(); // Nếu bạn dùng file .env

const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '123456', // <--- ĐIỀN MẬT KHẨU MYSQL CỦA BẠN (để trống nếu không có)
    database: 'bookstore_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

module.exports = pool.promise();