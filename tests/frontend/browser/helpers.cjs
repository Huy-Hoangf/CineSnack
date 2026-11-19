const { chromium } = require("playwright");
const { taoMayChu } = require("../fixtures/api-server.cjs");
async function chuanBi(t) {
  const fixture = taoMayChu();
  await new Promise((r) => fixture.server.listen(0, "127.0.0.1", r));
  const browser = await chromium.launch({
    headless: true,
    executablePath:
      process.env.TRINH_DUYET ||
      "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
  });
  t.after(async () => {
    await browser.close();
    fixture.server.closeAllConnections();
    await new Promise((r) => fixture.server.close(r));
  });
  const goc = `http://127.0.0.1:${fixture.server.address().port}/html/`,
    loi = [];
  async function trangMoi() {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    page.on("pageerror", (e) => loi.push(e.message));
    return page;
  }
  async function mo(page, file) {
    await page.goto(goc + file);
    const popup = page.locator("#lua-chon-tai-khoan[open]");
    await page.waitForTimeout(100);
    if (await popup.isVisible()) await page.locator("#tiep-tuc-khach").click();
  }
  async function dangNhap(page, email, next = "orders.html") {
    await mo(page, "authentication.html?next=" + encodeURIComponent(next));
    await page.locator("#email").fill(email);
    await page.locator("#mat-khau").fill("MoPhong123!");
    await page.locator("#gui-tai-khoan").click();
    await page.waitForURL(
      (url) => !url.pathname.endsWith("authentication.html"),
    );
  }
  return { ...fixture, browser, goc, loi, trangMoi, mo, dangNhap };
}
module.exports = { chuanBi };
