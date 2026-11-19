"use strict";
window.CineSnackPrint = (() => {
  const e = (tag, text, cls) => {
    const n = document.createElement(tag);
    if (text !== undefined) {
      n.textContent = text;
    }
    if (cls) {
      n.className = cls;
    }
    return n;
  };
  function taoBarcode(code) {
    if (typeof JsBarcode !== "function") {
      throw new Error("Chưa tải được thư viện tạo mã vạch. Vui lòng tải lại trang.");
    }
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("role", "img");
    svg.setAttribute("aria-label", "Mã vạch " + code);
    JsBarcode(svg, code, {
      format: "CODE128", width: 2, height: 65, displayValue: false, margin: 14,
    });
    return svg;
  }
  function ticketCard(ticket) {
    const card = e("article", undefined, "cinema-ticket print-page");
    card.append(
      e("p", "CINESNACK · VÉ XEM PHIM", "print-brand"),
      e("h3", ticket.movieName),
      e("p", ticket.roomName),
      e("p", CineSnack.dinhDangSuat(ticket.startsAt)),
      e("strong", "GHẾ " + ticket.seatCode, "print-seat"),
      e("p", "Giá vé: " + CineSnack.dinhDangTien(ticket.unitPrice)),
    );
    if (ticket.status === "DA_SOAT") {
      card.append(e("strong", "VÉ ĐÃ SOÁT · KHÔNG ĐƯỢC DÙNG LẠI"));
    } else {
      card.append(taoBarcode(ticket.code));
    }
    card.append(
      e("p", ticket.code, "print-code"),
      e("small", "Mỗi vé dành cho một ghế. Giữ mã vé để soát khi vào rạp."),
    );
    return card;
  }
  function invoiceCard(invoice) {
    const card = e("article", undefined, "cinema-invoice print-page");
    card.append(
      e("p", "CINESNACK", "print-brand"),
      e("h3", invoice.type === "VE" ? "HÓA ĐƠN VÉ" : "HÓA ĐƠN BỎNG NƯỚC"),
      e("p", "Mã hóa đơn: " + invoice.code),
      e("p", "Phát hành: " + CineSnack.dinhDangSuat(invoice.issuedAt)),
    );
    const lines = e("ul");
    for (const line of invoice.lines) {
      lines.append(
        e(
          "li",
          `${line.quantity} × ${line.description} · ${CineSnack.dinhDangTien(line.quantity * line.unitPrice)}`,
        ),
      );
    }
    card.append(
      lines,
      e("strong", "TỔNG: " + CineSnack.dinhDangTien(invoice.total)),
      e("p", "Đã thanh toán"),
    );
    if (invoice.type !== "VE") {
      card.append(taoBarcode(invoice.code), e("p", invoice.code, "print-code"),
        e("small", "Xuất trình mã vạch tại quầy để nhận bỏng nước. Mỗi hóa đơn chỉ nhận một lần."));
    }
    return card;
  }
  async function printCards(cards, title) {
    if (!cards.length) {
      return;
    }
    document.getElementById("cinema-print-frame")?.remove();
    const frame = document.createElement("iframe");
    frame.id = "cinema-print-frame";
    frame.title = title;
    frame.style.cssText =
      "position:fixed;left:-10000px;top:0;width:800px;height:1000px;border:0";
    document.body.append(frame);
    const doc = frame.contentDocument;
    doc.documentElement.lang = "vi";
    doc.title = title;
    const style = doc.createElement("style");
    style.textContent = `
      @page { size: 80mm 180mm; margin: 5mm; }
      * { box-sizing: border-box; }
      body { margin: 0; color: #000; background: #fff; font: 13px Arial, sans-serif; }
      article { max-width: 70mm; margin: 0 auto; padding: 4mm 0; }
      .print-page { break-after: page; page-break-after: always; }
      .cinema-ticket { break-inside: avoid; page-break-inside: avoid; }
      .print-page:last-child { break-after: auto; page-break-after: auto; }
      h3 { font-size: 19px; line-height: 1.3; margin: 12px 0; }
      p, li { line-height: 1.5; overflow-wrap: anywhere; }
      ul { padding-left: 16px; } li { margin: 10px 0; }
      .print-brand { font-size: 14px; font-weight: bold; border-bottom: 1px dashed; padding-bottom: 10px; }
      .print-seat { display: block; font-size: 30px; margin: 16px 0; }
      .print-code { font: 11px monospace; text-align: center; }
       svg { width: 100%; height: auto; display: block; }
       .ticket-qr { width: 36mm; height: 36mm; margin: 3mm auto; }
      small { display: block; padding-top: 10px; border-top: 1px dashed; }
    `;
    doc.head.append(style);
    for (const card of cards) {
      doc.body.append(doc.importNode(card, true));
    }
    frame.contentWindow.addEventListener("afterprint", () => frame.remove(), {
      once: true,
    });
    await doc.fonts.ready;
    frame.contentWindow.focus();
    frame.contentWindow.print();
  }
  function printIssued(ticket) {
    return printCards([ticketCard(ticket)], "Vé ghế " + ticket.seatCode);
  }
  function ticketsButton(tickets, run) {
    const b = e("button", "In vé từng ghế", "nut phu");
    b.type = "button";
    b.addEventListener("click", () => run(b, async () => {
      const printable = tickets.filter(t => t.status === "DA_THANH_TOAN" && !t.printed);
      if (!printable.length) throw new Error("Không còn vé đủ điều kiện in.");
      // Validate barcode generation before consuming any print claims.
      printable.forEach(ticketCard);
      const results = await CineSnackApi.goi("/api/staff/tickets/issue-batch", {
        method: "POST", body: { codes: printable.map(t => t.code) },
      });
      await printCards(results.map(r => ticketCard(r.ticket)), "CineSnack · Vé từng ghế");
    }));
    return b;
  }
  function invoiceButton(invoice) {
    const b = e(
      "button",
      invoice.type === "VE" ? "In hóa đơn vé" : "In hóa đơn bỏng nước",
      "nut phu",
    );
    b.type = "button";
    b.addEventListener("click", () =>
      printCards([invoiceCard(invoice)], "CineSnack · " + invoice.code),
    );
    return b;
  }
  return { printIssued, invoiceButton, ticketsButton };
})();
