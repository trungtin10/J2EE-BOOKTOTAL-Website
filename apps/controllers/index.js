var express = require("express");
var router = express.Router();
var multer = require("multer");

// --- 1. IMPORT CÁC CONTROLLER ---
var homeController = require("./homecontroller");
var adminController = require("./admin/admincontroller");
var userController = require("./admin/usercontroller"); 

// --- 2. CẤU HÌNH UPLOAD ẢNH (Dành cho Sản phẩm) ---
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, "public/images/"),
    filename: (req, file, cb) => {
        cb(null, Date.now() + "-" + file.originalname);
    }
});
const upload = multer({ storage: storage });

// --- 3. ĐỊNH NGHĨA ROUTE ---

// === 3.1. TRANG CHỦ (CLIENT) ===
router.get("/", homeController.index);

// === 3.2. QUẢN TRỊ SẢN PHẨM (ADMIN PRODUCT) ===
router.get("/admin", adminController.index);
router.get("/admin/create", adminController.create);
router.post("/admin/store", upload.single("image"), adminController.store);
router.get("/admin/view/:id", adminController.view);
router.get("/admin/edit/:id", adminController.edit);
router.post("/admin/update/:id", upload.single("image"), adminController.update);
router.get("/admin/delete/:id", adminController.delete);

// === 3.3. QUẢN TRỊ NGƯỜI DÙNG (ADMIN USER - THỐNG NHẤT KHÔNG "S") ===
// Trang danh sách người dùng
router.get('/admin/user', userController.index);

// Trang hiển thị form thêm mới (Luôn để trên các route có tham số :id)
router.get('/admin/user/add', userController.create);

// Xử lý lưu dữ liệu người dùng mới vào database
router.post('/admin/user/add', userController.store);

// Xử lý xóa người dùng
router.get('/admin/user/delete/:id', userController.delete);

// Nếu bạn cần thêm chức năng Sửa (Edit) người dùng, hãy bổ sung tại đây:
// router.get('/admin/user/edit/:id', userController.edit);
// router.post('/admin/user/update/:id', userController.update);

module.exports = router;