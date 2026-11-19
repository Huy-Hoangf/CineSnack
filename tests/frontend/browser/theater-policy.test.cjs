const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const { chuanBi } = require("./helpers.cjs");
const initialPrices = { weekdayRegular: 70000, weekdayVip: 90000, weekendRegular: 100000, weekendVip: 130000 };
async function routeSettings(page) {
  let prices = { ...initialPrices }, settings = { lateCheckInMinutes: null };
  const requests = [];
  await page.route("**/api/admin/rooms/1/pricing", async route => {
    if (route.request().method() === "PUT") { prices = route.request().postDataJSON(); requests.push(["prices", prices]); }
    await route.fulfill({ json: prices });
  });
  await page.route("**/api/admin/theater-policy", async route => {
    if (route.request().method() === "PUT") { settings = route.request().postDataJSON(); requests.push(["settings", settings]); }
    await route.fulfill({ json: settings });
  });
  await page.route("**/api/admin/rooms/management?*", route => route.fulfill({ json: [{ id: 1, name: "Phòng 01", type: "2D", status: "HOAT_DONG", seatCount: 0 }] }));
  await page.route("**/api/admin/rooms/1/seats", route => route.fulfill({ json: [] }));
  return requests;
}
test("admin saves room four prices and nullable global late cutoff on desktop and mobile", async t => {
  const app = await chuanBi(t), page = await app.trangMoi(), requests = await routeSettings(page);
  await app.dangNhap(page, "admin@example.test", "theater.html");
  await page.getByRole("button", { name: "Bảng giá", exact: true }).click();
  const form = page.locator("#room-pricing-form");
  await form.locator("[name=weekdayRegular]").fill("75000");
  await form.getByRole("button", { name: "Lưu bảng giá" }).click();
  await page.locator("#room-pricing-status").filter({ hasText: "Đã lưu" }).waitFor();
  assert.deepEqual(requests[0], ["prices", { ...initialPrices, weekdayRegular: 75000 }]);
  const policy = page.locator("#theater-policy-form");
  await policy.locator("[name=lateCheckInMinutes]").fill("15");
  await policy.getByRole("button", { name: "Lưu chính sách soát vé" }).click();
  await page.locator("#theater-policy-status").filter({ hasText: "Đã lưu" }).waitFor();
  assert.deepEqual(requests[1], ["settings", { lateCheckInMinutes: 15 }]);
  fs.mkdirSync(".review/ui", { recursive: true });
  await page.screenshot({ path: ".review/ui/policies-desktop.png", fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await policy.locator("[name=lateCheckInMinutes]").fill("");
  await policy.getByRole("button", { name: "Lưu chính sách soát vé" }).click();
  await page.waitForFunction(() => document.querySelector("#theater-policy-status").textContent.includes("Đã lưu"));
  assert.deepEqual(requests.at(-1), ["settings", { lateCheckInMinutes: null }]);
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  await page.screenshot({ path: ".review/ui/policies-mobile.png", fullPage: true });
  assert.deepEqual(app.loi, []);
});
test("POLICY show sends no manual price and restores persisted mode when edited", async t => {
  const app = await chuanBi(t), page = await app.trangMoi();
  app.catalog.showTime.pricingMode = "POLICY";
  let saved;
  await page.route("**/api/admin/showtimes/101", async route => { saved = route.request().postDataJSON(); await route.fulfill({ json: { ...app.catalog.showTime, ...saved } }); });
  await app.dangNhap(page, "admin@example.test", "admin.html");
  await page.locator("[data-muc=suat]").click();
  await page.getByRole("button", { name: "Danh sách", exact: true }).click();
  await page.getByRole("button", { name: "Sửa", exact: true }).first().click();
  await page.locator("#suat-hop[open]").waitFor();
  const form = page.locator("#suat-form");
  assert.equal(await form.locator("[name=pricingMode]").inputValue(), "POLICY");
  assert.equal(await form.locator("[name=price]").isDisabled(), true);
  await form.getByRole("button", { name: "Lưu suất chiếu" }).click();
  await page.locator("#suat-hop").waitFor({ state: "hidden" });
  assert.equal(saved.pricingMode, "POLICY");
  assert.equal(saved.price, undefined);
  assert.equal(saved.vipSurcharge, undefined);
  assert.deepEqual(app.loi, []);
});
test("staff cannot see or request admin policy forms", async t => {
  const app = await chuanBi(t), page = await app.trangMoi();
  const requests = [];
  page.on("request", r => { if (r.url().includes("theater-policy") || r.url().endsWith("/pricing")) requests.push(r.url()); });
  await app.dangNhap(page, "nhanvien@example.test", "theater.html");
  assert.equal(await page.locator("#theater-policy-panel").isVisible(), false);
  assert.equal(await page.locator("#room-pricing-panel").isVisible(), false);
  assert.deepEqual(requests, []);
  assert.deepEqual(app.loi, []);
});

test("late room save and load cannot change the newly selected room form", async t => {
  const app = await chuanBi(t), page = await app.trangMoi();
  await routeSettings(page);
  await page.route("**/api/admin/rooms/management?*", route => route.fulfill({ json: [1, 2].map(id => ({ id, name: `Phòng 0${id}`, type: "2D", status: "HOAT_DONG", seatCount: 0 })) }));
  let finishSave, finishLoad, saveStarted, loadStarted;
  let delayRoomOneLoad = false, delayRoomTwoLoad = true, finishOldLoad, oldLoadStarted;
  const oldLoadReady = new Promise(resolve => { oldLoadStarted = resolve; });
  const saveReady = new Promise(resolve => { saveStarted = resolve; });
  const loadReady = new Promise(resolve => { loadStarted = resolve; });
  await page.route("**/api/admin/rooms/1/pricing", async route => {
    if (route.request().method() === "PUT") {
      saveStarted();
      await new Promise(resolve => { finishSave = resolve; });
    } else if (delayRoomOneLoad) {
      oldLoadStarted();
      await new Promise(resolve => { finishOldLoad = resolve; });
    }
    await route.fulfill({ json: initialPrices });
  });
  await page.route("**/api/admin/rooms/2/pricing", async route => {
    if (delayRoomTwoLoad) {
      loadStarted();
      await new Promise(resolve => { finishLoad = resolve; });
    }
    await route.fulfill({ json: { ...initialPrices, weekdayRegular: 80000 } });
  });
  await app.dangNhap(page, "admin@example.test", "theater.html");
  const prices = page.locator("#room-pricing-form"), save = prices.getByRole("button", { name: "Lưu bảng giá" });
  await page.getByRole("button", { name: "Bảng giá", exact: true }).nth(0).click();
  await prices.locator("[name=weekdayRegular]").waitFor();
  await page.waitForFunction(() => document.querySelector("[name=weekdayRegular]").value === "70000");
  await save.click();
  await saveReady;
  await page.getByRole("button", { name: "Bảng giá", exact: true }).nth(1).click();
  await loadReady;
  finishSave();
  await page.waitForTimeout(100);
  assert.equal(await save.isDisabled(), true);
  assert.match(await page.locator("#room-pricing-status").textContent(), /Đang tải/);
  finishLoad();
  await page.waitForFunction(() => document.querySelector("[name=weekdayRegular]").value === "80000");
  assert.equal(await save.isDisabled(), false);
  assert.equal(await page.locator("#room-pricing-status").textContent(), "");
  delayRoomOneLoad = true;
  delayRoomTwoLoad = false;
  await page.getByRole("button", { name: "Bảng giá", exact: true }).nth(0).click();
  await oldLoadReady;
  await page.getByRole("button", { name: "Bảng giá", exact: true }).nth(1).click();
  await page.waitForFunction(() => document.querySelector("[name=weekdayRegular]").value === "80000");
  finishOldLoad();
  await page.waitForTimeout(100);
  assert.equal(await prices.locator("[name=weekdayRegular]").inputValue(), "80000");
  assert.equal(await page.locator("#room-pricing-title").textContent(), "Bảng giá · Phòng 02");
  assert.deepEqual(app.loi, []);
});
