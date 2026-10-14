const test = require("node:test");
const assert = require("node:assert/strict");
const base = process.env.CINESNACK_LIVE_URL;
const movies = [
  ["Dune: Hành tinh cát – Phần hai", "dune.jpg", "DANG_CHIEU"],
  ["Những mảnh ghép cảm xúc 2", "inside-out.jpg", "DANG_CHIEU"],
  ["Kung Fu Panda 4", "kung-fu-panda.jpg", "DANG_CHIEU"],
  ["Godzilla x Kong: Đế chế mới", "godzilla.jpg", "DANG_CHIEU"],
  ["Người Nhện: Du hành vũ trụ nhện", "spider-man.jpg", "SAP_CHIEU"],
  ["Interstellar: Hố đen tử thần", "interstellar.jpg", "SAP_CHIEU"],
];

test(
  "SQL Server thật: bốn phim có suất tương lai và sơ đồ 60 ghế cùng phòng",
  { skip: !base },
  async () => {
    const response = await fetch(base + "/api/showtimes");
    assert.equal(response.status, 200);
    const shows = await response.json();
    for (const [name, , status] of movies) {
      if (status !== "DANG_CHIEU") continue;
      const show = shows.find((row) => row.movieName === name);
      assert.ok(show, name + " thiếu suất chiếu");
      assert.ok(Date.parse(show.startsAt) > Date.now());
      assert.equal(Number(show.price), 75000);
      const seatsResponse = await fetch(
        base + "/api/showtimes/" + show.id + "/seats",
      );
      assert.equal(seatsResponse.status, 200);
      const seats = await seatsResponse.json();
      assert.equal(seats.length, 60);
      assert.equal(new Set(seats.map((row) => row.id)).size, 60);
      assert.ok(seats.some((row) => row.code === "A1"));
      assert.ok(seats.some((row) => row.code === "F10"));
      assert.ok(seats.every((row) => Number(row.price) === 75000));
    }
    assert.equal(new Set(shows.map((row) => row.roomId)).size, 1);
  },
);

test(
  "SQL Server thật: phim FE cũ có ID, trạng thái và ảnh truy cập được",
  { skip: !base },
  async () => {
    const response = await fetch(base + "/api/movies?size=100");
    assert.equal(response.status, 200);
    const rows = await response.json();
    for (const [name, image, status] of movies) {
      const found = rows.filter((row) => row.name === name);
      assert.equal(found.length, 1, name);
      assert.ok(Number.isInteger(found[0].id));
      assert.equal(found[0].status, status);
      assert.equal(found[0].posterUrl, "/assets/images/movies/" + image);
      const poster = await fetch(new URL(found[0].posterUrl, base));
      assert.equal(poster.status, 200, image);
      assert.match(poster.headers.get("content-type"), /^image\//);
    }
  },
);

test(
  "SQL Server thật: hai combo FE cũ xuất hiện một lần với giá gốc",
  { skip: !base },
  async () => {
    const response = await fetch(base + "/api/snacks");
    assert.equal(response.status, 200);
    const { combos } = await response.json();
    for (const [name, price] of [
      ["Combo Một Mình Vui", 59000],
      ["Combo Hẹn Hò", 99000],
    ]) {
      const found = combos.filter((row) => row.name === name);
      assert.equal(found.length, 1, name);
      assert.ok(Number.isInteger(found[0].id));
      assert.equal(Number(found[0].price), price);
    }
  },
);
