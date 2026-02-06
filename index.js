// index.js
const db = require('./db');

async function testConnection() {
    try {
        console.log('⏳ Đang lấy dữ liệu từ MySQL...');
        
        // Chạy câu lệnh SQL: Lấy tất cả danh mục
        const [rows, fields] = await db.query('SELECT * FROM categories');
        
        console.log('🎉 Danh sách Thể loại sách của bạn:');
        console.table(rows); // In ra bảng đẹp trong terminal

    } catch (error) {
        console.error('❌ Lỗi rồi:', error);
    }
}

testConnection();