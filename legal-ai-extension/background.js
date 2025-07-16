let dictionary = {};

fetch(chrome.runtime.getURL("dictionary.json"))
  .then(response => response.json())
  .then(json => {
    dictionary = json;
  });

chrome.runtime.onInstalled.addListener(() => {
  // 原有菜单：分析
  chrome.contextMenus.create({
    id: "analyzeClause",
    title: "🧠 Analyze selected clause",
    contexts: ["selection"]
  });

  // 新增菜单：解释术语
  chrome.contextMenus.create({
    id: "explainTerm",
    title: "📘 Explain legal term",
    contexts: ["selection"]
  });
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
    if (tab.url.startsWith("chrome://")) {
    return; // Do not execute scripts on restricted pages
  }
  const selectedText = info.selectionText.trim().toLowerCase();

  if (info.menuItemId === "explainTerm") {
    const explanation = dictionary[selectedText];
    if (explanation) {
      chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: (msg) => alert(msg),
        args: [`📘 Definition:\n${explanation}`]
      });
    } else {
      chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: () => alert("⚠️ No definition found for this term.")
      });
    }
  }

  // （可选）保留分析逻辑
  if (info.menuItemId === "analyzeClause") {
    fetch("http://localhost:8080/api/contracts/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: "Quick Analyze",
        content: selectedText
      })
    })
      .then(res => res.json())
      .then(data => {
        chrome.scripting.executeScript({
          target: { tabId: tab.id },
          func: (result) => alert("🧠 Analysis:\n" + JSON.stringify(result, null, 2)),
          args: [data]
        });
      })
      .catch(err => {
        chrome.scripting.executeScript({
          target: { tabId: tab.id },
          func: () => alert("❌ Error sending analysis request.")
        });
      });
  }
});