(function (g) {
    'use strict';

    /**
     * Đồng bộ với com.bookstore.model.Product#normalizeImageSrc
     */
    g.resolveStorefrontImageSrc = function (raw) {
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
    };
})(typeof window !== 'undefined' ? window : this);
