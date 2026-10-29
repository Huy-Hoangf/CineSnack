"use strict";
(async () => {
  async function layPhimChoBanner() {
    const ketQua = [];
    const size = 100;
    for (let page = 0; ; page++) {
      const ds = await CineSnackApi.goi(`/api/movies?status=DANG_CHIEU&page=${page}&size=${size}`);
      ketQua.push(...ds.filter((phim) => phim.status === "DANG_CHIEU"));
      if (ds.length < size) return ketQua;
    }
  }
  function khoiTaoSlide(phimDangChieu) {
    const banner = document.querySelector("#phim-noi-bat");
    const dieuKhien = document.querySelector("#banner-dieu-khien");
    const truoc = document.querySelector("#banner-truoc"), sau = document.querySelector("#banner-sau");
    const dung = document.querySelector("#banner-dung"), viTri = document.querySelector("#banner-vi-tri");
    if (!phimDangChieu.length) { banner.hidden = true; return; }
    let hienTai = 0, daDung = window.matchMedia("(prefers-reduced-motion: reduce)").matches, timer;
    function hienPhim() {
      const phim = phimDangChieu[hienTai], anh = document.querySelector("#noi-bat-anh");
      document.querySelector("#noi-bat-tieu-de").textContent = phim.name;
      document.querySelector("#noi-bat-thong-tin").textContent = `${phim.ageRating} · ${phim.duration} phút · ${phim.genre}`;
      anh.src = CineSnack.poster(phim.posterUrl);
      anh.alt = `Poster ${phim.name}`;
      document.querySelector("#noi-bat-dat-ve").href = "booking.html?phim=" + encodeURIComponent(phim.id);
      viTri.textContent = `${hienTai + 1} / ${phimDangChieu.length}`;
      banner.hidden = false;
    }
    function capNhatNutDung() {
      dung.textContent = daDung ? "Chạy slide" : "Dừng slide";
      dung.setAttribute("aria-pressed", String(daDung));
    }
    function chuyen(buoc) {
      hienTai = (hienTai + buoc + phimDangChieu.length) % phimDangChieu.length;
      hienPhim();
    }
    function datLich() {
      clearInterval(timer);
      if (!daDung && phimDangChieu.length > 1 && !document.hidden && !banner.matches(":hover") && !banner.contains(document.activeElement)) timer = setInterval(() => chuyen(1), 6000);
    }
    truoc.addEventListener("click", () => { chuyen(-1); datLich(); });
    sau.addEventListener("click", () => { chuyen(1); datLich(); });
    dung.addEventListener("click", () => { daDung = !daDung; capNhatNutDung(); datLich(); });
    document.addEventListener("visibilitychange", datLich);
    banner.addEventListener("mouseenter", datLich);
    banner.addEventListener("mouseleave", datLich);
    banner.addEventListener("focusin", datLich);
    banner.addEventListener("focusout", () => setTimeout(datLich, 0));
    dieuKhien.hidden = phimDangChieu.length < 2;
    capNhatNutDung(); hienPhim(); datLich();
  }
  try { khoiTaoSlide(await layPhimChoBanner()); }
  catch { document.querySelector("#phim-noi-bat").hidden = true; }
  for (const [selector, status] of [
    [".dang-luoi", "DANG_CHIEU"],
    [".chieu-luoi", "SAP_CHIEU"],
  ]) {
    const box = document.querySelector(selector);
    if (!box) continue;
    box.replaceChildren();
    try {
      const ds = await CineSnackApi.goi("/api/movies?status=" + status + "&size=8");
      const count = document.querySelector(
        status === "DANG_CHIEU" ? ".dang-nhan span" : ".chieu-nhan span",
      );
      if (count) count.textContent = ds.length === 8 ? "8+" : ds.length;
      if (!ds.length) {
        const p = document.createElement("p");
        p.textContent = "Chưa có phim trong danh mục.";
        box.append(p);
      }
      for (const p of ds) {
        const card = document.createElement("article");
        card.className = "the-phim";
        const a = document.createElement("a");
        a.className = "anh";
        a.href = "movies.html?phim=" + p.id;
        const img = document.createElement("img");
        img.src = CineSnack.poster(p.posterUrl);
        img.alt = "Poster " + p.name;
        img.width = 500;
        img.height = 750;
        a.append(img);
        const h = document.createElement("h3");
        h.textContent = p.name;
        const genre = document.createElement("p");
        genre.textContent = p.genre;
        const book = document.createElement("a");
        book.className = "nut";
        book.href = (status === "DANG_CHIEU" ? "booking.html?phim=" : "movies.html?phim=") + p.id;
        book.textContent = status === "DANG_CHIEU" ? "Đặt vé" : "Xem phim";
        card.append(a, genre, h, book);
        box.append(card);
      }
    } catch (e) {
      const p = document.createElement("p");
      p.className = "loi";
      p.textContent = e.message;
      box.append(p);
    }
  }
})();
