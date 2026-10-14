"use strict";
window.CineSnackApi = (() => {
  let csrf;
  async function layCsrf() {
    const ph = await fetch("/api/csrf", {
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });
    if (!ph.ok) throw new Error("Không thể lấy token phiên. Vui lòng thử lại.");
    csrf = await ph.json();
    return csrf;
  }
  async function goi(url, { method = "GET", body, form = false } = {}) {
    if (!/^\/api\//.test(url)) throw new Error("Đường dẫn API không hợp lệ.");
    const headers = { Accept: "application/json" };
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
      const token = csrf || (await layCsrf());
      headers[token.headerName] = token.token;
    }
    if (body !== undefined)
      headers["Content-Type"] = form ? "application/x-www-form-urlencoded" : "application/json";
    let ph;
    try {
      ph = await fetch(url, {
        method,
        headers,
        credentials: "same-origin",
        body:
          body === undefined ? undefined : form ? new URLSearchParams(body) : JSON.stringify(body),
      });
    } catch {
      throw new Error("Mất kết nối. Nếu đang tạo đơn, hãy kiểm tra Đơn của tôi trước khi đặt lại.");
    }
    const data = ph.status === 204 ? null : await ph.json().catch(() => null);
    if (!ph.ok) {
      const macDinh = {
        401: "Bạn cần đăng nhập.",
        403: "Bạn không có quyền hoặc phiên xác thực đã thay đổi. Tải lại trang để thử lại.",
        404: "Không tìm thấy dữ liệu.",
        409: "Dữ liệu đã thay đổi. Vui lòng kiểm tra lại lựa chọn.",
      };
      throw Object.assign(
        new Error(
          data?.message || macDinh[ph.status] || "Không thể xử lý yêu cầu. Vui lòng thử lại.",
        ),
        { status: ph.status },
      );
    }
    return data;
  }
  async function taiKhoan() {
    try {
      return await goi("/api/auth/me");
    } catch (e) {
      if (e.status === 401) return null;
      throw e;
    }
  }
  async function dangNhap(email, password) {
    await goi("/api/auth/login", {
      method: "POST",
      form: true,
      body: { username: email, password },
    });
    csrf = undefined;
    await layCsrf();
    return taiKhoan();
  }
  async function dangXuat() {
    await goi("/api/auth/logout", { method: "POST" });
    csrf = undefined;
  }
  return { goi, taiKhoan, dangNhap, dangXuat };
})();
