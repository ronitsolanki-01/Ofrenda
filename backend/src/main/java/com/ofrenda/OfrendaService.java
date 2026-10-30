package com.ofrenda;

import com.ofrenda.Models.Memory;
import com.ofrenda.Models.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OfrendaService {

    private static final Logger log = LoggerFactory.getLogger(OfrendaService.class);

    private final OfrendaStore store;
    private final VectorStore vectorStore;
    private final ChatClient chat;

    public OfrendaService(OfrendaStore store, VectorStore vectorStore, ChatClient.Builder builder) {
        this.store = store;
        this.vectorStore = vectorStore;
        this.chat = builder.build();
    }

    /** Re-embed every saved memory with nomic-embed-text when the app starts. */
    @EventListener(ApplicationReadyEvent.class)
    void reindex() {
        List<Document> docs = store.allMemories().stream().map(this::toDoc).toList();
        if (docs.isEmpty()) return;
        try {
            vectorStore.add(docs);
            log.info("Re-embedded {} memories", docs.size());
        } catch (Exception e) {
            log.warn("Could not embed memories (is Ollama running?): {}", e.getMessage());
        }
    }

    private Document toDoc(Memory m) {
        return new Document(m.id(), m.text(), Map.of("personId", m.personId(), "kind", m.kind()));
    }

    public Memory addMemory(String personId, String kind, String text) {
        store.person(personId);
        Memory m = new Memory(UUID.randomUUID().toString(), personId, kind, text.trim(), Instant.now().toString());
        vectorStore.add(List.of(toDoc(m)));   // embed first: if Ollama is down, nothing is saved
        store.addMemory(m);
        return m;
    }

    public void removeMemory(String id) {
        vectorStore.delete(List.of(id));
        store.removeMemory(id);
    }

    /** RAG: retrieve the closest memories for this person, answer only from them. */
    public String ask(String personId, String question) {
        Person p = store.person(personId);
        if (store.memoriesOf(personId).isEmpty()) {
            return "The ofrenda for " + p.name() + " is still empty. Add a food, a song, or a story and I'll have something to tell you.";
        }
        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder().eq("personId", personId).build())
                .build());
        String context = hits.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));

        String system = """
                You are the Keeper of an ofrenda (a Día de los Muertos altar) for %s, the user's %s.
                You are NOT %s and never pretend to be them. You are a gentle, warm storyteller who
                keeps their memories. Speak about %s in the third person.
                Use ONLY the memories below. Never invent facts, dates, names or quotes.
                If the memories don't answer the question, say that story isn't on the ofrenda yet
                and invite the user to add it. Keep answers short (2-5 sentences).

                MEMORIES:
                %s
                """.formatted(p.name(), p.relation(), p.name(), p.name(), context);

        return chat.prompt().system(system).user(question).call().content();
    }

    /** Writes a tribute or a playful calaverita literaria from all saved memories. */
    public String remembrance(String personId, String style) {
        Person p = store.person(personId);
        List<Memory> ms = store.memoriesOf(personId);
        if (ms.isEmpty()) return "Add a few memories to the ofrenda first.";
        String memories = ms.stream().limit(40)
                .map(m -> "- (" + m.kind() + ") " + m.text()).collect(Collectors.joining("\n"));

        String task = "calaverita".equalsIgnoreCase(style)
                ? "Write a calaverita literaria: a short, affectionate, humorous rhyming verse (8-12 lines) in the Mexican tradition that playfully teases Death and celebrates the person."
                : "Write a heartfelt remembrance of about 120 words, in prose, in the voice of someone who loved them.";

        String system = """
                %s
                The person is %s, the user's %s. Use ONLY these memories, invent no facts:
                %s
                """.formatted(task, p.name(), p.relation(), memories);

        return chat.prompt().system(system).user("Write it now.").call().content();
    }
}
