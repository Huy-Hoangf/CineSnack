const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

async function loginDefault(m, p, email, next) {
  await m.mo(p, "authentication.html" + (next ? "?next=" + encodeURIComponent(next) : ""));
  await p.locator("#email").fill(email);
  await p.locator("#mat-khau").fill("MoPhong123!");
  await p.locator("#gui-tai-khoan").click();
  await p.waitForURL((url) => !url.pathname.endsWith("authentication.html"), { timeout: 5000 });
}
async function csrf(m, p) {
  return (await p.request.get(new URL("/api/csrf", m.goc).href)).json();
}
async function taoDon(m, p, path = "/api/orders", seat = 11) {
  const token = await csrf(m, p);
  const res = await p.request.post(new URL(path, m.goc).href, {
    headers: { [token.headerName]: token.token },
    data: {
      email: "buyer@example.test",
      showTimeId: seat === null ? null : 101,
      seatIds: seat === null ? [] : [seat],
      snacks: [],
      combos: seat === null ? [{ comboId: 21, quantity: 1 }] : [],
    },
  });
  assert.equal(res.status(), 201);
  return res.json();
}

test("Guest đăng nhập trong cùng phiên vẫn mở được đơn guest và chuyển danh sách đúng nguồn", async (t) => {
  const m = await chuanBi(t),
    page = await m.trangMoi();
  const order = await taoDon(m, page, "/api/guest/orders", null);
  await loginDefault(m, page, "khach@example.test", "orders.html?code=" + order.code);
  await page.locator("#ma-don").filter({ hasText: order.code }).waitFor({ timeout: 5000 });
  assert.equal(await page.locator("#orders-scope").inputValue(), "guest");
  assert.equal(await page.locator("#huy-don").isEnabled(), true);
  await page.locator("#orders-scope").selectOption("account");
  assert.equal(await page.locator("#chi-tiet-don #ma-don").count(), 0);
  await page.locator("#orders-scope").selectOption("guest");
  await page
    .locator("#danh-sach-don button")
    .filter({ hasText: order.code })
    .waitFor({ timeout: 5000 });
});

test("4 role: default login, customer next, styled header and privileged navigation", async (t) => {
  const m = await chuanBi(t);
  for (const [email, role, target] of [
    ["khach@example.test", "User", "orders.html"],
    ["nhanvien@example.test", "Staff", "admin.html"],
    ["admin@example.test", "Admin", "admin.html"],
  ]) {
    const p = await m.trangMoi();
    await loginDefault(m, p, email);
    assert.ok(p.url().endsWith(target), `${role} must enter ${target}`);
    if (role !== "User") {
      await p
        .locator("#admin-role")
        .filter({ hasText: role === "Admin" ? "Quản trị viên" : "Nhân viên quầy" })
        .waitFor({ timeout: 5000 });
      assert.equal(await p.locator('[data-muc="don"]').isVisible(), true);
      await p.locator('[data-muc="don"]').click();
      assert.equal(await p.locator('[data-loc-don="DA_THANH_TOAN"]').isVisible(), true);
      assert.equal(await p.locator('[data-muc="phim"]').isVisible(), role === "Admin");
      assert.equal(await p.locator('[data-muc="suat"]').isVisible(), role === "Admin");
      assert.equal(await p.locator('[data-muc="ghe"]').count(), 0);
      const logoutStyle = await p.locator("#admin-logout").evaluate((button) => {
        const style = getComputedStyle(button);
        return { border: parseFloat(style.borderTopWidth), height: button.offsetHeight };
      });
      assert.ok(logoutStyle.border >= 1, "Logout must have a visible button border");
      assert.ok(logoutStyle.height >= 44, "Logout must have an accessible touch target");
      await p.locator("#admin-logout").click();
      await p.waitForURL("**/index.html", { timeout: 5000 });
      assert.equal((await p.request.get(new URL("/api/auth/me", m.goc).href)).status(), 401);
    }
    await m.mo(p, "index.html");
    await p.locator(".account-actions .account-button").first().waitFor({ timeout: 5000 });
    assert.equal(
      await p
        .locator(
          ".account-actions a:not(.account-button), .account-actions button:not(.account-button)",
        )
        .count(),
      0,
    );
  }
  const customer = await m.trangMoi();
  await loginDefault(m, customer, "khach@example.test", "checkout.html");
  assert.ok(customer.url().endsWith("checkout.html"));
  assert.deepEqual(m.loi, []);
});

