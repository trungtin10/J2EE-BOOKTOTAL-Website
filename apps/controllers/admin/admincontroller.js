const ProductModel = require('../../models/product');

module.exports = {
    // 1. Hiển thị danh sách sản phẩm
    index: async (req, res) => {
        try {
            const products = await ProductModel.getAllProducts();
            res.render('admin/product_list', { 
                products: products,
                activePage: 'products' 
            });
        } catch (error) {
            console.error("Lỗi index:", error);
            res.status(500).send("Lỗi tải danh sách sản phẩm");
        }
    },

    // 2. Hiển thị trang chi tiết sản phẩm (MỚI BỔ SUNG)
    view: async (req, res) => {
        try {
            const id = req.params.id;
            const product = await ProductModel.getProductById(id);
            if (!product) {
                return res.status(404).send("Sản phẩm không tồn tại");
            }
            res.render('admin/product_view', { product: product });
        } catch (error) {
            console.error("Lỗi view:", error);
            res.status(500).send("Lỗi khi tải chi tiết sản phẩm");
        }
    },

    // 3. Hiển thị form thêm mới
    create: async (req, res) => {
        try {
            const categories = await ProductModel.getCategories();
            res.render('admin/product_add', { categories: categories });
        } catch (error) {
            console.error("Lỗi create:", error);
            res.status(500).send("Lỗi tải danh mục");
        }
    },

    // 4. Xử lý lưu sản phẩm mới
    store: async (req, res) => {
        try {
            const data = {
                name: req.body.name,
                price: req.body.price,
                author: req.body.author,
                quantity: req.body.quantity,
                description: req.body.description,
                category_id: req.body.category_id,
                // Lưu tên file nếu upload, nếu không có thì để default
                image_url: req.file ? req.file.filename : 'default.jpg'
            };
            await ProductModel.createProduct(data);
            res.redirect('/admin');
        } catch (error) {
            console.error("Lỗi store:", error);
            res.status(500).send("Lỗi khi thêm sản phẩm");
        }
    },

    // 5. Mở trang sửa sản phẩm
    edit: async (req, res) => {
        try {
            const id = req.params.id;
            const product = await ProductModel.getProductById(id);
            const categories = await ProductModel.getCategories();
            if (!product) {
                return res.status(404).send("Sản phẩm không tồn tại");
            }
            res.render('admin/product_edit', { 
                product: product, 
                categories: categories 
            });
        } catch (error) {
            console.error("Lỗi edit:", error);
            res.status(500).send("Lỗi tải trang sửa");
        }
    },

    // 6. Xử lý cập nhật sản phẩm
    update: async (req, res) => {
        try {
            const id = req.params.id;
            const data = {
                name: req.body.name,
                price: req.body.price,
                author: req.body.author,
                quantity: req.body.quantity,
                description: req.body.description,
                category_id: req.body.category_id
            };
            // Nếu người dùng có chọn ảnh mới thì mới cập nhật lại image_url
            if (req.file) {
                data.image_url = req.file.filename;
            }
            
            await ProductModel.updateProduct(id, data);
            res.redirect('/admin');
        } catch (error) {
            console.error("Lỗi update:", error);
            res.status(500).send("Lỗi cập nhật sản phẩm");
        }
    },

    // 7. Xóa sản phẩm
    delete: async (req, res) => {
        try {
            const id = req.params.id;
            await ProductModel.deleteProduct(id);
            res.redirect('/admin');
        } catch (error) {
            console.error("Lỗi delete:", error);
            res.status(500).send("Lỗi khi xóa sản phẩm");
        }
    }
};