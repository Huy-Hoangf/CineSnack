const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

test("Refresh cũ cùng đơn không ghi đè biên nhận thu tiền", async (t) => {
  const m = await chuanBi(t),
    khach = await m.trangMoi(),
    staff = await m.trangMoi();
  await m.dangNhap(khach, "khach@example.test", "orders.html");
  const csrf = await (await khach.request.get(new URL("/api/csrf", m.goc).href)).json();
  const created = await khach.request.post(new URL("/api/orders", m.goc).href, {
    headers: { [csrf.headerName]: csrf.token },
    data: {
      email: "khach@example.test",
      showTimeId: 101,
      seatIds: [11],
      snacks: [],
      combos: [],
    },
  });
  const order = await created.json();
  await m.dangNhap(staff, "nhanvien@example.test", "admin.html");
  await staff.locator(`[data-don="${order.code}"]`).first().click();
  await staff.locator("#cash-form").waitFor();
  let release,
    captured,
    delivered,
    once = false;
  const capture = new Promise((r) => {
    captured = r;
  });
  const done = new Promise((r) => {
    delivered = r;
  });
  await staff.route("**/api/orders/" + order.code, async (route) => {
    if (once) return route.continue();
    once = true;
    const response = await route.fetch();
    captured();
    await new Promise((r) => {
      release = r;
    });
    await route.fulfill({ response });
    delivered();
  });
  await staff.locator("#tai-lai-don").click();
  await capture;
  await staff.locator("#tien-khach-dua").fill("100000");
  await staff.locator("#nhan-tien").click();
  await staff.locator("#tien-thua").waitFor();
  release();
  await done;
  await staff.waitForTimeout(150);
  assert.match(await staff.locator("#trang-thai-don").textContent(), /Đã thanh toán/);
  assert.equal(await staff.locator("#nhan-tien").isVisible(), false);
  assert.match(await staff.locator("#tien-thua").textContent(), /20.000/);
  assert.equal(m.orders.get(order.code).payments.length, 1);
  assert.deepEqual(m.loi, []);
});

test("Phim sắp chiếu từ trang chủ mở đúng ID và tab", async (t) => {
  const m = await chuanBi(t),
    p = await m.trangMoi();
  m.catalog.movie.status = "SAP_CHIEU";
  await p.route("**/api/movies/1", (r) => r.fulfill({ json: m.catalog.movie }));
  await m.mo(p, "index.html");
  await p.locator(".chieu-nhan").click();
  await p.locator(".chieu-luoi a.anh").click();
  await p.locator("#hop-phim[open]").waitFor({ timeout: 5000 });
  assert.equal(await p.locator("#chi-tiet-tieu-de").textContent(), m.catalog.movie.name);
  assert.equal(
    await p.locator('[data-trang-thai="sap-chieu"]').getAttribute("aria-pressed"),
    "true",
  );
  assert.deepEqual(m.loi, []);
});

test("Thu tiền thành công giữ trạng thái/tiền thừa khi tải hóa đơn lỗi", async (t) => {
  const m = await chuanBi(t),
    khach = await m.trangMoi(),
    staff = await m.trangMoi();
  await m.dangNhap(khach, "khach@example.test", "orders.html");
  const csrf = await (await khach.request.get(new URL("/api/csrf", m.goc).href)).json();
  const created = await khach.request.post(new URL("/api/orders", m.goc).href, {
    headers: { [csrf.headerName]: csrf.token },
    data: {
      email: "khach@example.test",
      showTimeId: 101,
      seatIds: [11],
      snacks: [],
      combos: [],
    },
  });
  assert.equal(created.status(), 201);
  const order = await created.json();
  await m.dangNhap(staff, "nhanvien@example.test", "admin.html");
  await staff.locator(`[data-don="${order.code}"]`).first().click();
  await staff.route("**/api/orders/*/invoices", (r) =>
    r.fulfill({ status: 503, json: { message: "Hóa đơn tạm không tải được" } }),
  );
  await staff.locator("#tien-khach-dua").fill("100000");
  await staff.locator("#nhan-tien").click();
  await staff
    .locator("#trang-thai-don")
    .filter({ hasText: "Đã thanh toán" })
    .waitFor({ timeout: 5000 });
  await staff.locator("#tien-thua").filter({ hasText: "20.000" }).waitFor();
  assert.equal(await staff.locator("#nhan-tien").isVisible(), false);
  assert.equal(m.orders.get(order.code).order.status, "DA_THANH_TOAN");
  assert.match(await staff.locator("#hoa-don").textContent(), /Chưa tải được hóa đơn/);
  assert.deepEqual(m.loi, []);
});
