console.log("[selection-buttons] injected!");
let floatingMenu = null;

document.addEventListener("mouseup", async (e) => {
  const selection = window.getSelection().toString().trim();

  if (selection.length === 0) {
    if (floatingMenu) floatingMenu.remove();
    return;
  }

  // 移除旧菜单
  if (floatingMenu) floatingMenu.remove();

  // 创建浮动菜单
  floatingMenu = document.createElement("div");
  floatingMenu.style.position = "absolute";
  floatingMenu.style.top = `${e.pageY - 40}px`;
  floatingMenu.style.left = `${e.pageX}px`;
  floatingMenu.style.background = "#fff";
  floatingMenu.style.border = "1px solid #ccc";
  floatingMenu.style.borderRadius = "8px";
  floatingMenu.style.padding = "5px 8px";
  floatingMenu.style.zIndex = "99999";
  floatingMenu.style.boxShadow = "0 2px 5px rgba(0,0,0,0.2)";
  floatingMenu.style.display = "flex";
  floatingMenu.style.gap = "6px";

  // Analyze 按钮
  const analyzeBtn = document.createElement("button");
  analyzeBtn.textContent = "🧠 Analyze";
  analyzeBtn.style.cursor = "pointer";
  analyzeBtn.onclick = () => {
    fetch("http://localhost:8080/api/contracts/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: "Inline Analyze",
        content: selection
      })
    })
      .then((res) => res.json())
      .then((data) => {
        alert("🧠 Analysis:\n" + JSON.stringify(data, null, 2));
      })
      .catch(() => {
        alert("❌ Error contacting server.");
      });
  };

  // Explain 按钮
  const explainBtn = document.createElement("button");
  explainBtn.textContent = "📘 Explain";
  explainBtn.style.cursor = "pointer";
  explainBtn.onclick = () => {
    chrome.runtime.sendMessage(
      { type: "lookupTerm", term: selection.toLowerCase() },
      (response) => {
        if (response?.definition) {
          alert("📘 Definition:\n" + response.definition);
        } else {
          alert("❌ No definition found.");
        }
      }
    );
  };

  floatingMenu.appendChild(analyzeBtn);
  floatingMenu.appendChild(explainBtn);
  document.body.appendChild(floatingMenu);
});

// 监听点击其他地方移除菜单
document.addEventListener("click", (e) => {
  if (floatingMenu && !floatingMenu.contains(e.target)) {
    floatingMenu.remove();
    floatingMenu = null;
  }
});