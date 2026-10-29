"use strict";
(async () => {
  document.querySelectorAll(".chinh-dieu-huong").forEach((menu) => {
    const khung = document.createElement("div");
    khung.className = "dieu-huong-khung";
    menu.before(khung);
    khung.append(menu);
    const update = () => khung.classList.toggle("con-noi-dung", menu.scrollLeft + menu.clientWidth < menu.scrollWidth - 1);
    menu.addEventListener("scroll", update);
    window.addEventListener("resize", update);
    update();
  });
  const form = document.querySelector("#bieu-mau-tai-khoan");
  const loginButton = document.querySelector("#gui-tai-khoan");
  if (loginButton) loginButton.disabled = true;
  let user;
  try {
    user = await CineSnackApi.taiKhoan();
  } catch {
    user = undefined;
  }
  const workRole = user && ["NHAN_VIEN", "QUAN_TRI"].includes(user.role);
  const redirectToAdmin =
    (user?.role === "QUAN_TRI" && /\/(booking|checkout|orders)\.html$/.test(location.pathname)) ||
    (user?.role === "NHAN_VIEN" && /\/orders\.html$/.test(location.pathname));
  if (redirectToAdmin) {
    location.replace("admin.html");
    return;
  }
  if (workRole) document.querySelectorAll(".dau-nut").forEach((node) => (node.hidden = true));
  const returnTo = location.pathname + location.search;
  for (const box of document.querySelectorAll(".account-actions")) {
    box.replaceChildren();
    if (user) {
      const identity = document.createElement("span");
      identity.className = "account-identity";
      identity.textContent =
        user.role === "QUAN_TRI"
          ? "Quản trị viên"
          : user.role === "NHAN_VIEN"
            ? "Nhân viên quầy"
            : "Tài khoản của bạn";
      box.append(identity);
    }
    const link = document.createElement("a");
    link.className = "account-button";
    link.href = workRole ? "admin.html" : user ? "orders.html" : "orders.html?guest=1";
    link.textContent = workRole
      ? user.role === "QUAN_TRI"
        ? "Quản trị rạp"
        : "Quầy nhân viên"
      : "Đơn của tôi";
    box.append(link);
    if (user) {
      const logout = document.createElement("button");
      logout.type = "button";
      logout.className = "account-button account-secondary";
      logout.textContent = "Đăng xuất";
      logout.addEventListener("click", async () => {
        logout.disabled = true;
        try {
          await CineSnackApi.dangXuat();
          location.href = "index.html";
        } catch (error) {
          logout.disabled = false;
          logout.textContent = "Thử đăng xuất lại";
          logout.title = error.message;
        }
      });
      box.append(logout);
    } else {
      const login = document.createElement("a");
      login.className = "account-button account-primary";
      login.href = "authentication.html?next=" + encodeURIComponent(returnTo);
      login.textContent = "Đăng nhập";
      box.append(login);
    }
  }
  if (!form) return;
  document.querySelectorAll("[data-hien-mat-khau]").forEach((button) => {
    button.addEventListener("click", () => {
      const input = document.getElementById(button.dataset.hienMatKhau);
      const hien = input.type === "password";
      input.type = hien ? "text" : "password";
      button.textContent = (hien ? "Ẩn " : "Hiện ") + button.dataset.nhanMatKhau;
      button.setAttribute("aria-pressed", String(hien));
    });
  });
  const message = document.querySelector("#thong-bao-tai-khoan");
  const params = new URLSearchParams(location.search);
  let cheDo = params.get("mode") === "register" ? "register" : "login";
  const next = params.get("next");
  let url = null;
  if (next) {
    try {
      url = new URL(next, location.href);
    } catch {
      url = null;
    }
  }
  const guest = document.querySelector("[data-tiep-tuc-khach]");
  if (guest && url) {
    if (
      url.origin === location.origin &&
      /^\/html\/(booking|checkout|orders|movies|index)\.html$/.test(url.pathname)
    )
      guest.href = url.pathname + url.search;
  }
  function destination(account) {
    const worker = ["NHAN_VIEN", "QUAN_TRI"].includes(account.role);
    const fallback = worker ? "admin.html" : "orders.html";
    if (!url) return fallback;
    const allowed =
      (account.role === "NHAN_VIEN" &&
        /^\/html\/(admin|booking|checkout)\.html$/.test(url.pathname)) ||
      (account.role === "QUAN_TRI" && url.pathname === "/html/admin.html") ||
      (!worker && /^\/html\/(booking|checkout|orders|movies|index)\.html$/.test(url.pathname));
    return url.origin === location.origin && allowed ? url.pathname + url.search : fallback;
  }
  function hienThiCheDo() {
    document.querySelectorAll("[data-che-do]").forEach((tab) => {
      tab.addEventListener("click", (event) => {
        event.preventDefault();
        cheDo = tab.dataset.cheDo;
        message.textContent = "";
        hienThiCheDoNoListeners();
      });
    });
    hienThiCheDoNoListeners();
  }
  function hienThiCheDoNoListeners() {
    const dangKy = cheDo === "register";
    document.querySelectorAll("[data-dang-ky]").forEach((field) => {
      field.hidden = !dangKy;
      const input = field.querySelector("input");
      input.disabled = !dangKy;
      input.required = dangKy;
    });
    document.querySelectorAll("[data-che-do]").forEach((tab) => {
      if (tab.dataset.cheDo === cheDo) tab.setAttribute("aria-current", "page");
      else tab.removeAttribute("aria-current");
    });
    const title = document.querySelector("#tieu-de-tai-khoan");
    const description = document.querySelector("#mo-ta-tai-khoan");
    const hint = document.querySelector("#goi-y-mat-khau");
    const password = document.querySelector("#mat-khau");
    if (title) title.textContent = dangKy ? "Đăng ký" : "Đăng nhập";
    if (description) description.textContent = dangKy ? "Tạo tài khoản khách hàng CineSnack." : "Chào mừng bạn trở lại với CineSnack.";
    if (hint) hint.hidden = !dangKy;
    if (password) {
      password.autocomplete = dangKy ? "new-password" : "current-password";
      password.minLength = dangKy ? 8 : 0;
    }
    loginButton.textContent = dangKy ? "Đăng ký" : "Đăng nhập";
  }
  hienThiCheDo();
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (loginButton.disabled || !form.reportValidity()) return;
    if (cheDo === "register") {
      const password = document.querySelector("#mat-khau").value;
      if (password !== document.querySelector("#xac-nhan-mat-khau").value) {
        message.textContent = "Mật khẩu xác nhận không khớp.";
        message.focus();
        return;
      }
      loginButton.disabled = true;
      loginButton.textContent = "Đang đăng ký…";
      loginButton.setAttribute("aria-busy", "true");
      message.textContent = "Đang đăng ký…";
      try {
        await CineSnackApi.dangKy({
          hoTen: document.querySelector("#ho-ten").value.trim(),
          email: document.querySelector("#email").value.trim(),
          matKhau: password,
        });
        document.querySelector("#mat-khau").value = "";
        document.querySelector("#xac-nhan-mat-khau").value = "";
        cheDo = "login";
        hienThiCheDoNoListeners();
        message.textContent = "Đăng ký thành công. Vui lòng đăng nhập.";
        message.focus();
      } catch (error) {
        message.textContent = error.status === 409 ? "Email đã được sử dụng." : error.message;
        message.focus();
      } finally {
        loginButton.disabled = false;
        loginButton.removeAttribute("aria-busy");
        loginButton.textContent = cheDo === "register" ? "Đăng ký" : "Đăng nhập";
      }
      return;
    }
    loginButton.disabled = true;
    loginButton.textContent = "Đang đăng nhập…";
    loginButton.setAttribute("aria-busy", "true");
    message.textContent = "Đang đăng nhập…";
    try {
      const account = await CineSnackApi.dangNhap(
        document.querySelector("#email").value.trim(),
        document.querySelector("#mat-khau").value,
      );
      document.querySelector("#mat-khau").value = "";
      location.href = destination(account);
    } catch (error) {
      message.textContent =
        error.status === 401 ? "Email hoặc mật khẩu không đúng." : error.message;
      message.focus();
    } finally {
      loginButton.disabled = false;
      loginButton.removeAttribute("aria-busy");
      loginButton.textContent = "Đăng nhập";
    }
  });
  loginButton.disabled = false;
})();
