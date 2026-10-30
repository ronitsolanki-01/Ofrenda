"use client";

import { useCallback, useEffect, useState } from "react";
import { api, Kind, Memory, Person } from "@/lib/api";

const KINDS: { id: Kind; label: string; glyph: string; hint: string }[] = [
  { id: "food", label: "Food", glyph: "🍞", hint: "Her pan de muerto, always with too much anise…" },
  { id: "song", label: "Song", glyph: "🎵", hint: "He hummed “Cielito Lindo” while washing the car…" },
  { id: "story", label: "Story", glyph: "📖", hint: "The time she drove four hours to bring me a forgotten jacket…" },
  { id: "object", label: "Object", glyph: "🕯️", hint: "His brown leather wallet, soft as cloth…" },
];

export default function Page() {
  const [people, setPeople] = useState<Person[]>([]);
  const [active, setActive] = useState<Person | null>(null);
  const [memories, setMemories] = useState<Memory[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");

  const [name, setName] = useState("");
  const [relation, setRelation] = useState("");
  const [kind, setKind] = useState<Kind>("story");
  const [text, setText] = useState("");

  const [question, setQuestion] = useState("");
  const [chat, setChat] = useState<{ role: "you" | "keeper"; text: string }[]>([]);
  const [remembrance, setRemembrance] = useState("");

  const run = useCallback(async (label: string, fn: () => Promise<void>) => {
    setBusy(label);
    setError("");
    try { await fn(); }
    catch (e) { setError((e as Error).message + " — is the backend running and Ollama up?"); }
    finally { setBusy(""); }
  }, []);

  useEffect(() => {
    run("", async () => {
      const list = await api.people();
      setPeople(list);
      if (list.length) setActive(list[0]);
    });
  }, [run]);

  useEffect(() => {
    setChat([]);
    setRemembrance("");
    if (!active) { setMemories([]); return; }
    run("", async () => setMemories(await api.memories(active.id)));
  }, [active, run]);

  const addPerson = () => run("person", async () => {
    if (!name.trim() || !relation.trim()) return;
    const p = await api.addPerson(name, relation);
    setPeople((l) => [...l, p]);
    setActive(p);
    setName(""); setRelation("");
  });

  const addMemory = () => run("memory", async () => {
    if (!active || !text.trim()) return;
    const m = await api.addMemory(active.id, kind, text);
    setMemories((l) => [...l, m]);
    setText("");
  });

  const removeMemory = (id: string) => run("", async () => {
    await api.removeMemory(id);
    setMemories((l) => l.filter((m) => m.id !== id));
  });

  const ask = () => run("ask", async () => {
    if (!active || !question.trim()) return;
    const q = question;
    setQuestion("");
    setChat((c) => [...c, { role: "you", text: q }]);
    const { answer } = await api.ask(active.id, q);
    setChat((c) => [...c, { role: "keeper", text: answer }]);
  });

  const write = (style: "tribute" | "calaverita") => run(style, async () => {
    if (!active) return;
    setRemembrance((await api.remembrance(active.id, style)).text);
  });

  return (
    <>
      <div className="papel" aria-hidden>
        {Array.from({ length: 18 }, (_, i) => <span key={i} />)}
      </div>

      <main className="shell">
        <aside>
          <h1>Ofrenda</h1>
          <p className="lede">An altar for the people you remember. Everything stays on this machine.</p>

          <ul className="people">
            {people.map((p) => (
              <li key={p.id}>
                <button className={p.id === active?.id ? "on" : ""} onClick={() => setActive(p)}>
                  <strong>{p.name}</strong><span>{p.relation}</span>
                </button>
              </li>
            ))}
          </ul>

          <div className="card">
            <h2>Honor someone</h2>
            <input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
            <input placeholder="Who they were to you (e.g. grandmother)" value={relation}
              onChange={(e) => setRelation(e.target.value)} />
            <button className="primary" onClick={addPerson} disabled={busy === "person"}>Add to the ofrenda</button>
          </div>
        </aside>

        <section>
          {error && <p className="error" role="alert">{error}</p>}

          {!active ? (
            <p className="empty">Start by adding the first person you want to remember.</p>
          ) : (
            <>
              <h2 className="who">{active.name}<small>{active.relation}</small></h2>

              <div className="altar">
                {memories.length === 0 && <p className="empty">Nothing has been placed here yet. Add a food, a song, a story or an object.</p>}
                {memories.map((m) => {
                  const k = KINDS.find((x) => x.id === m.kind)!;
                  return (
                    <article key={m.id} className={`offering ${m.kind}`}>
                      <span className="glyph" aria-label={k.label}>{k.glyph}</span>
                      <p>{m.text}</p>
                      <button className="x" onClick={() => removeMemory(m.id)} aria-label="Remove this memory">×</button>
                    </article>
                  );
                })}
              </div>

              <div className="card">
                <h2>Place an offering</h2>
                <div className="kinds" role="radiogroup" aria-label="Kind of memory">
                  {KINDS.map((k) => (
                    <button key={k.id} role="radio" aria-checked={kind === k.id}
                      className={kind === k.id ? "on" : ""} onClick={() => setKind(k.id)}>
                      {k.glyph} {k.label}
                    </button>
                  ))}
                </div>
                <textarea rows={3} value={text} onChange={(e) => setText(e.target.value)}
                  placeholder={KINDS.find((k) => k.id === kind)!.hint} />
                <button className="primary" onClick={addMemory} disabled={busy === "memory"}>
                  {busy === "memory" ? "Placing…" : "Place on the ofrenda"}
                </button>
              </div>

              <div className="card">
                <h2>Ask the Keeper</h2>
                <p className="note">The Keeper answers only from what you placed above. It never speaks as {active.name}.</p>
                <div className="chat" aria-live="polite">
                  {chat.map((c, i) => <p key={i} className={c.role}>{c.text}</p>)}
                  {busy === "ask" && <p className="keeper">Reading the candlelight…</p>}
                </div>
                <div className="row">
                  <input value={question} onChange={(e) => setQuestion(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && ask()}
                    placeholder={`What did ${active.name} love to cook?`} />
                  <button className="primary" onClick={ask} disabled={busy === "ask"}>Ask</button>
                </div>
              </div>

              <div className="card">
                <h2>Write something for them</h2>
                <div className="row">
                  <button onClick={() => write("tribute")} disabled={!!busy}>
                    {busy === "tribute" ? "Writing…" : "A remembrance"}
                  </button>
                  <button onClick={() => write("calaverita")} disabled={!!busy}>
                    {busy === "calaverita" ? "Writing…" : "A calaverita (playful verse)"}
                  </button>
                </div>
                {remembrance && <blockquote>{remembrance}</blockquote>}
              </div>
            </>
          )}
        </section>
      </main>
    </>
  );
}
