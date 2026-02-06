// Đi ngược ra 2 cấp: từ models -> apps -> thư mục gốc Web
const db = require("../../db"); 

module.exports = {
    // Lấy dữ liệu tất cả người dùng
    getAllUsers: async () => {
        try {
            // Sử dụng query cho các lệnh SELECT
            const [rows] = await db.query("SELECT * FROM users ORDER BY id DESC");
            return rows;
        } catch (error) {
            console.error("Lỗi tại getAllUsers:", error.message);
            throw error;
        }
    },

    // Thêm mới người dùng
    addUser: async (data) => {
        try {
            const sql = "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, ?)";
            const params = [data.username, data.password, data.email, data.full_name, data.role];
            
            // Sử dụng execute để tăng hiệu năng và bảo mật cho lệnh INSERT/UPDATE
            const [result] = await db.execute(sql, params);
            return result;
        } catch (error) {
            console.error("Lỗi tại addUser:", error.message);
            throw error;
        }
    },

    // Xóa người dùng theo ID
    deleteUser: async (id) => {
        try {
            const [result] = await db.execute("DELETE FROM users WHERE id = ?", [id]);
            return result;
        } catch (error) {
            console.error("Lỗi tại deleteUser:", error.message);
            throw error;
        }
    }
};