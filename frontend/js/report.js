/**
 * InternShield AI - Report Download
 * Reads the currently displayed analysis from the page and downloads
 * it as a standalone, printable HTML report (simulated demo result).
 */
document.addEventListener("DOMContentLoaded", () => {
  const btn = document.getElementById("downloadReportBtn");
  if (!btn) return;

  const esc = (s) =>
    String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");

  const text = (root, sel) => {
    const el = root.querySelector(sel);
    return el ? el.textContent.replace(/\s+/g, " ").trim() : "";
  };

  btn.addEventListener("click", () => {
    const result = document.getElementById("analysisResult");
    if (!result || !result.classList.contains("visible")) {
      alert("Run an analysis first, then download the report.");
      return;
    }

    const score = text(document, "#gaugeScoreValue");
    const classification = text(document, "#resultClassificationBadge");
    const heading = text(document, "#resultHeading");
    const summary = text(document, "#resultSummary");
    const offerText = (document.getElementById("offerInput") || {}).value || "";

    const verification = Array.from(document.querySelectorAll("#companyVerificationGrid .verification-item"))
      .map((el) => ({
        name: text(el, ".verification-name"),
        badge: text(el, ".verification-badge"),
        detail: text(el, ".verification-detail"),
      }));

    const warnings = Array.from(document.querySelectorAll("#warningCardsGrid .warning-card"))
      .map((el) => ({
        title: text(el, ".warning-title"),
        severity: text(el, ".badge"),
        observation: text(el, ".warning-observation"),
        why: text(el, ".why-matters-box").replace(/^Why this matters:\s*/i, ""),
      }));

    const actions = Array.from(document.querySelectorAll("#recommendedActionsList .action-step-item"))
      .map((el) => text(el, "span:last-child"));

    const when = new Date();
    const stamp = when.toLocaleString();
    const fileDate = when.toISOString().slice(0, 10);

    const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>InternShield AI Report</title>
<style>
  body{font-family:-apple-system,"Segoe UI",Roboto,Arial,sans-serif;background:#fdf8f3;color:#111827;margin:0;padding:40px 20px;line-height:1.6}
  .wrap{max-width:780px;margin:0 auto}
  h1{font-family:Georgia,serif;color:#1d4ed8;font-size:2.4rem;margin:0 0 4px}
  h2{font-family:Georgia,serif;color:#1d4ed8;font-size:1.3rem;margin:34px 0 12px}
  .meta{color:#6b7280;font-size:.85rem;margin-bottom:24px}
  .score{display:flex;gap:20px;align-items:center;background:#ebecfd;border-radius:24px;padding:22px 26px}
  .score b{font-family:Georgia,serif;font-size:3rem;color:#1d4ed8;line-height:1}
  .tag{display:inline-block;background:#111827;color:#fff;border-radius:99px;padding:3px 12px;font-size:.72rem;font-weight:700;text-transform:uppercase;letter-spacing:.05em}
  .card{background:#fff;border:1px solid #eadfd1;border-radius:16px;padding:14px 18px;margin-bottom:10px}
  .card h3{margin:0 0 4px;font-size:1rem}
  .why{background:#fef6d3;border-radius:10px;padding:8px 12px;margin-top:8px;font-size:.88rem}
  ol{padding-left:20px} li{margin-bottom:6px}
  pre{white-space:pre-wrap;background:#fff;border:1px dashed #eadfd1;border-radius:12px;padding:14px;font-family:inherit;font-size:.9rem}
  .note{margin-top:36px;font-size:.8rem;color:#6b7280;border-top:1px dashed #eadfd1;padding-top:14px}
  @media print{body{background:#fff}}
</style></head><body><div class="wrap">
  <h1>InternShield AI</h1>
  <div class="meta">Offer risk report &bull; Generated ${esc(stamp)} &bull; Simulated demo result</div>

  <div class="score">
    <b>${esc(score)}</b>
    <div><span class="tag">${esc(classification)}</span>
    <h2 style="margin:8px 0 4px">${esc(heading)}</h2>
    <div>${esc(summary)}</div></div>
  </div>

  ${offerText ? `<h2>Analyzed text</h2><pre>${esc(offerText)}</pre>` : ""}

  <h2>Company verification (demo indicators)</h2>
  ${verification.map(v => `<div class="card"><h3>${esc(v.name)} &mdash; ${esc(v.badge)}</h3><div>${esc(v.detail)}</div></div>`).join("")}

  <h2>Detected signals &amp; warning signs</h2>
  ${warnings.map(w => `<div class="card"><h3>${esc(w.title)} <span class="tag">${esc(w.severity)}</span></h3>
    <div>${esc(w.observation)}</div><div class="why"><b>Why this matters:</b> ${esc(w.why)}</div></div>`).join("")}

  <h2>Recommended safety actions</h2>
  <ol>${actions.map(a => `<li>${esc(a)}</li>`).join("")}</ol>

  <div class="note"><b>Disclaimer:</b> This report is educational guidance from a client-side simulation based on common fraud patterns. It does not replace independent due diligence. Always verify offers through the employer's official channels.</div>
</div></body></html>`;

    const blob = new Blob([html], { type: "text/html" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `internshield-report-${fileDate}.html`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });
});
