const test = require('node:test');
const assert = require('node:assert/strict');
const { chuanBi } = require('./helpers.cjs');

async function checkout(t, delayed = false) {
  const app=await chuanBi(t), page=await app.trangMoi();
  const now=Date.now();
  await page.clock.install({time:new Date(now)});
  await app.mo(page,'index.html');
  await page.evaluate(({now})=>sessionStorage.setItem('cinesnack-gio-v2',JSON.stringify({
    version:2,maSuatChieu:null,phim:null,suat:null,ghe:[],snacks:[{productId:31,name:'Nước',quantity:1,unitPrice:30000}],combos:[]
  })),{now});
  const order={code:'DHTIMER',createdAt:new Date(now).toISOString(),expiresAt:new Date(now+5000).toISOString()};
  await page.route('**/api/guest/orders',r=>r.fulfill({status:201,json:order}));
  let release;
  const gate = new Promise(r=>release=r);
  let requested;
  const pending=new Promise(r=>requested=r);
  await page.route('**/api/guest/orders/DHTIMER/payment-qr',async r=>{
    requested(); if(delayed)await gate;
    await r.fulfill({json:{amount:30000,content:order.code,accountNo:'12345678',bankName:'Demo',accountName:'CineSnack',
      imageUrl:'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="20" height="20"%3E%3Crect width="20" height="20"/%3E%3C/svg%3E',serverTime:order.createdAt,expiresAt:order.expiresAt}});
  });
  await app.mo(page,'checkout.html');
  await page.locator('#khach-email').fill('buyer@example.test');
  await page.locator('#mau-dong-y').check();
  return {app,page,pending,release,order};
}
test('checkout counts down and removes QR, download and copy actions at expiry',async t=>{
  const {app,page}=await checkout(t);
  await page.locator('#thanh-toan-bieu-mau button[type=submit]').click();
  await page.locator('#checkout-qr-image img').waitFor();
  assert.match(await page.locator('#checkout-countdown').textContent(),/00:0[1-5]/);
  await page.clock.fastForward(6000);
  assert.equal(await page.locator('#checkout-qr-image img').count(),0);
  assert.equal(await page.locator('#checkout-download-qr').isVisible(),false);
  assert.equal(await page.locator('#checkout-copy-content').isDisabled(),true);
  assert.match(await page.locator('#checkout-qr-error').textContent(),/hết/);
  assert.deepEqual(app.loi,[]);
});
test('a late QR response cannot restore expired payment actions',async t=>{
  const {app,page,pending,release}=await checkout(t,true);
  await page.locator('#thanh-toan-bieu-mau button[type=submit]').click(); await pending;
  await page.clock.fastForward(6000); release();
  await page.waitForTimeout(100);
  assert.equal(await page.locator('#checkout-qr-image img').count(),0);
  assert.equal(await page.locator('#checkout-open-qr').isDisabled(),true);
  assert.deepEqual(app.loi,[]);
});
test('a QR response arriving before expiry cannot extend the checkout deadline',async t=>{
  const {app,page,pending,release}=await checkout(t,true);
  await page.locator('#thanh-toan-bieu-mau button[type=submit]').click();await pending;
  await page.clock.fastForward(2000);release();
  await page.locator('#checkout-qr-image img').waitFor();
  await page.clock.fastForward(3500);
  assert.equal(await page.locator('#checkout-qr-image img').count(),0);
  assert.deepEqual(app.loi,[]);
});
test('a delayed order response does not extend the hold when QR is unavailable',async t=>{
  const {app,page,order}=await checkout(t);
  let release,requested;const gate=new Promise(r=>release=r),pending=new Promise(r=>requested=r);
  await page.route('**/api/guest/orders',async r=>{requested();await gate;await r.fulfill({status:201,json:order});});
  await page.route('**/api/guest/orders/DHTIMER/payment-qr',r=>r.fulfill({status:503,json:{message:'Chưa cấu hình QR'}}));
  await page.locator('#thanh-toan-bieu-mau button[type=submit]').click();await pending;
  await page.clock.fastForward(2000);release();
  await page.locator('#checkout-qr-error').filter({hasText:'Chưa cấu hình QR'}).waitFor();
  await page.clock.fastForward(3500);
  assert.equal(await page.locator('#checkout-countdown').textContent(),'00:00');
  assert.equal(await page.locator('#checkout-open-qr').isDisabled(),true);
  assert.deepEqual(app.loi,[]);
});
test('a PNG finishing after expiry is revoked without restoring download',async t=>{
  const {app,page}=await checkout(t);
  await page.evaluate(()=>{
    window.revoked=[];
    URL.revokeObjectURL=url=>window.revoked.push(url);
    CineSnack.qrPngUrl=()=>new Promise(resolve=>window.finishPng=resolve);
  });
  await page.locator('#thanh-toan-bieu-mau button[type=submit]').click();
  await page.waitForFunction(()=>typeof window.finishPng==='function');
  await page.clock.fastForward(6000);
  await page.evaluate(()=>window.finishPng('blob:late-qr'));
  await page.waitForFunction(()=>window.revoked.includes('blob:late-qr'));
  assert.equal(await page.locator('#checkout-download-qr').isVisible(),false);
  assert.deepEqual(app.loi,[]);
});
test('order details count down even when QR API is unavailable and retire expired controls',async t=>{
  const app=await chuanBi(t),page=await app.trangMoi();const now=Date.now();
  await page.clock.install({time:new Date(now)});
  const order={code:'DHTIMER',status:'CHO_THANH_TOAN',total:30000,email:'buyer@example.test',createdAt:new Date(now).toISOString(),expiresAt:new Date(now+5000).toISOString()};
  await page.route('**/api/guest/orders?*',r=>r.fulfill({json:[order]}));
  await page.route('**/api/guest/orders/DHTIMER',r=>r.fulfill({json:{order,tickets:[],snacks:[]}}));
  await page.route('**/api/guest/orders/DHTIMER/payment-qr',r=>r.fulfill({status:503,json:{message:'Chưa cấu hình QR'}}));
  await app.mo(page,'orders.html?guest=1&code=DHTIMER');
  await page.locator('.order-payment-qr output').waitFor();
  await page.clock.fastForward(6000);
  assert.match(await page.locator('.order-payment-qr').textContent(),/hết thời gian/);
  assert.equal(await page.locator('.order-payment-qr img').count(),0);
  assert.deepEqual(app.loi,[]);
});
test('returning to a cached checkout revalidates through the order page',async t=>{
  const {app,page,order}=await checkout(t);
  const details={order:{...order,status:'CHO_THANH_TOAN',total:30000,email:'buyer@example.test'},tickets:[],snacks:[]};
  await page.route('**/api/guest/orders?*',r=>r.fulfill({json:[details.order]}));
  await page.route('**/api/guest/orders/DHTIMER',r=>r.fulfill({json:details}));
  await page.locator('#thanh-toan-bieu-mau button[type=submit]').click();
  await page.locator('#checkout-qr-image img').waitFor();
  await page.evaluate(()=>window.dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true})));
  await page.waitForURL('**/orders.html?code=DHTIMER&guest=1');
  await page.locator('.order-payment-qr').waitFor();
  assert.deepEqual(app.loi,[]);
});

