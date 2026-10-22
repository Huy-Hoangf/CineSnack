"use strict";
(async () => {
  const g = CineSnack.layGio(),
    loi = document.querySelector("#thanh-toan-loi");
  if (!CineSnack.gioHopLe(g)) {
    document.querySelector("#trong-thanh-toan").hidden = false;
    return;
  }
  document.querySelector("#thanh-toan-noi-dung").hidden = false;
  CineSnack.hienTomTat(g);
  document.querySelector("#sua-dat-ve").href = g.phim
    ? "booking.html?phim=" + g.phim.id
    : "booking.html";
  const form = document.querySelector("#thanh-toan-bieu-mau"),
    nut = form.querySelector("button[type=submit]"),
    email = document.querySelector("#khach-email");
  let user,
    gui = false,
    maDaTao,
    khongRo = false;
  nut.disabled = true;
  try {
    user = await CineSnackApi.taiKhoan();
    if (user) email.value = user.email;
    else {
      document.querySelector("#checkout-guest-note").hidden = false;
      document.querySelector("#checkout-guest-note").textContent =
        "Bạn đang đặt với tư cách khách. Nhập email để tiếp tục, không cần đăng nhập.";
      document.querySelector("#checkout-login").hidden = false;
    }
    nut.disabled = false;
  } catch (e) {
    loi.textContent = e.message;
    nut.disabled = true;
  }
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (gui) return;
    if (maDaTao) {
      location.href = "orders.html?code=" + encodeURIComponent(maDaTao);
      return;
    }
    if (!CineSnack.gioHopLe(g)) {
      loi.textContent = "Suất chiếu đã bắt đầu hoặc giỏ không hợp lệ. Vui lòng chọn lại.";
      return;
    }
    gui = true;
    nut.disabled = true;
    nut.setAttribute("aria-busy", "true");
    loi.textContent = "Đang tạo đơn…";
    try {
      const d = await CineSnackApi.goi(user ? "/api/orders" : "/api/guest/orders", {
        method: "POST",
        body: CineSnack.taoYeuCau(g, email.value.trim()),
      });
      maDaTao = d.code;
      try {
        sessionStorage.removeItem(CineSnack.khoaGio);
      } catch {}
      location.href = "orders.html?code=" + encodeURIComponent(d.code) + (user ? "" : "&guest=1");
    } catch (error) {
      khongRo = !error.status;
      loi.textContent = error.message + " Bạn có thể kiểm tra Đơn của tôi trước khi thử lại.";
    } finally {
      gui = false;
      nut.disabled = khongRo;
      nut.removeAttribute("aria-busy");
    }
  });
})();
