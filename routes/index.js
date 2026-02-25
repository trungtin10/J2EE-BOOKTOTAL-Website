const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const Product = require('../apps/models/product');
const User = require('../apps/models/user');
const Order = require('../apps/models/order');
const Coupon = require('../apps/models/coupon');
const { JWT_SECRET } = require('../config/keys');
const { sendWelcomeEmail } = require('../utils/mailer'); // Import mailer

const adminRoutes = require('./admin');

const createToken = (id, username, role, full_name, email) => {
    return jwt.sign({ id, username, role, full_name, email }, JWT_SECRET, {
        expiresIn: '1d'
    });
};

// --- NỘI DUNG CÁC TRANG TĨNH (FULL) ---
const staticPages = {
    'terms': {
        title: 'Điều khoản sử dụng',
        content: `
            <h4>1. Giới thiệu</h4>
            <p>Chào mừng quý khách hàng đến với website BookTotal. Khi quý khách truy cập vào trang web của chúng tôi có nghĩa là quý khách đồng ý với các điều khoản này.</p>
            <h4>2. Hướng dẫn sử dụng website</h4>
            <p>Khi vào web của chúng tôi, khách hàng phải đảm bảo đủ 18 tuổi, hoặc truy cập dưới sự giám sát của cha mẹ hay người giám hộ hợp pháp. Khách hàng đảm bảo có đầy đủ hành vi dân sự để thực hiện các giao dịch mua bán hàng hóa theo quy định hiện hành của pháp luật Việt Nam.</p>
            <h4>3. Ý kiến khách hàng</h4>
            <p>Tất cả nội dung trang web và ý kiến phê bình của quý khách đều là tài sản của chúng tôi. Nếu chúng tôi phát hiện bất kỳ thông tin giả mạo nào, chúng tôi sẽ khóa tài khoản của quý khách ngay lập tức hoặc áp dụng các biện pháp khác theo quy định của pháp luật Việt Nam.</p>
        `
    },
    'privacy': {
        title: 'Chính sách bảo mật thông tin cá nhân',
        content: `
            <h4>1. Mục đích và phạm vi thu thập</h4>
            <p>Việc thu thập dữ liệu chủ yếu trên website BookTotal bao gồm: email, điện thoại, tên đăng nhập, mật khẩu đăng nhập, địa chỉ khách hàng. Đây là các thông tin mà BookTotal cần thành viên cung cấp bắt buộc khi đăng ký sử dụng dịch vụ và để BookTotal liên hệ xác nhận khi khách hàng đăng ký sử dụng dịch vụ trên website nhằm đảm bảo quyền lợi cho cho người tiêu dùng.</p>
            <h4>2. Phạm vi sử dụng thông tin</h4>
            <p>BookTotal sử dụng thông tin thành viên cung cấp để:</p>
            <ul>
                <li>Cung cấp các dịch vụ đến thành viên.</li>
                <li>Gửi các thông báo về các hoạt động trao đổi thông tin giữa thành viên và website BookTotal.</li>
                <li>Ngăn ngừa các hoạt động phá hủy tài khoản người dùng của thành viên hoặc các hoạt động giả mạo thành viên.</li>
                <li>Liên lạc và giải quyết với thành viên trong những trường hợp đặc biệt.</li>
            </ul>
        `
    },
    'payment-privacy': {
        title: 'Chính sách bảo mật thanh toán',
        content: `
            <h4>1. Cam kết bảo mật</h4>
            <p>Hệ thống thanh toán thẻ được cung cấp bởi các đối tác cổng thanh toán (VNPAY, MoMo...) đã được cấp phép hoạt động hợp pháp tại Việt Nam. Do đó, các tiêu chuẩn bảo mật thanh toán thẻ tại BookTotal đảm bảo tuân thủ theo các tiêu chuẩn bảo mật ngành.</p>
            <h4>2. Quy định bảo mật</h4>
            <p>Chính sách giao dịch thanh toán bằng thẻ quốc tế và thẻ nội địa (internet banking) đảm bảo tuân thủ các tiêu chuẩn bảo mật của các Đối Tác Cổng Thanh Toán gồm:</p>
            <ul>
                <li>Thông tin tài chính của Khách hàng sẽ được bảo vệ trong suốt quá trình giao dịch bằng giao thức SSL (Secure Sockets Layer).</li>
                <li>Chứng nhận tiêu chuẩn bảo mật dữ liệu thông tin thanh toán (PCI DSS) do Trustwave cung cấp.</li>
                <li>Mật khẩu sử dụng một lần (OTP) được gửi qua SMS để đảm bảo việc truy cập tài khoản được xác thực.</li>
            </ul>
        `
    },
    'about': {
        title: 'Giới thiệu BookTotal',
        content: `
            <p>BookTotal được thành lập với sứ mệnh mang tri thức đến mọi nhà. Chúng tôi là một trong những nhà sách trực tuyến hàng đầu tại Việt Nam.</p>
            <p>Với kho sách khổng lồ đa dạng các thể loại từ văn học, kinh tế, kỹ năng sống đến sách thiếu nhi, BookTotal cam kết cung cấp sách chính hãng với chất lượng tốt nhất.</p>
            <p>Đội ngũ nhân viên tận tâm của chúng tôi luôn sẵn sàng hỗ trợ bạn để có trải nghiệm mua sắm tuyệt vời nhất.</p>
            <h4>Tầm nhìn</h4>
            <p>Trở thành hệ thống nhà sách hàng đầu Việt Nam, nơi cung cấp nguồn tri thức vô tận cho mọi lứa tuổi.</p>
            <h4>Sứ mệnh</h4>
            <p>Lan tỏa văn hóa đọc, nâng cao dân trí và góp phần xây dựng xã hội học tập.</p>
        `
    },
    'store-system': {
        title: 'Hệ thống trung tâm - nhà sách',
        content: `
            <p>Hiện tại BookTotal có hệ thống cửa hàng tại các thành phố lớn:</p>
            <h4>TP. Hồ Chí Minh</h4>
            <ul>
                <li><b>BookTotal Nguyễn Văn Linh:</b> 123 Nguyễn Văn Linh, Quận 7 - ĐT: 028 3775 3225</li>
                <li><b>BookTotal Lê Lợi:</b> 456 Lê Lợi, Quận 1 - ĐT: 028 3822 5577</li>
                <li><b>BookTotal Thủ Đức:</b> 210 Võ Văn Ngân, TP. Thủ Đức - ĐT: 028 3722 8899</li>
            </ul>
            <h4>Hà Nội</h4>
            <ul>
                <li><b>BookTotal Cầu Giấy:</b> 789 Cầu Giấy, Quận Cầu Giấy - ĐT: 024 3767 8888</li>
                <li><b>BookTotal Bà Triệu:</b> 101 Bà Triệu, Quận Hoàn Kiếm - ĐT: 024 3943 6666</li>
            </ul>
            <p>Giờ mở cửa: 8:00 - 22:00 tất cả các ngày trong tuần (kể cả Lễ, Tết).</p>
        `
    },
    'return-policy': {
        title: 'Chính sách đổi - trả - hoàn tiền',
        content: `
            <h4>1. Điều kiện đổi trả</h4>
            <p>BookTotal hỗ trợ đổi trả sản phẩm trong vòng 30 ngày kể từ ngày nhận hàng với các điều kiện sau:</p>
            <ul>
                <li>Sản phẩm còn nguyên bao bì, tem mác, chưa qua sử dụng.</li>
                <li>Sản phẩm bị lỗi do nhà sản xuất (thiếu trang, in sai, bung gáy...).</li>
                <li>Sản phẩm bị hư hỏng trong quá trình vận chuyển.</li>
                <li>Giao sai sản phẩm so với đơn đặt hàng.</li>
            </ul>
            <h4>2. Phương thức hoàn tiền</h4>
            <p>Tùy theo lí do hoàn trả sản phẩm kết quả đánh giá chất lượng tại kho, BookTotal sẽ có những phương thức hoàn tiền với chi tiết sau:</p>
            <ul>
                <li>Hoàn tiền qua tài khoản ngân hàng (nếu thanh toán online).</li>
                <li>Hoàn tiền mặt tại cửa hàng (nếu mua trực tiếp).</li>
                <li>Hoàn tiền vào ví BookTotal để mua sắm lần sau.</li>
            </ul>
        `
    },
    'warranty-policy': {
        title: 'Chính sách bảo hành - bồi hoàn',
        content: `
            <h4>1. Thời hạn bảo hành</h4>
            <p>Đối với các sản phẩm điện tử, văn phòng phẩm có bảo hành, thời hạn bảo hành được tính từ ngày mua hàng ghi trên hóa đơn hoặc phiếu bảo hành.</p>
            <h4>2. Điều kiện bảo hành</h4>
            <ul>
                <li>Sản phẩm còn trong thời hạn bảo hành.</li>
                <li>Sản phẩm bị lỗi kỹ thuật do nhà sản xuất.</li>
                <li>Phiếu bảo hành còn nguyên vẹn, không chắp vá, bôi xóa.</li>
            </ul>
            <h4>3. Bồi hoàn</h4>
            <p>Trong trường hợp sản phẩm bị mất mát hoặc hư hỏng trong quá trình vận chuyển do lỗi của BookTotal hoặc đơn vị vận chuyển, chúng tôi sẽ bồi hoàn 100% giá trị đơn hàng cho quý khách.</p>
        `
    },
    'shipping-policy': {
        title: 'Chính sách vận chuyển',
        content: `
            <h4>1. Phí vận chuyển</h4>
            <ul>
                <li><b>Miễn phí vận chuyển</b> cho đơn hàng từ 150.000đ tại TP.HCM và Hà Nội.</li>
                <li><b>Miễn phí vận chuyển</b> cho đơn hàng từ 250.000đ tại các tỉnh thành khác.</li>
                <li>Phí vận chuyển tiêu chuẩn: 30.000đ (nếu không đủ điều kiện miễn phí).</li>
            </ul>
            <h4>2. Thời gian giao hàng</h4>
            <ul>
                <li><b>Nội thành TP.HCM & Hà Nội:</b> 1 - 2 ngày làm việc.</li>
                <li><b>Các tỉnh thành khác:</b> 3 - 5 ngày làm việc.</li>
            </ul>
            <p>Lưu ý: Thời gian giao hàng không tính thứ 7, Chủ nhật và các ngày Lễ, Tết.</p>
        `
    },
    'wholesale-policy': {
        title: 'Chính sách khách sỉ',
        content: `
            <p>BookTotal có chính sách chiết khấu hấp dẫn dành cho khách hàng mua sỉ, thư viện, trường học và doanh nghiệp.</p>
            <h4>1. Điều kiện áp dụng</h4>
            <p>Đơn hàng có giá trị từ 5.000.000đ trở lên hoặc số lượng từ 50 cuốn trở lên.</p>
            <h4>2. Mức chiết khấu</h4>
            <ul>
                <li>Từ 5tr - 10tr: Chiết khấu 20%</li>
                <li>Từ 10tr - 20tr: Chiết khấu 25%</li>
                <li>Trên 20tr: Chiết khấu 30%</li>
            </ul>
            <h4>3. Liên hệ</h4>
            <p>Vui lòng liên hệ email <b>wholesale@booktotal.com</b> hoặc hotline <b>0909 123 456</b> để nhận báo giá chi tiết và hỗ trợ đặt hàng.</p>
        `
    },
    'payment-methods': {
        title: 'Phương thức thanh toán và xuất HĐ',
        content: `
            <h4>1. Các phương thức thanh toán</h4>
            <ul>
                <li><b>Thanh toán khi nhận hàng (COD):</b> Quý khách thanh toán tiền mặt cho nhân viên giao hàng khi nhận được sản phẩm.</li>
                <li><b>Thẻ ATM nội địa / Internet Banking:</b> Hỗ trợ hầu hết các ngân hàng tại Việt Nam qua cổng thanh toán VNPAY.</li>
                <li><b>Thẻ tín dụng quốc tế (Visa/Mastercard/JCB):</b> Thanh toán an toàn và bảo mật.</li>
                <li><b>Ví điện tử:</b> Hỗ trợ thanh toán qua MoMo, ZaloPay, ShopeePay.</li>
            </ul>
            <h4>2. Xuất hóa đơn GTGT (VAT)</h4>
            <p>Quý khách có nhu cầu xuất hóa đơn vui lòng điền đầy đủ thông tin (Tên công ty, Mã số thuế, Địa chỉ) vào phần "Ghi chú" khi đặt hàng hoặc liên hệ bộ phận CSKH trong vòng 24h kể từ khi đặt hàng thành công.</p>
        `
    }
};

