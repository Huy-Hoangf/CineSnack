const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

test("Admin lưu được giá bắp nước nguyên đồng, không bắt buộc bội số 1000", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  const snack = {
    id: 1,
    kind: "COMBO",
    name: "Combo thử",
    price: 59000,
    status: "HOAT_DONG",
  };
  await page.route("**/api/admin/snacks", (route) =>
    route.fulfill({ json: [snack] }),
  );
  let saved;
  await page.route("**/api/admin/snacks/combos/1", (route) => {
    saved = route.request().postDataJSON();
    return route.fulfill({ json: { ...snack, ...saved } });
  });
  await app.dangNhap(page, "admin@example.test", "admin.html");
  await page.locator('[data-muc="bap"]').click();
  await page.locator("#bap-list summary").click();
  const price = page.getByRole("spinbutton", { name: "Giá Combo thử" });
  await price.fill("59500");
  assert.equal(await price.evaluate((input) => input.checkValidity()), true);
  await page.locator("#bap-list button[type=submit]").click();
  await page
    .locator("#admin-status")
    .filter({ hasText: "Đã cập nhật" })
    .waitFor();
  assert.deepEqual(saved, { price: 59500, status: "HOAT_DONG" });
  assert.deepEqual(app.loi, []);
});

test("Admin thêm, sửa và xóa món lẻ cùng combo", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  const snacks = [];
  let nextId = 1;
  await page.route("**/api/admin/snacks", (route) =>
    route.fulfill({ json: snacks }),
  );
  await page.route("**/api/admin/snacks/**", (route) => {
    const request = route.request();
    const parts = new URL(request.url()).pathname.split("/");
    const kind = parts[4] === "products" ? "PRODUCT" : "COMBO";
    if (request.method() === "POST") {
      const item = { id: nextId++, kind, ...request.postDataJSON() };
      snacks.push(item);
      return route.fulfill({ status: 201, json: item });
    }
    const id = Number(parts[5]);
    const index = snacks.findIndex(
      (item) => item.id === id && item.kind === kind,
    );
    if (request.method() === "PUT") {
      Object.assign(snacks[index], request.postDataJSON());
      return route.fulfill({ json: snacks[index] });
    }
    snacks.splice(index, 1);
    return route.fulfill({ status: 204, body: "" });
  });
  await app.dangNhap(page, "admin@example.test", "admin.html");
  await page.locator('[data-muc="bap"]').click();

  await page.locator("#muc-bap .ops-add summary").click();
  await page.locator('#snack-create [name="kind"]').selectOption("products");
  await page.locator('#snack-create [name="name"]').fill("Bắp lớn");
  await page.locator('#snack-create [name="productType"]').selectOption("BAP");
  await page.locator('#snack-create [name="price"]').fill("35000");
  await page.locator('#snack-create button[type="submit"]').click();
  await page.locator("#bap-list summary").filter({ hasText: "Bắp lớn" }).click();
  await page.getByRole("textbox", { name: "Tên Bắp lớn" }).waitFor();
  assert.equal(snacks[0].productType, "BAP");

  await page.getByRole("textbox", { name: "Tên Bắp lớn" }).fill("Bắp bơ lớn");
  await page.locator('#bap-list form button[type="submit"]').click();
  await page.locator("#bap-list summary").filter({ hasText: "Bắp bơ lớn" }).click();
  await page.getByRole("textbox", { name: "Tên Bắp bơ lớn" }).waitFor();
  assert.equal(snacks[0].name, "Bắp bơ lớn");

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Xóa Bắp bơ lớn" }).click();
  await page
    .getByRole("textbox", { name: "Tên Bắp bơ lớn" })
    .waitFor({ state: "detached" });
  assert.equal(snacks.length, 0);

  await page.locator('#snack-create [name="kind"]').selectOption("combos");
  await page.locator('#snack-create [name="name"]').fill("Combo đôi");
  await page.locator('#snack-create [name="price"]').fill("99000");
  await page.locator('#snack-create button[type="submit"]').click();
  await page.locator("#bap-list summary").filter({ hasText: "Combo đôi" }).click();
  await page.getByRole("textbox", { name: "Tên Combo đôi" }).waitFor();
  assert.equal(snacks[0].kind, "COMBO");
  assert.equal(snacks[0].price, 99000);
  assert.deepEqual(app.loi, []);
});

