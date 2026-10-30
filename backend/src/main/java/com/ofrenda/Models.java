package com.ofrenda;

public final class Models {
    private Models() {}
    public record Person(String id, String name, String relation) {}
    public record Memory(String id, String personId, String kind, String text, String createdAt) {}
}