test("Guest checkout requires email; own-session orders/invoices, cross-session denied; staff queue has both buyers", async (t) => {
  const m = await chuanBi(t),
    guest = await m.trangMoi(),
    other = await m.trangMoi(),
    staff = await m.trangMoi();
  await m.mo(guest, "booking.html?phim=1");
  await guest.locator('[data-ghe-id="11"]').click();
  await guest.locator("#tiep-dat-ve").click();
  await guest.waitForURL("**/checkout.html");
  await guest.locator("#mau-dong-y").check();
  await guest.locator("#thanh-toan-bieu-mau button[type=submit]").click();
  assert.equal(m.orders.size, 0, "Browser required email must prevent guest creation");
  await guest.locator("#khach-email").fill("guest@example.test");
  let sent;
  guest.on("request", (r) => {
    if (r.method() === "POST" && new URL(r.url()).pathname === "/api/guest/orders")
      sent = r.postDataJSON();
  });
  await guest.locator("#thanh-toan-bieu-mau button[type=submit]").click();
  await guest.waitForURL("**/orders.html?code=*", { timeout: 5000 });
  await guest.locator("#ma-don").waitFor({ timeout: 5000 });
  const code = await guest.locator("#ma-don").textContent();
  assert.deepEqual(sent.seatIds, [11]);
  assert.equal(sent.userId, undefined);
  assert.equal(m.orders.get(code).order.userId, null);
  assert.match(await guest.locator(".order-show").textContent(), /Chuyến phiêu lưu CineSnack/);
  assert.match(await guest.locator(".order-show").textContent(), /Phòng 01/);
  assert.match(await guest.locator(".order-show").textContent(), /Ghế A1/);
  assert.equal(await guest.locator("#cash-form").count(), 0);
  await csrf(m, other);
  for (const suffix of ["", "/invoices"]) {
    const res = await other.request.get(new URL(`/api/guest/orders/${code}${suffix}`, m.goc).href);
    assert.equal(res.status(), 403, "Another browser cannot access guest order using known code");
  }
  const otherToken = await csrf(m, other);
  const forbiddenCancel = await other.request.post(
    new URL(`/api/guest/orders/${code}/cancel`, m.goc).href,
    { headers: { [otherToken.headerName]: otherToken.token } },
  );
  assert.equal(forbiddenCancel.status(), 403);
  await m.dangNhap(other, "khach@example.test");
  const userOrder = await taoDon(m, other, "/api/orders", 12);
  await loginDefault(m, staff, "nhanvien@example.test");
  assert.equal((await staff.locator(`[data-don="${code}"]`).count()) > 0, true);
  assert.equal((await staff.locator(`[data-don="${userOrder.code}"]`).count()) > 0, true);
  await staff.locator(`[data-don="${code}"]`).first().click();
  await staff.locator("#tien-khach-dua").fill("100000");
  await staff.locator("#nhan-tien").click();
  await staff.locator("#trang-thai-don").filter({ hasText: "Đã thanh toán" }).waitFor();
  await guest.locator("#tai-lai-don").click();
  await guest.locator("#hoa-don article").waitFor({ timeout: 5000 });
  assert.equal(await guest.locator("#huy-don").isEnabled(), false);
  const second = await taoDon(m, guest, "/api/guest/orders", 13);
  const ownToken = await csrf(m, guest);
  const canceled = await guest.request.post(
    new URL(`/api/guest/orders/${second.code}/cancel`, m.goc).href,
    { headers: { [ownToken.headerName]: ownToken.token } },
  );
  assert.equal(canceled.status(), 200);
  assert.equal((await canceled.json()).status, "HUY");
  assert.deepEqual(m.loi, []);
});

