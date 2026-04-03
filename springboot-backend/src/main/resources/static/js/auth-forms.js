(function () {
    'use strict';

    var EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    function clearFieldGroupInvalid(input) {
        if (!input) return;
        input.classList.remove('is-invalid');
        var group = input.closest('.mb-3, .mb-4');
        if (!group) return;
        var pw = group.querySelector('.password-field');
        if (pw) pw.classList.remove('field-invalid');
        var fb = group.querySelector('.invalid-feedback');
        if (fb) {
            fb.textContent = '';
            fb.style.display = 'none';
        }
    }

    function setFieldInvalid(input, message) {
        if (!input) return;
        var group = input.closest('.mb-3, .mb-4');
        if (!group) return;
        input.classList.add('is-invalid');
        var pw = group.querySelector('.password-field');
        if (pw) pw.classList.add('field-invalid');
        var fb = group.querySelector('.invalid-feedback');
        if (fb) {
            fb.textContent = message;
            fb.style.display = 'block';
        }
    }

    window.togglePasswordVisibility = function (button) {
        var wrap = button.closest('.password-field');
        if (!wrap) return;
        var input = wrap.querySelector('input');
        if (!input) return;
        var show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        button.setAttribute('aria-pressed', show ? 'true' : 'false');
        button.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu');
        var icon = button.querySelector('i');
        if (icon) {
            icon.classList.toggle('fa-eye', !show);
            icon.classList.toggle('fa-eye-slash', show);
        }
    };

    function wirePasswordToggles(root) {
        root = root || document;
        root.querySelectorAll('.btn-toggle-password').forEach(function (btn) {
            btn.addEventListener('click', function () {
                togglePasswordVisibility(btn);
            });
        });
    }

    function validateLoginForm(form) {
        var username = form.querySelector('[name="username"]');
        var password = form.querySelector('[name="password"]');
        var ok = true;

        if (username) {
            clearFieldGroupInvalid(username);
            var u = (username.value || '').trim();
            if (!u) {
                setFieldInvalid(username, 'Vui lòng nhập tên đăng nhập.');
                ok = false;
            } else if (u.indexOf('@') !== -1 && !EMAIL_RE.test(u)) {
                setFieldInvalid(username, 'Email không đúng định dạng.');
                ok = false;
            }
        }

        if (password) {
            clearFieldGroupInvalid(password);
            if (!(password.value || '').trim()) {
                setFieldInvalid(password, 'Vui lòng nhập mật khẩu.');
                ok = false;
            }
        }

        return ok;
    }

    function validateRegisterForm(form) {
        var fullName = form.querySelector('input[name="fullName"]');
        var email = form.querySelector('input[name="email"]');
        var username = form.querySelector('input[name="username"]');
        var password = form.querySelector('input[name="password"]');
        var ok = true;

        [fullName, email, username, password].forEach(function (el) {
            if (el) clearFieldGroupInvalid(el);
        });

        if (fullName && !(fullName.value || '').trim()) {
            setFieldInvalid(fullName, 'Vui lòng nhập họ và tên.');
            ok = false;
        }

        if (email) {
            var em = (email.value || '').trim();
            if (!em) {
                setFieldInvalid(email, 'Vui lòng nhập email.');
                ok = false;
            } else if (!EMAIL_RE.test(em)) {
                setFieldInvalid(email, 'Email không đúng định dạng.');
                ok = false;
            }
        }

        if (username) {
            var un = (username.value || '').trim();
            if (!un) {
                setFieldInvalid(username, 'Vui lòng nhập tên đăng nhập.');
                ok = false;
            } else if (!/^[a-zA-Z0-9_]+$/.test(un)) {
                setFieldInvalid(username, 'Tên đăng nhập chỉ gồm chữ, số và dấu gạch dưới.');
                ok = false;
            } else if (un.length < 3 || un.length > 50) {
                setFieldInvalid(username, 'Tên đăng nhập từ 3 đến 50 ký tự.');
                ok = false;
            }
        }

        if (password) {
            if (!(password.value || '').trim()) {
                setFieldInvalid(password, 'Vui lòng nhập mật khẩu.');
                ok = false;
            } else if ((password.value || '').length < 6) {
                setFieldInvalid(password, 'Mật khẩu tối thiểu 6 ký tự.');
                ok = false;
            }
        }

        return ok;
    }

    function bindLiveClear(form) {
        form.querySelectorAll('input').forEach(function (inp) {
            inp.addEventListener('input', function () {
                clearFieldGroupInvalid(inp);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        wirePasswordToggles(document);

        function setSubmittingState(form) {
            if (!form) return;
            var btn = form.querySelector('button[type="submit"]');
            if (!btn || btn.disabled) return;
            if (btn.dataset.submitting === '1') return;

            btn.dataset.submitting = '1';
            btn.disabled = true;
            btn.classList.add('is-entering');

            // Save original content for safety if user cancels via validation.
            if (!btn.dataset.originalHtml) {
                btn.dataset.originalHtml = btn.innerHTML;
            }

            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang đăng nhập...';
        }

        var loginForm = document.getElementById('pageLoginForm');
        if (loginForm) {
            loginForm.setAttribute('novalidate', '');
            bindLiveClear(loginForm);
            loginForm.addEventListener('submit', function (e) {
                var ok = validateLoginForm(loginForm);
                if (!ok) {
                    e.preventDefault();
                } else {
                    setSubmittingState(loginForm);
                }
            });
        }

        var regForm = document.getElementById('pageRegisterForm');
        if (regForm) {
            regForm.setAttribute('novalidate', '');
            bindLiveClear(regForm);
            regForm.addEventListener('submit', function (e) {
                var ok = validateRegisterForm(regForm);
                if (!ok) {
                    e.preventDefault();
                }
            });
        }

        var modalForm = document.getElementById('modalLoginForm');
        if (modalForm) {
            modalForm.setAttribute('novalidate', '');
            bindLiveClear(modalForm);
            modalForm.addEventListener('submit', function (e) {
                var ok = validateLoginForm(modalForm);
                if (!ok) {
                    e.preventDefault();
                    return;
                }
                setSubmittingState(modalForm);
            });
        }

        var modalRegForm = document.getElementById('modalRegisterForm');
        if (modalRegForm) {
            modalRegForm.setAttribute('novalidate', '');
            bindLiveClear(modalRegForm);
            modalRegForm.addEventListener('submit', function (e) {
                if (!validateRegisterForm(modalRegForm)) e.preventDefault();
            });
        }
    });
})();
