const db = require('../../db'); 

module.exports = {
    // 1. Lấy tất cả sản phẩm (kèm thông tin Category dùng JOIN)
    getAllProducts: async () => {
        const query = `
            SELECT products.*, categories.name as category_name 
            FROM products 
            LEFT JOIN categories ON products.category_id = categories.id
        `;
        const [rows] = await db.query(query);
        return rows;
    },

    // 2. Lấy sản phẩm theo ID
    getProductById: async (id) => {
        const [rows] = await db.query('SELECT * FROM products WHERE id = ?', [id]);
        return rows[0]; // Trả về 1 đối tượng duy nhất
    },

    // 3. Tạo sản phẩm mới
    createProduct: async (data) => {
        const { name, price, author, quantity, description, image_url, category_id } = data;
        const query = `
            INSERT INTO products (name, price, author, quantity, description, image_url, category_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `;
        const [result] = await db.query(query, [name, price, author, quantity, description, image_url, category_id]);
        return result;
    },

    // 4. Cập nhật sản phẩm
    updateProduct: async (id, data) => {
        const query = 'UPDATE products SET ? WHERE id = ?';
        const [result] = await db.query(query, [data, id]);
        return result;
    },

    // 5. Xóa sản phẩm
    deleteProduct: async (id) => {
        const [result] = await db.query('DELETE FROM products WHERE id = ?', [id]);
        return result;
    },

    // 6. Lấy danh sách danh mục (cho trang thêm/sửa sản phẩm)
    getCategories: async () => {
        const [rows] = await db.query('SELECT * FROM categories');
        return rows;
    }
    
};