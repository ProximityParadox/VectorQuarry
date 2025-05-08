# VectorQuarry Architecture and Excavation Model

This document outlines the core architectural principles and systems topology of **VectorQuarry**. It is intended for developers, reviewers, and technical integrators seeking to understand the internal mechanics and performance strategies of the mod. It omits high-level justification and philosophical design goals (see `readme/concept.md`) and focuses strictly on how the quarry operates in the Minecraft runtime environment.

---

## 1. Execution Model: Deterministic, Layered Excavation

VectorQuarry quarries perform **physical excavation**, not abstracted ore generation. The excavation algorithm is **deterministic** and **layer-oriented**, proceeding from the upper Y-boundary of the defined quarry volume downward, one Y-level at a time. This approach guarantees:

- **Spatial predictability** - every excavation cycle modifies a contiguous slice of the world.
- **Debuggability** - state transitions can be traced in strict vertical order.
- **Completeness** - no blocks are skipped unless filtered or suppressed by external config.

There is no random-access mining or opportunistic prioritization; the algorithm executes in structured sweeps over bounded 3D volumes.

All excavation logic is centralized in a global controller (CentralQuarryManager). This system owns both configuration and execution state, enforces tick ordering, and performs all block manipulation. No quarry ever executes mining logic independently or on a per-block basis.

This model ensures total control over execution semantics and prohibits divergence in tick behavior. Quarries are not autonomous actors; they are globally scheduled participants in a shared, deterministic batch loop.

---

## 2. Tick Coalescence and Batch Processing

To avoid the tick-scaling pathologies of legacy quarry systems (e.g., BuildCraft), VectorQuarry rejects per-block or per-tile tick updates. Instead, it operates on a **batch-processing model**, where:

- All mining logic is executed within a **single tick frame** per quarry.
- Excavation proceeds over a configurable number of blocks per tick.
- Blocks are selected and broken as a group, minimizing function call overhead and improving CPU cache locality.

This model supports both **temporal scalability** (increased tick density without exponential tick cost) and **spatial scalability** (thousands of quarries without multiplicative engine load). Performance profiling indicates:

- **500 quarries** yield sub-**0.5ms/tick** overhead.
- **5000 quarries** sustain operation below **15ms/tick**, depending on terrain complexity and output pathways.

These figures are derived from controlled simulation environments with instrumentation overlays. Profiling reports will be published separately.

---

## 3. Energy Cost

Quarry operation is gated by energy input. Each block broken consumes a fixed, configurable amount of energy. The default philosophical invariant is:

> **Energy-per-block remains constant**, regardless of upgrade level or mining speed.

Speed upgrades increase total blocks mined per tick and raise energy consumption proportionally. This enforces a **conservation constraint** across all execution paths, maintaining gameplay balance and allowing predictable scaling.

This invariant can be relaxed or modified via the configuration system, but it defines the default behavior.

---

## 4. Output Semantics and Item Flow

All excavated blocks are immediately voided. As of the current development state:

- There is **no internal item buffer or inventory** within the quarry.

---

## 5. Client-Server Topology

All operational logic runs **server-side only**. The client handles:

- Visual feedback (quarry animations, frame visibility)
- Debug overlays (see SDR)
- GUI rendering (currently placeholder)

No mining logic, state transitions, or upgrade effects are computed client-side. This architecture ensures:

- **Determinism** - clients are passive observers of validated server state.
- **Security** - no client-driven events can modify quarry execution.
- **Synchronization integrity** - state divergence is structurally precluded.


BlockEntity Role and Constraints

QuarryBlockEntities are not responsible for active behavior. They function solely as passive anchors - spatially fixed structures used for configuration, UI access, and teardown hooks. All excavation logic, tick participation, and state evolution is handled externally by centralized systems.

Tile entity ticking is categorically disallowed. Runtime logic must not be introduced at the block entity level under any circumstances. This constraint ensures predictable tick cost, prevents per-tile update inflation, and enforces architectural clarity

---

## 6. Placement and Initialization Semantics

Upon placement, a quarry:

- Automatically begins excavation using default configuration.
- Registers its volume (currently static, dynamic sizing in progress).
- Does **not** require redstone signals or startup rituals (planned for future versions).

Interaction is minimal and primarily intended for developers at this stage. A placeholder GUI and debug commands are available, but these are noncanonical interfaces and may change without warning.

Volume is statically registered at placement in current builds. Planned support for dynamic resizing via a "mirror block" system is under development (see roadmap.md)


---

## Persistence and Regeneration Semantics (Planned)

Persistence for dynamic quarry state - including excavation progress, suppression boundaries, mirror-linked volumes, and upgrade metadata - is not yet implemented.

- Current behavior is non-persistent: all active state is reset on world reload.
- Future builds will serialize both static and derived state, with regeneration logic for suppressed fields and immutable pools.

This system will be version-aware but not version-stable. Fields may be marked volatile or discarded upon incompatibility. World load errors from obsolete state formats will fail fast and emit diagnostic logs under devmode.


## 7. Suppression and Global State Systems (GSI, ISP)

Two internal subsystems - **Global Suppression Index (GSI)** and **Immutable State Pool (ISP)** - govern large-scale behavior coordination and data immutability enforcement. These are:

- Not exposed in the public repository.
- Non-defensive and unsafe for external use.
- Subject to runtime-invisible failure if misused.

They are engineered for maximal performance and minimal branching but rely on strict execution invariants. See `readme/internal-api.md` for epistemic warnings and access policies.

These features are architecturally scaffolded but not yet exposed.

---
