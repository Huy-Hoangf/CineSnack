"use strict";
window.CineSnackShowPricing = (() => {
  function setMode(form, mode) {
    form.elements.namedItem("pricingMode").value = mode || "MANUAL";
    const manual = form.elements.namedItem("pricingMode").value === "MANUAL";
    for (const name of ["price", "vipSurcharge"]) {
      const input = form.elements.namedItem(name);
      input.required = manual;
      input.disabled = !manual;
    }
  }
  function request(form) {
    const pricingMode = form.elements.namedItem("pricingMode").value;
    return pricingMode === "POLICY" ? { pricingMode } : {
      pricingMode, price: Number(form.elements.namedItem("price").value),
      vipSurcharge: Number(form.elements.namedItem("vipSurcharge").value)
    };
  }
  document.getElementById("suat-form").elements.namedItem("pricingMode").addEventListener("change", event => setMode(event.target.form, event.target.value));
  return { setMode, request };
})();
