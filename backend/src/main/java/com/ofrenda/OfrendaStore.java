package com.ofrenda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofrenda.Models.Memory;
import com.ofrenda.Models.Person;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tiny JSON-file persistence so your altar survives restarts. */
@Component
public class OfrendaStore {

    public static class Data {
        public List<Person> people = new ArrayList<>();
        public List<Memory> memories = new ArrayList<>();
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file = Path.of("data", "ofrenda.json");
    private Data data = new Data();

    @PostConstruct
    void load() throws IOException {
        if (Files.exists(file)) data = mapper.readValue(file.toFile(), Data.class);
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized List<Person> people() { return List.copyOf(data.people); }
    public synchronized List<Memory> allMemories() { return List.copyOf(data.memories); }

    public synchronized Person person(String id) {
        return data.people.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such person: " + id));
    }

    public synchronized List<Memory> memoriesOf(String personId) {
        return data.memories.stream().filter(m -> m.personId().equals(personId)).toList();
    }

    public synchronized Person addPerson(String name, String relation) {
        Person p = new Person(UUID.randomUUID().toString(), name, relation);
        data.people.add(p);
        save();
        return p;
    }

    public synchronized void addMemory(Memory m) { data.memories.add(m); save(); }

    public synchronized void removeMemory(String id) {
        data.memories.removeIf(m -> m.id().equals(id));
        save();
    }
}
