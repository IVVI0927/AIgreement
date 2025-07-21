// options.js
document.addEventListener('DOMContentLoaded', () => {
  const glossary = document.getElementById('enableGlossary');
  const riskTag = document.getElementById('enableRiskTag');
  const apiKey = document.getElementById('apiKey');

  // 加载存储的配置
  chrome.storage.sync.get(['glossary', 'riskTag', 'apiKey'], (data) => {
    glossary.checked = data.glossary || false;
    riskTag.checked = data.riskTag || false;
    apiKey.value = data.apiKey || '';
  });

  document.getElementById('saveBtn').addEventListener('click', () => {
    chrome.storage.sync.set({
      glossary: glossary.checked,
      riskTag: riskTag.checked,
      apiKey: apiKey.value
    }, () => {
      alert('✅ 设置已保存');
    });
  });
});