test("Đơn đã thanh toán hiển thị đúng vé, trạng thái món và hóa đơn", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  const details = {
    order: {
      id: 1,
      userId: null,
      code: "DHREVIEW",
      status: "DA_THANH_TOAN",
      total: 140000,
      email: "buyer@example.test",
      createdAt: "2026-11-05T10:00:00+07:00",
      expiresAt: null,
    },
    tickets: [
      {
        seatCode: "A1",
        code: "VE1",
        unitPrice: 80000,
        movieName: "Phim thử",
        roomName: "Phòng 1",
        startsAt: "2026-12-01T19:00:00+07:00",
      },
    ],
    snacks: [
      { name: "Bắp", quantity: 1, unitPrice: 30000, status: "CHO_NHAN" },
      { name: "Nước", quantity: 1, unitPrice: 30000, status: "DA_NHAN" },
    ],
  };
  await page.route("**/api/guest/orders?*", (route) =>
    route.fulfill({ json: [details.order] }),
  );
  await page.route("**/api/guest/orders/DHREVIEW", (route) =>
    route.fulfill({ json: details }),
  );
  await page.route("**/api/guest/orders/DHREVIEW/invoices", (route) =>
    route.fulfill({
      json: [
        {
          code: "HD1",
          type: "VE",
          total: 80000,
          issuedAt: details.tickets[0].startsAt,
          lines: [{ quantity: 1, description: "Vé A1", unitPrice: 80000 }],
        },
      ],
    }),
  );
  await app.mo(page, "orders.html?guest=1&code=DHREVIEW");
  await page.locator("#hoa-don").filter({ hasText: "HD1" }).waitFor();
  await page.locator(".ticket-codes summary").click();
  const text = await page.locator("#chi-tiet-don").innerText();
  assert.match(text, /Chờ nhận/);
  assert.match(text, /Đã giao/);
  assert.match(text, /VE1/);
  assert.equal(
    await page.getByRole("button", { name: "Xác nhận đã giao" }).count(),
    0,
  );
  assert.deepEqual(app.loi, []);
});

test("Nhân viên giao món một lần và thấy trạng thái đã giao sau khi tải lại", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  const details = {
    order: { code: "DHGIAOMON", status: "DA_THANH_TOAN", total: 30000 },
    tickets: [],
    snacks: [
      { id: 7, name: "Bắp", quantity: 1, unitPrice: 30000, status: "CHO_NHAN" },
    ],
  };
  await page.route("**/api/orders/DHGIAOMON", (route) =>
    route.fulfill({ json: details }),
  );
  await page.route("**/api/orders/DHGIAOMON/invoices", (route) =>
    route.fulfill({ json: [] }),
  );
  let deliveries = 0;
  await page.route(
    "**/api/admin/orders/DHGIAOMON/snacks/7/deliver",
    (route) => {
      assert.equal(route.request().method(), "POST");
      deliveries++;
      details.snacks[0].status = "DA_NHAN";
      return route.fulfill({ json: details });
    },
  );
  await app.dangNhap(
    page,
    "nhanvien@example.test",
    "admin.html?code=DHGIAOMON",
  );
  await page.getByRole("button", { name: "Xác nhận đã giao" }).click();
  await page
    .getByRole("button", { name: "Xác nhận đã giao" })
    .waitFor({ state: "detached" });
  await page.locator("#don-chi-tiet").filter({ hasText: "Đã giao" }).waitFor();
  assert.equal(deliveries, 1);
  assert.equal(
    await page.getByRole("button", { name: "Xác nhận đã giao" }).count(),
    0,
  );
  assert.deepEqual(app.loi, []);
});
