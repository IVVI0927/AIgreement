function createTooltip(text, x, y) {
  const tooltip = document.createElement('div');
  tooltip.id = 'legalAI-tooltip';
  tooltip.style.position = 'absolute';
  tooltip.style.top = `${y - 40}px`;
  tooltip.style.left = `${x + 10}px`;
  tooltip.style.background = '#fff';
  tooltip.style.border = '1px solid #ccc';
  tooltip.style.padding = '5px 10px';
  tooltip.style.borderRadius = '8px';
  tooltip.style.boxShadow = '0 2px 8px rgba(0,0,0,0.2)';
  tooltip.style.zIndex = 9999;
  tooltip.style.display = 'flex';
  tooltip.style.gap = '8px';

  const analyzeBtn = document.createElement('button');
  analyzeBtn.textContent = '🧠 Analyze';
  analyzeBtn.onclick = () => {
    sendToAnalyze(text);
    document.body.removeChild(tooltip);
  };

  const explainBtn = document.createElement('button');
  explainBtn.textContent = '📘 Explain';
  explainBtn.onclick = () => {
    explainTerm(text);
    document.body.removeChild(tooltip);
  };

  tooltip.appendChild(analyzeBtn);
  tooltip.appendChild(explainBtn);
  document.body.appendChild(tooltip);
}

function sendToAnalyze(text) {
  fetch('http://localhost:8080/api/contracts/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: 'Quick Analyze', content: text })
  })
    .then(res => res.json())
    .then(data => {
      alert('🧠 Analysis:\n' + JSON.stringify(data, null, 2));
    })
    .catch(() => {
      alert('❌ Error sending analysis request.');
    });
}

function explainTerm(term) {
  fetch(chrome.runtime.getURL("dictionary.json"))
    .then(response => response.json())
    .then(dictionary => {
      const def = dictionary[term.toLowerCase()];
      alert(def ? `📘 ${term}:\n${def}` : '❌ No definition found.');
    });
}

document.addEventListener('mouseup', (e) => {
  const selectedText = window.getSelection().toString().trim();
  const existing = document.getElementById('legalAI-tooltip');
  if (existing) existing.remove();

  if (selectedText.length > 0) {
    createTooltip(selectedText, e.pageX, e.pageY);
  }
});