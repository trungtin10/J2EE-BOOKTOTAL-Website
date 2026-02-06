// apps/models/product.js
const db = require('../../db'); // Trỏ ra root/db.js

module.exports = {
    getAllProducts: async () => {
        try {
            const sql = `
                SELECT p.*, c.name AS category_name 
                FROM products p 
                LEFT JOIN categories c ON p.category_id = c.id 
                ORDER BY p.id DESC`;
            const [rows] = await db.query(sql);
            return rows;
        } catch (error) { throw error; }
    },

    getCategories: async () => {
        const [rows] = await db.query("SELECT * FROM categories");
        return rows;
    },

    createProduct: async (data) => {
        try {
            const sql = `INSERT INTO products (name, price, author, category_id, description, image_url, quantity) VALUES (?, ?, ?, ?, ?, ?, ?)`;
            const values = [data.name, data.price, data.author, data.category_id, data.description, data.image_url, data.quantity];
            const [result] = await db.query(sql, values);
            return result;
        } catch (error) { throw error; }
    }
};