// Route trang chủ
router.get('/', async (req, res) => {
    try {
        const products = await Product.getAllProducts();
        res.render('home', { products: products });
    } catch (err) {
        console.error(err);
        res.render('home', { products: [] });
    }
});

// --- ROUTE TÌM KIẾM ---
router.get('/search', async (req, res) => {
    try {
        const keyword = req.query.q;
        if (!keyword || keyword.trim() === "") {
            const products = await Product.getAllProducts();
            return res.render('home', { products: products, searchError: "Vui lòng nhập từ khóa để tìm kiếm!" });
        }
        const products = await Product.searchProducts(keyword);
        res.render('home', { products: products, keyword: keyword });
    } catch (err) {
        console.error(err);
        res.status(500).send("Lỗi tìm kiếm");
    }
});

// Route trang tĩnh
router.get('/page/:slug', (req, res) => {
    const slug = req.params.slug;
    const pageData = staticPages[slug];
    if (pageData) {
        res.render('page', { title: pageData.title, content: pageData.content });
    } else {
        res.status(404).render('page', { title: 'Không tìm thấy trang', content: '<p>Nội dung không tồn tại.</p>' });
    }
});

// --- LOGIN ---
router.post('/login', async (req, res) => {
    const { username, password, returnUrl } = req.body;
    try {
        const user = await User.login(username, password);
        if (user) {
            const role = user.role ? user.role.trim().toLowerCase() : 'user';
            const token = createToken(user.id, user.username, role, user.full_name, user.email);
            res.cookie('jwt', token, { httpOnly: true, maxAge: 24 * 60 * 60 * 1000 });

            if (role === 'admin') return res.redirect('/admin');
            return res.redirect(returnUrl || '/');
        } else {
            const redirectUrl = returnUrl ? returnUrl + '?loginError=Sai tài khoản hoặc mật khẩu!' : '/?loginError=Sai tài khoản hoặc mật khẩu!';
            return res.redirect(redirectUrl);
        }
    } catch (err) {
        console.error(err);
        return res.redirect('/?loginError=Lỗi hệ thống!');
    }
});

