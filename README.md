# Meetwise – AI-Powered Conversation Intelligence Platform  


Meetwise is an AI-driven meeting analysis platform that automatically extracts **action items**, generates **Jira tickets**, and produces **concise summaries** from long meeting transcripts.  
It is built using a modern, distributed, event-driven architecture integrating **React**, **Spring Boot**, **Kafka**, **Python LLM workers (Ollama)**, and **MongoDB**.

---

## 🚀 Features

### 🔹 **Automated Action Item Extraction**
Meetwise analyzes raw transcripts and identifies:
- Tasks  
- Bugs  
- Enhancements  
- Owners / assignees  
- Priority  
- Concise generated summaries  

### 🔹 **AI-Generated Meeting Summaries**
A dedicated Python LLM worker (Ollama) generates:
- Concise summaries  
- Key discussion points  
- High-level meeting outcomes  

### 🔹 **One-Click Jira Ticket Creation**
Users can:
- Review AI-generated action items  
- Edit fields  
- Click **Submit** to create Jira issues automatically  

### 🔹 **Real-Time UI Updates (SSE)**
Spring Boot pushes:
- Action items  
- Summaries  
to the UI in **real time** using **Server-Sent Events (SSE)**.

### 🔹 **Modern Interactive Dashboard**
- Upload transcript files  
- Live action item table (editable)  
- Summary editor  
- Jira ticket links displayed instantly  

---

## 🧠 System Architecture

Meetwise follows a distributed microservice-inspired architecture:


Worker Responsibilities:
- **Action Items Worker** extracts tasks via Ollama  
- **Summary Worker** generates summary text  
- Both workers consume Kafka events and push results to MongoDB

The backend:
- Streams results live via SSE  
- Creates Jira tickets on demand  

The frontend:
- Displays everything dynamically  
- Allows editing and submission  

---

## 📦 Tech Stack

### **Frontend**
- React (Material UI)
- EventSource (SSE)
- DataGrid (Action Item editor)

### **Backend**
- Spring Boot
- Kafka Consumers/Producers
- SSE streaming
- Jira Cloud API

### **AI Workers**
- Python 3
- Ollama (Local LLM inference)
- JSON parsing & data shaping

### **Infrastructure**
- Kafka + Zookeeper (Docker)
- MongoDB (Docker)
- Maven build system

---


---

# ⚙️ Prerequisites

- Node.js (v16+)
- Java 11 or 17  
- Maven  
- Docker  
- Python 3.10+  
- Ollama  
- Jira Cloud account + API token  

---



## 📝 Installation Guide

🔧 Backend Setup (Spring Boot)
Install Java 11
brew install openjdk@11

Install Maven
brew install maven

Run Spring Boot Application
cd summary-generator
mvn spring-boot:run




# 🔧 1. Start MongoDB (Docker)

```bash
docker run --name mongodb -d -p 27017:27017 mongo
```
# Start Kafka & Zookeeper

From the project root (where the kafka-docker-compose.yml file is):
```bash
docker-compose -f kafka-docker-compose.yml up -d
```

(Optional, only first time) – create topics:
```bash
docker exec -it kafka bash
```

Inside the container:
```bash
kafka-topics --create --topic llm_service.events.generate.action.items --bootstrap-server localhost:9092
kafka-topics --create --topic llm_service.events.generate.summary --bootstrap-server localhost:9092
kafka-topics --create --topic summary.generator.events.action.items --bootstrap-server localhost:9092
kafka-topics --create --topic summary.generator.events.summary --bootstrap-server localhost:9092
exit
```

# Start the Spring Boot Backend
```bash
cd summary-generator
mvn clean install
mvn spring-boot:run
```
# Start Python LLM Workers
```bash
cd llmService
pip install -r requirements.txt
python3 action_items_worker.py
python3 summaryPromptService.py
```
# Start Front end 
```bash
cd front-end
npm install
npm run dev
```

