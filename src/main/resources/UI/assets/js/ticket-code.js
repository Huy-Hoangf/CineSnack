"use strict";
window.CineSnackTicketCode = (() => {
  function create(code) {
    if (typeof code !== "string" || !/^[A-Za-z0-9]{1,30}$/.test(code))
      throw new Error("Mã vé không hợp lệ.");
    if (!window.ZXingBrowser?.BrowserQRCodeSvgWriter)
      throw new Error("Không tải được bộ tạo QR vé. Bạn vẫn có thể dùng mã văn bản.");
    const svg = new ZXingBrowser.BrowserQRCodeSvgWriter().write(code, 160, 160);
    svg.classList.add("ticket-qr");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", "QR vé " + code);
    svg.style.background = "#fff";
    return svg;
  }
  return { create };
})();
