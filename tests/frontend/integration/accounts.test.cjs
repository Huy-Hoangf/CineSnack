const test = require("node:test");
const assert = require("node:assert/strict");
const base = process.env.CINESNACK_LIVE_URL;

for (const [name, role] of [
  ["admin", "QUAN_TRI"],
  ["user", "KHACH_HANG"],
  ["staff", "NHAN_VIEN"],
]) {
  test(
    `Tài khoản SQL thật ${name}: đăng nhập, vai trò, phân quyền và logout`,
    { skip: !base },
    async () => {
      const jar = new Map();
      async function request(path, options = {}) {
        const response = await fetch(base + path, {
          ...options,
          redirect: "manual",
          headers: {
            ...options.headers,
            Cookie: [...jar].map(([k, v]) => `${k}=${v}`).join("; "),
          },
        });
        for (const cookie of response.headers.getSetCookie()) {
          const pair = cookie.split(";")[0],
            at = pair.indexOf("=");
          jar.set(pair.slice(0, at), pair.slice(at + 1));
        }
        return response;
      }
      let csrf = await (await request("/api/csrf")).json();
      const email = `${name}@cinesnack.local`;
      const login = await request("/api/auth/login", {
        method: "POST",
        headers: {
          [csrf.headerName]: csrf.token,
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
          username: email,
          password: "CineSnack@123",
        }),
      });
      assert.equal(login.status, 204);
      const me = await request("/api/auth/me");
      assert.equal(me.status, 200);
      const account = await me.json();
      assert.equal(account.email, email);
      assert.equal(account.role, role);
      assert.equal(Object.hasOwn(account, "password"), false);
      const admin = await request("/api/admin/orders");
      assert.equal(admin.status, name === "user" ? 403 : 200);
      csrf = await (await request("/api/csrf")).json();
      const cash = await request("/api/orders/DHNOTEXIST/payments/cash", {
        method: "POST",
        headers: {
          [csrf.headerName]: csrf.token,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ amountReceived: 100000 }),
      });
      assert.equal(cash.status, name === "user" ? 403 : 404);
      const logout = await request("/api/auth/logout", {
        method: "POST",
        headers: { [csrf.headerName]: csrf.token },
      });
      assert.equal(logout.status, 204);
      assert.equal((await request("/api/auth/me")).status, 401);
    },
  );
}
