"use strict";
(async () => {
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
  if (workRole && /\/(booking|checkout|orders)\.html$/.test(location.pathname)) {
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
  const message = document.querySelector("#thong-bao-tai-khoan");
  const params = new URLSearchParams(location.search);
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
    const allowed = worker
      ? url.pathname === "/html/admin.html"
      : /^\/html\/(booking|checkout|orders|movies|index)\.html$/.test(url.pathname);
    return url.origin === location.origin && allowed ? url.pathname + url.search : fallback;
  }
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (loginButton.disabled || !form.reportValidity()) return;
    loginButton.disabled = true;
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
    }
  });
  loginButton.disabled = false;
})();
