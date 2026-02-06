
// apps/controllers/homecontroller.js
const ProductModel = require('../models/product');

module.exports = {
    index: async (req, res) => {
        try {
            const products = await ProductModel.getAllProducts();
            // Render file apps/views/home.ejs
            res.render('home', { products: products });
        } catch (error) {
            console.log(error);
            res.status(500).send("Lỗi Server");
        }
    }
};
