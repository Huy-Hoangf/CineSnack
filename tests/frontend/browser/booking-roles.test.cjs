const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

test("admin and staff browse schedules and available seats without starting online orders", async (t) => {
  const app = await chuanBi(t);
  for (const email of ["admin@example.test", "nhanvien@example.test"]) {
    const page = await app.trangMoi();
    await app.dangNhap(page, email, "booking.html?phim=1");
    await page.locator("#booking-view-note").waitFor({ timeout: 5000 });
    assert.match(page.url(), /booking\.html\?phim=1$/);
    await page.locator('#chon-gio label').first().click();
    const seat = page.locator('[data-ghe-id="11"]');
    await seat.waitFor();
    assert.equal(await seat.getAttribute("aria-disabled"), "true");
    assert.match(await seat.getAttribute("aria-label"), /còn trống/);
    await seat.evaluate(el => el.click());
    assert.equal(await seat.getAttribute("aria-pressed"), "false");
    assert.equal(await page.locator("#tiep-dat-ve").isVisible(), false);
    assert.equal(await page.locator("#combo").isVisible(), false);
    assert.equal(await page.locator("#counter-booking-link").isVisible(), email.startsWith("nhanvien"));
    if (email.startsWith("nhanvien")) {
      await page.locator("#counter-booking-link").click();
      // Select a show on the dedicated counter page before selecting a seat.
      await page.locator('#chon-gio label').first().click();
      await page.locator('[data-ghe-id="11"]').click();
      await page.locator("#tiep-dat-ve").click();
      await page.waitForURL("**/checkout.html?mode=counter");
      await page.locator("#checkout-payment-title").filter({ hasText: "Tạo đơn tại quầy" }).waitFor();
      assert.equal(await page.locator("#khach-email").getAttribute("required"), null);
    } else {
      await app.mo(page, "booking.html?mode=counter&phim=1");
      await page.locator("#booking-view-note").waitFor();
      assert.equal(await page.locator("#counter-booking-link").isVisible(), false);
    }
  }
  assert.deepEqual(app.loi, []);
});

test("customers and guests can select seats in the online booking flow", async (t) => {
  const app = await chuanBi(t);
  for (const email of [null, "khach@example.test"]) {
    const page = await app.trangMoi();
    if (email) await app.dangNhap(page, email, "booking.html?phim=1");
    else await app.mo(page, "booking.html?phim=1");
    await page.locator("#chon-gio label").first().click();
    await page.locator('[data-ghe-id="11"]').click();
    assert.equal(await page.locator('[data-ghe-id="11"]').getAttribute("aria-pressed"), "true");
    await page.locator("#tiep-dat-ve").click();
    await page.waitForURL("**/checkout.html");
    await page.locator("#thanh-toan-bieu-mau button[type=submit]:enabled").waitFor();
  }
  assert.deepEqual(app.loi, []);
});
