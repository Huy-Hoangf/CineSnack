const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const script = fs.readFileSync(path.join(__dirname, "../../main/resources/UI/assets/js/authentication.js"), "utf8");

async function redirectedFrom(role, pathname) {
  const destinations = [];
  await vm.runInNewContext(script, {
    CineSnackApi: { taiKhoan: async () => ({ role, email: "staff@example.com" }) },
    document: {
      querySelector: () => null,
      querySelectorAll: () => [],
    },
    location: {
      pathname,
      search: "",
      replace: (destination) => destinations.push(destination),
    },
  });
  return destinations;
}

test("staff can open booking and checkout, while admin remains outside counter ordering", async () => {
  assert.deepEqual(await redirectedFrom("NHAN_VIEN", "/html/booking.html"), []);
  assert.deepEqual(await redirectedFrom("NHAN_VIEN", "/html/checkout.html"), []);
  assert.deepEqual(await redirectedFrom("NHAN_VIEN", "/html/orders.html"), ["admin.html"]);
  assert.deepEqual(await redirectedFrom("QUAN_TRI", "/html/booking.html"), ["admin.html"]);
  assert.deepEqual(await redirectedFrom("QUAN_TRI", "/html/checkout.html"), ["admin.html"]);
});

test("staff login returns to booking, admin login returns to workspace", async () => {
  for (const [role, expected] of [
    ["NHAN_VIEN", "/html/booking.html"],
    ["QUAN_TRI", "admin.html"],
  ]) {
    let submit;
    const location = {
      href: "http://localhost:8080/html/authentication.html?next=%2Fhtml%2Fbooking.html",
      origin: "http://localhost:8080",
      pathname: "/html/authentication.html",
      search: "?next=%2Fhtml%2Fbooking.html",
    };
    const password = { value: "secret" };
    const form = {
      reportValidity: () => true,
      addEventListener: (_, handler) => { submit = handler; },
    };
    const button = { disabled: false, setAttribute: () => {}, removeAttribute: () => {} };
    const fields = {
      "#bieu-mau-tai-khoan": form,
      "#gui-tai-khoan": button,
      "#thong-bao-tai-khoan": { textContent: "", focus: () => {} },
      "#email": { value: "staff@example.com" },
      "#mat-khau": password,
      "[data-tiep-tuc-khach]": null,
    };
    await vm.runInNewContext(script, {
      CineSnackApi: {
        taiKhoan: async () => null,
        dangNhap: async () => ({ role }),
      },
      document: {
        querySelector: (selector) => fields[selector],
        querySelectorAll: () => [],
      },
      location,
      URL,
      URLSearchParams,
    });
    await submit({ preventDefault: () => {} });
    assert.equal(location.href, expected);
  }
});