test("Admin movie/show CRUD through forms; staff API cannot mutate, no seat map editor", async (t) => {
  const m = await chuanBi(t),
    admin = await m.trangMoi(),
    staff = await m.trangMoi();
  admin.on("dialog", (d) => d.accept());
  await loginDefault(m, admin, "admin@example.test");
  await admin.locator('[data-muc="phim"]').click();
  await admin.locator("#them-phim").click();
  const form = admin.locator("#phim-form");
  for (const [name, value] of Object.entries({
    name: "Phim mới <script>bad()</script>",
    genre: "Hành động",
    duration: "95",
  }))
    await form.locator(`[name="${name}"]`).fill(value);
  await form.locator('[name="ageRating"]').selectOption("T13");
  await form.locator('[name="status"]').selectOption("DANG_CHIEU");
  await form.locator('button[type="submit"]').click();
  await admin.locator('[data-edit-movie="2"]').waitFor({ timeout: 5000 });
  await admin.locator('[data-edit-movie="2"]').click();
  await form.locator('[name="name"]').fill("Phim đã sửa");
  await form.locator('button[type="submit"]').click();
  await admin.locator("#quan-tri-phim").filter({ hasText: "Phim đã sửa" }).waitFor();
  await admin.locator('[data-muc="suat"]').click();
  await admin.locator("#them-suat").click();
  const show = admin.locator("#suat-form");
  await show.locator('[name="movieId"]').selectOption("2");
  await show.locator('[name="roomId"]').selectOption("1");
  const startsAt = new Date(Date.now() + 86400000).toISOString().slice(0, 16);
  await show.locator('[name="startsAt"]').fill(startsAt);
  await show.locator('[name="price"]').fill("90000");
  await show.locator('[name="status"]').selectOption("SAP_CHIEU");
  await show.locator('button[type="submit"]').click();
  await admin.locator('[data-edit-show="102"]').waitFor({ timeout: 5000 });
  await admin.locator('[data-edit-show="102"]').click();
  await show.locator('[name="price"]').fill("100000");
  await show.locator('button[type="submit"]').click();
  await admin.locator('[data-delete-show="102"]').click();
  await admin.locator("#confirm-delete").click();
  await admin.locator('[data-edit-show="102"]').waitFor({ state: "detached", timeout: 5000 });
  await admin.locator('[data-muc="phim"]').click();
  await admin.locator('[data-delete-movie="2"]').click();
  await admin.locator("#confirm-delete").click();
  await admin.locator('[data-edit-movie="2"]').waitFor({ state: "detached", timeout: 5000 });
  await loginDefault(m, staff, "nhanvien@example.test");
  const token = await csrf(m, staff);
  for (const resource of ["movies", "showtimes"])
    for (const method of ["POST", "PUT", "DELETE"]) {
      const response = await staff.request.fetch(
        new URL(`/api/admin/${resource}${method === "POST" ? "" : "/1"}`, m.goc).href,
        {
          method,
          headers: { [token.headerName]: token.token },
          ...(method === "DELETE" ? {} : { data: {} }),
        },
      );
      assert.equal(response.status(), 403);
    }
  assert.equal(await admin.locator('[data-muc="ghe"]').count(), 0);
  assert.deepEqual(m.loi, []);
});

test("Staff tìm mã đơn Guest ở ngoài trang hiện tại bằng truy vấn toàn rạp", async (t) => {
  const m = await chuanBi(t),
    guest = await m.trangMoi(),
    staff = await m.trangMoi();
  const oldOrder = await taoDon(m, guest, "/api/guest/orders", null);
  await m.dangNhap(staff, "nhanvien@example.test", "admin.html");
  for (let i = 0; i < 21; i++) await taoDon(m, staff, "/api/orders", null);
  await m.mo(staff, "admin.html");
  await staff.locator("#quan-tri-don tr").first().waitFor();
  assert.equal(await staff.locator(`[data-don="${oldOrder.code}"]`).count(), 0);
  await staff.locator('[data-muc="don"]').click();
  await staff.locator("#quan-tri-don-tim").fill(oldOrder.code);
  await staff.locator(`#quan-tri-don [data-don="${oldOrder.code}"]`).waitFor({ timeout: 5000 });
  assert.deepEqual(m.loi, []);
});
