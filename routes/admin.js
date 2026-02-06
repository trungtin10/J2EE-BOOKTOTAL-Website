const express = require('express');
const router = express.Router();
// Giả sử bạn dùng Model Product từ Mongoose hoặc một mảng dữ liệu
const Product = require('../models/Product'); 

// 1. Trang danh sách sản phẩm (Trang bạn đang chụp ảnh)
router.get('/admin', async (req, res) => {
    try {
        const products = await Product.find(); // Lấy tất cả sản phẩm từ DB
        res.render('admin/index', { products: products }); 
    } catch (err) {
        res.status(500).send("Lỗi lấy dữ liệu");
    }
});

// 2. Trang hiện form SỬA (Đây là cái bạn đang bấm không ăn)
router.get('/admin/edit/:id', async (req, res) => {
    try {
        // Tìm sản phẩm theo ID lấy từ URL
        const product = await Product.findById(req.params.id);
        
        if (!product) {
            return res.status(404).send("Không tìm thấy sản phẩm này");
        }
        
        // Render file ejs làm form sửa (ví dụ: edit.ejs)
        res.render('admin/edit', { item: product });
    } catch (err) {
        console.error(err);
        res.status(500).send("Lỗi server khi tìm sản phẩm");
    }
});

// 3. Xử lý logic CẬP NHẬT khi nhấn Submit trong form sửa
router.post('/admin/edit/:id', async (req, res) => {
    try {
        await Product.findByIdAndUpdate(req.params.id, req.body);
        res.redirect('/admin'); // Sửa xong quay lại trang danh sách
    } catch (err) {
        res.status(500).send("Lỗi khi cập nhật");
    }
});

module.exports = router;