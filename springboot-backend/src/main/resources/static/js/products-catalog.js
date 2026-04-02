(function () {
    'use strict';

    var boot = document.getElementById('shopCatalogBootstrap');
    if (!boot) return;

    var maxPriceCeil = parseFloat(boot.getAttribute('data-max-price')) || 1000000;
    maxPriceCeil = Math.max(10000, Math.ceil(maxPriceCeil / 10000) * 10000);

    var fminAttr = boot.getAttribute('data-filter-min');
    var fmaxAttr = boot.getAttribute('data-filter-max');

    var initialCategory = boot.getAttribute('data-category-id');
    initialCategory = initialCategory && initialCategory !== '' ? initialCategory : '';
    var initialSearch = boot.getAttribute('data-search') || '';

    var pageSize = parseInt(boot.getAttribute('data-page-size'), 10);
    if (isNaN(pageSize) || pageSize < 1) pageSize = 12;
    pageSize = Math.min(pageSize, 48);

    var apiPageAttr = parseInt(boot.getAttribute('data-api-page'), 10);
    var initialApiPage = isNaN(apiPageAttr) ? 0 : Math.max(0, apiPageAttr);

    var sortAttr = boot.getAttribute('data-filter-sort') || 'newest';

    var categoryId = initialCategory === '' ? null : initialCategory;
    var keyword = initialSearch;
    var sort = sortAttr;
    var minPrice = 0;
    var maxPrice = maxPriceCeil;
    var currentPage = initialApiPage;
    var totalPages = 1;

    var grid = document.getElementById('productGrid');
    var emptyEl = document.getElementById('productGridEmpty');
    var loadingEl = document.getElementById('productGridLoading');
    var paginationNav = document.getElementById('shopPagination');
    var categorySelect = document.getElementById('shopCategorySelect');
    var searchInput = document.getElementById('shopKeyword');
    var sortSelect = document.getElementById('shopSort');
    var minRange = document.getElementById('priceMinRange');
    var maxRange = document.getElementById('priceMaxRange');
    var minNum = document.getElementById('priceMinInput');
    var maxNum = document.getElementById('priceMaxInput');
    var priceResetBtn = document.getElementById('priceResetBtn');
    var topPaginationWrap = document.getElementById('shopPaginationTop');
    var priceRangeLabel = document.getElementById('shopPriceRangeLabel');

    function esc(s) {
        if (s == null || s === '') return '';
        var d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML;
    }

    function imageUrl(p) {
        if (!p.imageUrl) return '/img/placeholder.svg';
        var u = p.imageUrl;
        if (u.indexOf('http') === 0 || u.indexOf('/uploads/') === 0) return u;
        return '/uploads/' + u;
    }

    function formatPrice(n) {
        return Math.round(Number(n) || 0).toLocaleString('vi-VN') + ' đ';
    }

    function discountPercentOf(p) {
        if (p.discountPercent != null && p.discountPercent !== '') {
            var x = parseInt(p.discountPercent, 10);
            if (!isNaN(x)) return x;
        }
        var op = p.originalPrice;
        var pr = p.price;
        if (op == null || pr == null) return null;
        var o = Number(op);
        var s = Number(pr);
        if (!(o > 0) || !(s < o)) return null;
        return Math.round((1.0 - s / o) * 100.0);
    }

    function setLoading(on) {
        if (!grid) return;
        grid.classList.toggle('opacity-50', on);
        grid.style.pointerEvents = on ? 'none' : '';
        if (loadingEl) loadingEl.hidden = !on;
    }

    function syncRangesFromNumbers() {
        if (minRange) minRange.value = String(Math.min(minPrice, maxPrice));
        if (maxRange) maxRange.value = String(Math.max(minPrice, maxPrice));
        if (minNum) minNum.value = String(Math.round(Math.min(minPrice, maxPrice)));
        if (maxNum) maxNum.value = String(Math.round(Math.max(minPrice, maxPrice)));
    }

    function readNumbersFromInputs() {
        var a = minNum ? parseFloat(minNum.value) : 0;
        var b = maxNum ? parseFloat(maxNum.value) : maxPriceCeil;
        if (isNaN(a)) a = 0;
        if (isNaN(b)) b = maxPriceCeil;
        if (a > b) {
            var t = a;
            a = b;
            b = t;
        }
        minPrice = Math.max(0, a);
        maxPrice = Math.min(maxPriceCeil, b);
        syncRangesFromNumbers();
    }

    function readRangesFromSliders() {
        var a = minRange ? parseFloat(minRange.value) : 0;
        var b = maxRange ? parseFloat(maxRange.value) : maxPriceCeil;
        if (isNaN(a)) a = 0;
        if (isNaN(b)) b = maxPriceCeil;
        if (a > b) {
            var t = a;
            a = b;
            b = t;
        }
        minPrice = a;
        maxPrice = b;
        syncRangesFromNumbers();
    }

    function syncCategorySelect() {
        if (!categorySelect) return;
        categorySelect.value = categoryId == null ? '' : String(categoryId);
    }

    function updatePriceRangeLabel() {
        if (!priceRangeLabel) return;
        priceRangeLabel.textContent =
            Math.round(0).toLocaleString('vi-VN') +
            ' - ' +
            Math.round(maxPriceCeil).toLocaleString('vi-VN');
    }

    function buildQuery() {
        var q = new URLSearchParams();
        q.set('sort', sort);
        q.set('page', String(currentPage));
        q.set('size', String(pageSize));
        if (categoryId != null && categoryId !== '') q.set('categoryId', String(categoryId));
        if (keyword && keyword.trim()) q.set('keyword', keyword.trim());
        q.set('minPrice', String(minPrice));
        q.set('maxPrice', String(maxPrice));
        return q.toString();
    }

    function pushUrl() {
        var path = '/products';
        var q = new URLSearchParams();
        if (categoryId != null && categoryId !== '') q.set('categoryId', String(categoryId));
        if (keyword && keyword.trim()) q.set('search', keyword.trim());
        if (minPrice > 0) q.set('minPrice', String(Math.round(minPrice)));
        if (maxPrice < maxPriceCeil) q.set('maxPrice', String(Math.round(maxPrice)));
        if (sort && sort !== 'newest') q.set('sort', sort);
        if (currentPage > 0) q.set('page', String(currentPage + 1));
        var qs = q.toString();
        if (window.history && window.history.replaceState) {
            window.history.replaceState({}, '', qs ? path + '?' + qs : path);
        }
    }

    function paginationHtml() {
        var prevDis = currentPage <= 0 ? ' disabled' : '';
        var nextDis = currentPage >= totalPages - 1 ? ' disabled' : '';
        return (
            '<div class="btn-group btn-group-sm" role="group" aria-label="Pagination">' +
            '<a class="btn btn-outline-secondary' + prevDis + '" href="#" data-page-action="prev">Trước</a>' +
            '<span class="btn btn-outline-secondary disabled">' + (currentPage + 1) + ' / ' + totalPages + '</span>' +
            '<a class="btn btn-outline-secondary' + nextDis + '" href="#" data-page-action="next">Sau</a>' +
            '</div>'
        );
    }

    function renderPagination() {
        if (paginationNav) {
            if (totalPages <= 1) {
                paginationNav.hidden = true;
                paginationNav.innerHTML = '';
            } else {
                paginationNav.hidden = false;
                paginationNav.innerHTML =
                    '<div class="d-flex justify-content-center">' + paginationHtml() + '</div>';
            }
        }
        if (topPaginationWrap) {
            if (totalPages <= 1) {
                topPaginationWrap.hidden = true;
                topPaginationWrap.innerHTML = '';
            } else {
                topPaginationWrap.hidden = false;
                topPaginationWrap.innerHTML = paginationHtml();
            }
        }
    }

    function parsePagedResponse(data) {
        if (data == null) return { list: [], totalPages: 0 };
        if (Array.isArray(data)) {
            return { list: data, totalPages: data.length ? 1 : 0 };
        }
        var list = data.content || [];
        var tp = data.totalPages;
        if (typeof tp !== 'number' || isNaN(tp)) tp = list.length ? 1 : 0;
        return { list: list, totalPages: tp };
    }

    var fetchTimer = null;

    function fetchProducts() {
        if (!grid) return;
        setLoading(true);
        var url = '/api/products?' + buildQuery();
        fetch(url)
            .then(function (r) {
                if (!r.ok) throw new Error('fetch');
                return r.json();
            })
            .then(function (data) {
                var parsed = parsePagedResponse(data);
                totalPages = Math.max(0, parsed.totalPages);
                if (totalPages > 0 && currentPage >= totalPages) {
                    currentPage = totalPages - 1;
                    fetchProducts();
                    return;
                }
                render(parsed.list || []);
                renderPagination();
                pushUrl();
            })
            .catch(function () {
                totalPages = 0;
                render([]);
                renderPagination();
            })
            .finally(function () {
                setLoading(false);
            });
    }

    function scheduleFetch(immediate, resetPage) {
        if (resetPage) currentPage = 0;
        if (fetchTimer) clearTimeout(fetchTimer);
        if (immediate) {
            fetchProducts();
            return;
        }
        fetchTimer = setTimeout(fetchProducts, 0);
    }

    function render(list) {
        if (!grid) return;
        if (!list.length) {
            grid.innerHTML = '';
            if (emptyEl) emptyEl.hidden = false;
            return;
        }
        if (emptyEl) emptyEl.hidden = true;
        var html = list.map(function (p) {
            var img = esc(imageUrl(p));
            var name = esc(p.name);
            var id = p.id;
            var price = formatPrice(p.price);
            var pct = discountPercentOf(p);
            var badge = pct != null
                ? '<span class="position-absolute top-0 end-0 m-2 badge bg-danger product-sale-badge">-' + esc(String(pct)) + '%</span>'
                : '';
            var priceBlock;
            if (pct != null && p.originalPrice != null) {
                priceBlock =
                    '<div class="mt-3">' +
                    '<span class="text-muted text-decoration-line-through small d-block">' + formatPrice(p.originalPrice) + '</span>' +
                    '<span class="text-danger fw-bold fs-5">' + price + '</span>' +
                    '</div>';
            } else {
                priceBlock =
                    '<div class="d-flex justify-content-between align-items-center mt-3">' +
                    '<span class="text-danger fw-bold fs-5">' + price + '</span>' +
                    '</div>';
            }
            return (
                '<div class="col">' +
                '<div class="card h-100 border-0 shadow-sm rounded-3 product-card">' +
                '<a href="/product/' + id + '" class="text-decoration-none">' +
                '<div class="position-relative overflow-hidden">' +
                badge +
                '<img src="' + img + '" class="card-img-top p-3 rounded-4" alt="' + name + '" style="height:250px;object-fit:contain;" onerror="this.src=\'/img/placeholder.svg\'">' +
                '</div>' +
                '<div class="card-body">' +
                '<h6 class="card-title text-dark fw-bold mb-2 text-truncate-2" style="height:48px;">' + name + '</h6>' +
                priceBlock +
                '</div></a>' +
                '<div class="card-footer bg-white border-0 pb-3 d-grid">' +
                '<button type="button" class="btn btn-outline-danger btn-sm rounded-pill fw-bold" onclick="addToCart(' + id + ')">THÊM VÀO GIỎ</button>' +
                '</div></div></div>'
            );
        }).join('');
        grid.innerHTML = html;
    }

    function init() {
        if (sortSelect) sortSelect.value = sort;

        function bindPaginationClicks(el) {
            if (!el || el.dataset.bound) return;
            el.dataset.bound = '1';
            el.addEventListener('click', function (e) {
                var a = e.target.closest('[data-page-action]');
                if (!a) return;
                e.preventDefault();
                var act = a.getAttribute('data-page-action');
                if (act === 'prev' && currentPage > 0) {
                    currentPage--;
                    scheduleFetch(true, false);
                } else if (act === 'next' && currentPage < totalPages - 1) {
                    currentPage++;
                    scheduleFetch(true, false);
                }
            });
        }
        bindPaginationClicks(paginationNav);
        bindPaginationClicks(topPaginationWrap);

        if (minRange) {
            minRange.min = '0';
            minRange.max = String(maxPriceCeil);
            minRange.step = String(Math.max(1000, Math.round(maxPriceCeil / 200)));
        }
        if (maxRange) {
            maxRange.min = '0';
            maxRange.max = String(maxPriceCeil);
            maxRange.step = minRange ? minRange.step : '10000';
        }
        if (minNum) {
            minNum.min = '0';
            minNum.max = String(maxPriceCeil);
        }
        if (maxNum) {
            maxNum.min = '0';
            maxNum.max = String(maxPriceCeil);
        }
        minPrice = 0;
        maxPrice = maxPriceCeil;
        if (fminAttr && fminAttr !== '' && !isNaN(parseFloat(fminAttr))) {
            minPrice = Math.max(0, parseFloat(fminAttr));
        }
        if (fmaxAttr && fmaxAttr !== '' && !isNaN(parseFloat(fmaxAttr))) {
            maxPrice = Math.min(maxPriceCeil, parseFloat(fmaxAttr));
        }
        if (minPrice > maxPrice) {
            var s = minPrice;
            minPrice = maxPrice;
            maxPrice = s;
        }
        syncRangesFromNumbers();

        if (searchInput) searchInput.value = initialSearch;

        syncCategorySelect();

        if (categorySelect) {
            categorySelect.addEventListener('change', function () {
                var v = categorySelect.value;
                categoryId = v === '' || v == null ? null : v;
                scheduleFetch(true, true);
            });
        }

        if (searchInput) {
            var t;
            searchInput.addEventListener('input', function () {
                keyword = searchInput.value;
                clearTimeout(t);
                t = setTimeout(function () {
                    scheduleFetch(true, true);
                }, 280);
            });
        }

        if (sortSelect) {
            sortSelect.addEventListener('change', function () {
                sort = sortSelect.value || 'newest';
                scheduleFetch(true, true);
            });
        }

        if (minRange) {
            minRange.addEventListener('input', function () {
                readRangesFromSliders();
                scheduleFetch(true, true);
            });
        }
        if (maxRange) {
            maxRange.addEventListener('input', function () {
                readRangesFromSliders();
                scheduleFetch(true, true);
            });
        }

        function onNumberChange() {
            readNumbersFromInputs();
            scheduleFetch(true, true);
        }

        if (minNum) {
            minNum.addEventListener('change', onNumberChange);
            minNum.addEventListener('blur', onNumberChange);
        }
        if (maxNum) {
            maxNum.addEventListener('change', onNumberChange);
            maxNum.addEventListener('blur', onNumberChange);
        }

        if (priceResetBtn) {
            priceResetBtn.addEventListener('click', function () {
                minPrice = 0;
                maxPrice = maxPriceCeil;
                syncRangesFromNumbers();
                scheduleFetch(true, true);
            });
        }

        updatePriceRangeLabel();

        scheduleFetch(true, false);
    }

    init();
})();
