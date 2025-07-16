document.addEventListener('DOMContentLoaded', () => {
  const contractArea = document.getElementById('contract');
  contractArea.addEventListener('mouseup', async () => {
    const selectedText = window.getSelection().toString().trim();
    if (selectedText.length > 0) {
      await explainTerm(selectedText);
    }
  });
  const analyzeBtn = document.getElementById('analyzeBtn');
  const historyBtn = document.getElementById('historyBtn');
  const resultArea = document.getElementById('result');

  analyzeBtn.addEventListener('click', () => {
    const title = document.getElementById('title').value;
    const content = document.getElementById('contract').value;

    fetch('http://localhost:8080/api/contracts/analyze', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ title, content })
    })
      .then(response => response.json())
      .then(data => {
        resultArea.innerHTML = '';
        data.analysisResult.forEach(item => {
          const div = document.createElement('div');
          div.className = 'analysis-result';

          let riskClass = '';
          if (item["risk level"] === 'High') riskClass = 'risk-high';
          else if (item["risk level"] === 'Medium') riskClass = 'risk-medium';
          else if (item["risk level"] === 'Low') riskClass = 'risk-low';

          div.innerHTML = `
            <p><strong>Clause:</strong> ${item.clause}</p>
            <p><strong>Reason:</strong> ${item.reason}</p>
            <p><strong>Risk Level:</strong> <span class="${riskClass}">${item["risk level"]}</span></p>
          `;
          resultArea.appendChild(div);
        });
      })
      .catch(err => {
        resultArea.innerText = "❌ Error: " + err.message;
      });
  });

  historyBtn.addEventListener('click', () => {
    fetch('http://localhost:8080/api/contracts')
      .then(response => response.json())
      .then(data => {
        resultArea.innerHTML = '';
        data.forEach(item => {
          const div = document.createElement('div');
          div.innerHTML = `<strong>${item.title}</strong><br/>${item.content.substring(0, 100)}...<br/><br/>`;
          resultArea.appendChild(div);
        });
      })
      .catch(err => {
        resultArea.innerText = "❌ Error loading history: " + err.message;
      });
  });

  const fileInput = document.getElementById('fileInput');
  const uploadBtn = document.getElementById('uploadBtn');

  uploadBtn.addEventListener('click', () => {
    const file = fileInput.files[0];
    if (!file) {
      alert('Please select a file to upload.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    fetch('http://localhost:8080/api/contracts/upload', {
      method: 'POST',
      body: formData
    })
      .then(response => response.text())
      .then(data => {
        document.getElementById('contract').value = data;

        const title = file.name || "Uploaded Contract";

        fetch('http://localhost:8080/api/contracts/analyze', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ title, content: data })
        })
          .then(response => response.json())
          .then(data => {
            const resultArea = document.getElementById('result');
            resultArea.innerHTML = '';
            data.analysisResult.forEach(item => {
              const div = document.createElement('div');
              div.className = 'analysis-result';

              let riskClass = '';
              if (item["risk level"] === 'High') riskClass = 'risk-high';
              else if (item["risk level"] === 'Medium') riskClass = 'risk-medium';
              else if (item["risk level"] === 'Low') riskClass = 'risk-low';

              div.innerHTML = `
                <p><strong>Clause:</strong> ${item.clause}</p>
                <p><strong>Reason:</strong> ${item.reason}</p>
                <p><strong>Risk Level:</strong> <span class="${riskClass}">${item["risk level"]}</span></p>
              `;
              resultArea.appendChild(div);
            });
          })
          .catch(err => {
            console.error("Error analyzing contract:", err);
          });
      })
      .catch(err => {
        alert('❌ File upload failed: ' + err.message);
      });
  });

  async function loadLegalDictionary() {
    const response = await fetch(chrome.runtime.getURL("dictionary.json"));
    return await response.json();
  }

  async function explainTerm(term) {
    const dict = await loadLegalDictionary();
    const found = dict.find(entry => entry.term.toLowerCase() === term.toLowerCase());
    if (found) {
      alert(`📘 ${found.term}: ${found.definition}`);
    } else {
      alert("Term not found in dictionary.");
    }
  }
});
