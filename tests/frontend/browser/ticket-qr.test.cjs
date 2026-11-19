const test=require('node:test');
const assert=require('node:assert/strict');
const {chuanBi}=require('./helpers.cjs');
test('each paid ticket has a local readable QR and scanned tickets lose the QR',async t=>{
  const app=await chuanBi(t),page=await app.trangMoi();
  const data={order:{code:'DHQR',status:'DA_THANH_TOAN',total:160000,email:'buyer@example.test',createdAt:new Date().toISOString()},snacks:[],tickets:[
    {code:'VEQR1',seatCode:'A1',status:'DA_THANH_TOAN',unitPrice:80000},
    {code:'VEQR2',seatCode:'A2',status:'DA_THANH_TOAN',unitPrice:80000}]};
  await page.route('**/api/guest/orders?*',r=>r.fulfill({json:[data.order]}));
  await page.route('**/api/guest/orders/DHQR',r=>r.fulfill({json:data}));
  await page.route('**/api/guest/orders/DHQR/invoices',r=>r.fulfill({json:[]}));
  await app.mo(page,'orders.html?guest=1&code=DHQR');
  await page.locator('.ticket-codes summary').click();
  assert.equal(await page.locator('.ticket-codes .ticket-qr').count(),2);
  const values=await page.evaluate(async()=>{
    const codes=[];
    for(const svg of document.querySelectorAll('.ticket-codes .ticket-qr')){
      const image=new Image();const url=URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(svg)],{type:'image/svg+xml'}));
      try {image.src=url;await image.decode();codes.push((await new ZXingBrowser.BrowserQRCodeReader().decodeFromImageElement(image)).getText());}
      finally{URL.revokeObjectURL(url);}
    }return codes;
  });
  assert.deepEqual(values,['VEQR1','VEQR2']);
  data.tickets[1].status='DA_SOAT';await page.locator('#tai-lai-don').click();
  await page.locator('.ticket-codes').filter({hasText:'Vé đã soát'}).waitFor();
  assert.equal(await page.locator('.ticket-codes .ticket-qr').count(),1);
  assert.deepEqual(app.loi,[]);
});
