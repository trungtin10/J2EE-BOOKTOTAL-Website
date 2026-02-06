const express = require("express");
const path = require("path");
const session = require("express-session"); // Cần cài đặt: npm install express-session
const app = express();

// 1. Cấu hình View Engine
app.set("view engine", "ejs");
app.set("views", path.join(__dirname, "apps", "views"));

// 2. Middleware
app.use(express.static(path.join(__dirname, "public")));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// 3. Cấu hình Session (Quan trọng để giữ trạng thái Đăng nhập)
app.use(session({
    secret: 'bookstore_secret_key',
    resave: false,
    saveUninitialized: true,
    cookie: { maxAge: 3600000 } // Session tồn tại 1 tiếng
}));

// 4. Middleware truyền thông tin user ra tất cả các file EJS
app.use((req, res, next) => {
    res.locals.user = req.session.user || null;
    next();
});

// 5. Nạp router
const routes = require("./apps/controllers"); 
app.use(routes);

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`Server chạy tại: http://localhost:${PORT}`);
});