document.addEventListener('DOMContentLoaded', function() {
    updateDeliveryDate();
});

function increaseQty() {
    let input = document.getElementById('qtyInput');
    input.value = parseInt(input.value) + 1;
}

function decreaseQty() {
    let input = document.getElementById('qtyInput');
    if (parseInt(input.value) > 1) {
        input.value = parseInt(input.value) - 1;
    }
}

function addToCart(productId) {
    const raw = document.getElementById('qtyInput').value;
    const quantity = Math.max(1, parseInt(raw, 10) || 1);
    const params = new URLSearchParams();
    params.append('productId', productId);
    params.append('quantity', quantity);

    fetch('/cart/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            // Hiển thị popup thành công ở giữa màn hình
            const popup = document.getElementById('centerSuccessPopup');
            if (popup) {
                popup.classList.add('show');
                setTimeout(() => {
                    popup.classList.remove('show');
                }, 2000);
            } else {
                Swal.fire({
                    icon: 'success',
                    title: 'Thành công',
                    text: 'Đã thêm sản phẩm vào giỏ hàng',
                    timer: 2000,
                    showConfirmButton: false
                });
            }
            
            // Cập nhật số lượng trên header
            if (typeof refreshCartDropdownHeader === 'function') refreshCartDropdownHeader();
            if (window.booktotalCart && typeof window.booktotalCart.persistFromServer === 'function') {
                window.booktotalCart.persistFromServer().catch(function () { /* ignore */ });
            }
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Lỗi',
                text: data.message || 'Không thể thêm vào giỏ hàng'
            });
        }
    })
    .catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Lỗi kết nối',
            text: 'Có lỗi xảy ra, vui lòng thử lại sau.'
        });
    });
}

function buyNow(productId) {
    const raw = document.getElementById('qtyInput').value;
    const quantity = Math.max(1, parseInt(raw, 10) || 1);
    const params = new URLSearchParams();
    params.append('productId', productId);
    params.append('quantity', quantity);

    fetch('/cart/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            if (window.booktotalCart && typeof window.booktotalCart.persistFromServer === 'function') {
                window.booktotalCart.persistFromServer().catch(function () { /* ignore */ });
            }
            window.location.href = '/cart';
        } else {
            Swal.fire({ 
                icon: 'error', 
                title: 'Lỗi', 
                text: data.message || 'Không thể mua ngay' 
            });
        }
    })
    .catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Lỗi kết nối',
            text: 'Có lỗi xảy ra, vui lòng thử lại sau.'
        });
    });
}

function updateDeliveryDate() {
    const deliveryEl = document.getElementById('deliveryDate');
    if (!deliveryEl) return;

    const today = new Date();
    const deliveryDate = new Date(today);
    deliveryDate.setDate(today.getDate() + 2);

    const options = { weekday: 'long', day: 'numeric', month: 'numeric' };
    const dateString = deliveryDate.toLocaleDateString('vi-VN', options);
    const formattedDate = dateString.replace(/^\w/, (c) => c.toUpperCase());

    deliveryEl.innerText = `Dự kiến giao ${formattedDate}`;
}

// Đóng modal Bootstrap an toàn khi getInstance() chưa có (tránh lỗi 'backdrop' trong modal.js)
function hideBootstrapModal(modalElement) {
    if (!modalElement || typeof bootstrap === 'undefined' || !bootstrap.Modal) return;
    try {
        const Modal = bootstrap.Modal;
        const inst = Modal.getInstance(modalElement) ?? Modal.getOrCreateInstance(modalElement);
        inst.hide();
    } catch (e) {
        modalElement.classList.remove('show');
        modalElement.setAttribute('aria-hidden', 'true');
        modalElement.removeAttribute('aria-modal');
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('padding-right');
        document.body.style.removeProperty('overflow');
        document.querySelectorAll('.modal-backdrop').forEach((b) => b.remove());
    }
}

function setRatingText(text) {
    const ratingTextEl = document.getElementById('ratingText');
    if (ratingTextEl) ratingTextEl.innerText = text;
}

function updateDistricts() {
    const provinceSelect = document.getElementById('provinceSelect');
    const districtSelect = document.getElementById('districtSelect');
    if (!provinceSelect || !districtSelect) return;

    const province = provinceSelect.value;
    districtSelect.innerHTML = '';

    let districts = [];
    if (province === 'Hồ Chí Minh') districts = ['Quận 1', 'Quận 3', 'Thủ Đức', 'Bình Thạnh'];
    else if (province === 'Hà Nội') districts = ['Hoàn Kiếm', 'Ba Đình', 'Cầu Giấy'];
    else districts = ['Quận trung tâm', 'Huyện ngoại thành'];

    districts.forEach(d => {
        const option = document.createElement('option');
        option.value = d;
        option.text = d;
        districtSelect.appendChild(option);
    });
}

function saveAddress() {
    const provinceSelect = document.getElementById('provinceSelect');
    const districtSelect = document.getElementById('districtSelect');
    const currentAddressEl = document.getElementById('currentAddress');
    const addressModalEl = document.getElementById('addressModal');

    if (provinceSelect && districtSelect && currentAddressEl) {
        const province = provinceSelect.value;
        const district = districtSelect.value;
        currentAddressEl.innerText = `${district}, ${province}`;

        if (addressModalEl) {
            hideBootstrapModal(addressModalEl);
        }
        updateDeliveryDate();
    }
}