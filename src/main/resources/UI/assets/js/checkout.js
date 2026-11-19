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
  document.querySelector("#xac-nhan-phim").textContent =
    g.phim?.name || "Đơn bắp nước";
  document.querySelector("#xac-nhan-suat").textContent = g.suat
    ? CineSnack.dinhDangSuat(g.suat.startsAt) + " · " + g.suat.roomName
    : "Không đặt vé";
  document.querySelector("#xac-nhan-ghe").textContent = g.ghe.length
    ? g.ghe.map((ghe) => ghe.code).join(", ")
    : "Không đặt vé";
  document.querySelector("#xac-nhan-tong").textContent = CineSnack.dinhDangTien(
    CineSnack.tinhTien(g).tong,
  );
  document.querySelector("#xac-nhan-sua").href =
    document.querySelector("#sua-dat-ve").href;
  const form = document.querySelector("#thanh-toan-bieu-mau"),
    nut = form.querySelector("button[type=submit]"),
    email = document.querySelector("#khach-email"),
    qrDialog = document.querySelector("#checkout-qr-dialog"),
    openQr = document.querySelector("#checkout-open-qr"),
    downloadQr = document.querySelector("#checkout-download-qr"),
    qrStatus = document.querySelector("#checkout-qr-action-status");
  let currentQr = null;
  let currentQrPngUrl = null;
  let deadline = null;
  function expireQr() {
    currentQr = null;
    openQr.disabled = true;
    for (const id of ["account", "content"])
      document.querySelector("#checkout-copy-" + id).disabled = true;
    downloadQr.hidden = true;
    downloadQr.removeAttribute("href");
    if (currentQrPngUrl) URL.revokeObjectURL(currentQrPngUrl);
    currentQrPngUrl = null;
    document.querySelector("#checkout-qr-image").replaceChildren();
    const text = "Đã hết thời gian giữ chỗ. Không chuyển tiền cho đơn này. Mở đơn để kiểm tra trạng thái.";
    document.querySelector("#checkout-qr-error").textContent = text;
    loi.textContent = text;
  }
  function watchDeadline(expiresAt, serverTime, requestedAt) {
    deadline?.stop();
    deadline = CineSnackDeadline.start({ expiresAt, serverTime, requestedAt,
      onTick: (text) => document.querySelector("#checkout-countdown").textContent = text,
      onExpire: expireQr,
    });
  }
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) deadline?.refresh();
  });
  window.addEventListener("pagehide", () => {
    deadline?.stop();
    currentQr = null;
    document.querySelector("#checkout-qr-image").replaceChildren();
    downloadQr.hidden = true;
    downloadQr.removeAttribute("href");
    if (currentQrPngUrl) URL.revokeObjectURL(currentQrPngUrl);
  });
  window.addEventListener("pageshow", (event) => {
    if (event.persisted && maDaTao) {
      const target = user?.role === "NHAN_VIEN" ? "admin.html" : "orders.html";
      location.replace(target + "?code=" + encodeURIComponent(maDaTao) + (user ? "" : "&guest=1"));
    }
  });
  let user,
    gui = false,
    maDaTao,
    khongRo = false;
  nut.disabled = true;
  try {
    user = await CineSnackApi.taiKhoan();
    if (user?.role === "QUAN_TRI" || (user?.role === "NHAN_VIEN" &&
        new URLSearchParams(location.search).get("mode") !== "counter")) {
      location.replace("booking.html");
      return;
    }
    if (user?.role === "NHAN_VIEN") {
      document.querySelector("#counter-total").hidden = false;
      document.querySelector("#counter-total strong").textContent = CineSnack.dinhDangTien(CineSnack.tinhTien(g).tong);
      document.querySelector("#sua-dat-ve").href += (g.phim ? "&" : "?") + "mode=counter";
      document.querySelector("#xac-nhan-sua").href = document.querySelector("#sua-dat-ve").href;
    }
    if (user) email.value = user.email;
    if (user?.role === "NHAN_VIEN") {
      email.value = "";
      email.required = false;
      document.querySelector("#khach-email-label").textContent =
        "Email khách (không bắt buộc)";
      document.querySelector("#khach-email-help").textContent =
        "Để trống nếu khách không cần nhận thông tin đơn qua email.";
      document.querySelector("#checkout-payment-title").textContent =
        "Tạo đơn tại quầy";
      document.querySelector("#checkout-payment-note").textContent =
        "Chọn cách khách thanh toán. Nhân viên xác nhận sau khi đã nhận tiền.";
      document.querySelector("#checkout-staff-method").hidden = false;
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
  openQr.addEventListener("click", () => {
    if (deadline?.isActive()) qrDialog.showModal();
  });
  async function copyQr(value) {
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      qrStatus.textContent = "Đã sao chép.";
    } catch {
      qrStatus.textContent =
        "Không thể sao chép tự động. Hãy nhấn giữ để chọn và sao chép.";
    }
  }
  document
    .querySelector("#checkout-copy-account")
    .addEventListener("click", () => copyQr(currentQr?.accountNo));
  document
    .querySelector("#checkout-copy-content")
    .addEventListener("click", () => copyQr(currentQr?.content));
  async function hienQr(ma, orderUrl, staff) {
    const qrError = document.querySelector("#checkout-qr-error");
    document.querySelector("#checkout-order-link").href = orderUrl;
    document.querySelector("#checkout-order-link").textContent = staff
      ? "Mở đơn để xác nhận thanh toán"
      : "Xem trạng thái đơn";
    document.querySelector("#checkout-qr-code").textContent = ma;
    if (!deadline?.isActive()) return;
    document.querySelector("#checkout-qr-image").replaceChildren();
    for (const id of ["bank", "account", "account-name", "amount", "content"])
      document.querySelector("#checkout-" + id).textContent = "";
    currentQr = null;
    if (currentQrPngUrl) URL.revokeObjectURL(currentQrPngUrl);
    currentQrPngUrl = null;
    downloadQr.hidden = true;
    downloadQr.removeAttribute("href");
    qrStatus.textContent = "";
    qrError.textContent = "";
    qrDialog.showModal();
    try {
      const prefix = user ? "/api/orders" : "/api/guest/orders";
      const requestedAt = Date.now();
      const qr = await CineSnackApi.goi(
        `${prefix}/${encodeURIComponent(ma)}/payment-qr`,
      );
      if (!deadline.isActive()) return;
      if (qr.expiresAt && qr.serverTime) watchDeadline(qr.expiresAt, qr.serverTime, requestedAt);
      if (!deadline.isActive()) return;
      currentQr = qr;
      const image = document.createElement("img");
      image.width = 260;
      image.height = 260;
      image.crossOrigin = "anonymous";
      image.alt = `QR chuyển khoản ${CineSnack.dinhDangTien(qr.amount)}, nội dung ${qr.content}`;
      image.addEventListener("load", async () => {
        if (!deadline.isActive() || currentQr !== qr) return;
        try {
          const png = await CineSnack.qrPngUrl(image);
          if (!deadline.isActive() || currentQr !== qr) {
            URL.revokeObjectURL(png);
            return;
          }
          currentQrPngUrl = png;
          downloadQr.href = currentQrPngUrl;
          downloadQr.download = `CineSnack-${ma}.png`;
          downloadQr.hidden = false;
        } catch {
          if (!deadline.isActive()) return;
          qrStatus.textContent =
            "Không tạo được ảnh PNG từ QR. Bạn vẫn có thể quét mã đang hiển thị.";
        }
      });
      image.addEventListener("error", () => {
        if (!deadline.isActive()) return;
        image.remove();
        qrError.textContent = "Không tải được ảnh QR. Mở đơn để thử lại.";
      });
      image.src = qr.imageUrl;
      document.querySelector("#checkout-qr-image").replaceChildren(image);
      for (const [id, value] of Object.entries({
        bank: qr.bankName,
        account: qr.accountNo,
        "account-name": qr.accountName,
        amount: CineSnack.dinhDangTien(qr.amount),
        content: qr.content,
      }))
        document.querySelector("#checkout-" + id).textContent = value || "—";
    } catch (error) {
      if (!deadline.isActive()) return;
      qrError.textContent =
        error.message + " Đơn đã được tạo; bạn có thể mở đơn để xem lại.";
    }
  }
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (gui) return;
    if (maDaTao) {
      location.href =
        "orders.html?code=" +
        encodeURIComponent(maDaTao) +
        (user ? "" : "&guest=1");
      return;
    }
    if (!CineSnack.gioHopLe(g)) {
      loi.textContent =
        "Suất chiếu đã bắt đầu hoặc giỏ không hợp lệ. Vui lòng chọn lại.";
      return;
    }
    gui = true;
    nut.disabled = true;
    nut.setAttribute("aria-busy", "true");
    loi.textContent = "Đang tạo đơn…";
    try {
      const staff = user?.role === "NHAN_VIEN";
      const requestedAt = Date.now();
      const d = await CineSnackApi.goi(
        staff
          ? "/api/admin/orders"
          : user
            ? "/api/orders"
            : "/api/guest/orders",
        {
          method: "POST",
          body: CineSnack.taoYeuCau(g, email.value.trim()),
        },
      );
      maDaTao = d.code;
      try {
        sessionStorage.removeItem(CineSnack.khoaGio);
      } catch {}
      const orderUrl = staff
        ? "admin.html?code=" + encodeURIComponent(d.code)
        : "orders.html?code=" +
          encodeURIComponent(d.code) +
          (user ? "" : "&guest=1");
      if (staff && form.elements.paymentMethod.value === "cash") {
        location.href = orderUrl;
        return;
      }
      nut.hidden = true;
      openQr.hidden = false;
      email.readOnly = true;
      loi.textContent = "";
      watchDeadline(d.expiresAt, d.createdAt, requestedAt);
      await hienQr(d.code, orderUrl, staff);
    } catch (error) {
      khongRo = !error.status;
      loi.textContent =
        error.message + " Bạn có thể kiểm tra Đơn của tôi trước khi thử lại.";
    } finally {
      gui = false;
      nut.disabled = khongRo;
      nut.removeAttribute("aria-busy");
    }
  });
})();
