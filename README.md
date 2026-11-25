<img width="1889" height="857" alt="image" src="https://github.com/user-attachments/assets/c81f78b4-88af-40b7-a937-0c034c89b80f" /># 🛡️ InternShield AI
### Research Before You Apply.

**An AI-powered internship verification platform that helps students identify potential risks in internship opportunities through web research, evidence collection, and intelligent risk assessment.**

---

## 🌟 Overview

Finding an internship is an exciting step in a student's career, but not every opportunity advertised online is trustworthy.

Some listings may involve unexpected fees, misleading promises, unrelated training programs, or suspicious application processes.

**InternShield AI** helps students research internship opportunities before applying.

Enter a company name and internship URL to receive an AI-assisted risk assessment, potential warning signs, supporting evidence, and a recommendation.

## 🚩 The Problem

Students often struggle to distinguish genuine internship opportunities from suspicious listings.

Common warning signs include:
- Unexpected registration or application fees.
- Mandatory paid courses or training.
- Unverified company information.
- Unrealistic internship promises.
- Suspicious application links and negative reports.

## 💡 Our Solution

InternShield AI combines web research, AI-assisted analysis, and evidence-based risk assessment to help students make informed decisions.

### ✨ Key Features

- **Internship Analysis:** Extracts relevant information from internship listings.
- **Multi-Source Research:** Uses Octen Search to discover publicly available information.
- **AI-Powered Analysis:** Integrates Google Gemini to help identify potential warning signs.
- **Evidence Collection:** Processes and organizes research findings.
- **Risk Assessment:** Evaluates potential risks and generates recommendations.
- **Security:** Includes URL validation and SSRF protection for listing extraction.

## ⚙️ How It Works

```text
Company Name + Internship URL
              |
              v
       React Frontend
              |
              v
      Spring Boot Backend
              |
              v
    Internship Information
          Extraction
              |
              v
     Research Query Generation
              |
              v
        Octen Search
              |
              v
      Evidence Collection
              |
              v
        Gemini AI Analysis
              |
              v
      Risk Assessment Engine
              |
              v
         Final Report
```

### 📊 Risk Recommendations

| Recommendation | Meaning |
|---|---|
| 🟢 APPLY | No significant warning signs identified in available research. |
| 🟡 CAUTION | Potential concerns require further verification. |
| 🔴 AVOID | Serious warning signs suggest avoiding the opportunity. |

## 🏗️ Technology Stack

| Technology | Usage |
|---|---|
| Java 21 | Backend development |
| Spring Boot | REST APIs and application logic |
| Spring Data JPA | Database interaction |
| MySQL | Data storage |
| React | Frontend development |
| JavaScript | Frontend programming |
| Octen Search API | Web research |
| Google Gemini API | AI-assisted analysis |
| Maven | Dependency management |
| Git & GitHub | Version control |

## 🖥️ Application Preview

<!-- Add actual screenshots to docs/screenshots/ and replace these paths -->

| Home Page | Risk Assessment |
|---|---|
| <img width="600" alt="InternShield Home Page" src="https://github.com/user-attachments/assets/e12e1062-6c1c-43e0-853a-d9abbf9dbc1e" /> | <img width="600" alt="InternShield Risk Assessment" src="https://github.com/user-attachments/assets/8f94b8f1-3547-4be4-98fd-785fca06fc57" /> |

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven
- Node.js and npm
- MySQL
- Octen Search API key
- Google Gemini API key

### 1. Clone the repository

```bash
git clone https://github.com/sai-coder1/InternShield_ai.git
cd InternShield_ai
```

### 2. Run the Backend

Navigate to the Spring Boot project directory:

```bash
cd backend
```

Configure your database and API credentials, then run:

```bash
mvn spring-boot:run
```

### 3. Run the Frontend

Open another terminal and navigate to the React project directory:

```bash
cd frontend
npm install
npm run dev
```

Open the local URL provided by Vite.

*Note: Update the directory names and commands if your final repository uses a different structure.*

## 🔐 Environment Configuration

Configure your API credentials and database settings using environment variables or the application's supported configuration.

Example:

```env
OCTEN_API_KEY=your_octen_api_key
GEMINI_API_KEY=your_gemini_api_key
```

Never commit actual API keys, passwords, or other secrets to GitHub.

## 🧪 Testing

Run the backend test suite:

```bash
mvn test
```

The integrated backend and AI code previously passed **151 automated tests** with zero failures and errors.

## 🔮 Future Improvements

- Browser extension for checking internship listings.
- Integration with popular internship platforms.
- Additional research sources.
- Community-based internship reporting.
- Improved evidence verification and risk explanations.

## ⚠️ Disclaimer

InternShield AI is an informational research tool. Its recommendations are based on available public information and AI-assisted analysis.

A low-risk result does not guarantee that an internship is genuine, and a high-risk result does not independently prove fraud. Users should independently verify opportunities before sharing personal information or making payments.

## 👩‍💻 Contributors

Built collaboratively by a team of three students.

| Role | Contribution |
|---|---|
| Frontend Developer | React UI and user experience |
| Backend Developer | Spring Boot APIs and database integration |
| AI & Research Developer | Octen Search, Gemini integration, evidence processing, and risk assessment |


## 🔗 Links

- **GitHub Repository:** [InternShield AI](https://github.com/sai-coder1/InternShield_ai)
- **Report Issues:** [GitHub Issues](https://github.com/sai-coder1/InternShield_ai/issues)

---

<div align="center">

**Built with ❤️ to help students research before they apply.**

### InternShield AI — Make Informed Internship Decisions.

</div>
