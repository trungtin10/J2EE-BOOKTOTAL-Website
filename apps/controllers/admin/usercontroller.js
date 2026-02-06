// Kiểm tra đường dẫn: từ controllers/admin/ đi ra 2 cấp là vào apps/models/
const UserModel = require('../../models/user');

module.exports = {
    // Hiển thị danh sách người dùng
    index: async (req, res) => {
        try {
            const users = await UserModel.getAllUsers();
            // Log dữ liệu giúp bạn debug nhanh hơn
            console.log("Truy vấn thành công. Số lượng users:", users.length);
            
            res.render('admin/user/user_list', { users: users });
        } catch (error) {
            console.error("Lỗi Controller Index:", error);
            res.status(500).send("Lỗi kết nối database khi lấy danh sách");
        }
    },

    // Hiển thị form thêm mới
    create: (req, res) => {
        res.render('admin/user/add');
    },

    // Lưu người dùng mới vào DB
    store: async (req, res) => {
        try {
            const { username, password, email, full_name, role } = req.body;
            
            // Log dữ liệu nhận được từ form để kiểm tra
            console.log("Dữ liệu nhận được:", req.body);

            await UserModel.addUser({ username, password, email, full_name, role });
            res.redirect('/admin/user'); 
        } catch (error) {
            console.error("Lỗi Controller Store:", error);
            res.status(500).send("Lỗi khi thêm người dùng vào database");
        }
    },

    // Xóa người dùng
    delete: async (req, res) => {
        try {
            const id = req.params.id;
            await UserModel.deleteUser(id);
            console.log(`Đã xóa user ID: ${id}`);
            res.redirect('/admin/user');
        } catch (error) {
            console.error("Lỗi Controller Delete:", error);
            res.status(500).send("Lỗi khi thực hiện lệnh xóa");
        }
    }
};