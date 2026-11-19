"use strict";
window.CineSnackOrderQr = (() => {
  function create(order, prefix, onExpire = () => {}) {
    const element = (tag, text) => {
      const node = document.createElement(tag);
      if (text !== undefined) node.textContent = text;
      return node;
    };
    const node = element("section");
    node.className = "invoice order-payment-qr";
    const countdown = element("output");
    countdown.setAttribute("role", "timer");
    const time = element("p", "Thời gian giữ chỗ còn lại: ");
    time.append(countdown);
    const content = element("div");
    node.append(element("h3", "QR chuyển khoản đúng tổng tiền"), time, content);
    let deadline, disposed = false, pngUrl = null;
    function dispose() {
      disposed = true;
      deadline?.stop();
      content.replaceChildren();
      if (pngUrl) URL.revokeObjectURL(pngUrl);
      pngUrl = null;
      window.removeEventListener("pagehide", dispose);
      document.removeEventListener("visibilitychange", refresh);
    }
    function refresh() {
      if (!document.hidden) deadline?.refresh();
    }
    function start(expiresAt, serverTime, requestedAt) {
      deadline?.stop();
      deadline = CineSnackDeadline.start({ expiresAt, serverTime, requestedAt,
        onTick(text) {
          countdown.textContent = text;
          if (!node.isConnected) dispose();
        },
        onExpire() {
          if (pngUrl) URL.revokeObjectURL(pngUrl);
          pngUrl = null;
          content.replaceChildren(element("p", "Đã hết thời gian giữ chỗ. Không chuyển tiền cho đơn này. Cập nhật đơn để xem trạng thái."));
          onExpire();
        },
      });
      if (disposed) deadline.stop();
    }
    function active() { return !disposed && node.isConnected && deadline?.isActive(); }
    async function activate() {
      window.addEventListener("pagehide", dispose);
      document.addEventListener("visibilitychange", refresh);
      try {
        countdown.textContent = "Đang kiểm tra thời hạn…";
        const requestedAt = Date.now();
        const qr = await CineSnackApi.goi(`${prefix}/${encodeURIComponent(order.code)}/payment-qr`);
        if (disposed || !node.isConnected) return;
        start(qr.expiresAt || order.expiresAt, qr.serverTime, requestedAt);
        if (!active()) return;
        const image = element("img");
        Object.assign(image, { width:260, height:260, crossOrigin:"anonymous", alt:`QR chuyển khoản ${CineSnack.dinhDangTien(qr.amount)}, nội dung ${qr.content}` });
        const download = element("a", "Tải ảnh QR (PNG)");
        download.className = "nut phu";
        download.hidden = true;
        image.addEventListener("load", async () => {
          if (!active()) return;
          try {
            const url = await CineSnack.qrPngUrl(image);
            if (!active()) { URL.revokeObjectURL(url); return; }
            pngUrl = url;
            download.href = url;
            download.download = `CineSnack-${order.code}.png`;
            download.hidden = false;
          } catch {
            if (active()) content.append(element("p", "Không tạo được ảnh PNG. Bạn vẫn có thể quét QR."));
          }
        });
        image.addEventListener("error", () => {
          if (!active()) return;
          image.remove();
          content.append(element("p", "Không tải được QR. Cập nhật đơn để thử lại."));
        });
        image.src = qr.imageUrl;
        content.replaceChildren(image,
          element("p", `${qr.bankName || "Ngân hàng"} · ${qr.accountNo} · ${qr.accountName || ""}`),
          element("p", `Số tiền: ${CineSnack.dinhDangTien(qr.amount)} · Nội dung: ${qr.content}`),
          element("p", "Sau khi chuyển tiền, chờ nhân viên đối chiếu và xác nhận. Quét QR không tự hoàn tất đơn."), download);
      } catch (error) {
        if (disposed || !node.isConnected) return;
        if (!deadline) {
          try { start(order.expiresAt); }
          catch { content.replaceChildren(element("p", "Không xác định được thời hạn. Cập nhật đơn để kiểm tra.")); return; }
        }
        if (!deadline.isActive()) return;
        content.replaceChildren(element("p", error.message));
      }
    }
    return { node, activate, dispose };
  }
  return { create };
})();
