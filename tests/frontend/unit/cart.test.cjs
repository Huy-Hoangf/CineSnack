const thu = require("node:test");
const kt = require("node:assert/strict");
const rap = require("../../../src/main/resources/UI/assets/js/cinema.js");
const luc = new Date("2026-10-14T12:00:00+07:00");
const gio = () => ({
  version: 2,
  maSuatChieu: 101,
  phim: { id: 1, name: "Phim" },
  suat: { id: 101, startsAt: "2026-10-14T13:00:00+07:00" },
  ghe: [
    { id: 11, code: "A1", price: 80000 },
    { id: 12, code: "A2", price: 80000 },
  ],
  snacks: [],
  combos: [{ comboId: 21, quantity: 1, name: "Combo", unitPrice: 59000 }],
});
thu("Giỏ ID thật được chấp nhận và tính 219000", () => {
  kt.equal(rap.gioHopLe(gio(), luc), true);
  kt.equal(rap.tinhTien(gio()).tong, 219000);
});
thu("Chặn suất đã bắt đầu, kể cả cùng ngày và đúng ranh giới", () => {
  kt.equal(rap.gioHopLe(gio(), new Date("2026-10-14T13:00:00+07:00")), false);
});
thu(
  "Request chỉ có ID, số lượng, email; không gửi giá, tổng hay chủ đơn",
  () => {
    const body = rap.taoYeuCau(gio(), "khach@example.com");
    kt.deepEqual(body, {
      email: "khach@example.com",
      showTimeId: 101,
      seatIds: [11, 12],
      snacks: [],
      combos: [{ comboId: 21, quantity: 1 }],
    });
  },
);
thu("Cho mua chỉ bắp nước, không yêu cầu ghế/suất", () => {
  const g = gio();
  g.maSuatChieu = null;
  g.suat = null;
  g.phim = null;
  g.ghe = [];
  kt.equal(rap.gioHopLe(g, luc), true);
});
thu("Từ chối giỏ cũ/rỗng, ID trùng, thiếu dữ liệu và quantity sai", () => {
  kt.equal(rap.gioHopLe({ ...gio(), version: 1 }, luc), false);
  kt.equal(
    rap.gioHopLe({ ...gio(), ghe: [gio().ghe[0], gio().ghe[0]] }, luc),
    false,
  );
  for (const q of [0, 11, 1.5, "1"])
    kt.equal(
      rap.gioHopLe(
        { ...gio(), combos: [{ comboId: 21, quantity: q, unitPrice: 59000 }] },
        luc,
      ),
      false,
    );
  for (const g of [
    null,
    {},
    [],
    { ...gio(), ghe: null },
    { ...gio(), combos: null },
    { ...gio(), ghe: [], combos: [], maSuatChieu: null },
  ])
    kt.equal(rap.gioHopLe(g, luc), false);
});
