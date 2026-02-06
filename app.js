// app.js
const express = require("express");
const app = express();
const port = 3000;

// 1. Cấu hình View Engine (EJS)
app.set("view engine", "ejs");
app.set("views", __dirname + "/apps/views"); // Trỏ đúng thư mục Views

// 2. Cấu hình Public (CSS, JS, Ảnh)
app.use(express.static(__dirname + "/public"));

// 3. Cấu hình nhận dữ liệu Form
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// 4. GỌI CONTROLLER CHÍNH (Router)
// Dòng này sẽ tự động tìm file index.js trong thư mục apps/controllers
app.use(require(__dirname + "/apps/controllers")); 

// Khởi động Server
app.listen(port, () => {
    console.log(`Server đang chạy tại: http://localhost:${port}`);
});