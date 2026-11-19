const test = require('node:test');
const assert = require('node:assert/strict');
const timeline = require('../../main/resources/UI/assets/js/showtime-timeline.js');

test('Vietnam day clips both overnight edges and excludes exact day boundaries', () => {
  const shows = [
    { id: 1, roomId: 2, startsAt: '2026-11-08T23:00:00+07:00', endsAt: '2026-11-09T01:00:00+07:00' },
    { id: 2, roomId: 2, startsAt: '2026-11-09T16:00:00Z', endsAt: '2026-11-09T18:00:00Z' },
    { id: 3, roomId: 2, startsAt: '2026-11-08T22:00:00+07:00', endsAt: '2026-11-09T00:00:00+07:00' },
    { id: 4, roomId: 2, startsAt: '2026-11-10T00:00:00+07:00', endsAt: '2026-11-10T01:00:00+07:00' },
  ];
  const items = timeline.layout(shows, '2026-11-09');
  assert.deepEqual(items.map(s => [s.id, s.startMinute, s.endMinute]), [[1, 0, 60], [2, 1380, 1440]]);
});

test('room ordering is stable and overlapping cancelled shows use separate lanes', () => {
  const base = { startsAt: '2026-11-09T10:00:00+07:00', endsAt: '2026-11-09T12:00:00+07:00' };
  const result = timeline.layout([{ ...base, id: 3, roomId: 2 }, { ...base, id: 2, roomId: 1 }, { ...base, id: 1, roomId: 1 }], '2026-11-09');
  assert.deepEqual(result.map(s => [s.id, s.lane]), [[1, 0], [2, 1], [3, 0]]);
});

test('date navigation stays in Vietnam regardless of browser timezone', () => {
  assert.equal(timeline.shiftDate('2026-12-31', 1), '2027-01-01');
  assert.equal(timeline.shiftDate('2026-01-01', -1), '2025-12-31');
});
