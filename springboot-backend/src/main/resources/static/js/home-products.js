(function () {
    'use strict';

    var grid = document.getElementById('homeProductGrid');
    var btn = document.getElementById('homeLoadMoreBtn');
    if (!grid || !btn) return;

    function esc(s) {
        if (s == null || s === '') return '';
        var d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML;
    }

    function imgSrc(p) {
        var raw = p && (p.imageUrl != null ? p.imageUrl : p.image_url);
        if (typeof resolveStorefrontImageSrc === 'function') {
            return resolveStorefrontImageSrc(raw);
        }
        if (raw == null || raw === '') return '/img/placeholder.svg';
        var u = String(raw).trim().replace(/\\/g, '/');
        if (!u) return '/img/placeholder.svg';
        var low = u.toLowerCase();
        if (low.indexOf('http://') === 0 || low.indexOf('https://') === 0) return u;
        if (u.indexOf('//') === 0) return u;
        var up = low.lastIndexOf('/uploads/');
        if (up >= 0) return u.substring(up);
        if (u.indexOf('/uploads/') === 0 || u.indexOf('/img/') === 0 || u.indexOf('/images/') === 0) return u;
        if (/^[a-z]:\//i.test(u)) {
            var slash = u.lastIndexOf('/');
            var fname = slash >= 0 ? u.slice(slash + 1) : u;
            if (fname && fname.indexOf(':') < 0) return '/uploads/' + fname;
        }
        if (u.indexOf('uploads/') === 0) return '/' + u;
        if (u.charAt(0) === '/') return '/uploads' + u;
        return '/uploads/' + u;
    }

    function formatPrice(n) {
        return Math.round(Number(n) || 0).toLocaleString('vi-VN') + ' đ';
    }

    function cardHtml(p) {
        var id = p.id;
        var name = esc(p.name);
        var img = esc(imgSrc(p));
        var author = p.authorName ? esc(p.authorName) : 'Đang cập nhật';
        var price = esc(formatPrice(p.price));
        var discount = p.discountPercent != null && p.discountPercent !== '' ? p.discountPercent : null;
        var orig =
            discount != null && p.originalPrice != null ? esc(formatPrice(p.originalPrice)) : '';
        var priceRow =
            discount != null
                ? '<div class="home-pcard-price-row"><span class="home-pcard-old">' +
                  orig +
                  '</span><span class="home-pcard-badge">-' +
                  esc(String(discount)) +
                  '%</span></div>'
                : '';
        return (
            '<div class="col home-product-col">' +
            '<a href="/product/' +
            id +
            '" class="product-link">' +
            '<div class="card book-card shadow-sm h-100 home-pcard">' +
            '<div class="home-card-img-wrap">' +
            '<img src="' +
            img +
            '" class="card-img-top" alt="' +
            name +
            '" onerror="this.onerror=null;this.src=\'/img/placeholder.svg\'">' +
            '</div>' +
            '<div class="card-body">' +
            '<h5 class="card-title home-pcard-title" title="' +
            name +
            '">' +
            name +
            '</h5>' +
            '<div class="home-pcard-author text-truncate">' +
            author +
            '</div>' +
            '<div class="home-pcard-price-block mt-2">' +
            priceRow +
            '<div class="home-pcard-price">' +
            price +
            '</div>' +
            '</div>' +
            '<div class="rating-sold home-pcard-stars">' +
            '<div class="stars stars-muted" aria-hidden="true">' +
            '<i class="fas fa-star"></i><i class="fas fa-star"></i><i class="fas fa-star"></i>' +
            '<i class="fas fa-star"></i><i class="fas fa-star"></i>' +
            '</div></div>' +
            '</div></div></a></div>'
        );
    }

    var loading = false;

    btn.addEventListener('click', function () {
        if (loading) return;
        var next = parseInt(btn.getAttribute('data-next-page'), 10);
        var size = parseInt(btn.getAttribute('data-page-size'), 10);
        if (isNaN(next) || isNaN(size) || size < 1) return;
        loading = true;
        btn.disabled = true;
        var label = btn.querySelector('.home-load-more-label');
        var orig = label ? label.textContent : '';
        if (label) label.textContent = 'Đang tải...';
        var sort = btn.getAttribute('data-sort') || 'newest';
        fetch(
            '/api/products?page=' +
                next +
                '&size=' +
                size +
                '&sort=' +
                encodeURIComponent(sort)
        )
            .then(function (r) {
                if (!r.ok) throw new Error('fetch');
                return r.json();
            })
            .then(function (data) {
                var list = data.content || [];
                var frag = list.map(cardHtml).join('');
                if (frag) grid.insertAdjacentHTML('beforeend', frag);
                if (data.last || list.length === 0) {
                    btn.style.display = 'none';
                } else {
                    btn.setAttribute('data-next-page', String(next + 1));
                }
            })
            .catch(function () {})
            .finally(function () {
                loading = false;
                btn.disabled = false;
                if (label && btn.style.display !== 'none') label.textContent = orig;
            });
    });
})();
