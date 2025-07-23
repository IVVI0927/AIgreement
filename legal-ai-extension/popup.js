document.addEventListener('DOMContentLoaded', () => {
  const API_BASE_URL = 'http://localhost:8080'; // API Gateway 地址
  
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

  analyzeBtn.addEventListener('click', async () => {
    const title = document.getElementById('title').value;
    const content = document.getElementById('contract').value;

    try {
      // 通过 API Gateway 调用合同分析服务
      const response = await fetch(`${API_BASE_URL}/api/contracts/analyze`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ title, content })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      displayAnalysisResults(data.analysisResult);
    } catch (err) {
        resultArea.innerText = "❌ Error: " + err.message;
      console.error('Analysis error:', err);
    }
  });

  historyBtn.addEventListener('click', async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/contracts`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      displayHistoryResults(data);
    } catch (err) {
        resultArea.innerText = "❌ Error loading history: " + err.message;
      console.error('History error:', err);
    }
  });

  const fileInput = document.getElementById('fileInput');
  const uploadBtn = document.getElementById('uploadBtn');

  uploadBtn.addEventListener('click', async () => {
    const file = fileInput.files[0];
    if (!file) {
      alert('Please select a file to upload.');
      return;
    }

    try {
      // 1. 上传文件到合同服务
    const formData = new FormData();
    formData.append('file', file);

      const uploadResponse = await fetch(`${API_BASE_URL}/api/contracts/upload`, {
      method: 'POST',
      body: formData
      });

      if (!uploadResponse.ok) {
        throw new Error(`Upload failed: ${uploadResponse.status}`);
      }

      const uploadData = await uploadResponse.json();
      const content = uploadData.content;
      document.getElementById('contract').value = content;

      // 2. 调用 LLM 服务进行分析
        const title = file.name || "Uploaded Contract";

      const analysisResponse = await fetch(`${API_BASE_URL}/api/llm/analyze`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
        body: JSON.stringify({
          content: content
        })
      });

      if (!analysisResponse.ok) {
        throw new Error(`Analysis failed: ${analysisResponse.status}`);
      }

      const analysisData = await analysisResponse.json();
      displayAnalysisResults(analysisData.analysisResult);

    } catch (err) {
      alert('❌ Error: ' + err.message);
      console.error('Upload/Analysis error:', err);
    }
  });

  function displayAnalysisResults(results) {
            resultArea.innerHTML = '';
    if (!results || results.length === 0) {
      resultArea.innerHTML = '<p>No analysis results found.</p>';
      return;
    }

    results.forEach(item => {
              const div = document.createElement('div');
              div.className = 'analysis-result';

              let riskClass = '';
      const riskLevel = item["risk level"] || item["risk_level"];
      if (riskLevel === 'High' || riskLevel === 'HIGH') riskClass = 'risk-high';
      else if (riskLevel === 'Medium' || riskLevel === 'MEDIUM') riskClass = 'risk-medium';
      else if (riskLevel === 'Low' || riskLevel === 'LOW') riskClass = 'risk-low';

              div.innerHTML = `
        <p><strong>Clause:</strong> ${item.clause || 'N/A'}</p>
        <p><strong>Reason:</strong> ${item.reason || 'N/A'}</p>
        <p><strong>Risk Level:</strong> <span class="${riskClass}">${riskLevel || 'N/A'}</span></p>
              `;
              resultArea.appendChild(div);
            });
  }

  function displayHistoryResults(contracts) {
    resultArea.innerHTML = '';
    if (!contracts || contracts.length === 0) {
      resultArea.innerHTML = '<p>No contract history found.</p>';
      return;
    }

    contracts.forEach(item => {
      const div = document.createElement('div');
      div.className = 'history-item';
      div.innerHTML = `
        <strong>${item.title || 'Untitled'}</strong><br/>
        ${(item.content || '').substring(0, 100)}...<br/>
        <small>Created: ${item.createdAt || 'Unknown'}</small><br/><br/>
      `;
      resultArea.appendChild(div);
      });
  }

  async function loadLegalDictionary() {
    try {
    const response = await fetch(chrome.runtime.getURL("dictionary.json"));
    return await response.json();
    } catch (error) {
      console.error('Failed to load dictionary:', error);
      return [];
    }
  }

  async function explainTerm(term) {
    try {
    const dict = await loadLegalDictionary();
    const found = dict.find(entry => entry.term.toLowerCase() === term.toLowerCase());
    if (found) {
      alert(`📘 ${found.term}: ${found.definition}`);
    } else {
        // 如果字典中没有找到，尝试使用 LLM 服务解释
        const response = await fetch(`${API_BASE_URL}/api/llm/analyze`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            content: `Explain the legal term: ${term}`
          })
        });

        if (response.ok) {
          const data = await response.json();
          alert(`🤖 ${term}: ${data.analysisResult || 'No explanation available'}`);
        } else {
          alert("Term not found in dictionary and LLM service unavailable.");
    }
  }
    } catch (error) {
      console.error('Error explaining term:', error);
      alert("Error explaining term. Please try again.");
    }
  }

  // 添加健康检查
  async function checkServiceHealth() {
    try {
      const response = await fetch(`${API_BASE_URL}/actuator/health`);
      if (response.ok) {
        console.log('✅ API Gateway is healthy');
      } else {
        console.warn('⚠️ API Gateway health check failed');
      }
    } catch (error) {
      console.error('❌ API Gateway health check error:', error);
    }
  }

  // 页面加载时检查服务健康状态
  checkServiceHealth();
});
