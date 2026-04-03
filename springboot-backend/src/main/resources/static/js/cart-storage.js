// Persist cart to LocalStorage and optionally sync to server session.
(function () {
  'use strict';

  var STORAGE_KEY = 'booktotal_cart_v1';

  function safeJsonParse(s, fallback) {
    try { return JSON.parse(s); } catch (_) { return fallback; }
  }

  function getLocalCartItems() {
    var raw = localStorage.getItem(STORAGE_KEY);
    var obj = safeJsonParse(raw, null);
    if (!obj || !Array.isArray(obj.items)) return [];
    return obj.items
      .filter(function (it) { return it && it.productId != null && it.quantity != null; })
      .map(function (it) { return { productId: Number(it.productId), quantity: Number(it.quantity) }; })
      .filter(function (it) { return Number.isFinite(it.productId) && Number.isFinite(it.quantity) && it.quantity > 0; });
  }

  function setLocalCartFromSummary(summary) {
    if (!summary || !Array.isArray(summary.items)) return;
    var items = summary.items.map(function (it) {
      return { productId: it.id, quantity: it.quantity };
    }).filter(function (it) { return it.productId != null && it.quantity > 0; });
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ items: items, savedAt: Date.now() }));
  }

  function setHeaderBadge(totalQty) {
    var badge = document.getElementById('cartBadge');
    if (!badge) return;
    badge.textContent = String(totalQty || 0);
    badge.style.display = totalQty > 0 ? 'inline-block' : 'none';
  }

  async function fetchServerSummary() {
    var r = await fetch('/cart/summary', { credentials: 'same-origin' });
    if (!r.ok) throw new Error('summary');
    return await r.json();
  }

  async function syncLocalToServer(localItems) {
    if (!localItems || !localItems.length) return;
    await fetch('/cart/sync', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items: localItems })
    });
  }

  // Public helpers used by product pages
  window.booktotalCart = {
    persistFromServer: async function () {
      var summary = await fetchServerSummary();
      setLocalCartFromSummary(summary);
      setHeaderBadge(summary.totalQty || 0);
      return summary;
    },
    syncIfServerEmpty: async function () {
      var localItems = getLocalCartItems();
      if (!localItems.length) return;
      var summary = await fetchServerSummary();
      if ((summary.totalQty || 0) === 0) {
        await syncLocalToServer(localItems);
        await window.booktotalCart.persistFromServer();
      } else {
        // server has data, keep local updated
        setLocalCartFromSummary(summary);
      }
    },
    bootHeaderFromLocal: function () {
      var localItems = getLocalCartItems();
      var qty = localItems.reduce(function (acc, it) { return acc + (it.quantity || 0); }, 0);
      if (qty > 0) setHeaderBadge(qty);
    }
  };

  function handlePostLogoutQuery() {
    try {
      var params = new URLSearchParams(window.location.search);
      if (params.get('btLogout') !== '1') {
        return;
      }
      localStorage.removeItem(STORAGE_KEY);
      setHeaderBadge(0);
      params.delete('btLogout');
      var qs = params.toString();
      var clean = window.location.pathname + (qs ? '?' + qs : '') + window.location.hash;
      if (window.history && window.history.replaceState) {
        window.history.replaceState({}, '', clean);
      }
      if (typeof refreshCartDropdownHeader === 'function') {
        refreshCartDropdownHeader();
      }
    } catch (e) { /* ignore */ }
  }

  function handlePostOrderSuccessQuery() {
    try {
      var params = new URLSearchParams(window.location.search);
      if (params.get('btCartClear') !== '1') {
        return;
      }
      localStorage.removeItem(STORAGE_KEY);
      setHeaderBadge(0);
      params.delete('btCartClear');
      var qs = params.toString();
      var clean = window.location.pathname + (qs ? '?' + qs : '') + window.location.hash;
      if (window.history && window.history.replaceState) {
        window.history.replaceState({}, '', clean);
      }
      if (typeof refreshCartDropdownHeader === 'function') {
        refreshCartDropdownHeader();
      }
    } catch (e) { /* ignore */ }
  }

  document.addEventListener('DOMContentLoaded', function () {
    handlePostLogoutQuery();
    handlePostOrderSuccessQuery();
    window.booktotalCart.bootHeaderFromLocal();
    window.booktotalCart.syncIfServerEmpty().catch(function () { /* ignore */ });
  });
})();

