"use strict";
(async () => {
  document
    .querySelectorAll("[data-trang-thai] span")
    .forEach((s) => s.remove());
  const q = (s) => document.querySelector(s);
  let trang = 0,
    status = "DANG_CHIEU",
    ds = [],
    lan = 0,
    timer;
  function text(tag, value, cls) {
    const e = document.createElement(tag);
    e.textContent = value;
    if (cls) e.className = cls;
    return e;
  }
  function chiTiet(p) {
    const box = q("#phim-chi-tiet");
    box.replaceChildren();
    const h = text("h2", p.name);
    h.id = "chi-tiet-tieu-de";
    box.append(
      h,
      text("p", p.genre + " · " + p.duration + " phút · " + p.ageRating),
    );
    if (p.status === "DANG_CHIEU") {
      const a = text("a", "Chọn suất chiếu", "nut");
      a.href = "booking.html?phim=" + p.id;
      box.append(a);
    } else box.append(text("p", "Chưa mở đặt vé."));
    q("#hop-phim").showModal();
  }
  async function tai() {
    const n = ++lan;
    const lo = q("#luoi-phim");
    q("#khong-phim").hidden = true;
    q("#so-phim").textContent = "Đang tải…";
    try {
      const data = await CineSnackApi.goi(
        `/api/movies?status=${status}&q=${encodeURIComponent(q("#tim-phim").value.trim())}&page=${trang}&size=20`,
      );
      if (n !== lan) return;
      ds = data;
      lo.replaceChildren();
      for (const p of ds) {
        const card = document.createElement("article");
        card.className = "the-phim";
        const b = document.createElement("button");
        b.type = "button";
        b.className = "anh";
        b.dataset.chiTiet = p.id;
        b.setAttribute("aria-label", "Thông tin phim " + p.name);
        const img = document.createElement("img");
        img.src = CineSnack.poster(p.posterUrl);
        img.alt = "Poster " + p.name;
        img.width = 500;
        img.height = 750;
        img.loading = "lazy";
        b.append(img);
        b.addEventListener("click", () => chiTiet(p));
        card.append(
          b,
          text("div", p.genre, "phim-thong-tin"),
          text("h3", p.name),
          text("p", p.duration + " phút", "thoi-luong"),
        );
        if (p.status === "DANG_CHIEU") {
          const a = text("a", "Đặt vé", "nut phim-nut");
          a.href = "booking.html?phim=" + p.id;
          card.append(a);
        } else card.append(text("span", "Chưa mở đặt vé", "sap-nhan"));
        lo.append(card);
      }
      q("#khong-phim").hidden = ds.length > 0;
      q("#so-phim").textContent = `Trang ${trang + 1} · ${ds.length} phim`;
      q("#phim-truoc").disabled = trang === 0;
      q("#phim-sau").disabled = ds.length < 20;
    } catch (e) {
      if (n === lan) {
        lo.replaceChildren();
        q("#so-phim").textContent = e.message;
      }
    }
  }
  const paging = document.createElement("div");
  paging.className = "bao phan-trang";
  for (const [id, label, change] of [
    ["phim-truoc", "Trang trước", -1],
    ["phim-sau", "Trang sau", 1],
  ]) {
    const b = text("button", label, "nut phu");
    b.type = "button";
    b.id = id;
    b.addEventListener("click", () => {
      trang = Math.max(0, trang + change);
      tai();
    });
    paging.append(b);
  }
  q("#luoi-phim").parentElement.append(paging);
  document.querySelectorAll("[data-trang-thai]").forEach((b) =>
    b.addEventListener("click", () => {
      status = b.dataset.trangThai === "sap-chieu" ? "SAP_CHIEU" : "DANG_CHIEU";
      trang = 0;
      document.querySelectorAll("[data-trang-thai]").forEach((x) => {
        x.classList.toggle("da-chon", x === b);
        x.setAttribute("aria-pressed", String(x === b));
      });
      tai();
    }),
  );
  q("#tim-phim").addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      trang = 0;
      tai();
    }, 200);
  });
  q("#xoa-tim").addEventListener("click", () => {
    q("#tim-phim").value = "";
    trang = 0;
    tai();
    q("#tim-phim").focus();
  });
  await tai();
  const id = Number(new URLSearchParams(location.search).get("phim"));
  if (Number.isSafeInteger(id) && id > 0) {
    try {
      const p = await CineSnackApi.goi("/api/movies/" + id);
      if (status !== p.status) {
        status = p.status;
        trang = 0;
        document.querySelectorAll("[data-trang-thai]").forEach((b) => {
          const active =
            (b.dataset.trangThai === "sap-chieu"
              ? "SAP_CHIEU"
              : "DANG_CHIEU") === status;
          b.classList.toggle("da-chon", active);
          b.setAttribute("aria-pressed", String(active));
        });
        await tai();
      }
      chiTiet(p);
    } catch (error) {
      q("#so-phim").textContent = error.message;
    }
  }
})();