// --- REGISTER ---
router.post('/register', async (req, res) => {
    try {
        const { username, email, full_name, password } = req.body;

        // 1. Validate Email (Regex)
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            return res.redirect('/?registerError=Email không hợp lệ!');
        }

        // 2. Kiểm tra trùng Username
        const existingUser = await User.getUserByUsername(username);
        if (existingUser) return res.redirect('/?registerError=Tên đăng nhập đã tồn tại!');

        // 3. Thêm User
        await User.addUser(req.body);

        // 4. Gửi Email Chào mừng (Không await để tránh user phải chờ lâu)
        sendWelcomeEmail(email, full_name || username);

        return res.redirect('/?registerSuccess=Đăng ký thành công! Vui lòng kiểm tra email.');
    } catch (err) {
        console.error(err);
        // Bắt lỗi duplicate email từ DB nếu có
        if (err.code === 'ER_DUP_ENTRY' && err.message.includes('email')) {
            return res.redirect('/?registerError=Email đã được sử dụng!');
        }
        return res.redirect('/?registerError=Lỗi khi đăng ký!');
    }
});

// --- LOGOUT ---
router.get('/auth/logout', (req, res) => {
    res.cookie('jwt', '', { maxAge: 1 });
    res.redirect('/');
});

// --- GIỎ HÀNG ---
router.get('/cart', (req, res) => {
    const cart = req.cart || [];
    const totalAmount = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    res.render('cart', { cart: cart, totalAmount: totalAmount });
});

