const test = require("node:test");
const assert = require("node:assert/strict");
const { chuanBi } = require("./helpers.cjs");

test("Camera điền mã cho hai bước và không tự sử dụng lượt in hoặc soát vé", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  page.setDefaultTimeout(5000);
  await page.route("**/assets/vendor/zxing-browser-0.2.1.min.js", (route) =>
    route.fulfill({
      contentType: "text/javascript",
      body: `window.ZXingBrowser = {
        BarcodeFormat: { QR_CODE: 11, CODE_128: 4 },
        BrowserMultiFormatReader: class {
          set possibleFormats(value) { window.cameraFormats = value; }
          async decodeFromVideoDevice(device, video, callback) {
            const controls = { stop() { window.cameraStopped = true; } };
            window.fakeCameraScan = (code) => callback({ getText: () => code }, null, controls);
            return controls;
          }
        }
      };`,
    }),
  );
  let issueCount = 0;
  let scanCount = 0;
  await page.route("**/api/staff/tickets/issue", (route) => {
    issueCount++;
    return route.fulfill({ json: {} });
  });
  await page.route("**/api/staff/tickets/scan", (route) => {
    scanCount++;
    return route.fulfill({
      json: {
        ticket: {
          code: "VE456",
          seatCode: "A1",
          movieName: "Phim thử",
          roomName: "Phòng 01",
          startsAt: "2026-11-05T19:00:00+07:00",
        },
        checkedAt: "2026-11-05T18:45:00+07:00",
      },
    });
  });
  await app.dangNhap(page, "nhanvien@example.test", "theater.html");

  await page.locator("#issue-camera-start").click();
  await page.evaluate(() => window.fakeCameraScan("VE123"));
  assert.equal(await page.locator("#issue-code").inputValue(), "VE123");
  assert.equal(issueCount, 0);
  assert.deepEqual(await page.evaluate(() => window.cameraFormats), [11, 4]);
  assert.equal(await page.evaluate(() => window.cameraStopped), true);

  await page.locator("#choose-scan").click();
  await page.locator("#checkin-camera-start").click();
  await page.evaluate(() => window.fakeCameraScan("VE456"));
  assert.equal(await page.locator("#ticket-code").inputValue(), "VE456");
  assert.equal(scanCount, 0);
  await page.locator('#checkin-form button[type="submit"]').click();
  await page.getByText("Đã soát vé · Ghế A1").waitFor();
  assert.equal(scanCount, 1);
  assert.deepEqual(app.loi, []);
});

test("Thư viện đọc được mã CODE128 đang in trên vé", async (t) => {
  const app = await chuanBi(t);
  const page = await app.trangMoi();
  await app.mo(page, "theater.html");
  const decoded = await page.evaluate(async () => {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    JsBarcode(svg, "VE123", {
      format: "CODE128",
      width: 3,
      height: 100,
      margin: 20,
      displayValue: false,
    });
    const image = new Image();
    const url = URL.createObjectURL(
      new Blob([new XMLSerializer().serializeToString(svg)], {
        type: "image/svg+xml",
      }),
    );
    try {
      image.src = url;
      await image.decode();
      const reader = new ZXingBrowser.BrowserMultiFormatOneDReader();
      return (await reader.decodeFromImageElement(image)).getText();
    } finally {
      URL.revokeObjectURL(url);
    }
  });
  assert.equal(decoded, "VE123");
});