async function orderPage(t, skew = 0) {
  const app=await chuanBi(t),page=await app.trangMoi();page.setDefaultTimeout(5000);
  const now=Date.now();await page.clock.install({time:new Date(now+skew)});
  const order={code:'DHORDER',status:'CHO_THANH_TOAN',total:30000,email:'buyer@example.test',createdAt:new Date(now).toISOString(),expiresAt:new Date(now+5000).toISOString()};
  const state={details:0,qr:0,failDetails:false};
  await page.route('**/api/guest/orders?*',r=>r.fulfill({json:[order]}));
  await page.route('**/api/guest/orders/DHORDER',r=>{
    state.details++;
    return state.failDetails?r.fulfill({status:503,json:{message:'Không tải được đơn'}}):r.fulfill({json:{order,tickets:[],snacks:[]}});
  });
  await page.route('**/api/guest/orders/DHORDER/payment-qr',r=>{
    state.qr++;return r.fulfill({json:{expiresAt:order.expiresAt,serverTime:new Date(now).toISOString(),amount:30000,content:order.code,
      accountNo:'12345678',imageUrl:'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="20" height="20"%3E%3Crect width="20" height="20"/%3E%3C/svg%3E'}});
  });
  await app.mo(page,'orders.html?guest=1&code=DHORDER');
  return {app,page,order,state};
}
test('order QR requests authoritative server time before applying an ahead client clock',async t=>{
  const {app,page,state}=await orderPage(t,7*60000);
  await page.locator('.order-payment-qr img').waitFor();
  assert.equal(state.qr,1);
  assert.match(await page.locator('.order-payment-qr output').textContent(),/00:0[1-5]/);
  await page.clock.fastForward(6000);
  assert.equal(await page.locator('.order-payment-qr img').count(),0);
  assert.deepEqual(app.loi,[]);
});
test('cached order pages reload current state instead of preserving an expired QR',async t=>{
  const {app,page,order,state}=await orderPage(t);
  await page.locator('.order-payment-qr img').waitFor();
  await page.evaluate(()=>window.dispatchEvent(new PageTransitionEvent('pagehide',{persisted:true})));
  assert.equal(await page.locator('.order-payment-qr img').count(),0);
  order.status='HUY';await page.clock.fastForward(6000);
  await page.evaluate(()=>window.dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true})));
  await page.locator('#trang-thai-don').filter({hasText:'Đã hủy'}).waitFor();
  assert.ok(state.details>=2);
  assert.equal(await page.locator('.order-payment-qr img').count(),0);
  assert.deepEqual(app.loi,[]);
});
test('cancel success removes payment QR even when subsequent detail refresh fails',async t=>{
  const {app,page,order,state}=await orderPage(t);
  await page.locator('.order-payment-qr img').waitFor();
  await page.route('**/api/guest/orders/DHORDER/cancel',r=>{
    state.failDetails=true;return r.fulfill({json:{...order,status:'HUY'}});
  });
  await page.locator('#huy-don').click();
  await page.getByText('Không tải được đơn',{exact:true}).waitFor();
  assert.equal(await page.locator('.order-payment-qr img').count(),0);
  assert.equal(await page.locator('#huy-don').isDisabled(),true);
  assert.match(await page.locator('#trang-thai-don').textContent(),/Đã hủy/);
  assert.deepEqual(app.loi,[]);
});
