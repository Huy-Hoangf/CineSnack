"use strict";
// Chỉ là máy chủ thử nghiệm lưu trong RAM. Không dùng làm backend sản phẩm.
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { randomBytes } = require("node:crypto");

function taoMayChu() {
  let hienTai = Date.now();
  let soDon = 0,
    soThanhToan = 0,
    loiHoaDon = false;
  const orders = new Map(),
    phien = new Map();
  const catalog = {
    movie: {
      id: 1,
      name: "Chuyến phiêu lưu CineSnack",
      status: "DANG_CHIEU",
      duration: 100,
      genre: "Phiêu lưu",
      ageRating: "T13",
      posterUrl: "../assets/images/movies/dune.jpg",
    },
    showTime: {
      id: 101,
      movieId: 1,
      movieName: "Chuyến phiêu lưu CineSnack",
      roomId: 1,
      roomName: "Phòng 01",
      startsAt: new Date(hienTai + 3600000).toISOString(),
      endsAt: new Date(hienTai + 9600000).toISOString(),
      price: 80000,
      status: "SAP_CHIEU",
    },
    seats: Array.from({ length: 12 }, (_, i) => ({
      id: i + 11,
      code: `A${i + 1}`,
      roomId: 1,
      status: "HOAT_DONG",
    })),
    combos: [{ id: 21, name: "Combo Một Mình Vui", price: 59000, status: "HOAT_DONG" }],
    products: [{ id: 31, name: "Nước ngọt", price: 30000, status: "HOAT_DONG" }],
  };
  const taiKhoan = [
    {
      id: 1,
      email: "khach@example.test",
      name: "Khách mô phỏng",
      role: "KHACH_HANG",
    },
    {
      id: 2,
      email: "nhanvien@example.test",
      name: "Nhân viên mô phỏng",
      role: "NHAN_VIEN",
    },
    {
      id: 3,
      email: "khac@example.test",
      name: "Khách khác",
      role: "KHACH_HANG",
    },
    { id: 4, email: "admin@example.test", name: "Quản trị mô phỏng", role: "QUAN_TRI" },
  ];
  const movies = new Map([[catalog.movie.id, catalog.movie]]);
  const shows = new Map([[catalog.showTime.id, catalog.showTime]]);
  const rooms = [{ id: 1, name: "Phòng 01", type: "2D", status: "HOAT_DONG" }];
  let movieId = 1,
    showId = 101;
  const tokenMoi = () => randomBytes(24).toString("hex");
  function loi(status, message) {
    throw Object.assign(new Error(message), { status });
  }
  function doiTrangThai(ct, trangThai) {
    ct.order.status = trangThai;
    for (const hd of ct.invoices) hd.status = trangThai;
    for (const ve of ct.tickets) ve.status = trangThai;
    for (const dong of ct.snacks)
      dong.status = trangThai === "DA_THANH_TOAN" ? "CHO_NHAN" : trangThai;
  }
  function xuLyHetHan() {
    for (const ct of orders.values()) {
      if (ct.order.status === "CHO_THANH_TOAN" && Date.parse(ct.order.expiresAt) <= hienTai)
        doiTrangThai(ct, "HET_HAN");
    }
  }
  function gheDaGiu(id) {
    return [...orders.values()].some((ct) =>
      ct.tickets.some(
        (ve) =>
          ve.seatId === id && ["CHO_THANH_TOAN", "DA_THANH_TOAN", "DA_SOAT"].includes(ve.status),
      ),
    );
  }
  function kiemTraDon(body) {
    if (
      !body ||
      typeof body.email !== "string" ||
      body.email.length > 150 ||
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email)
    )
      loi(400, "Email không hợp lệ.");
    for (const [key, max] of [
      ["seatIds", 10],
      ["snacks", 20],
      ["combos", 20],
    ]) {
      if (!Array.isArray(body[key]) || body[key].length > max)
        loi(400, "Danh sách lựa chọn không hợp lệ.");
    }
    if (!body.seatIds.length && !body.snacks.length && !body.combos.length)
      loi(400, "Đơn hàng đang trống.");
    if (body.seatIds.length ? !Number.isInteger(body.showTimeId) : body.showTimeId != null)
      loi(400, "Suất và ghế phải được chọn cùng nhau.");
    if (new Set(body.seatIds).size !== body.seatIds.length) loi(400, "Ghế bị trùng.");
    if (body.seatIds.length) {
      if (
        body.showTimeId !== catalog.showTime.id ||
        catalog.showTime.status !== "SAP_CHIEU" ||
        Date.parse(catalog.showTime.startsAt) <= hienTai
      )
        loi(409, "Suất chiếu đã bắt đầu hoặc không nhận đặt vé.");
      for (const id of body.seatIds) {
        const ghe = catalog.seats.find((g) => g.id === id);
        if (!ghe || ghe.roomId !== catalog.showTime.roomId || ghe.status !== "HOAT_DONG")
          loi(400, "Ghế không hợp lệ hoặc sai phòng.");
        if (gheDaGiu(id)) loi(409, "Ghế đã được người khác giữ hoặc mua.");
      }
    }
    const dsDong = [];
    for (const [key, idKey, dsCatalog] of [
      ["snacks", "productId", catalog.products],
      ["combos", "comboId", catalog.combos],
    ]) {
      const dsId = new Set();
      for (const chon of body[key]) {
        if (
          !chon ||
          !Number.isInteger(chon[idKey]) ||
          chon[idKey] <= 0 ||
          dsId.has(chon[idKey]) ||
          !Number.isInteger(chon.quantity) ||
          chon.quantity < 1 ||
          chon.quantity > 10
        )
          loi(400, "Sản phẩm/combo hoặc số lượng không hợp lệ.");
        dsId.add(chon[idKey]);
        const hang = dsCatalog.find((g) => g.id === chon[idKey]);
        if (!hang || hang.status !== "HOAT_DONG") loi(409, "Sản phẩm hoặc combo không còn bán.");
        dsDong.push({
          productId: key === "snacks" ? hang.id : null,
          comboId: key === "combos" ? hang.id : null,
          name: hang.name,
          quantity: chon.quantity,
          unitPrice: hang.price,
          status: "CHO_THANH_TOAN",
        });
      }
    }
    return dsDong;
  }
  function taoDon(body, nguoiDung, guestSession) {
    const dsDong = kiemTraDon(body);
    const tienVe = body.seatIds.length * catalog.showTime.price;
    const tienHang = dsDong.reduce((a, g) => a + g.quantity * g.unitPrice, 0);
    if (tienVe + tienHang > 9999999999) loi(400, "Tổng tiền vượt giới hạn.");
    const id = ++soDon,
      code = `DHMP${String(id).padStart(4, "0")}`;
    const ct = {
      order: {
        id,
        userId: nguoiDung?.id ?? null,
        email: body.email,
        code,
        total: tienVe + tienHang,
        status: "CHO_THANH_TOAN",
        createdAt: new Date(hienTai).toISOString(),
        expiresAt: new Date(
          Math.min(
            hienTai + 15 * 60000,
            body.seatIds.length ? Date.parse(catalog.showTime.startsAt) : Infinity,
          ),
        ).toISOString(),
      },
      tickets: body.seatIds.map((seatId, i) => ({
        id: id * 100 + i,
        showTimeId: body.showTimeId,
        seatId,
        seatCode: catalog.seats.find((g) => g.id === seatId).code,
        movieName: catalog.movie.name,
        roomName: catalog.showTime.roomName,
        startsAt: catalog.showTime.startsAt,
        code: `VEMP${id}${i}`,
        name: `${catalog.movie.name} | ${catalog.showTime.roomName} | ${catalog.seats.find((g) => g.id === seatId).code}`,
        quantity: 1,
        unitPrice: catalog.showTime.price,
        status: "CHO_THANH_TOAN",
      })),
      snacks: dsDong.map((dong, i) => ({ id: id * 100 + i, ...dong })),
      invoices: [],
      payments: [],
      guestSession,
    };
    for (const [type, total, lines] of [
      ["VE", tienVe, ct.tickets],
      ["BONG_NUOC", tienHang, ct.snacks],
    ]) {
      if (lines.length)
        ct.invoices.push({
          code: `${type === "VE" ? "HDVMP" : "HDBMP"}${id}`,
          type,
          orderId: id,
          total,
          email: body.email,
          status: "CHO_THANH_TOAN",
          issuedAt: null,
          lines: lines.map((l) => ({
            description: l.name,
            quantity: l.quantity,
            unitPrice: l.unitPrice,
          })),
        });
    }
    orders.set(code, ct);
    return ct.order;
  }
  function traTien(ct, body) {
    const tienDua = body?.amountReceived;
    if (!Number.isSafeInteger(tienDua) || tienDua < ct.order.total || tienDua > 999999999999)
      loi(400, "Số tiền khách đưa phải là số nguyên và đủ tổng tiền.");
    if (ct.order.status === "DA_THANH_TOAN")
      return { payment: ct.payments[0], change: tienDua - ct.order.total };
    if (ct.order.status !== "CHO_THANH_TOAN") loi(409, "Đơn không còn được thanh toán.");
    const banCu = structuredClone(ct);
    try {
      const payment = {
        id: ++soThanhToan,
        orderId: ct.order.id,
        code: `TMMP${soThanhToan}`,
        provider: "TAI_QUAY",
        method: "TIEN_MAT",
        amount: ct.order.total,
        status: "THANH_CONG",
        paidAt: new Date(hienTai).toISOString(),
      };
      ct.payments.push(payment);
      doiTrangThai(ct, "DA_THANH_TOAN");
      for (const hd of ct.invoices) hd.issuedAt = new Date(hienTai).toISOString();
      if (loiHoaDon) {
        loiHoaDon = false;
        throw new Error("Lỗi phát hành được tiêm trong kiểm thử.");
      }
      return { payment, change: tienDua - ct.order.total };
    } catch (error) {
      orders.set(ct.order.code, banCu);
      throw error;
    }
  }
  const server = http.createServer(async (req, res) => {
    const gui = (status, data) => {
      res.writeHead(status, {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": "no-store",
      });
      res.end(status === 204 ? undefined : JSON.stringify(data));
    };
    try {
      const url = new URL(req.url, "http://127.0.0.1");
      if (!url.pathname.startsWith("/api/")) {
        const goc = path.resolve(__dirname, "../../../src/main/resources/UI");
        const file = path.resolve(
          goc,
          "." + (url.pathname === "/" ? "/html/index.html" : decodeURIComponent(url.pathname)),
        );
        if (!file.startsWith(goc + path.sep) || req.method !== "GET")
          return gui(404, { message: "Không tìm thấy." });
        const mime = {
          ".html": "text/html",
          ".js": "text/javascript",
          ".css": "text/css",
          ".svg": "image/svg+xml",
          ".jpg": "image/jpeg",
          ".png": "image/png",
          ".webp": "image/webp",
        }[path.extname(file)];
        if (!mime || !fs.existsSync(file)) return gui(404, { message: "Không tìm thấy." });
        res.writeHead(200, {
          "Content-Type": mime + "; charset=utf-8",
          "Cache-Control": "no-store",
        });
        return res.end(fs.readFileSync(file));
      }
      const idPhien = /(?:^|;\s*)SIMSESSION=([a-f0-9]+)/.exec(req.headers.cookie || "")?.[1];
      let session = phien.get(idPhien);
      if (url.pathname === "/api/csrf" && req.method === "GET") {
        if (!session) {
          const id = tokenMoi();
          session = { token: tokenMoi(), user: null };
          phien.set(id, session);
          res.setHeader("Set-Cookie", `SIMSESSION=${id}; HttpOnly; SameSite=Strict; Path=/`);
        }
        return gui(200, { headerName: "X-CSRF-TOKEN", token: session.token });
      }
      if (
        ["POST", "PUT", "DELETE"].includes(req.method) &&
        (!session || req.headers["x-csrf-token"] !== session.token)
      )
        loi(403, "Thiếu hoặc sai token CSRF.");
      let body;
      if (["POST", "PUT"].includes(req.method)) {
        let noiDung = "";
        for await (const chunk of req) {
          noiDung += chunk;
          if (Buffer.byteLength(noiDung) > 16384) loi(413, "Dữ liệu quá lớn.");
        }
        try {
          body = req.headers["content-type"]?.startsWith("application/x-www-form-urlencoded")
            ? Object.fromEntries(new URLSearchParams(noiDung))
            : JSON.parse(noiDung || "{}");
        } catch {
          loi(400, "Dữ liệu không hợp lệ.");
        }
      }
      if (url.pathname === "/api/auth/login" && req.method === "POST") {
        const user = taiKhoan.find((u) => u.email === body?.username);
        if (!user || body.password !== "MoPhong123!")
          loi(401, "Email hoặc mật khẩu mô phỏng không đúng.");
        phien.delete(idPhien);
        const id = tokenMoi();
        Object.assign(session, { token: tokenMoi(), user });
        phien.set(id, session);
        res.setHeader("Set-Cookie", `SIMSESSION=${id}; HttpOnly; SameSite=Strict; Path=/`);
        return gui(204);
      }
      xuLyHetHan();
      if (url.pathname === "/api/guest/orders" && req.method === "GET" && !session)
        return gui(200, []);
      if (req.method === "GET") {
        if (url.pathname === "/api/movies")
          return gui(
            200,
            [...movies.values()].filter(
              (m) => !url.searchParams.get("status") || m.status === url.searchParams.get("status"),
            ),
          );
        const movieMatch = /^\/api\/movies\/(\d+)$/.exec(url.pathname);
        if (movieMatch) {
          const movie = movies.get(Number(movieMatch[1]));
          if (!movie) loi(404, "Không tìm thấy phim.");
          return gui(200, movie);
        }
        if (url.pathname === "/api/showtimes")
          return gui(
            200,
            [...shows.values()].filter(
              (s) =>
                Date.parse(s.startsAt) > hienTai &&
                s.status === "SAP_CHIEU" &&
                movies.get(s.movieId)?.status === "DANG_CHIEU" &&
                (!url.searchParams.has("movieId") ||
                  Number(url.searchParams.get("movieId")) === s.movieId),
            ),
          );
        if (url.pathname === "/api/showtimes/101/seats")
          return gui(
            200,
            catalog.seats.map((g) => ({
              ...g,
              price: catalog.showTime.price,
              available: !gheDaGiu(g.id) && Date.parse(catalog.showTime.startsAt) > hienTai,
            })),
          );
        if (url.pathname === "/api/snacks")
          return gui(200, {
            products: catalog.products,
            combos: catalog.combos,
          });
      }
      if (url.pathname === "/api/guest/orders" && session) {
        if (req.method === "POST") return gui(201, taoDon(body, null, session));
        if (req.method === "GET") {
          const page = Number(url.searchParams.get("page") || 0),
            size = Number(url.searchParams.get("size") || 20);
          return gui(
            200,
            [...orders.values()]
              .filter((c) => c.guestSession === session)
              .map((c) => c.order)
              .sort((a, b) => b.id - a.id)
              .slice(page * size, (page + 1) * size),
          );
        }
      }
      const guestMatch = /^\/api\/guest\/orders\/([A-Za-z0-9]+)(?:\/(cancel|invoices))?$/.exec(
        url.pathname,
      );
      if (guestMatch) {
        const ct = orders.get(guestMatch[1]);
        if (!session || !ct || ct.guestSession !== session)
          loi(403, "Không có quyền xem đơn của phiên khách khác.");
        if (!guestMatch[2] && req.method === "GET")
          return gui(200, { order: ct.order, tickets: ct.tickets, snacks: ct.snacks });
        if (guestMatch[2] === "invoices" && req.method === "GET") {
          if (ct.order.status !== "DA_THANH_TOAN") loi(404, "Chưa có hóa đơn.");
          return gui(200, ct.invoices);
        }
        if (guestMatch[2] === "cancel" && req.method === "POST") {
          if (ct.order.status === "DA_THANH_TOAN") loi(409, "Không hủy đơn đã thanh toán.");
          if (ct.order.status === "CHO_THANH_TOAN") doiTrangThai(ct, "HUY");
          return gui(200, ct.order);
        }
        loi(405, "Phương thức không được hỗ trợ.");
      }
      const user = session?.user;
      if (!user) loi(401, "Bạn cần đăng nhập.");
      if (url.pathname === "/api/auth/me" && req.method === "GET") return gui(200, user);
      if (url.pathname === "/api/auth/logout" && req.method === "POST") {
        phien.delete(idPhien);
        res.setHeader("Set-Cookie", "SIMSESSION=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
        return gui(204);
      }
      const laNhanVien = ["NHAN_VIEN", "QUAN_TRI"].includes(user.role);
      const adminCatalog = /^\/api\/admin\/(movies|showtimes|rooms)(?:\/(\d+))?$/.exec(
        url.pathname,
      );
      if (adminCatalog) {
        if (user.role !== "QUAN_TRI") loi(403, "Chỉ quản trị được sửa danh mục.");
        const [, kind, rawId] = adminCatalog;
        if (kind === "rooms") {
          if (req.method !== "GET") loi(405, "Phòng chỉ được xem.");
          return gui(200, rooms);
        }
        const records = kind === "movies" ? movies : shows;
        if (req.method === "GET")
          return gui(200, rawId ? records.get(Number(rawId)) : [...records.values()]);
        if (req.method === "DELETE") {
          if (!records.has(Number(rawId))) loi(404, "Không tìm thấy.");
          const id = Number(rawId);
          const relatedShows =
            kind === "movies"
              ? [...shows.values()].filter((s) => s.movieId === id).map((s) => s.id)
              : [id];
          const relatedOrders = [...orders.values()].filter((c) =>
            c.tickets.some((v) => relatedShows.includes(v.showTimeId)),
          );
          if (kind === "showtimes" && relatedOrders.length)
            loi(409, "Suất chiếu đã có lịch sử vé.");
          if (kind === "movies" && relatedOrders.some((c) => c.order.status === "CHO_THANH_TOAN"))
            loi(409, "Phim còn vé chờ thanh toán.");
          if (kind === "movies" && relatedShows.length) records.get(id).status = "NGUNG_CHIEU";
          else records.delete(id);
          return gui(204);
        }
        if (!["POST", "PUT"].includes(req.method)) loi(405, "Phương thức không hỗ trợ.");
        if (
          kind === "movies" &&
          (!body.name?.trim() || !Number.isInteger(body.duration) || body.duration < 1)
        )
          loi(400, "Phim không hợp lệ.");
        if (
          kind === "showtimes" &&
          (!movies.has(body.movieId) ||
            body.roomId !== 1 ||
            !Number.isFinite(Date.parse(body.startsAt)) ||
            !Number.isFinite(body.price))
        )
          loi(400, "Suất không hợp lệ.");
        const id = rawId ? Number(rawId) : kind === "movies" ? ++movieId : ++showId;
        if (req.method === "PUT" && !records.has(id)) loi(404, "Không tìm thấy.");
        const value = { ...body, id };
        if (kind === "showtimes")
          Object.assign(value, {
            roomName: rooms[0].name,
            movieName: movies.get(body.movieId).name,
            endsAt: new Date(
              Date.parse(body.startsAt) + movies.get(body.movieId).duration * 60000,
            ).toISOString(),
          });
        records.set(id, value);
        return gui(req.method === "POST" ? 201 : 200, value);
      }
      if (url.pathname === "/api/orders") {
        if (req.method === "POST") return gui(201, taoDon(body, user));
        if (req.method === "GET")
          return gui(
            200,
            [...orders.values()].filter((ct) => ct.order.userId === user.id).map((ct) => ct.order),
          );
      }
      if (url.pathname === "/api/admin/orders" && req.method === "GET") {
        if (!laNhanVien) loi(403, "Chỉ nhân viên được xem danh sách vận hành.");
        const query = (url.searchParams.get("q") || "").toLowerCase();
        const state = url.searchParams.get("status") || "";
        const size = Number(url.searchParams.get("size") || 20),
          page = Number(url.searchParams.get("page") || 0);
        return gui(
          200,
          [...orders.values()]
            .map((ct) => ct.order)
            .filter(
              (d) =>
                (!state || d.status === state) &&
                (d.code + " " + d.email).toLowerCase().includes(query),
            )
            .sort((a, b) => b.id - a.id)
            .slice(page * size, (page + 1) * size),
        );
      }
      const match = /^\/api\/orders\/([A-Za-z0-9]+)(?:\/(cancel|invoices|payments\/cash))?$/.exec(
        url.pathname,
      );
      if (!match) loi(404, "Không tìm thấy API.");
      const ct = orders.get(match[1]);
      if (!ct) loi(404, "Không tìm thấy đơn.");
      if (!laNhanVien && ct.order.userId !== user.id)
        loi(403, "Bạn không có quyền truy cập đơn này.");
      if (!match[2] && req.method === "GET")
        return gui(200, {
          order: ct.order,
          tickets: ct.tickets,
          snacks: ct.snacks,
        });
      if (match[2] === "invoices" && req.method === "GET") {
        if (ct.order.status !== "DA_THANH_TOAN") loi(404, "Đơn chưa có hóa đơn đã thanh toán.");
        return gui(200, ct.invoices);
      }
      if (match[2] === "cancel" && req.method === "POST") {
        if (ct.order.status === "DA_THANH_TOAN") loi(409, "Không hủy đơn đã thanh toán.");
        if (ct.order.status === "CHO_THANH_TOAN") doiTrangThai(ct, "HUY");
        return gui(200, ct.order);
      }
      if (match[2] === "payments/cash" && req.method === "POST") {
        if (!laNhanVien) loi(403, "Chỉ nhân viên được nhận tiền mặt.");
        return gui(200, traTien(ct, body));
      }
      loi(405, "Phương thức không được hỗ trợ.");
    } catch (error) {
      gui(error.status || 500, {
        message: error.status
          ? error.message
          : "Lỗi mô phỏng; thay đổi đã được hoàn tác trong RAM.",
      });
    }
  });
  return {
    server,
    orders,
    catalog,
    advance(ms) {
      hienTai += ms;
    },
    failNextInvoice() {
      loiHoaDon = true;
    },
  };
}
module.exports = { taoMayChu };
if (require.main === module) {
  const moPhong = taoMayChu();
  moPhong.server.listen(Number(process.env.SIM_PORT || 18766), "127.0.0.1", () =>
    console.log(`CineSnack FE fixture: http://127.0.0.1:${moPhong.server.address().port}`),
  );
}
