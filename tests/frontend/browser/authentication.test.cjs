const thu = require("node:test"),
  kt = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");
thu(
  "FE thật: đăng nhập sai có lỗi, đăng nhập đúng lưu session; logout bỏ quyền",
  async (t) => {
    const m = await chuanBi(t),
      p = await m.trangMoi();
    await m.mo(p, "authentication.html?next=orders.html");
    await p.locator("#email").fill("khach@example.test");
    await p.locator("#mat-khau").fill("sai");
    await p.locator("#gui-tai-khoan").click();
    await p
      .locator("#thong-bao-tai-khoan")
      .filter({ hasText: "mật khẩu không đúng" })
      .waitFor();
    await p.locator("#mat-khau").fill("MoPhong123!");
    await p.locator("#gui-tai-khoan").click();
    await p.waitForURL("**/orders.html");
    await p.locator("#orders-content").waitFor({ state: "visible" });
    await p.locator(".account-actions button").click();
    await p.waitForURL("**/index.html");
    await m.mo(p, "orders.html");
    await p.locator("#orders-login").waitFor({ state: "visible" });
    kt.deepEqual(m.loi, []);
  },
);
thu(
  "FE thật: guest tiếp tục xem catalog, không vào trang đơn/admin; không open redirect",
  async (t) => {
    const m = await chuanBi(t),
      p = await m.trangMoi();
    await m.mo(p, "index.html");
    await p.locator(".dang-luoi .the-phim").first().waitFor();
    await m.mo(p, "orders.html");
    await p.locator("#orders-login").waitFor({ state: "visible" });
    await m.mo(p, "admin.html");
    await p.locator("#admin-login").waitFor({ state: "visible" });
    await m.dangNhap(p, "khach@example.test", "https://example.com/steal");
    kt.ok(p.url().startsWith(m.goc));
    kt.ok(p.url().endsWith("/orders.html"));
    kt.deepEqual(m.loi, []);
  },
);
