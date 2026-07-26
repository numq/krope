[![Maven Central](https://img.shields.io/maven-central/v/io.github.numq/krope-core)](https://central.sonatype.com/artifact/io.github.numq/krope-core)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

# Krope — High-Performance Rope-Based Text Buffer for Kotlin Multiplatform

**Krope** is a Kotlin Multiplatform library providing an immutable, persistent text buffer backed by a **Rope** data
structure. It's designed for applications that require efficient, memory-safe text manipulation on large strings — ideal
for collaborative editors, code editors, IDEs, and reactive text processing pipelines.

---

## Why Rope?

Unlike `String` or `StringBuilder`, a rope represents text as a balanced binary tree of character chunks. This offers
significant benefits for editing large documents:

- **O(log N) complexity** for inserts, deletes, and substring extraction (vs. O(N) for flat strings)
- **Structural sharing** — editing returns a new tree without copying unchanged portions, reducing memory allocation
- **Cached metadata** at each node for O(1) access to total characters, bytes, lines, and maximum line length
- **Automatic rebalancing** using a red-black tree variant to maintain performance over thousands of edits

---

## Features

- 📝 **Efficient text editing** — Insert, delete, replace, and batch operations in logarithmic time
- 📊 **Line-aware indexing** — Resolve line/column positions to character offsets and vice versa
- 🧵 **Multi-encoding support** — UTF-8, UTF-16 LE/BE, UTF-32 LE/BE with accurate byte offset calculations
- ⚡ **Batch operations** — Group multiple edits into a single structural update using `withBatch` for atomic, optimized
  mutations
- 🔄 **Reactive & observable** — Coroutine-based `SharedFlow` emits `TextEdit.Data` events on every change; immutable
  `StateFlow` snapshots provide consistent reads
- 🧠 **Memory pooling** — Optional string interning and node caching reduce heap pressure under repetitive edits
- 🌍 **Cross-platform** — Targets JVM, Android, iOS (x64, arm64, simulator), and WASM (browser) via Kotlin Multiplatform
- ✅ **Functional core** — Uses an `Either<L, R>` type for error handling, keeping operations chainable and
  exception-safe
- 📐 **Line ending normalization** — Automatically converts `\r\n` and `\r` to `\n` internally, restores on snapshot
  access

---

## Architecture

```
library/
├── core/       → Rope data structure, node types, encoding, LRU caches, SoftReference
├── text/       → TextBuffer interface, RopeTextBuffer, TextPosition, TextRange, TextEdit
├── io/         → Serialization-ready module (kotlinx.serialization)
└── diff/       → Placeholder for future diff/comparison algorithms
```

### Core Rope

The `Rope` class implements the full rope interface: insert, delete, split, concatenate, rebalance, and batch apply. It
maintains internal caches for line offsets, byte offsets, and full-text retrieval. Metadata (byte count, line breaks,
max line length) is aggregated efficiently at each node.

### Text Buffer (`TextBuffer`)

The public API surface for end users. It wraps a `Rope` with a coroutine-safe mutex, a `StateFlow<TextSnapshot>` for
querying current state, and a `SharedFlow<TextEdit.Data>` for observing edits.

Operations:

- `insert(position, text)`
- `replace(range, text)`
- `delete(range)`
- `withBatch { ... }`
- `changeEncoding(encoding)`
- `changeLineEnding(lineEnding)`

### Text Snapshot

`TextSnapshot` provides consistent access to:

- Total lines, characters, bytes
- Maximum line length
- Line-by-line text and length
- Substring extraction by `TextRange`
- Conversion between line/column positions, character offsets, and byte offsets

### Text Edit Data

Every mutation produces a `TextEdit.Data` subtype:

- `Single.Insert` — text inserted at a position
- `Single.Delete` — text removed from a range
- `Single.Replace` — text replaced in a range
- `Batch` — multiple singles grouped atomically

---

## Example Usage

```kotlin
import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.*

// 1. Create buffer
val factory = RopeTextBufferFactory()
val buffer = factory.create(
    text = "Hello World",
    encoding = Encoding.UTF8,
    lineEnding = TextLineEnding.LF,
    enablePooling = true
).fold(
    onLeft = { error -> /* handle error */ },
    onRight = { it }
)

// 2. Observe edits
launch {
    buffer.data.collect { editData ->
        when (editData) {
            is TextEdit.Data.Single.Insert -> println("Inserted at ${editData.startPosition}")

            is TextEdit.Data.Single.Delete -> println("Deleted from ${editData.startPosition}")

            is TextEdit.Data.Single.Replace -> println("Replaced '${editData.oldText}' with '${editData.newText}'")

            is TextEdit.Data.Batch -> println("Batch of ${editData.singles.size} edits")
        }
    }
}

// 3. Read current snapshot
val snapshot = buffer.snapshot.value
println(snapshot.text) // "Hello World"
println(snapshot.lines) // 1
println(snapshot.maxLineLength) // 11

// 4. Edit
buffer.insert(TextPosition(0, 5), " Beautiful") // "Hello Beautiful World"
buffer.replace(TextRange(TextPosition(0, 6), TextPosition(0, 15)), "Wonderful") // "Hello Wonderful World"
buffer.delete(TextRange(TextPosition(0, 5), TextPosition(0, 16))) // "Hello World"

// 5. Batch edits
buffer.withBatch { batch ->
    batch.replace(TextRange(TextPosition(0, 0), TextPosition(0, 5)), "Hi")
    batch.insert(TextPosition(0, 2), "!")
} // "Hi! World"

// 6. Byte offsets (useful for interop with native file I/O)
val bytePos = snapshot.getBytePosition(TextPosition(0, 6)) // 6 (UTF-8)
```

For a full simulation of a collaborative editor with reactive events and batch refactoring, see
`example/src/commonMain/kotlin/io/github/numq/krope/example/EditorSimulation.kt`.

---

## Benchmark

How does Krope perform on massive files?

We ran JMH benchmarks on the JVM simulating a "Real Editor Scenario": 100 consecutive edits occurring right in the
middle of a document.

| Document Size | Krope (100 edits) | StringBuilder (100 edits) | Krope Advantage |
|---------------|-------------------|---------------------------|-----------------|
| 100,000       | ~0.05 ms          | ~0.16 ms                  | ~3x faster      |
| 1,000,000     | ~0.19 ms          | ~1.74 ms                  | ~9x faster      |
| 10,000,000    | ~2.07 ms          | ~17.86 ms                 | ~8.5x faster    |
| 100,000,000   | ~19.32 ms         | ~186.41 ms                | ~9.6x faster    |

Why the massive difference? `StringBuilder` suffers from $O(N)$ complexity. Inserting into the middle of a 200MB string
requires `System.arraycopy` to shift 100MB of RAM. Doing this 100 times forces the CPU to move *10 gigabytes* of memory,
causing severe UI stutter.

Krope uses a Red-Black tree and structural sharing. It simply splits a small leaf node and updates a few pointers up to
the root ($O(\log N)$). Krope maintains incredibly flat, predictable latency regardless of file size, keeping your UI
perfectly smooth while drastically reducing heap allocations.

(Note: The benchmark code for Krope actually included the time to parse the initial string into a Rope tree from scratch
on every iteration. This makes Krope's performance here even more impressive!)

[Source](benchmark/src/jvmMain/kotlin/io.github.numq.krope.benchmark/RopeBenchmark.kt)

---

## Supported Platforms

| Platform                    | Status |
|-----------------------------|--------|
| JVM (17+)                   | ✅      |
| Android (API 24+)           | ✅      |
| iOS (x64, arm64, simulator) | ✅      |
| WASM (browser)              | ✅      |

---

## Dependencies

- **kotlinx.atomicfu** — for lock-free atomic state (internal)
- **kotlinx.coroutines** — for `StateFlow`, `SharedFlow`, and mutex-based synchronization
- **kotlinx.serialization** — for the `io` module's serialization support

---

## Gradle Installation

The project is hosted on Maven Central. Make sure your project repositories include it:

```kotlin
repositories {
    mavenCentral()
}
```

Add the desired module to your `build.gradle.kts` dependencies block:

```kotlin
dependencies {
    // Core rope data structure
    implementation("io.github.numq:krope-core:1.0.0")

    // Full TextBuffer API (depends on core)
    implementation("io.github.numq:krope-text:1.0.0")

    // Future modules (Coming soon 🚧)
    // implementation("io.github.numq:krope-diff:1.0.0")
    // implementation("io.github.numq:krope-io:1.0.0")
}

```

---

## License

Apache-2.0 License - see [LICENSE](LICENSE) file for details.

---

<p align="center">
  <a href="https://numq.github.io/support">
    <img src="https://api.qrserver.com/v1/create-qr-code/?size=112x112&data=https://numq.github.io/support&bgcolor=1a1b26&color=7aa2f7" 
         width="112" 
         height="112" 
         style="border-radius: 4px;" 
         alt="Support QR code">
  </a>
  <br>
  <a href="https://numq.github.io/support" style="text-decoration: none;">
    <code><font color="#bb9af7">Support Development: numq.github.io/support</font></code>
  </a>
</p>