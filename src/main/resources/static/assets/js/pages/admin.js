"use strict";
(function () {
  const C = CineSnack;
  const orders = [
    {
      id: "CS-26001",
      movie: "dune",
      seats: ["D1", "D2"],
      solo: 0,
      duo: 1,
      status: "paid",
      time: "19:30",
    },
    {
      id: "CS-26002",
      movie: "inside-out",
      seats: ["B1", "B2", "B3"],
      solo: 1,
      duo: 1,
      status: "paid",
      time: "16:30",
    },
    {
      id: "CS-26003",
      movie: "kung-fu-panda",
      seats: ["A1", "A2"],
      solo: 0,
      duo: 1,
      status: "pending",
      time: "13:15",
    },
    {
      id: "CS-26004",
      movie: "godzilla",
      seats: ["F9", "F10"],
      solo: 2,
      duo: 0,
      status: "paid",
      time: "21:45",
    },
    {
      id: "CS-26005",
      movie: "dune",
      seats: ["C1"],
      solo: 1,
      duo: 0,
      status: "cancelled",
      time: "10:00",
    },
    {
      id: "CS-26006",
      movie: "inside-out",
      seats: ["E1", "E2"],
      solo: 0,
      duo: 1,
      status: "paid",
      time: "19:30",
    },
  ];
  const statusLabels = {
    paid: "Đã thanh toán",
    pending: "Chờ thanh toán",
    cancelled: "Đã hủy",
  };
  const titles = {
    overview: [
      "Tổng quan rạp phim",
      "Một góc nhìn rõ ràng cho những trải nghiệm điện ảnh trọn vẹn.",
      "Tổng quan",
    ],
    movies: [
      "Danh mục phim",
      "Những câu chuyện trên màn ảnh CineSnack.",
      "Quản lý phim",
    ],
    shows: [
      "Lịch chiếu của rạp",
      "Theo dõi các suất chiếu trong không gian minh họa.",
      "Suất chiếu",
    ],
    orders: [
      "Đơn đặt vé",
      "Xem thông tin và trạng thái của những đơn vé mẫu.",
      "Đơn đặt vé",
    ],
  };
  const amount = (order) =>
    C.totals({
      seats: order.seats,
      combos: { solo: order.solo, duo: order.duo },
    }).total;
  const movieOf = (order) => C.movies.find((m) => m.id === order.movie);
  const badge = (order) =>
    `<span class="status-badge ${order.status}">${statusLabels[order.status]}</span>`;
  function orderRows(list) {
    return list.length
      ? list
          .map(
            (o) =>
              `<tr><td><strong>${o.id}</strong></td><td class="table-movie-name">${movieOf(o).title}</td><td>${o.seats.join(", ")}</td><td>${C.money(amount(o))}</td><td>${badge(o)}</td><td><button type="button" class="order-detail-button" data-order="${o.id}" aria-label="Chi tiết đơn ${o.id}">Chi tiết</button></td></tr>`,
          )
          .join("")
      : '<tr><td class="table-empty" colspan="6">Không có đơn phù hợp với bộ lọc.</td></tr>';
  }
  const paid = orders.filter((o) => o.status === "paid");
  const revenue = paid.reduce((sum, o) => sum + amount(o), 0);
  document.querySelector("#stats").innerHTML = [
    ["Doanh thu mẫu", C.money(revenue), "Tổng các đơn đã thanh toán", "₫"],
    [
      "Vé đã bán",
      paid.reduce((sum, o) => sum + o.seats.length, 0),
      "Ghế trong đơn đã thanh toán",
      "◇",
    ],
    ["Phim đang chiếu", 4, "Trong danh mục minh họa", "▤"],
    [
      "Đơn chờ thanh toán",
      orders.filter((o) => o.status === "pending").length,
      "Chỉ là trạng thái mẫu",
      "◷",
    ],
  ]
    .map(
      ([label, value, note, icon]) =>
        `<article class="stat-card"><span class="stat-label">${label}<span class="stat-symbol" aria-hidden="true">${icon}</span></span><strong>${value}</strong><p>${note}</p></article>`,
    )
    .join("");
  document.querySelector("#admin-date").textContent = new Intl.DateTimeFormat(
    "vi-VN",
    { weekday: "short", day: "2-digit", month: "2-digit", year: "numeric" },
  ).format(new Date());
  const history = [620000, 880000, 710000, 1060000, 830000, 960000, revenue];
  document.querySelector("#revenue-chart").innerHTML = history
    .map((value, i) => {
      const d = new Date();
      d.setDate(d.getDate() - 6 + i);
      return `<li><span class="chart-value" aria-label="${C.money(value)}">${Math.round(value / 1000)}k</span><div class="revenue-bar" aria-hidden="true" style="height:${Math.round((value / Math.max(...history)) * 145)}px"></div><span class="chart-date">${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}</span></li>`;
    })
    .join("");
  document.querySelector("#recent-orders").innerHTML = orderRows(
    orders.slice().reverse().slice(0, 4),
  );
  function renderMovies() {
    const query = C.normalize(
      document.querySelector("#admin-movie-search").value,
    );
    const list = C.movies.filter((m) =>
      C.normalize(m.title + " " + m.genre).includes(query),
    );
    document.querySelector("#movies-count").textContent = `${list.length} phim`;
    document.querySelector("#admin-movies").innerHTML = list.length
      ? list
          .map(
            (m) =>
              `<tr><td><div class="table-film"><img src="assets/images/movies/${m.image}" alt="" width="38" height="56"><div><strong>${m.title}</strong><small>${m.genre}</small></div></div></td><td>${m.duration} phút</td><td>${m.age}</td><td><span class="status-badge ${m.status}">${m.status === "now" ? "Đang chiếu" : "Sắp chiếu"}</span></td><td><a class="text-button" href="phim.html?movie=${m.id}">Xem phim ↗</a></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="5" class="table-empty">Không tìm thấy phim phù hợp.</td></tr>';
  }
  function renderShows() {
    const query = C.normalize(
      document.querySelector("#admin-show-search").value,
    );
    const shows = C.times
      .map((time, i) => ({ time, movie: C.movies[i % 4] }))
      .filter((s) => C.normalize(s.movie.title + " " + s.time).includes(query));
    document.querySelector("#shows-count").textContent =
      `${shows.length} suất chiếu`;
    document.querySelector("#admin-shows").innerHTML = shows.length
      ? shows
          .map(
            (s) =>
              `<tr><td class="table-movie-name">${s.movie.title}</td><td><strong>${s.time}</strong><br><span class="hint">${C.displayDate(C.dates()[0])}</span></td><td>Phòng 01 · 2D</td><td>${C.occupied(s.movie.id, C.dates()[0], s.time).size} / 60</td><td><span class="status-badge">Lịch mẫu</span></td></tr>`,
          )
          .join("")
      : '<tr><td class="table-empty" colspan="5">Không tìm thấy suất chiếu phù hợp.</td></tr>';
  }
  function renderOrders() {
    const query = C.normalize(
        document.querySelector("#admin-order-search").value,
      ),
      status = document.querySelector("#order-status").value;
    const list = orders.filter(
      (o) =>
        (status === "all" || o.status === status) &&
        C.normalize(o.id + " " + movieOf(o).title).includes(query),
    );
    document.querySelector("#orders-count").textContent =
      `${list.length} đơn vé`;
    document.querySelector("#admin-orders").innerHTML = orderRows(list);
  }
  document.querySelectorAll("[data-view]").forEach((button) =>
    button.addEventListener("click", () => {
      const view = button.dataset.view;
      document
        .querySelectorAll(".admin-view")
        .forEach((panel) => (panel.hidden = panel.id !== "view-" + view));
      document.querySelectorAll(".admin-nav-button").forEach((b) => {
        const active = b.dataset.view === view;
        b.classList.toggle("active", active);
        b.setAttribute("aria-pressed", String(active));
      });
      document.querySelector("#admin-title").textContent = titles[view][0];
      document.querySelector("#admin-subtitle").textContent = titles[view][1];
      document.querySelector("#admin-breadcrumb").textContent = titles[view][2];
      if (!button.classList.contains("admin-nav-button")) {
        document.querySelector("#noi-dung").focus();
        document.querySelector("#noi-dung").scrollIntoView();
      }
    }),
  );
  document
    .querySelector("#admin-movie-search")
    .addEventListener("input", renderMovies);
  document
    .querySelector("#admin-show-search")
    .addEventListener("input", renderShows);
  document
    .querySelector("#admin-order-search")
    .addEventListener("input", renderOrders);
  document
    .querySelector("#order-status")
    .addEventListener("change", renderOrders);
  document.querySelector(".admin-main").addEventListener("click", (event) => {
    const button = event.target.closest("[data-order]");
    if (!button) return;
    const order = orders.find((o) => o.id === button.dataset.order);
    document.querySelector("#order-detail-title").textContent =
      "Đơn " + order.id;
    document.querySelector("#order-detail").innerHTML = [
      ["Phim", movieOf(order).title],
      ["Suất chiếu", order.time + " · " + C.displayDate(C.dates()[0])],
      ["Ghế", order.seats.join(", ")],
      [
        "Bắp & nước",
        C.combos
          .filter((c) => order[c.id])
          .map((c) => order[c.id] + " × " + c.name)
          .join(", ") || "Không chọn",
      ],
      ["Tổng tiền", C.money(amount(order))],
      ["Trạng thái", statusLabels[order.status]],
    ]
      .map(([label, value]) => `<div><dt>${label}</dt><dd>${value}</dd></div>`)
      .join("");
    document.querySelector("#order-dialog").showModal();
  });
  renderMovies();
  renderShows();
  renderOrders();
})();
