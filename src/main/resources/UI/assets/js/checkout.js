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
  document.querySelector("#xac-nhan-phim").textContent = g.phim?.name || "Đơn bắp nước";
  document.querySelector("#xac-nhan-suat").textContent = g.suat
    ? CineSnack.dinhDangSuat(g.suat.startsAt) + " · " + g.suat.roomName
    : "Không đặt vé";
  document.querySelector("#xac-nhan-ghe").textContent = g.ghe.length
    ? g.ghe.map((ghe) => ghe.code).join(", ") : "Không đặt vé";
  document.querySelector("#xac-nhan-tong").textContent = CineSnack.dinhDangTien(CineSnack.tinhTien(g).tong);
  document.querySelector("#xac-nhan-sua").href = document.querySelector("#sua-dat-ve").href;
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
    if (user?.role === "NHAN_VIEN") {
      email.value = "";
      document.querySelector("#checkout-payment-title").textContent = "Tạo đơn tại quầy";
      document.querySelector("#checkout-payment-note").textContent =
        "Nhập email khách nhận đơn. Sau khi tạo, xác nhận tiền mặt hoặc chuyển khoản tại màn hình nhân viên.";
    } else if (!user) {
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
      const staff = user?.role === "NHAN_VIEN";
      const d = await CineSnackApi.goi(
        staff ? "/api/admin/orders" : user ? "/api/orders" : "/api/guest/orders",
        {
          method: "POST",
          body: CineSnack.taoYeuCau(g, email.value.trim()),
        },
      );
      maDaTao = d.code;
      try {
        sessionStorage.removeItem(CineSnack.khoaGio);
      } catch {}
      location.href = staff
        ? "admin.html?code=" + encodeURIComponent(d.code)
        : "orders.html?code=" + encodeURIComponent(d.code) + (user ? "" : "&guest=1");
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
