package com.ofrenda;

import com.ofrenda.Models.Memory;
import com.ofrenda.Models.Person;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OfrendaController {

    record NewPerson(String name, String relation) {}
    record NewMemory(String kind, String text) {}
    record Question(String message) {}

    private final OfrendaStore store;
    private final OfrendaService service;

    public OfrendaController(OfrendaStore store, OfrendaService service) {
        this.store = store;
        this.service = service;
    }

    @GetMapping("/people")
    List<Person> people() { return store.people(); }

    @PostMapping("/people")
    Person addPerson(@RequestBody NewPerson in) { return store.addPerson(in.name().trim(), in.relation().trim()); }

    @GetMapping("/people/{id}/memories")
    List<Memory> memories(@PathVariable String id) { return store.memoriesOf(id); }

    @PostMapping("/people/{id}/memories")
    Memory addMemory(@PathVariable String id, @RequestBody NewMemory in) {
        return service.addMemory(id, in.kind(), in.text());
    }

    @DeleteMapping("/memories/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) {
        service.removeMemory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/people/{id}/ask")
    Map<String, String> ask(@PathVariable String id, @RequestBody Question q) {
        return Map.of("answer", service.ask(id, q.message()));
    }

    @PostMapping("/people/{id}/remembrance")
    Map<String, String> remembrance(@PathVariable String id, @RequestParam(defaultValue = "tribute") String style) {
        return Map.of("text", service.remembrance(id, style));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> fail(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
