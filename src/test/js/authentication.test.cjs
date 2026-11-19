const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const script = fs.readFileSync(
  path.join(__dirname, "../../main/resources/UI/assets/js/authentication.js"),
  "utf8",
);

async function redirectedFrom(role, pathname, search = "") {
  const destinations = [];
  await vm.runInNewContext(script, {
    CineSnackApi: {
      taiKhoan: async () => ({ role, email: "staff@example.com" }),
    },
    URLSearchParams,
    document: {
      body: { classList: { add() {} } },
      querySelector: selector => selector.startsWith(".trang-mo-dau") || selector === "#trong-thanh-toan a" ? {} : null,
      querySelectorAll: () => [],
    },
    location: {
      pathname,
      search,
      replace: (destination) => destinations.push(destination),
    },
  });
  return destinations;
}

test("staff can open booking and checkout, while admin remains outside counter ordering", async () => {
  assert.deepEqual(await redirectedFrom("NHAN_VIEN", "/html/booking.html"), []);
  assert.deepEqual(
    await redirectedFrom("NHAN_VIEN", "/html/checkout.html"),
    ["admin.html"],
  );
  assert.deepEqual(
    await redirectedFrom("NHAN_VIEN", "/html/checkout.html", "?mode=counter"),
    [],
  );
  assert.deepEqual(await redirectedFrom("NHAN_VIEN", "/html/orders.html"), [
    "admin.html",
  ]);
  assert.deepEqual(await redirectedFrom("QUAN_TRI", "/html/booking.html"), []);
  assert.deepEqual(await redirectedFrom("QUAN_TRI", "/html/checkout.html"), [
    "admin.html",
  ]);
});

test("both operators can return to the schedule after login", async () => {
  for (const [role, expected] of [
    ["NHAN_VIEN", "/html/booking.html"],
    ["QUAN_TRI", "/html/booking.html"],
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
      addEventListener: (_, handler) => {
        submit = handler;
      },
    };
    const button = {
      disabled: false,
      setAttribute: () => {},
      removeAttribute: () => {},
    };
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
