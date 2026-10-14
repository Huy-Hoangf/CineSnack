const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

test("Chưa có suất: giải thích ngay trong phần ghế, không hiện sơ đồ rỗng", async (t) => {
  const m = await chuanBi(t),
    page = await m.trangMoi();
  await page.route("**/api/showtimes?movieId=1", (route) => route.fulfill({ json: [] }));
  await m.mo(page, "booking.html?phim=1");
  await page.waitForFunction(() =>
    document.querySelector("#ghe-trang-thai")?.textContent.includes("chưa có suất"),
  );
  assert.equal(await page.locator("#ghe-so-do-khung").isVisible(), false);
  assert.equal(await page.locator(".nut-ghe").count(), 0);
  await page.locator("#chon-phim").selectOption("");
  assert.match(await page.locator("#ghe-trang-thai").textContent(), /Chỉ mua bắp nước/);
  assert.deepEqual(m.loi, []);
});

test("Suất chưa có ghế và lỗi API ghế có thông báo riêng", async (t) => {
  const m = await chuanBi(t),
    page = await m.trangMoi();
  await page.route("**/api/showtimes/101/seats", (route) => route.fulfill({ json: [] }));
  await m.mo(page, "booking.html?phim=1");
  await page.waitForFunction(() =>
    document.querySelector("#ghe-trang-thai")?.textContent.includes("chưa được cấu hình ghế"),
  );
  assert.equal(await page.locator("#ghe-so-do-khung").isVisible(), false);
  await page.unroute("**/api/showtimes/101/seats");
  await page.route("**/api/showtimes/101/seats", (route) =>
    route.fulfill({
      status: 503,
      json: { message: "Không thể tải ghế lúc này" },
    }),
  );
  await page.reload();
  await page.waitForFunction(() =>
    document.querySelector("#ghe-trang-thai")?.textContent.includes("Không tải được ghế"),
  );
  assert.equal(await page.locator("#ghe-so-do-khung").isVisible(), false);
  assert.deepEqual(m.loi, []);
});

test("Có ghế: hiện sơ đồ và chọn ghế đúng ID; mua chỉ bắp nước xóa lựa chọn ghế", async (t) => {
  const m = await chuanBi(t),
    page = await m.trangMoi();
  await m.mo(page, "booking.html?phim=1");
  await page.locator('[data-ghe-id="11"]').waitFor();
  assert.equal(await page.locator("#ghe-so-do-khung").isVisible(), true);
  await page.locator('[data-ghe-id="11"]').click();
  assert.equal(await page.locator('[data-ghe-id="11"]').getAttribute("aria-pressed"), "true");
  await page.locator("#chon-phim").selectOption("");
  assert.equal(await page.locator(".nut-ghe").count(), 0);
  assert.equal(await page.locator("#ghe-so-do-khung").isVisible(), false);
  assert.deepEqual(m.loi, []);
});
