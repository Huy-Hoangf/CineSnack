const test = require("node:test");
const assert = require("node:assert/strict");
const C = require("../../src/main/resources/static/assets/js/shared/cinema.js");
const now = new Date(2026, 9, 9, 12);
const cart = () => ({
  movie: "dune",
  date: "2026-10-09",
  time: "19:30",
  seats: ["A1", "D1"],
  combos: { solo: 1, duo: 0 },
});
test("Vietnamese search ignores accents, case and surrounding spaces", () => {
  assert.equal(C.normalize("  HÀNH ĐỘNG  "), "hanh dong");
  assert.equal(C.normalize("Đế chế"), "de che");
});
test("dates use local calendar and cross a month boundary", () => {
  assert.deepEqual(C.dates(new Date(2026, 0, 31, 12)), [
    "2026-01-31",
    "2026-02-01",
    "2026-02-02",
  ]);
});
test("regular and VIP tickets plus snack total exactly", () => {
  assert.deepEqual(C.totals(cart()), {
    tickets: 170000,
    snacks: 59000,
    total: 229000,
  });
});
test("valid cart accepted; zero seats rejected", () => {
  assert.equal(C.validCart(cart(), now), true);
  assert.equal(C.validCart({ ...cart(), seats: [] }, now), false);
});
test("reject duplicate, invalid and occupied seats", () => {
  for (const seats of [
    ["A1", "A1"],
    ["Z99"],
    [...C.occupied("dune", "2026-10-09", "19:30")].slice(0, 1),
  ])
    assert.equal(C.validCart({ ...cart(), seats }, now), false);
});
test("only now-showing movies and known slots can be booked", () => {
  for (const patch of [
    { movie: "interstellar" },
    { movie: "bad" },
    { time: "99:00" },
    { date: "2026-09-09" },
    { date: "2026-10-12" },
  ])
    assert.equal(C.validCart({ ...cart(), ...patch }, now), false);
});
test("reject excessive or malformed snack quantities and oversized seat selection", () => {
  for (const solo of [-1, 9, 1.5, "1", Infinity])
    assert.equal(
      C.validCart({ ...cart(), combos: { solo, duo: 0 } }, now),
      false,
    );
  const unavailable = C.occupied("dune", "2026-10-09", "19:30");
  const available = [..."AB"].flatMap((row) =>
    Array.from({ length: 10 }, (_, i) => row + (i + 1)).filter(
      (seat) => !unavailable.has(seat),
    ),
  );
  assert.equal(
    C.validCart({ ...cart(), seats: available.slice(0, 8) }, now),
    true,
  );
  assert.equal(
    C.validCart({ ...cart(), seats: available.slice(0, 9) }, now),
    false,
  );
});
test("malformed storage never throws", () => {
  for (const value of [
    null,
    [],
    {},
    "bad",
    { ...cart(), combos: null },
    { ...cart(), seats: null },
  ])
    assert.equal(C.validCart(value, now), false);
});