router.post('/cart/add/:id', async (req, res) => {
    const productId = req.params.id;
    const quantity = parseInt(req.body.quantity) || 1;
    try {
        const product = await Product.getProductById(productId);
        if (!product) return res.status(404).json({ success: false, message: "Sản phẩm không tồn tại" });

        let cart = req.cart || [];
        const existingItem = cart.find(item => item.id == productId);
        if (existingItem) existingItem.quantity += quantity;
        else cart.push({ id: product.id, name: product.name, price: product.price, image_url: product.image_url, quantity: quantity });

        res.cookie('cart', JSON.stringify(cart), { maxAge: 24 * 60 * 60 * 1000 });
        const totalQty = cart.reduce((sum, item) => sum + item.quantity, 0);
        res.json({ success: true, message: "Thêm vào giỏ hàng thành công!", totalQty: totalQty });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "Lỗi server" });
    }
});

router.get('/cart/update/:id', (req, res) => {
    const productId = req.params.id;
    const action = req.query.action;
    let cart = req.cart || [];
    const item = cart.find(item => item.id == productId);
    if (item) {
        if (action === 'increase') item.quantity++;
        else if (action === 'decrease') {
            item.quantity--;
            if (item.quantity <= 0) cart = cart.filter(i => i.id != productId);
        }
    }
    res.cookie('cart', JSON.stringify(cart), { maxAge: 24 * 60 * 60 * 1000 });
    res.redirect('/cart');
});

