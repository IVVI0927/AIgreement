# AIgreement: Legal LLM Chrome Extension

**AIgreement** is a Chrome extension that helps users analyze legal terms and contracts using a local LLM (via API) and a built-in legal dictionary. It is designed for law students, professionals, and privacy-conscious users who want AI-powered contract interpretation without uploading sensitive data to the cloud.

---

## 🔍 Features

- ✨ Highlight any contract clause and get:
  - A plain-English explanation powered by a local LLM (e.g. LLaMA 3 via Ollama)
  - Definitions of key legal terms from a built-in dictionary
- 📑 Supports Residential Lease, Employment, Service Agreements, Terms of Service, and more
- 📘 Dictionary stored locally (JSON-based)
- 🧠 LLM runs locally or via custom backend endpoint
- ✅ Lightweight, fast, and privacy-preserving

---

## 🛠 Tech Stack

- **Frontend**: Chrome Extension (Manifest V3)
- **LLM Backend**: Java + Spring Boot API, PostgreSQL
- **Local Model**: Ollama + LLaMA 3 integration (optional)
- **Deployment**: GitHub Actions + GCP Cloud Run (for backend)

---

## 🚀 Usage

1. Install the Chrome Extension from source (Load unpacked).
2. Right-click any legal text and select:
   - "Explain with LLM"
   - "Define Legal Terms"
3. The result will pop up in a floating UI near the selected text.

---

## 📦 Project Structure

```bash
legal-ai-extension/
├── background.js            # Handles context menu and messaging
├── content-script.js        # Injects floating buttons and captures selection
├── selection-buttons.js     # UI logic for the floating Explain/Define buttons
├── dictionary.json          # Local legal term definitions
├── popup.html / popup.js    # (Optional) Extension popup view
├── manifest.json            # Chrome extension manifest (V3)
└── backend/                 # Spring Boot project for LLM API
```

---

## 📄 Legal Term Dictionary Format

```json
{
  "indemnify": "To compensate for harm or loss.",
  "termination clause": "A contract provision that allows either party to end the agreement under certain conditions."
}
```

---

## ☁️ Deployment (Planned)

- GCP Cloud Run hosts the Spring Boot API
- GitHub Actions for CI/CD (auto-deploy on push to `main`)

---

## 📘 License

MIT — feel free to fork, customize, and contribute!

---

## 🤝 Contributions

Suggestions, issues, and pull requests are welcome. You can also contribute legal terms to the dictionary!
