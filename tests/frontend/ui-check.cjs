const assert = require("node:assert/strict");
const { chromium } = require("playwright");
const base =
  process.env.BASE_URL || "http://127.0.0.1:5500/src/main/resources/static/";
(async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(process.env.BROWSER_EXECUTABLE
      ? { executablePath: process.env.BROWSER_EXECUTABLE }
      : {}),
  });
  try {
    const page = await browser.newPage();
    const errors = [];
    const failedAssets = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("response", (response) => {
      if (
        response.status() >= 400 &&
        ["stylesheet", "script", "image"].includes(
          response.request().resourceType(),
        )
      ) {
        failedAssets.push(`${response.status()} ${response.url()}`);
      }
    });
    const go = (name) => page.goto(new URL(name, base).href);
    await go("phim.html?movie=interstellar");
    await page.keyboard.press("Escape");
    assert.equal(
      await page.locator('[data-status="soon"]').getAttribute("aria-pressed"),
      "true",
    );
    assert.equal(await page.locator(".catalog-grid .movie-card").count(), 2);
    await go("phim.html");
    assert.equal(await page.locator(".catalog-grid .movie-card").count(), 4);
    await page.locator("#movie-search").fill("HANH DONG");
    assert.equal(await page.locator(".catalog-grid .movie-card").count(), 2);
    await page.locator("#movie-search").fill("khongcophimnay");
    assert(await page.locator("#no-movies").isVisible());
    await page.locator("#clear-search").click();
    const detail = page.locator('[data-detail="inside-out"]').first();
    await detail.focus();
    await page.keyboard.press("Enter");
    assert(await page.locator("#movie-dialog").isVisible());
    await page.keyboard.press("Escape");
    assert.equal(
      await page.evaluate(() => document.activeElement.dataset.detail),
      "inside-out",
    );
    await page.locator('[data-status="soon"]').click();
    assert.equal(await page.locator(".catalog-grid .movie-card").count(), 2);
    assert.equal(await page.locator(".catalog-grid .movie-button").count(), 0);
    await go("dat-ve.html?movie=inside-out");
    assert.equal(
      await page.locator("#booking-movie").inputValue(),
      "inside-out",
    );
    assert(await page.locator("#continue-booking").isDisabled());
    assert.equal(await page.locator(".seat:disabled").count(), 12);
    await page.locator('[data-seat="A1"]').click();
    await page.locator('[data-seat="D1"]').click();
    await page.locator('[data-combo="solo"][data-change="1"]').click();
    const expected = await page.evaluate(() => CineSnack.money(229000));
    assert.equal(await page.locator("#summary-total").textContent(), expected);
    await page
      .locator("#time-options label")
      .filter({ hasText: "16:30" })
      .click();
    assert(await page.locator("#continue-booking").isDisabled());
    assert.equal(await page.locator('.seat[aria-pressed="true"]').count(), 0);
    const seats = page.locator(".seat:not(:disabled)");
    for (let i = 0; i < 9; i++) await seats.nth(i).click();
    assert.equal(await page.locator('.seat[aria-pressed="true"]').count(), 8);
    assert((await page.locator("#seat-error").textContent()).includes("8"));
    await page.locator("#booking-movie").selectOption("dune");
    await page.locator('[data-seat="A1"]').click();
    await page.locator('[data-seat="D1"]').click();
    const total = await page.locator("#summary-total").textContent();
    await page.locator("#continue-booking").click();
    await page.waitForURL("**/thanh-toan.html");
    assert.equal(await page.locator("#summary-total").textContent(), total);
    await page.locator("#edit-booking").click();
    assert.equal(await page.locator('.seat[aria-pressed="true"]').count(), 2);
    assert.equal(await page.locator("#qty-solo").textContent(), "1");
    await page.locator("#continue-booking").click();
    await page.waitForURL("**/thanh-toan.html");
    await page.locator('#checkout-form button[type="submit"]').click();
    assert(await page.locator("#checkout-content").isVisible());
    assert(
      await page.locator("#customer-name").evaluate((i) => !i.validity.valid),
    );
    await page.locator("#customer-name").fill("Khách Demo");
    await page.locator("#customer-email").fill("not-an-email");
    await page.locator("#customer-phone").fill("1234");
    await page.locator('#checkout-form button[type="submit"]').click();
    assert(
      await page.locator("#customer-email").evaluate((i) => !i.validity.valid),
    );
    await page.locator("#customer-email").fill("demo@example.com");
    await page.locator('#checkout-form button[type="submit"]').click();
    assert(
      await page.locator("#customer-phone").evaluate((i) => !i.validity.valid),
    );
    await page.locator("#customer-phone").fill("0901234567");
    await page.locator('#checkout-form button[type="submit"]').click();
    assert(
      await page.locator("#demo-consent").evaluate((i) => !i.validity.valid),
    );
    await page.locator("#demo-consent").check();
    await page.locator('#checkout-form button[type="submit"]').click();
    assert(await page.locator("#checkout-success").isVisible());
    assert.equal(
      await page.evaluate(() => sessionStorage.getItem(CineSnack.storageKey)),
      null,
    );
    await page.reload();
    assert(await page.locator("#empty-checkout").isVisible());
    await page.evaluate(() =>
      sessionStorage.setItem(CineSnack.storageKey, '{"seats":["fake"]}'),
    );
    await page.reload();
    assert(await page.locator("#empty-checkout").isVisible());
    await go("admin.html");
    await page.locator('.admin-nav-button[data-view="movies"]').click();
    await page.locator("#admin-movie-search").fill("hanh tinh");
    assert.equal(await page.locator("#admin-movies tr").count(), 1);
    await page.locator('.admin-nav-button[data-view="shows"]').click();
    assert.equal(await page.locator("#admin-shows tr").count(), 5);
    await page.locator('.admin-nav-button[data-view="orders"]').click();
    await page.locator("#order-status").selectOption("pending");
    assert.equal(await page.locator("#admin-orders [data-order]").count(), 1);
    await page.locator("#admin-orders [data-order]").click();
    assert(await page.locator("#order-dialog").isVisible());
    await page.keyboard.press("Escape");
    await page.locator("#admin-order-search").fill("does-not-exist");
    assert(await page.locator("#admin-orders .table-empty").isVisible());
    for (const width of [1440, 1024, 768, 375, 320]) {
      await page.setViewportSize({ width, height: 900 });
      for (const file of [
        "index.html",
        "phim.html",
        "dat-ve.html",
        "thanh-toan.html",
        "admin.html",
      ]) {
        await go(file);
        if (file === "thanh-toan.html") {
          await page.evaluate(() =>
            sessionStorage.setItem(
              CineSnack.storageKey,
              JSON.stringify({
                movie: "dune",
                date: CineSnack.dates()[0],
                time: "19:30",
                seats: ["A1", "D1"],
                combos: { solo: 1, duo: 0 },
              }),
            ),
          );
          await page.reload();
        }
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth,
          ),
          false,
          `${file}: overflow at ${width}`,
        );
        if (process.env.SCREENSHOT_DIR && [1440, 375].includes(width)) {
          await page.evaluate(() =>
            document
              .querySelectorAll("img")
              .forEach((i) => (i.loading = "eager")),
          );
          await page.waitForFunction(() =>
            [...document.images].every((i) => i.complete && i.naturalWidth),
          );
          await page.screenshot({
            path: require("node:path").join(
              process.env.SCREENSHOT_DIR,
              `${file}-${width}.png`,
            ),
            fullPage: true,
          });
        }
        for (const href of await page
          .locator("a")
          .evaluateAll((a) => [
            ...new Set(a.map((i) => i.getAttribute("href"))),
          ])) {
          if (!href || href.startsWith("#")) continue;
          const response = await page.request.get(
            new URL(href, page.url()).href,
          );
          assert(response.ok(), `Broken link: ${href}`);
        }
      }
    }
    await page.emulateMedia({ reducedMotion: "reduce" });
    assert.equal(
      await page.evaluate(
        () => getComputedStyle(document.documentElement).scrollBehavior,
      ),
      "auto",
    );
    const blocked = await browser.newPage();
    await blocked.addInitScript(() => {
      Storage.prototype.setItem = function () {
        throw new DOMException("Storage blocked", "SecurityError");
      };
    });
    await blocked.goto(new URL("dat-ve.html", base).href);
    await blocked.locator('[data-seat="A1"]').click();
    await blocked.locator("#continue-booking").click();
    assert(
      (await blocked.locator("#booking-error").textContent()).includes(
        "lưu lựa chọn",
      ),
    );
    assert(blocked.url().endsWith("dat-ve.html"));
    await blocked.close();
    assert.deepEqual(errors, []);
    assert.deepEqual(
      failedAssets,
      [],
      "All CSS, JavaScript and image URLs must load",
    );
    console.log(
      "PASS: catalog, dialog/keyboard, booking limits/prices/reset, checkout validation/state, admin filters, links, 5 responsive widths.",
    );
  } finally {
    await browser.close();
  }
})();
