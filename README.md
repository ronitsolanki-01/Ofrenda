# Ofrenda
A private, local altar for people you remember. Place foods, songs, stories and objects on it.
Memories are embedded with `nomic-embed-text` into a Spring AI vector store. The "Keeper" (`llama3.1:8b`)
answers using retrieval (RAG) from those memories only, and can write a remembrance or a playful *calaverita*.

## Run
1. `ollama pull llama3.1:8b && ollama pull nomic-embed-text` (Ollama running on :11434)
2. Backend (Java 21): `cd backend && mvn spring-boot:run`   → http://localhost:8080
3. Frontend: `cd frontend && npm install && npm run dev`      → http://localhost:3000

Data is saved to `backend/data/ofrenda.json` and re-embedded on every startup.
