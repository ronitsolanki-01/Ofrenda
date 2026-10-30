const BASE = process.env.NEXT_PUBLIC_API ?? "http://localhost:8080/api";

export type Person = { id: string; name: string; relation: string };
export type Memory = { id: string; personId: string; kind: Kind; text: string; createdAt: string };
export type Kind = "food" | "song" | "story" | "object";

async function call<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: { "Content-Type": "application/json" },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? `Request failed (${res.status})`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

const post = (body: unknown): RequestInit => ({ method: "POST", body: JSON.stringify(body) });

export const api = {
  people: () => call<Person[]>("/people"),
  addPerson: (name: string, relation: string) => call<Person>("/people", post({ name, relation })),
  memories: (id: string) => call<Memory[]>(`/people/${id}/memories`),
  addMemory: (id: string, kind: Kind, text: string) => call<Memory>(`/people/${id}/memories`, post({ kind, text })),
  removeMemory: (id: string) => call<void>(`/memories/${id}`, { method: "DELETE" }),
  ask: (id: string, message: string) => call<{ answer: string }>(`/people/${id}/ask`, post({ message })),
  remembrance: (id: string, style: "tribute" | "calaverita") =>
    call<{ text: string }>(`/people/${id}/remembrance?style=${style}`, { method: "POST" }),
};
