const test = require('node:test');
const assert = require('node:assert/strict');
const { start } = require('../../../src/main/resources/UI/assets/js/payment-deadline.js');

function clock() {
  let time = Date.parse('2026-11-09T10:00:00+07:00');
  const jobs = new Set();
  return { now: () => time, schedule: fn => (jobs.add(fn), fn), cancel: fn => jobs.delete(fn),
    advance(ms) { time += ms; for (const job of [...jobs]) job(); }, jobs };
}
test('uses server time despite a client clock seven minutes ahead', () => {
  const c = clock(); const ticks = []; let expired = 0;
  const d = start({ expiresAt: '2026-11-09T10:03:00+07:00', serverTime: '2026-11-09T09:53:00+07:00',
    now:c.now, schedule:c.schedule, cancel:c.cancel, onTick:v=>ticks.push(v), onExpire:()=>expired++ });
  assert.equal(ticks.at(-1),'10:00'); c.advance(600000);
  assert.equal(ticks.at(-1),'00:00'); assert.equal(expired,1); assert.equal(d.isActive(),false);
  c.advance(1000); assert.equal(expired,1); assert.equal(c.jobs.size,0);
});
test('recomputes after a sleeping tab and stop prevents late callbacks', () => {
  const c=clock(); let expired=0;
  const d=start({expiresAt:'2026-11-09T10:00:30+07:00',now:c.now,schedule:c.schedule,cancel:c.cancel,onExpire:()=>expired++});
  c.advance(31000); assert.equal(d.isActive(),false); assert.equal(expired,1);
  const other=start({expiresAt:'2026-11-09T11:00:00+07:00',now:c.now,schedule:c.schedule,cancel:c.cancel,onExpire:()=>expired++});
  other.stop(); c.advance(3600000); assert.equal(other.isActive(),false); assert.equal(expired,1);
});
test('rejects invalid deadlines and expires immediately at the boundary', () => {
  const c=clock(); assert.throws(()=>start({expiresAt:null}),/hạn/);
  let expired=0;
  const d=start({expiresAt:'2026-11-09T10:00:00+07:00',now:c.now,schedule:c.schedule,cancel:c.cancel,onExpire:()=>expired++});
  assert.equal(expired,1); assert.equal(d.isActive(),false); assert.equal(c.jobs.size,0);
});
test('response latency is charged against the server deadline', () => {
  const c=clock(),requestedAt=c.now(),ticks=[];c.advance(10000);
  const d=start({expiresAt:'2026-11-09T10:00:30+07:00',serverTime:'2026-11-09T10:00:00+07:00',requestedAt,
    now:c.now,schedule:c.schedule,cancel:c.cancel,onTick:v=>ticks.push(v)});
  assert.equal(ticks.at(-1),'00:20');c.advance(20000);assert.equal(d.isActive(),false);
});
