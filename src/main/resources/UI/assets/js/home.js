"use strict";
(async () => {
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
