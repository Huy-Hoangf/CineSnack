"use strict";
(function () {
  const C = CineSnack;
  const search = document.querySelector("#movie-search");
  const dialog = document.querySelector("#movie-dialog");
  const requestedMovie = C.movies.find(
    (m) => m.id === new URLSearchParams(location.search).get("movie"),
  );
  let status = requestedMovie?.status || "now";
  function detail(movie) {
    // Movie data is a fixed, local catalog; search text never enters this template.
    document.querySelector("#movie-detail").innerHTML =
      `<div class="movie-detail-layout"><img src="assets/images/movies/${movie.image}" alt="Poster ${movie.title}" width="200" height="300"><div><span class="pill">${movie.status === "now" ? "Đang chiếu" : "Sắp chiếu"} · ${movie.age}</span><h2 id="detail-title">${movie.title}</h2><div class="detail-facts"><span>${movie.duration} phút</span><span>· ${movie.genre}</span></div><div class="detail-director">Đạo diễn: ${movie.director}</div><p>${movie.description}</p>${movie.status === "now" ? `<a class="button" href="dat-ve.html?movie=${movie.id}">Chọn suất chiếu ↗</a>` : '<p class="notice">Chưa mở đặt vé. Lịch chiếu sẽ được cập nhật sau.</p>'}<p class="hint">Thông tin phim là nội dung minh họa cho giao diện.</p></div></div>`;
    dialog.showModal();
  }
  function render() {
    document.querySelectorAll("[data-status]").forEach((button) => {
      const active = button.dataset.status === status;
      button.classList.toggle("selected", active);
      button.setAttribute("aria-pressed", String(active));
    });
    const query = C.normalize(search.value);
    const movies = C.movies.filter(
      (m) =>
        m.status === status &&
        C.normalize(m.title + " " + m.genre).includes(query),
    );
    document.querySelector("#movie-count").textContent =
      `${movies.length} phim ${status === "now" ? "đang chiếu" : "sắp chiếu"}`;
    document.querySelector("#no-movies").hidden = movies.length > 0;
    document.querySelector("#catalog-grid").innerHTML = movies
      .map(
        (m) =>
          `<article class="movie-card"><button type="button" class="poster" data-detail="${m.id}" aria-label="Thông tin phim ${m.title}"><img src="assets/images/movies/${m.image}" alt="Poster ${m.title}" width="500" height="750" loading="lazy"><span class="age ${m.age === "P" ? "all-ages" : ""}">${m.age}</span><span class="format">2D · Phụ đề</span></button><div class="movie-meta"><span>${m.genre}</span><span>${m.year}</span></div><h3><button type="button" class="movie-title-button" data-detail="${m.id}">${m.title}</button></h3><p class="duration">${m.duration} phút</p>${m.status === "now" ? `<a class="button movie-button" href="dat-ve.html?movie=${m.id}" aria-label="Đặt vé ${m.title}">Đặt vé <span aria-hidden="true">↗</span></a>` : '<span class="coming-label">Chưa mở đặt vé</span>'}</article>`,
      )
      .join("");
  }
  document.querySelectorAll("[data-status]").forEach((button) =>
    button.addEventListener("click", () => {
      status = button.dataset.status;
      render();
    }),
  );
  search.addEventListener("input", render);
  document.querySelector("#clear-search").addEventListener("click", () => {
    search.value = "";
    render();
    search.focus();
  });
  document.querySelector("#catalog-grid").addEventListener("click", (event) => {
    const button = event.target.closest("[data-detail]");
    if (button) detail(C.movies.find((m) => m.id === button.dataset.detail));
  });
  render();
  if (requestedMovie) detail(requestedMovie);
})();
