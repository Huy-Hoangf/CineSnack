"use strict";
window.CineSnackReport = (() => {
  const form = document.getElementById("baocao-form");
  const result = document.getElementById("baocao-result");
  const status = document.getElementById("report-status");
  const money = value => CineSnack.dinhDangTien(value);
  const dateLabel = date => date.split("-").reverse().join("/");
  let generation = 0;
  function node(tag, text, className) {
    const el = document.createElement(tag);
    if (text != null) el.textContent = text;
    if (className) el.className = className;
    return el;
  }
  function period(days) {
    const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh" }).format(new Date());
    form.elements.to.value = today;
    const start = new Date(today + "T00:00:00Z");
    start.setUTCDate(start.getUTCDate() - days + 1);
    form.elements.from.value = start.toISOString().slice(0, 10);
  }
  function render(days) {
    const total = { orders: 0, tickets: 0, snacks: 0, ticketRevenue: 0, snackRevenue: 0, totalRevenue: 0 };
    for (const day of days) for (const key of Object.keys(total)) total[key] += Number(day[key]);
    const summary = node("div", null, "report-summary");
    const metrics = [
      ["Tổng doanh thu", money(total.totalRevenue)],
      ["Đơn đã thanh toán", total.orders], ["Vé bán", total.tickets], ["Món bắp nước", total.snacks],
    ];
    if (total.orders) metrics.push(["Trung bình mỗi đơn", money(total.totalRevenue / total.orders)]);
    for (const [label, value] of metrics) {
      const card = node("article", null, "workspace-stat");
      card.append(node("span", label), node("strong", String(value)));
      summary.append(card);
    }
    result.append(summary);
    if (!total.orders) {
      result.append(node("p", "Chưa có đơn đã thanh toán trong khoảng này."));
    }
    const panel = node("section", null, "workspace-card report-panel");
    const legend = node("p", null, "report-legend");
    legend.append(node("span", "■ Vé", "tickets"), node("span", "■ Bắp nước", "snacks"));
    panel.append(node("h2", "Doanh thu theo ngày"),
      node("p", "Tính theo ngày thanh toán tại Việt Nam. Chọn một cột để xem chi tiết.", "workspace-hint"), legend);
    const detail = node("p", "", "report-day-detail");
    detail.setAttribute("role", "status");
    const selectDay = day => {
      detail.textContent = dateLabel(day.date) + " · Vé " + money(day.ticketRevenue) +
        " · Bắp nước " + money(day.snackRevenue) + " · Tổng " + money(day.totalRevenue) +
        " · " + day.orders + " đơn · " + day.tickets + " vé · " + day.snacks + " món";
    };
    const peak = Math.max(0, ...days.map(day => Number(day.totalRevenue)));
    panel.append(node("p", "Mốc cao nhất " + money(peak), "workspace-hint"));
    const scroll = node("div", null, "report-chart-scroll");
    scroll.tabIndex = 0;
    scroll.setAttribute("role", "region");
    scroll.setAttribute("aria-label", "Biểu đồ doanh thu, cuộn ngang để xem các ngày");
    const chart = node("div", null, "report-chart");
    for (const day of days) {
      const item = node("div", null, "report-chart-day");
      const button = node("button", null, "report-column");
      button.type = "button";
      button.setAttribute("aria-label", dateLabel(day.date) + ", tổng " + money(day.totalRevenue));
      button.title = button.getAttribute("aria-label");
      for (const [type, value] of [["tickets", day.ticketRevenue], ["snacks", day.snackRevenue]]) {
        const segment = node("span", null, "report-segment " + type);
        segment.style.height = (peak ? 100 * Number(value) / peak : 0) + "%";
        button.append(segment);
      }
      button.addEventListener("click", () => {
        chart.querySelector('[aria-pressed="true"]')?.setAttribute("aria-pressed", "false");
        button.setAttribute("aria-pressed", "true");
        selectDay(day);
      });
      button.setAttribute("aria-pressed", "false");
      item.append(button, node("small", dateLabel(day.date).slice(0, 5)));
      chart.append(item);
    }
    scroll.append(chart);
    panel.append(scroll, detail);
    if (days.length) selectDay(days[days.length - 1]);
    result.append(panel);
    const composition = node("section", null, "workspace-card");
    composition.append(node("h2", "Cơ cấu doanh thu"));
    if (total.totalRevenue > 0) {
      const share = 100 * total.ticketRevenue / total.totalRevenue;
      const bar = node("div", null, "report-composition");
      const ticket = node("span", null, "tickets");
      ticket.style.width = share + "%";
      bar.append(ticket);
      bar.setAttribute("aria-hidden", "true");
      composition.append(bar, node("p", "Vé " + share.toFixed(1) + "% · " + money(total.ticketRevenue)),
        node("p", "Bắp nước " + (100 - share).toFixed(1) + "% · " + money(total.snackRevenue)));
    } else composition.append(node("p", "Vé " + money(total.ticketRevenue) + " · Bắp nước " + money(total.snackRevenue)));
    result.append(composition);
    const tablePanel = node("section", null, "workspace-card");
    tablePanel.append(node("h2", "Chi tiết theo ngày"));
    const table = node("table", null, "workspace-table report-table");
    table.append(node("caption", "Doanh thu và số lượng theo ngày thanh toán"));
    const header = node("tr");
    const labels = ["Ngày", "Đơn", "Vé", "Món", "Doanh thu vé", "Doanh thu bắp nước", "Tổng thu"];
    for (const label of labels) { const th = node("th", label); th.scope = "col"; header.append(th); }
    const head = node("thead"); head.append(header); table.append(head);
    const body = node("tbody");
    for (const day of days) {
      const row = node("tr");
      [dateLabel(day.date), day.orders, day.tickets, day.snacks, money(day.ticketRevenue), money(day.snackRevenue), money(day.totalRevenue)]
        .forEach((value, index) => { const cell = node("td", String(value)); cell.dataset.label = labels[index]; row.append(cell); });
      body.append(row);
    }
    table.append(body); tablePanel.append(table); result.append(tablePanel);
  }
  async function load() {
    const current = ++generation;
    result.removeAttribute("aria-busy");
    if (!form.reportValidity()) return;
    const from = form.elements.from.value, to = form.elements.to.value;
    const span = (Date.parse(to) - Date.parse(from)) / 86400000;
    result.replaceChildren();
    if (span < 0 || span > 365) {
      status.textContent = "Chọn khoảng từ 1 đến 366 ngày, ngày kết thúc từ ngày bắt đầu trở đi.";
      return;
    }
    status.textContent = "Đang tải báo cáo…";
    result.setAttribute("aria-busy", "true");
    try {
      const days = await CineSnackApi.goi("/api/admin/reports/range?" + new URLSearchParams({ from, to }));
      if (current !== generation) return;
      render(days);
      status.textContent = dateLabel(from) + " – " + dateLabel(to) + " · " + days.length + " ngày";
    } catch (error) {
      if (current === generation) status.textContent = "Không tải được báo cáo. " + error.message;
    } finally {
      if (current === generation) result.removeAttribute("aria-busy");
    }
  }
  form.addEventListener("submit", event => { event.preventDefault(); load(); });
  form.querySelectorAll("[data-report-days]").forEach(button => button.addEventListener("click", () => {
    period(Number(button.dataset.reportDays));
    load();
  }));
  period(7);
  return { load };
})();
