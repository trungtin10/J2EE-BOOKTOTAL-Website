var express = require("express");
var router = express.Router();

router.get("/", function(req, res) {
    res.render("home");
});

router.use("/product", require(__dirname + "/productcontroller"));

// --- KIỂM TRA KỸ DÒNG NÀY ---
// Dòng này giúp nối đuôi "/admin" vào trước các link trong admincontroller
router.use("/admin", require(__dirname + "/admin/admincontroller")); 
// -----------------------------

module.exports = router;