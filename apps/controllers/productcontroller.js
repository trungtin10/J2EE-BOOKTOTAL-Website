const ProductModel = require('../../models/product');
// Thêm thư viện fs và path để xử lý file ảnh
const fs = require('fs');
const path = require('path');

module.exports = {
    // --- DANH SÁCH ---
    index: async (req, res) => {
        try {
            const products = await ProductModel.getAllProducts();
            res.render('admin/product_list', { products: products });
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi Server: Không thể tải danh sách sản phẩm");
        }
    },

    // --- FORM THÊM MỚI ---
    create: async (req, res) => {
        try {
            const categories = await ProductModel.getCategories();
            res.render('admin/product_add', { categories: categories });
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi Server");
        }
    },

    // --- LƯU MỚI ---
    store: async (req, res) => {
        try {
            const data = {
                name: req.body.name,
                price: req.body.price,
                author: req.body.author,
                quantity: req.body.quantity,
                category_id: req.body.category_id,
                description: req.body.description,
                image_url: req.file ? req.file.filename : null // Lưu tên file nếu có
            };
            await ProductModel.createProduct(data);
            res.redirect('/admin');
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi khi thêm sản phẩm");
        }
    },

    // --- XEM CHI TIẾT ---
    view: async (req, res) => {
        try {
            const id = req.params.id;
            const product = await ProductModel.getProductById(id);
            res.render('admin/product_detail', { product: product });
        } catch (error) {
            console.log(error);
            res.send("Không tìm thấy sản phẩm");
        }
    },

    // --- FORM SỬA ---
    edit: async (req, res) => {
        try {
            const id = req.params.id;
            const product = await ProductModel.getProductById(id);
            const categories = await ProductModel.getCategories();
            
            if (!product) return res.redirect('/admin'); // Nếu ID sai thì về trang chủ
            
            res.render('admin/product_edit', { product: product, categories: categories });
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi tải form sửa");
        }
    },

    // --- CẬP NHẬT (QUAN TRỌNG: CÓ XÓA ẢNH CŨ) ---
    update: async (req, res) => {
        try {
            const id = req.params.id;
            const data = {
                name: req.body.name,
                price: req.body.price,
                author: req.body.author,
                quantity: req.body.quantity,
                category_id: req.body.category_id,
                description: req.body.description
            };

            // Kiểm tra nếu có upload ảnh mới
            if (req.file) {
                data.image_url = req.file.filename;

                // Lấy thông tin sản phẩm cũ để tìm tên ảnh cũ
                const oldProduct = await ProductModel.getProductById(id);
                
                // Logic xóa ảnh cũ
                if (oldProduct && oldProduct.image_url) {
                    const oldPath = path.join(__dirname, '../../../public/images/', oldProduct.image_url);
                    if (fs.existsSync(oldPath)) {
                        fs.unlinkSync(oldPath); // Xóa file
                    }
                }
            }

            await ProductModel.updateProduct(id, data);
            res.redirect('/admin');
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi cập nhật sản phẩm");
        }
    },

    // --- XÓA (QUAN TRỌNG: CÓ XÓA ẢNH) ---
    delete: async (req, res) => {
        try {
            const id = req.params.id;
            
            // Lấy thông tin trước khi xóa để biết tên ảnh
            const product = await ProductModel.getProductById(id);
            
            if (product && product.image_url) {
                const imgPath = path.join(__dirname, '../../../public/images/', product.image_url);
                if (fs.existsSync(imgPath)) {
                    fs.unlinkSync(imgPath); // Xóa ảnh khỏi folder
                }
            }

            await ProductModel.deleteProduct(id);
            res.redirect('/admin');
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi khi xóa sản phẩm");
        }
    }
};