const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

test("admin has no public-home or schedule link and homepage redirects to workspace", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  await app.dangNhap(page, "admin@example.test", "index.html");
  await page.locator("#admin-workspace").waitFor();
  assert.match(page.url(), /admin\.html$/);
  assert.equal(await page.locator('.workspace-sidebar a[href="booking.html"]').isVisible(), false);
  assert.equal(await page.locator(".workspace-back").count(), 0);
  await app.mo(page, "index.html");
  await page.waitForURL("**/admin.html");
  assert.deepEqual(app.loi, []);
});

test("staff cannot open homepage but retains schedule viewing and counter sale links", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  await app.dangNhap(page, "nhanvien@example.test", "admin.html");
  await page.locator("#admin-workspace").waitFor();
  assert.equal(await page.locator('.workspace-sidebar a[href="booking.html"]').isVisible(), true);
  assert.equal(await page.locator('a[href="booking.html?mode=counter"]').isVisible(), true);
  assert.equal(await page.locator(".workspace-back").count(), 0);
  await app.mo(page, "index.html");
  await page.waitForURL("**/admin.html");
  await page.locator('a[href="booking.html?mode=counter"]').click();
  await page.locator("#chon-phim").selectOption("1");
  await page.locator("#chon-gio label").first().click();
  await page.locator('[data-ghe-id="11"]').click();
  await page.locator("#tiep-dat-ve").click();
  await page.waitForURL("**/checkout.html?mode=counter");
  await page.locator("#checkout-payment-title").filter({ hasText: "Tạo đơn tại quầy" }).waitFor();
  assert.deepEqual(app.loi, []);
});
test("mobile workspace opens the authorized menu and fits the daily report", async t => {
  const app = await chuanBi(t), page = await app.trangMoi();
  await page.setViewportSize({ width: 375, height: 812 });
  await page.route("**/api/admin/reports/range?*", route => route.fulfill({ json: [{ date: "2026-11-19",
    orders: 27, tickets: 41, snacks: 33, ticketRevenue: 3100000,
    snackRevenue: 1400000, totalRevenue: 4500000,
  }] }));
  await app.dangNhap(page, "admin@example.test", "admin.html");
  const menu = page.locator("#ops-menu");
  await menu.click();
  await page.locator("[data-muc=baocao]").click();
  await page.locator("#baocao-result article").first().waitFor();
  assert.equal(await menu.getAttribute("aria-expanded"), "false");
  assert.match(await page.locator("#baocao-result article").first().innerText(), /Tổng doanh thu/);
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  await page.setViewportSize({ width: 1440, height: 900 });
  assert.equal(await page.locator("#ops-navigation").isVisible(), true);
});
