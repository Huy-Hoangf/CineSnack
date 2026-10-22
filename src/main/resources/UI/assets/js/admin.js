"use strict";
(async () => {
  const o = (id) => document.getElementById(id),
    e = CineSnackOrders.e;
  const nhan = {
    ...CineSnackOrders.nhan,
    DANG_CHIEU: "Đang chiếu",
    SAP_CHIEU: "Sắp chiếu",
    NGUNG_CHIEU: "Ngừng chiếu",
    DA_CHIEU: "Đã chiếu",
  };
  let user,
    section = "don",
    page = 0,
    moviePage = 0,
    showPage = 0,
    listGeneration = 0,
    detailGeneration = 0;
  let movies = [],
    shows = [],
    rooms = [],
    movieOptions = [];
  const isAdmin = () => user?.role === "QUAN_TRI";
  function error(err) {
    o("admin-loi").textContent = err.message;
    o("admin-loi").focus();
  }
  function notice(text) {
    o("admin-status").textContent = text;
    o("admin-status").hidden = !text;
  }
  function button(text, action, attrs = {}) {
    const b = e("button", text, "account-button");
    b.type = "button";
    Object.assign(b.dataset, attrs);
    b.addEventListener("click", async () => {
      if (b.disabled) return;
      b.disabled = true;
      try {
        await action();
      } catch (err) {
        error(err);
      } finally {
        b.disabled = false;
      }
    });
    return b;
  }
  function cell(row, text) {
    const td = e("td", text);
    row.append(td);
    return td;
  }
  function badge(text, paid = false) {
    return e("span", text, "workspace-badge" + (paid ? " paid" : ""));
  }
  async function showOrder(code) {
    const generation = ++detailGeneration;
    o("admin-loi").textContent = "";
    await CineSnackOrders.hienChiTiet(
      o("don-chi-tiet"),
      code,
      user,
      loadOrders,
      () => generation === detailGeneration,
    );
    if (generation !== detailGeneration) return;
    o("don-chi-tiet-tieu-de").textContent = "Đơn " + code;
    if (!o("don-hop").open) o("don-hop").showModal();
  }
  o("don-hop").addEventListener("close", () => detailGeneration++);
  async function loadOrders() {
    const generation = ++listGeneration;
    const q = o("quan-tri-don-tim").value.trim(),
      status = o("don-trang-thai").value;
    const ds = await CineSnackApi.goi(
      `/api/admin/orders?page=${page}&size=20&q=${encodeURIComponent(q)}&status=${encodeURIComponent(status)}`,
    );
    if (generation !== listGeneration) return;
    const tbody = o("quan-tri-don");
    tbody.replaceChildren();
    if (!ds.length) {
      const row = e("tr");
      cell(row, "Không có đơn phù hợp trên trang này.").colSpan = 5;
      tbody.append(row);
    }
    for (const d of ds) {
      const row = e("tr"),
        first = cell(row);
      first.append(
        e("strong", d.code),
        e("small", d.email),
        badge(d.userId == null ? "Guest" : "User"),
      );
      cell(row, CineSnack.dinhDangSuat(d.createdAt));
      cell(row, CineSnack.dinhDangTien(d.total));
      cell(row).append(badge(nhan[d.status] || d.status, d.status === "DA_THANH_TOAN"));
      cell(row).append(
        button(d.status === "DA_THANH_TOAN" ? "Hóa đơn" : "Chi tiết", () => showOrder(d.code), {
          don: d.code,
        }),
      );
      tbody.append(row);
    }
    o("don-so").textContent =
      `Trang ${page + 1} · ${ds.length} đơn phù hợp. Tìm kiếm áp dụng toàn rạp.`;
    o("admin-truoc").disabled = page === 0;
    o("admin-sau").disabled = ds.length < 20;
    o("thong-ke").replaceChildren();
    for (const [label, count] of [
      ["Đơn trên trang", ds.length],
      ["Chờ thanh toán", ds.filter((d) => d.status === "CHO_THANH_TOAN").length],
      ["Đã thanh toán", ds.filter((d) => d.status === "DA_THANH_TOAN").length],
    ]) {
      const card = e("article", undefined, "workspace-stat");
      card.append(e("span", label), e("strong", String(count)));
      o("thong-ke").append(card);
    }
  }
  async function loadMovies() {
    movies = await CineSnackApi.goi(`/api/admin/movies?page=${moviePage}&size=100`);
    o("quan-tri-phim").replaceChildren();
    for (const p of movies) {
      const row = e("tr"),
        first = cell(row);
      first.append(e("strong", p.name), e("small", p.genre));
      cell(row, p.duration + " phút");
      cell(row, p.ageRating);
      cell(row).append(badge(nhan[p.status] || p.status, p.status === "DANG_CHIEU"));
      const actions = e("div", undefined, "workspace-row-actions");
      actions.append(
        button("Sửa", () => editMovie(p), { editMovie: p.id }),
        button(
          "Xóa",
          () =>
            confirmDelete(
              `Xóa phim “${p.name}”? Phim có lịch được chuyển sang ngừng chiếu; dữ liệu vé và hóa đơn được giữ.`,
              async () => {
                await CineSnackApi.goi(`/api/admin/movies/${p.id}`, { method: "DELETE" });
                await loadMovies();
                notice("Đã cập nhật danh mục phim.");
              },
            ),
          { deleteMovie: p.id },
        ),
      );
      cell(row).append(actions);
      o("quan-tri-phim").append(row);
    }
    o("dem-phim").textContent = `Trang ${moviePage + 1} · ${movies.length} phim`;
    o("phim-truoc").disabled = moviePage === 0;
    o("phim-sau").disabled = movies.length < 100;
  }
  async function loadShows() {
    shows = await CineSnackApi.goi(`/api/admin/showtimes?page=${showPage}&size=100`);
    o("quan-tri-suat").replaceChildren();
    for (const s of shows) {
      const row = e("tr"),
        first = cell(row);
      first.append(e("strong", s.movieName), e("small", s.roomName));
      const time = cell(row);
      time.append(
        e("strong", CineSnack.dinhDangSuat(s.startsAt)),
        e("small", "Đến " + CineSnack.dinhDangSuat(s.endsAt)),
      );
      cell(row, CineSnack.dinhDangTien(s.price));
      cell(row).append(badge(nhan[s.status] || s.status));
      const actions = e("div", undefined, "workspace-row-actions");
      actions.append(
        button("Sửa", () => editShow(s), { editShow: s.id }),
        button(
          "Xóa",
          () =>
            confirmDelete(
              "Xóa suất chiếu này? Suất đã phát sinh vé sẽ được hệ thống bảo vệ.",
              async () => {
                await CineSnackApi.goi(`/api/admin/showtimes/${s.id}`, { method: "DELETE" });
                await loadShows();
                notice("Đã cập nhật lịch chiếu.");
              },
            ),
          { deleteShow: s.id },
        ),
      );
      cell(row).append(actions);
      o("quan-tri-suat").append(row);
    }
    o("suat-so").textContent = `Trang ${showPage + 1} · ${shows.length} suất`;
    o("suat-truoc").disabled = showPage === 0;
    o("suat-sau").disabled = shows.length < 100;
  }
  async function openSection(id) {
    if ((id === "phim" || id === "suat") && !isAdmin()) return;
    if (!user) return;
    const previous = section;
    section = id;
    const titles = {
      don: ["Đơn toàn rạp", "Tra cứu đơn của user và guest, đối chiếu trước khi nhận tiền."],
      "hoa-don": ["Hóa đơn đã thanh toán", "Mở chi tiết đơn để xem hóa đơn vé và bắp nước."],
      phim: ["Phim tại rạp", "Quản lý danh mục, nội dung và trạng thái phim."],
      suat: ["Lịch chiếu", "Xếp lịch theo phòng, giữ nguyên sơ đồ ghế có sẵn."],
    };
    o("quan-tri-tieu-de").textContent = titles[id][0];
    o("quan-tri-duong-dan").textContent = titles[id][0];
    o("quan-tri-phu").textContent = titles[id][1];
    document
      .querySelectorAll(".quan-tri-muc")
      .forEach((x) => (x.hidden = x.id !== "muc-" + (id === "hoa-don" ? "don" : id)));
    document
      .querySelectorAll("[data-muc]")
      .forEach((x) => x.setAttribute("aria-pressed", String(x.dataset.muc === id)));
    if (id === "phim") await loadMovies();
    else if (id === "suat") await loadShows();
    else {
      if (previous !== id) {
        page = 0;
        o("don-trang-thai").value = id === "hoa-don" ? "DA_THANH_TOAN" : "";
      }
      o("don-trang-thai").disabled = id === "hoa-don";
      await loadOrders();
    }
  }
  function field(form, name) {
    return form.elements.namedItem(name);
  }
  function editMovie(movie) {
    const form = o("phim-form");
    form.reset();
    o("phim-form-error").textContent = "";
    field(form, "id").value = movie?.id || "";
    if (movie)
      for (const name of ["name", "genre", "duration", "ageRating", "posterUrl", "status"])
        field(form, name).value = movie[name] ?? "";
    o("phim-form-title").textContent = movie ? "Sửa phim" : "Thêm phim";
    o("phim-hop").showModal();
  }
  async function editShow(show) {
    const form = o("suat-form");
    form.reset();
    o("suat-form-error").textContent = "";
    [movieOptions, rooms] = await Promise.all([
      CineSnackApi.goi("/api/admin/movies?size=100"),
      CineSnackApi.goi("/api/admin/rooms"),
    ]);
    const movieSelect = field(form, "movieId"),
      roomSelect = field(form, "roomId");
    movieSelect.replaceChildren();
    roomSelect.replaceChildren();
    for (const p of movieOptions) {
      const option = e("option", p.name);
      option.value = p.id;
      movieSelect.append(option);
    }
    if (show && !movieOptions.some((p) => p.id === show.movieId)) {
      const option = e("option", show.movieName);
      option.value = show.movieId;
      movieSelect.append(option);
    }
    for (const r of rooms) {
      const option = e("option", r.name);
      option.value = r.id;
      roomSelect.append(option);
    }
    if (show && !rooms.some((r) => r.id === show.roomId)) {
      const option = e("option", show.roomName);
      option.value = show.roomId;
      roomSelect.append(option);
    }
    field(form, "id").value = show?.id || "";
    if (show) {
      field(form, "movieId").value = show.movieId;
      field(form, "roomId").value = show.roomId;
      field(form, "startsAt").value = show.startsAt.slice(0, 16);
      field(form, "price").value = show.price;
      field(form, "status").value = show.status;
    }
    o("suat-form-title").textContent = show ? "Sửa suất chiếu" : "Thêm suất chiếu";
    o("suat-hop").showModal();
  }
  function confirmDelete(message, action) {
    o("confirm-message").textContent = message;
    o("confirm-error").textContent = "";
    o("confirm-delete").onclick = async () => {
      const b = o("confirm-delete");
      b.disabled = true;
      try {
        await action();
        o("confirm-hop").close();
      } catch (err) {
        o("confirm-error").textContent = err.message;
      } finally {
        b.disabled = false;
      }
    };
    o("confirm-hop").showModal();
  }
  o("confirm-cancel").addEventListener("click", () => o("confirm-hop").close());
  o("phim-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!isAdmin()) return;
    const form = event.currentTarget,
      submit = form.querySelector("button[type=submit]");
    if (submit.disabled || !form.reportValidity()) return;
    submit.disabled = true;
    o("phim-form-error").textContent = "";
    try {
      const id = field(form, "id").value;
      const body = Object.fromEntries(
        ["name", "genre", "ageRating", "status"].map((name) => [
          name,
          field(form, name).value.trim(),
        ]),
      );
      body.duration = Number(field(form, "duration").value);
      body.posterUrl = field(form, "posterUrl").value.trim() || null;
      await CineSnackApi.goi("/api/admin/movies" + (id ? "/" + id : ""), {
        method: id ? "PUT" : "POST",
        body,
      });
      o("phim-hop").close();
      await loadMovies();
      notice("Đã lưu phim.");
    } catch (err) {
      o("phim-form-error").textContent = err.message;
    } finally {
      submit.disabled = false;
    }
  });
  o("suat-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!isAdmin()) return;
    const form = event.currentTarget,
      submit = form.querySelector("button[type=submit]");
    if (submit.disabled || !form.reportValidity()) return;
    submit.disabled = true;
    o("suat-form-error").textContent = "";
    try {
      const id = field(form, "id").value,
        raw = field(form, "startsAt").value;
      const body = {
        movieId: Number(field(form, "movieId").value),
        roomId: Number(field(form, "roomId").value),
        startsAt: raw + (raw.length === 16 ? ":00" : "") + "+07:00",
        price: Number(field(form, "price").value),
        status: field(form, "status").value,
      };
      await CineSnackApi.goi("/api/admin/showtimes" + (id ? "/" + id : ""), {
        method: id ? "PUT" : "POST",
        body,
      });
      o("suat-hop").close();
      await loadShows();
      notice("Đã lưu suất chiếu.");
    } catch (err) {
      o("suat-form-error").textContent = err.message;
    } finally {
      submit.disabled = false;
    }
  });
  o("them-phim").addEventListener("click", () => {
    if (isAdmin()) editMovie(null);
  });
  o("them-suat").addEventListener("click", () => {
    if (isAdmin()) editShow(null).catch(error);
  });
  document
    .querySelectorAll("[data-muc]")
    .forEach((b) => b.addEventListener("click", () => openSection(b.dataset.muc).catch(error)));
  o("don-search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    page = 0;
    loadOrders().catch(error);
  });
  let searchTimer;
  o("quan-tri-don-tim").addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      page = 0;
      loadOrders().catch(error);
    }, 250);
  });
  o("don-trang-thai").addEventListener("change", () => {
    page = 0;
    loadOrders().catch(error);
  });
  o("admin-refresh").addEventListener("click", () => loadOrders().catch(error));
  for (const [id, delta] of [
    ["admin-truoc", -1],
    ["admin-sau", 1],
  ])
    o(id).addEventListener("click", () => {
      page = Math.max(0, page + delta);
      loadOrders().catch(error);
    });
  for (const [id, delta] of [
    ["phim-truoc", -1],
    ["phim-sau", 1],
  ])
    o(id).addEventListener("click", () => {
      moviePage = Math.max(0, moviePage + delta);
      loadMovies().catch(error);
    });
  for (const [id, delta] of [
    ["suat-truoc", -1],
    ["suat-sau", 1],
  ])
    o(id).addEventListener("click", () => {
      showPage = Math.max(0, showPage + delta);
      loadShows().catch(error);
    });
  o("tra-don-form").addEventListener("submit", (event) => {
    event.preventDefault();
    showOrder(o("tra-ma-don").value.trim()).catch(error);
  });
  o("admin-logout").addEventListener("click", async () => {
    o("admin-logout").disabled = true;
    try {
      await CineSnackApi.dangXuat();
      location.href = "index.html";
    } catch (err) {
      error(err);
      o("admin-logout").disabled = false;
    }
  });
  try {
    user = await CineSnackApi.taiKhoan();
    if (!user) {
      o("admin-login").hidden = false;
      return;
    }
    if (!["NHAN_VIEN", "QUAN_TRI"].includes(user.role)) {
      error(new Error("Tài khoản không có quyền vào không gian làm việc."));
      return;
    }
    o("admin-role").textContent = isAdmin() ? "Quản trị viên" : "Nhân viên quầy";
    o("admin-email").textContent = user.email;
    o("admin-logout").hidden = false;
    o("quan-tri-ngay").textContent = new Intl.DateTimeFormat("vi-VN", {
      timeZone: "Asia/Ho_Chi_Minh",
    }).format(new Date());
    document.querySelectorAll("[data-admin-only]").forEach((node) => (node.hidden = !isAdmin()));
    o("admin-workspace").hidden = false;
    await openSection(isAdmin() ? "phim" : "don");
  } catch (err) {
    error(err);
  }
})();
