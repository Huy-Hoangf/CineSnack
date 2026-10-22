const thu = require("node:test"),
  kt = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");
thu("FE thật: đăng nhập sai có lỗi, đăng nhập đúng lưu session; logout bỏ quyền", async (t) => {
  const m = await chuanBi(t),
    p = await m.trangMoi();
  await m.mo(p, "authentication.html?next=orders.html");
  await p.locator("#email").fill("khach@example.test");
  await p.locator("#mat-khau").fill("sai");
  await p.locator("#gui-tai-khoan").click();
  await p.locator("#thong-bao-tai-khoan").filter({ hasText: "mật khẩu không đúng" }).waitFor();
  await p.locator("#mat-khau").fill("MoPhong123!");
  await p.locator("#gui-tai-khoan").click();
  await p.waitForURL("**/orders.html");
  await p.locator("#orders-content").waitFor({ state: "visible" });
  await p.locator(".account-actions button").click();
  await p.waitForURL("**/index.html");
  await m.mo(p, "orders.html");
  await p.locator("#orders-content").waitFor({ state: "visible" });
  kt.equal((await p.request.get(new URL("/api/auth/me", m.goc).href)).status(), 401);
  kt.deepEqual(m.loi, []);
});
thu(
  "FE thật: guest xem catalog và đơn của phiên, không vào admin; không open redirect",
  async (t) => {
    const m = await chuanBi(t),
      p = await m.trangMoi();
    await m.mo(p, "index.html");
    await p.locator(".dang-luoi .the-phim").first().waitFor();
    await m.mo(p, "orders.html");
    await p.locator("#orders-content").waitFor({ state: "visible" });
    await m.mo(p, "admin.html");
    await p.locator("#admin-login").waitFor({ state: "visible" });
    await m.dangNhap(p, "khach@example.test", "https://example.com/steal");
    kt.ok(p.url().startsWith(m.goc));
    kt.ok(p.url().endsWith("/orders.html"));
    kt.deepEqual(m.loi, []);
  },
);

thu(
  "FE thật: next sai cú pháp vẫn đăng nhập được và dùng đích mặc định theo vai trò",
  async (t) => {
    const m = await chuanBi(t);
    for (const [email, target] of [
      ["khach@example.test", "orders.html"],
      ["nhanvien@example.test", "admin.html"],
      ["admin@example.test", "admin.html"],
    ]) {
      const p = await m.trangMoi();
      await m.mo(p, "authentication.html?next=" + encodeURIComponent("http://["));
      await p.waitForFunction(
        () => !document.querySelector("#gui-tai-khoan").disabled,
        null,
        { timeout: 5000 },
      );
      kt.equal(await p.locator("[data-tiep-tuc-khach]").getAttribute("href"), "index.html");
      await p.locator("#email").fill(email);
      await p.locator("#mat-khau").fill("MoPhong123!");
      await p.locator("#gui-tai-khoan").click();
      await p.waitForURL(m.goc + target, { timeout: 5000 });
      kt.equal(p.url(), m.goc + target);
    }
    kt.deepEqual(m.loi, []);
  },
);