router.get('/cart/remove/:id', (req, res) => {
    const productId = req.params.id;
    let cart = req.cart || [];
    cart = cart.filter(item => item.id != productId);
    res.cookie('cart', JSON.stringify(cart), { maxAge: 24 * 60 * 60 * 1000 });
    res.redirect('/cart');
});

// --- CHECKOUT ---
router.post('/checkout', (req, res) => {
    const selectedIds = req.body.selected_items;
    let cart = req.cart || [];
    if (!selectedIds || selectedIds.length === 0) return res.redirect('/cart');

    const checkoutItems = cart.filter(item => selectedIds.includes(item.id.toString()));
    const totalAmount = checkoutItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const productDiscount = parseFloat(req.body.product_discount) || 0;
    const shippingFee = totalAmount > 500000 ? 0 : 30000;

    res.cookie('checkoutData', JSON.stringify({ items: checkoutItems, totalAmount, productDiscount, shippingFee }), { maxAge: 10 * 60 * 1000 });
    res.render('checkout', { cart: checkoutItems, totalAmount, productDiscount, shippingFee });
});

// --- ORDER ---
router.post('/order', async (req, res) => {
    if (res.locals.user && res.locals.user.role === 'admin') {
        return res.status(403).send("Admin không thể đặt hàng!");
    }

    let checkoutData = {};
    if (req.cookies.checkoutData) {
        try { checkoutData = JSON.parse(req.cookies.checkoutData); } catch(e) {}
    }
    if (!checkoutData.items || checkoutData.items.length === 0) return res.redirect('/cart');

    const { full_name, phone, email, address, note } = req.body;
    const userId = res.locals.user ? res.locals.user.id : null;
    const { totalAmount, shippingFee, productDiscount } = checkoutData;
    const finalTotal = totalAmount + shippingFee - productDiscount;

    try {
        const orderId = await Order.createOrder({
            user_id: userId,
            total_money: totalAmount,
            shipping_fee: shippingFee,
            discount_amount: productDiscount,
            final_total: finalTotal,
            shipping_address: `${full_name}, ${phone}, ${address} (${note})`,
            status: 'PENDING'
        });

        for (const item of checkoutData.items) {
            await Order.addOrderDetail(orderId, item.id, item.price, item.quantity);
            await Product.updateStock(item.id, item.quantity);
        }

        let cart = req.cart || [];
        const boughtIds = checkoutData.items.map(item => item.id);
        cart = cart.filter(item => !boughtIds.includes(item.id));

        res.cookie('cart', JSON.stringify(cart), { maxAge: 24 * 60 * 60 * 1000 });
        res.clearCookie('checkoutData');

        res.send(`<div style="text-align:center; padding: 50px;"><h2 style="color: green;">Đặt hàng thành công!</h2><p>Mã đơn hàng: #${orderId}</p><a href="/">Về trang chủ</a></div>`);
    } catch (err) {
        console.error(err);
        res.status(500).send("Lỗi khi đặt hàng");
    }
});

// --- PRODUCT DETAIL ---
router.get('/product/:id', async (req, res) => {
    try {
        const productId = req.params.id;
        const product = await Product.getProductById(productId);
        if (!product) return res.status(404).send("Sản phẩm không tồn tại");

        const relatedProducts = await Product.getAllProducts();
        const reviews = await Product.getReviews(productId);

        let canReview = false;
        let reviewMessage = "Vui lòng đăng nhập để đánh giá.";
        if (res.locals.user) {
            canReview = true;
            reviewMessage = "";
        }

        res.render('product_detail', {
            product, relatedProducts, reviews, canReview, reviewMessage
        });
    } catch (err) {
        console.error(err);
        res.status(500).send("Lỗi server");
    }
});

router.post('/product/:id/review', async (req, res) => {
    if (!res.locals.user) return res.redirect('/auth/login');
    if (res.locals.user.role === 'admin') return res.status(403).send("Admin không thể đánh giá!");

    try {
        const { rating, comment } = req.body;
        await Product.addReview(res.locals.user.id, req.params.id, rating, comment);
        res.send(`<script>alert('Đánh giá đã gửi và đang chờ duyệt!'); window.location.href = '/product/${req.params.id}';</script>`);
    } catch (err) {
        console.error(err);
        res.status(500).send("Lỗi khi gửi đánh giá");
    }
});

router.use(adminRoutes);

module.exports = router;