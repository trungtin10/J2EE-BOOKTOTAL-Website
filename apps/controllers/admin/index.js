var express = require("express");
var router = express.Router();
var multer = require("multer");

// --- 1. IMPORT CÁC CONTROLLER ---
var homeController = require("./homecontroller");
var adminController = require("./admin/admincontroller");
var userController = require("./admin/usercontroller"); 

// --- 2. CẤU HÌNH UPLOAD ẢNH (MULTER) ---
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, "public/images/"),
    filename: (req, file, cb) => {
        cb(null, Date.now() + "-" + file.originalname);
    }
});
const upload = multer({ storage: storage });

// --- 3. ĐỊNH NGHĨA ROUTE ---

// === KHU VỰC NGƯỜI DÙNG (CLIENT) ===
router.get("/", homeController.index);

// === KHU VỰC QUẢN TRỊ SẢN PHẨM (Sử dụng /admin/...) ===
router.get("/admin", adminController.index);
router.get("/admin/create", adminController.create);
router.post("/admin/store", upload.single("image"), adminController.store);
router.get("/admin/view/:id", adminController.view);
router.get("/admin/edit/:id", adminController.edit);
router.post("/admin/update/:id", upload.single("image"), adminController.update);
router.get("/admin/delete/:id", adminController.delete);

// === KHU VỰC QUẢN TRỊ NGƯỜI DÙNG (Thống nhất dùng /admin/users) ===
// Lưu ý: Đặt các route cụ thể lên trên route chứa tham số (:id)
router.get('/admin/user', userController.index);           // Danh sách
router.get('/admin/user/add', userController.create);       // Form thêm (GET)
router.post('/admin/user/add', userController.store);
    // Lưu dữ liệu (POST)
router.get('/admin/user/delete/:id', userController.delete); // Xóa

module.exports = router;