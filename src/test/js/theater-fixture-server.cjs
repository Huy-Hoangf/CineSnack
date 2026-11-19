// Browser QA fixture: fake accounts/orders kept in memory; never connects to SQL Server.
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../../main/resources/UI");
let rooms = [
  {
    id: 1,
    name: "Phòng thử nghiệm 1",
    type: "2D",
    status: "HOAT_DONG",
    seatCount: 6,
  },
];
const layouts = new Map([
  [
    1,
    ["A1", "A2", "A3", "B1", "B2", "B3"].map((code, i) => ({
      id: i + 1,
      code,
      type: i === 4 ? "VIP" : "THUONG",
      status: "HOAT_DONG",
    })),
  ],
]);
const order = {
  id: 1,
  code: "DEMO1",
  email: "demo@example.invalid",
  total: 299000,
  status: "DA_THANH_TOAN",
  createdAt: "2026-11-03T17:00:00+07:00",
  expiresAt: null,
};
const tickets = ["A1", "A2", "A3"].map((seatCode, i) => ({
  id: i + 1,
  seatId: i + 1,
  seatCode,
  code: "VEDEMO" + (i + 1),
  movieName: "Phim thử nghiệm",
  roomName: "Phòng thử nghiệm 1",
  startsAt: "2026-11-03T19:00:00+07:00",
  unitPrice: 80000,
  status: "DA_THANH_TOAN",
}));
const checked = new Set();
const printed = new Set();
const send = (res, status, value) => {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(value));
};
http
  .createServer(async (req, res) => {
    try {
      const url = new URL(req.url, "http://localhost");
      if (!url.pathname.startsWith("/api/")) {
        const file = path.resolve(root, "." + decodeURIComponent(url.pathname));
        if (
          !file.startsWith(root + path.sep) ||
          !fs.existsSync(file) ||
          !fs.statSync(file).isFile()
        ) {
          res.writeHead(404);
          res.end();
          return;
        }
        res.writeHead(200, {
          "Content-Type":
            {
              ".html": "text/html",
              ".js": "text/javascript",
              ".css": "text/css",
              ".svg": "image/svg+xml",
            }[path.extname(file)] || "application/octet-stream",
        });
        fs.createReadStream(file).pipe(res);
        return;
      }
      let body = "";
      for await (const chunk of req) body += chunk;
      const input = body ? JSON.parse(body) : {};
      const route = url.pathname;
      if (route === "/api/auth/me")
        return send(
          res,
          200,
          (req.headers.referer || "").includes("orders.html")
            ? null
            : { id: 1, email: "admin@example.invalid", role: "QUAN_TRI" },
        );
      if (route === "/api/csrf")
        return send(res, 200, {
          headerName: "X-CSRF-TOKEN",
          token: "fixture-only",
        });
      if (route === "/api/admin/rooms/management") return send(res, 200, rooms);
      if (route === "/api/admin/rooms" && req.method === "POST") {
        const id = rooms.length + 1;
        rooms.unshift({ ...input, id, seatCount: 0 });
        layouts.set(id, []);
        return send(res, 201, { id });
      }
      const setup = /^\/api\/admin\/rooms\/(\d+)\/seats\/setup$/.exec(route);
      if (setup) {
        const id = Number(setup[1]),
          seats = [];
        if (layouts.get(id).length)
          return send(res, 409, { message: "Phòng đã có sơ đồ ghế." });
        for (let r = 0; r < input.rows; r++)
          for (let c = 1; c <= input.columns; c++)
            seats.push({
              id: seats.length + 100,
              code: String.fromCharCode(65 + r) + c,
              type: input.type,
              status: "HOAT_DONG",
            });
        layouts.set(id, seats);
        rooms.find((r) => r.id === id).seatCount = seats.length;
        return send(res, 201, seats);
      }
      const seatUpdate = /^\/api\/admin\/rooms\/(\d+)\/seats\/(\d+)$/.exec(
        route,
      );
      if (seatUpdate) {
        const seats = layouts.get(Number(seatUpdate[1]));
        const index = seats.findIndex((s) => s.id === Number(seatUpdate[2]));
        seats[index] = { ...seats[index], ...input };
        return send(res, 200, seats[index]);
      }
      const seatList = /^\/api\/admin\/rooms\/(\d+)\/seats$/.exec(route);
      if (seatList)
        return send(res, 200, layouts.get(Number(seatList[1])) || []);
      const roomUpdate = /^\/api\/admin\/rooms\/(\d+)$/.exec(route);
      if (roomUpdate) {
        const room = rooms.find((r) => r.id === Number(roomUpdate[1]));
        Object.assign(room, input);
        return send(res, 200, { id: room.id });
      }
      if (route === "/api/staff/tickets/issue") {
        const ticket = tickets.find((t) => t.code === input.code);
        if (!ticket || printed.has(ticket.code) || checked.has(ticket.code))
          return send(res, 409, {
            message: "Vé không hợp lệ, đã in hoặc đã soát.",
          });
        printed.add(ticket.code);
        return send(res, 200, {
          ticket,
          staffId: 1,
          printedAt: "2026-11-03T17:15:00+07:00",
        });
      }
      if (route === "/api/staff/tickets/scan") {
        const ticket = tickets.find((t) => t.code === input.code);
        if (!ticket || checked.has(ticket.code))
          return send(res, 409, { message: "Vé không hợp lệ hoặc đã soát." });
        checked.add(ticket.code);
        return send(res, 200, {
          ticket,
          staffId: 1,
          checkedAt: "2026-11-03T18:45:00+07:00",
        });
      }
      if (route === "/api/guest/orders") return send(res, 200, [order]);
      if (route === "/api/guest/orders/DEMO1")
        return send(res, 200, {
          order,
          tickets,
          snacks: [{ name: "Combo thử nghiệm", quantity: 1, unitPrice: 59000 }],
        });
      if (route === "/api/guest/orders/DEMO1/invoices")
        return send(res, 200, [
          {
            id: 1,
            code: "HDVDEMO1",
            type: "VE",
            total: 240000,
            issuedAt: "2026-11-03T17:01:00+07:00",
            lines: tickets.map((t) => ({
              quantity: 1,
              description: t.movieName + " · Ghế " + t.seatCode,
              unitPrice: t.unitPrice,
            })),
          },
          {
            id: 2,
            code: "HDBDEMO1",
            type: "BONG_NUOC",
            total: 59000,
            issuedAt: "2026-11-03T17:01:00+07:00",
            lines: [
              {
                quantity: 1,
                description: "Combo thử nghiệm",
                unitPrice: 59000,
              },
            ],
          },
        ]);
      send(res, 404, { message: "Fixture route not found" });
    } catch (err) {
      send(res, 500, { message: err.message });
    }
  })
  .listen(8091, "127.0.0.1", () =>
    console.log(
      "Browser QA fixture: http://localhost:8091/html/theater.html (in-memory data only)",
    ),
  );
