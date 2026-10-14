"use strict";
(async () => {
  const loginButton = document.querySelector("#gui-tai-khoan");
  if (loginButton) loginButton.disabled = true;
  function ghiNhan() {
    try {
      sessionStorage.setItem("cinesnack-welcome-dismissed", "true");
    } catch {}
  }
  document
    .querySelectorAll("[data-tiep-tuc-khach]")
    .forEach((a) => a.addEventListener("click", ghiNhan));
  const popup = document.querySelector("#lua-chon-tai-khoan");
  if (popup) {
    document.querySelector("#tiep-tuc-khach").addEventListener("click", () => {
      ghiNhan();
      popup.close();
    });
    popup.addEventListener("close", ghiNhan);
    popup.addEventListener("cancel", ghiNhan);
    popup.querySelectorAll("a").forEach((a) => a.addEventListener("click", ghiNhan));
  }
  let user;
  try {
    user = await CineSnackApi.taiKhoan();
  } catch {
    user = null;
  }
  for (const box of document.querySelectorAll(".account-actions")) {
    box.replaceChildren();
    const a = document.createElement("a");
    a.className = "account-login";
    a.href = user ? "orders.html" : "authentication.html";
    a.textContent = user ? "Đơn của tôi" : "Đăng nhập";
    box.append(a);
    if (user) {
      const b = document.createElement("button");
      b.type = "button";
      b.className = "chu-nut";
      b.textContent = "Đăng xuất";
      b.addEventListener("click", async () => {
        b.disabled = true;
        try {
          await CineSnackApi.dangXuat();
          location.href = "index.html";
        } catch (e) {
          b.disabled = false;
          alert(e.message);
        }
      });
      box.append(b);
      if (["NHAN_VIEN", "QUAN_TRI"].includes(user.role)) {
        const q = document.createElement("a");
        q.href = "admin.html";
        q.textContent = "Quản trị";
        box.append(q);
      }
    }
  }
  if (popup && !user) {
    let dismiss = false;
    try {
      dismiss = sessionStorage.getItem("cinesnack-welcome-dismissed") === "true";
    } catch {}
    if (!dismiss) popup.showModal();
  }
  const form = document.querySelector("#bieu-mau-tai-khoan");
  if (!form) return;
  const message = document.querySelector("#thong-bao-tai-khoan"),
    nut = document.querySelector("#gui-tai-khoan");
  const params = new URLSearchParams(location.search);
  if (params.get("mode") === "register") {
    message.textContent =
      "Đăng ký chưa được cung cấp. Hãy dùng tài khoản đã được cấp để đăng nhập.";
  }
  function duongDanSauLogin() {
    const next = params.get("next") || "orders.html";
    const url = new URL(next, location.href);
    return url.origin === location.origin &&
      /^\/html\/(booking|checkout|orders|admin|movies|index)\.html$/.test(url.pathname)
      ? url.pathname + url.search
      : "orders.html";
  }
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (nut.disabled || !form.reportValidity()) return;
    nut.disabled = true;
    message.textContent = "Đang đăng nhập…";
    try {
      await CineSnackApi.dangNhap(
        document.querySelector("#email").value.trim(),
        document.querySelector("#mat-khau").value,
      );
      document.querySelector("#mat-khau").value = "";
      ghiNhan();
      location.href = duongDanSauLogin();
    } catch (error) {
      message.textContent =
        error.status === 401 ? "Email hoặc mật khẩu không đúng." : error.message;
      message.focus();
    } finally {
      nut.disabled = false;
    }
  });
  nut.disabled = false;
})();
