// apps/controllers/admin/admincontroller.js
const ProductModel = require('../../models/product');

module.exports = {
    index: async (req, res) => {
        try {
            const products = await ProductModel.getAllProducts();
            // Render file apps/views/admin/product_list.ejs
            res.render('admin/product_list', { products: products });
        } catch (error) {
            res.status(500).send("Lỗi Server");
        }
    },

    create: async (req, res) => {
        try {
            const categories = await ProductModel.getCategories();
            // Render file apps/views/admin/product_add.ejs
            res.render('admin/product_add', { categories: categories });
        } catch (error) {
            res.status(500).send("Lỗi tải form");
        }
    },

    store: async (req, res) => {
        try {
            const data = {
                name: req.body.name,
                price: req.body.price,
                author: req.body.author,
                quantity: req.body.quantity,
                category_id: req.body.category_id,
                description: req.body.description,
                image_url: req.file ? req.file.filename : null
            };
            await ProductModel.createProduct(data);
            res.redirect('/admin');
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi lưu dữ liệu");
        }
    }
};