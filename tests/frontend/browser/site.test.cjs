const thu = require("node:test"),
  kt = require("node:assert/strict"),
  fs = require("node:fs"),
  path = require("node:path");
const { chuanBi } = require("./helpers.cjs");
thu("Chỉnh sửa giỏ giữ ID và số lượng, lấy lại giá mới từ API", async (t) => {
  const m = await chuanBi(t),
    p = await m.trangMoi();
  await m.dangNhap(p, "khach@example.test", "booking.html?phim=1");
  await p.locator('[data-ghe-id="11"]').click();
  await p.locator('[data-ghe-id="12"]').click();
  await p.locator('[data-combo="21"][data-doi="1"]').click();
  await p.locator("#tiep-dat-ve").click();
  await p.waitForURL("**/checkout.html");
  m.catalog.showTime.price = 90000;
  m.catalog.combos[0].price = 69000;
  await p.locator("#sua-dat-ve").click();
  await p.waitForURL("**/booking.html?phim=1");
  await p.locator('[data-ghe-id="11"]').waitFor();
  kt.equal(
    await p.locator('[data-ghe-id="11"]').getAttribute("aria-pressed"),
    "true",
  );
  kt.equal(await p.locator("#luong-combos-21").textContent(), "1");
  kt.equal(
    await p.locator("#tom-tat-tong").textContent(),
    await p.evaluate(() => CineSnack.dinhDangTien(249000)),
  );
  kt.deepEqual(m.loi, []);
});
thu(
  "Đổi đơn trong lúc refresh: phản hồi cũ không ghi đè đơn mới",
  async (t) => {
    const m = await chuanBi(t),
      p = await m.trangMoi();
    await m.dangNhap(p, "khach@example.test", "orders.html");
    const token = await (
      await p.request.get(new URL("/api/csrf", m.goc).href)
    ).json();
    const tao = async (id) =>
      (
        await (
          await p.request.post(new URL("/api/orders", m.goc).href, {
            headers: { [token.headerName]: token.token },
            data: {
              email: "khach@example.test",
              showTimeId: 101,
              seatIds: [id],
              snacks: [],
              combos: [],
            },
          })
        ).json()
      ).code;
    const a = await tao(11),
      b = await tao(12);
    await p.goto(m.goc + "orders.html?code=" + a);
    await p.locator("#ma-don").filter({ hasText: a }).waitFor();
    let release, daNhan;
    const nhan = new Promise((r) => {
      daNhan = r;
    });
    await p.route("**/api/orders/" + a, async (route) => {
      const response = await route.fetch();
      daNhan();
      await new Promise((r) => {
        release = r;
      });
      await route.fulfill({ response });
    });
    await p.locator("#tai-lai-don").click();
    await nhan;
    await p.locator("#danh-sach-don button").filter({ hasText: b }).click();
    await p.locator("#ma-don").filter({ hasText: b }).waitFor();
    release();
    await p.waitForTimeout(200);
    kt.equal(await p.locator("#ma-don").textContent(), b);
    kt.deepEqual(m.loi, []);
  },
);
thu(
  "FE thật: tạo đơn bằng ID API, nhân viên cash, khách xem hai hóa đơn; responsive",
  async (t) => {
    const m = await chuanBi(t),
      khach = await m.trangMoi(),
      nv = await m.trangMoi();
    await m.dangNhap(khach, "khach@example.test", "booking.html?phim=1");
    await khach.locator('[data-ghe-id="11"]').click();
    await khach.locator('[data-ghe-id="12"]').click();
    await khach.locator('[data-combo="21"][data-doi="1"]').click();
    const tien = await khach.evaluate(() => CineSnack.dinhDangTien(219000));
    kt.equal(await khach.locator("#tom-tat-tong").textContent(), tien);
    await khach.locator("#tiep-dat-ve").click();
    await khach.waitForURL("**/checkout.html");
    await khach.locator("#mau-dong-y").check();
    let post = 0,
      body;
    khach.on("request", (r) => {
      if (
        r.method() === "POST" &&
        new URL(r.url()).pathname === "/api/orders"
      ) {
        post++;
        body = r.postDataJSON();
      }
    });
    await khach.locator("#thanh-toan-bieu-mau button[type=submit]").dblclick();
    await khach.waitForURL("**/orders.html?code=*");
    await khach
      .locator("#trang-thai-don")
      .filter({ hasText: "Chờ thanh toán" })
      .waitFor();
    const code = await khach.locator("#ma-don").textContent();
    kt.equal(post, 1);
    kt.deepEqual(body.seatIds, [11, 12]);
    kt.equal(body.total, undefined);
    kt.equal(body.unitPrice, undefined);
    kt.equal(body.userId, undefined);
    kt.equal(await khach.locator("#cash-form").count(), 0);
    await m.dangNhap(nv, "nhanvien@example.test", "admin.html");
    await nv.locator(`[data-don="${code}"]`).first().click();
    await nv.locator("#tien-khach-dua").fill("250000");
    await nv.locator("#nhan-tien").click();
    await nv
      .locator("#trang-thai-don")
      .filter({ hasText: "Đã thanh toán" })
      .waitFor();
    await nv.locator("#tien-thua").filter({ hasText: "31.000" }).waitFor();
    await khach.locator("#tai-lai-don").click();
    await khach.locator("#hoa-don article").nth(1).waitFor();
    kt.equal(await khach.locator("#hoa-don article").count(), 2);
    kt.equal(await khach.locator("#huy-don").isEnabled(), false);
    const anh = process.env.THU_MUC_ANH;
    if (anh) {
      fs.mkdirSync(anh, { recursive: true });
      await khach.screenshot({
        path: path.join(anh, "orders-desktop.png"),
        fullPage: true,
      });
    }
    for (const width of [375, 768, 1280]) {
      await khach.setViewportSize({ width, height: 900 });
      kt.equal(
        await khach.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
        true,
      );
    }
    if (anh)
      await khach.screenshot({
        path: path.join(anh, "orders-responsive.png"),
        fullPage: true,
      });
    kt.deepEqual(m.loi, []);
  },
);
thu(
  "FE thật: API catalog lỗi không fallback; khách chưa đăng nhập không tạo đơn",
  async (t) => {
    const m = await chuanBi(t),
      page = await m.trangMoi();
    await page.route("**/api/movies*", (r) =>
      r.fulfill({
        status: 503,
        contentType: "application/json",
        body: JSON.stringify({ message: "Catalog chưa kết nối" }),
      }),
    );
    await m.mo(page, "booking.html");
    await page
      .locator("#dat-ve-loi")
      .filter({ hasText: "Catalog chưa kết nối" })
      .waitFor();
    kt.equal(await page.locator("#tiep-dat-ve").isEnabled(), false);
    kt.equal(await page.locator("[data-ghe-id]").count(), 0);
    kt.deepEqual(m.loi, []);
  },
);
thu("FE thật: xung đột ghế 409 giữ giỏ và không báo thành công", async (t) => {
  const m = await chuanBi(t),
    page = await m.trangMoi();
  await m.dangNhap(page, "khach@example.test", "booking.html?phim=1");
  await page.locator('[data-ghe-id="11"]').click();
  await page.locator("#tiep-dat-ve").click();
  await page.waitForURL("**/checkout.html");
  await page.route("**/api/orders", (r) =>
    r.request().method() === "POST"
      ? r.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "Ghế đã được giữ" }),
        })
      : r.continue(),
  );
  await page.locator("#mau-dong-y").check();
  await page.locator("#thanh-toan-bieu-mau button[type=submit]").click();
  await page
    .locator("#thanh-toan-loi")
    .filter({ hasText: "Ghế đã được giữ" })
    .waitFor();
  kt.ok(page.url().endsWith("checkout.html"));
  kt.equal(
    await page.locator("#thanh-toan-bieu-mau button[type=submit]").isEnabled(),
    true,
  );
  await page.reload();
  await page.locator("#thanh-toan-noi-dung").waitFor({ state: "visible" });
  kt.equal(await page.locator("#tom-tat-ghe").textContent(), "A1");
  kt.deepEqual(m.loi, []);
});
