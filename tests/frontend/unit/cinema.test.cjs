const thu = require("node:test");
const kt = require("node:assert/strict");
const rap = require("../../../src/main/resources/UI/assets/js/cinema.js");
thu("Tìm kiếm bỏ qua dấu, chữ hoa và khoảng trắng", () => {
  kt.equal(rap.boDau("  HÀNH ĐỘNG  "), "hanh dong");
  kt.equal(rap.boDau("Đế chế"), "de che");
});
thu("Ngày theo lịch địa phương và qua cuối tháng", () => {
  kt.deepEqual(rap.layNgay(new Date(2026, 0, 31, 12)), ["2026-01-31", "2026-02-01", "2026-02-02"]);
});
thu("Giới hạn 10 ghế, không có giá VIP tự đặt", () => {
  const luc = new Date("2026-10-14T12:00:00+07:00");
  const g = {
    version: 2,
    maSuatChieu: 1,
    phim: { id: 1 },
    suat: { id: 1, startsAt: "2026-10-14T13:00:00+07:00" },
    ghe: Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      code: "A" + (i + 1),
      price: 80000,
    })),
    snacks: [],
    combos: [],
  };
  kt.equal(rap.gioHopLe(g, luc), true);
  kt.equal(rap.tinhTien(g).tong, 800000);
  g.ghe.push({ id: 11, code: "A11", price: 80000 });
  kt.equal(rap.gioHopLe(g, luc), false);
});
