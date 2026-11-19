const test = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const { chuanBi } = require("./helpers.cjs");

test("separate ticket pages and snack invoice contain decodable CODE128 barcodes", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  await page.setContent('<html lang="vi"><body></body></html>');
  for (const file of ["vendor/JsBarcode.code128-3.11.6.min.js", "vendor/zxing-browser-0.2.1.min.js"])
    await page.addScriptTag({ path: path.resolve("src/main/resources/UI/assets", file) });
  await page.evaluate(() => {
    window.CineSnack = { dinhDangSuat: x => x, dinhDangTien: x => x + "đ" };
    window.tickets = ["VE12345678901234567890123456789", "VE22345678901234567890123456789"].map((code, i) => ({
      code, movieName: "Dune: Hành tinh cát", roomName: "Phòng 08", seatCode: "D" + (i + 4),
      startsAt: "20:00 · 15/11/2026", status: "DA_THANH_TOAN", unitPrice: 75000,
    }));
    window.CineSnackApi = { goi: async (url, options) => {
      window.claim = { url, codes: options.body.codes };
      return tickets.map(ticket => ({ ticket }));
    }};
  });
  await page.addScriptTag({ path: path.resolve("src/main/resources/UI/assets/js/ticket-print.js") });
  await page.evaluate(async () => {
    const b = CineSnackPrint.ticketsButton(tickets, async (_, action) => {
      try { await action(); } catch (e) { window.printError = e.message; }
    });
    document.body.append(b); b.click();
  });
  await page.waitForFunction(() => document.querySelector("#cinema-print-frame")?.contentDocument.querySelectorAll("article").length === 2);
  assert.equal(await page.evaluate(() => window.printError), undefined);
  assert.deepEqual(await page.evaluate(() => claim.codes), await page.evaluate(() => tickets.map(t => t.code)));
  async function decode() {
    return page.evaluate(async () => {
      const doc = document.querySelector("#cinema-print-frame").contentDocument;
      const values = [];
      for (const svg of doc.querySelectorAll("svg")) {
        const img = new Image();
        const url = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(svg)], { type: "image/svg+xml" }));
        try {
          img.src = url; await img.decode();
          values.push((await new ZXingBrowser.BrowserMultiFormatReader().decodeFromImageElement(img)).getText());
        } finally { URL.revokeObjectURL(url); }
      }
      return values;
    });
  }
  assert.deepEqual(await decode(), await page.evaluate(() => tickets.map(t => t.code)));
  const html = await page.evaluate(() => document.querySelector("#cinema-print-frame").contentDocument.documentElement.outerHTML);
  const preview = await app.trangMoi();
  await preview.setContent(html);
  const pdf = await preview.pdf({ preferCSSPageSize: true });
  assert.equal((pdf.toString("latin1").match(/\/Type\s*\/Page\b/g) || []).length, 2);
  await page.evaluate(() => {
    const b = CineSnackPrint.invoiceButton({ type: "BONG_NUOC", code: "HDB1234567890123456789012345678", issuedAt: "15/11/2026", total: 59000,
      lines: [{ quantity: 1, description: "Combo bỏng nước", unitPrice: 59000 }] });
    document.body.append(b); b.click();
  });
  await page.waitForFunction(() => document.querySelector("#cinema-print-frame")?.contentDocument.querySelector(".cinema-invoice"));
  assert.deepEqual(await decode(), ["HDB1234567890123456789012345678"]);
  assert.deepEqual(app.loi, []);
});

test("snack counter accepts barcode Enter and displays a reused-code rejection", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  let scans = 0;
  await page.route("**/api/admin/orders/snack-pickups", r => r.fulfill({ json: [] }));
  await page.route("**/api/admin/orders/snack-pickups/scan", r => {
    assert.equal(r.request().postDataJSON().code, "HDB1");
    scans++;
    return scans === 1
      ? r.fulfill({ json: { deliveredLines: 2 } })
      : r.fulfill({ status: 409, json: { message: "Hóa đơn đã nhận hết" } });
  });
  await app.dangNhap(page, "nhanvien@example.test", "admin.html");
  await page.locator('[data-muc="giao"]').click();
  await page.locator("#giao-scan-code").fill("HDB1");
  await page.locator("#giao-scan-code").press("Enter");
  await page.locator("#giao-scan-result").filter({ hasText: "Đã giao 2" }).waitFor();
  assert.equal(await page.locator("#giao-scan-code").inputValue(), "");
  await page.locator("#giao-scan-code").fill("HDB1");
  await page.locator("#giao-scan-code").press("Enter");
  await page.locator("#giao-scan-result").filter({ hasText: "Hóa đơn đã nhận hết" }).waitFor();
  assert.equal(scans, 2);
  assert.deepEqual(app.loi, []);
});

test("pickup groups orders, keeps pending lines and restores the mobile queue", async t => {
  const app = await chuanBi(t), page = await app.trangMoi();
  let lines = [
    { orderCode: "CS01", itemId: 12, name: "Bắp", quantity: 2, email: "buyer@example.test" },
    { orderCode: "CS01", itemId: 13, name: "Nước", quantity: 1 },
    { orderCode: "CS02", itemId: 14, name: "Combo", quantity: 1 },
  ];
  await page.route("**/api/admin/orders/snack-pickups", r => r.fulfill({ json: lines }));
  await page.route("**/api/admin/orders/CS01/snacks/*/deliver", r => {
    assert.equal(r.request().method(), "POST");
    const id = Number(r.request().url().split("/").at(-2));
    lines = lines.filter(item => item.itemId !== id);
    return r.fulfill({ json: {} });
  });
  await app.dangNhap(page, "nhanvien@example.test", "admin.html");
  await page.locator('[data-muc="giao"]').click();
  const order = page.getByRole("button", { name: /CS01.*Chờ nhận/ });
  await order.click();
  assert.equal(await page.locator(".pickup-order").count(), 2);
  await page.getByRole("button", { name: "Giao món này 2 × Bắp, đơn CS01" }).click();
  await order.filter({ hasText: "1 dòng món" }).waitFor();
  await page.setViewportSize({ width: 390, height: 844 });
  await order.click();
  await page.locator("#pickup-dialog[open]").waitFor();
  await page.getByRole("button", { name: "← Hàng chờ" }).click();
  await page.waitForFunction(() => document.activeElement?.classList.contains("pickup-order"));
  await order.click();
  await page.getByRole("button", { name: "Giao món này 1 × Nước, đơn CS01" }).click();
  await page.locator("#pickup-dialog").waitFor({ state: "hidden" });
  await page.getByRole("button", { name: /CS02.*Chờ nhận/ }).waitFor();
  assert.equal(await order.count(), 0);
  assert.deepEqual(app.loi, []);
});
