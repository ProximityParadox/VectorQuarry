# **VectorQuarry Glossary**

### **Core Subsystems and Data Structures**

* **GSI (Global Suppression Index)**
  An internal system that tracks and enforces region-based suppression across excavation volumes. Prevents world updates, block changes, or redundant computation within defined suppression fields. Not exposed publicly; unsafe and semantically volatile.

* **ISP (Immutable State Pool)**
  A deduplicated, read-only data structure for shared runtime state. Used to enforce immutability constraints and avoid memory churn during batch execution. Unsafe outside controlled call graphs; not validated at runtime.

* **SDR (Suppression Debug Renderer)**
  A nonstandard, high-performance renderer for visualizing suppression fields in-world. Uses custom matrix and vertex math to bypass Minecraft's standard rendering pipeline. Currently closed-source and unsafe under typical conditions.

* **Suppression Field**
  A bounded spatial exclusion zone used to prevent redundant updates or physical side effects during excavation. Internally implemented using `BitSet`-like structures for efficient volumetric masking.

* **Mirror System**
  A user-facing mechanism for defining quarry dimensions in-world. Enables diegetic specification of volume boundaries without config files or UI sliders.

* **Scratchpad State**
  A per-tick, ephemeral memory structure used to store intermediate excavation or suppression data. Some allocations are pooled to reduce GC overhead; others are instantiated dynamically. Unsafe to mutate across tick boundaries.

---

### **Execution and Temporal Semantics**

* **Execution Path**
  A generalized term for a semantically valid progression through quarry logic. May refer to low-level function call order, high-level system interaction patterns, or rule-preserving world state transitions. Used both architecturally (system flows) and operationally (tick-stage behavior).

* **Tick Coalescence**
  A performance strategy wherein many operations (e.g., block breaking) are aggregated into a single tick cycle. Prevents per-tile update pathologies by enforcing bulk execution over discrete time units.

* **Batch Processing**
  The core execution model for excavation logic. Instead of handling one block at a time per tick, VectorQuarry executes operations in pre-sized batches for improved CPU locality and reduced branching.

* **Excavation Cycle**
  One complete pass of quarry logic over a discrete Y-level or batch-sized volume segment. Cycles are deterministic and advance layer-by-layer from top to bottom.

* **Deterministic Excavation**
    Deterministic Excavation: The excavation algorithm produces identical outputs for identical initial conditions - quarry position, volume definition, configuration state, and world state. No randomness, concurrency variance, or environmental sampling is permitted. Used to guarantee replayability, synchronization, and debugging integrity

---

### **Systemic Constraints and Performance Invariants**

* **Throughput Scaling**
  The mod's capacity to increase excavation rate without violating energy constraints or incurring nonlinear CPU cost. Achieved through upgrade systems and tick batching.

* **Memory Locality**
  The architectural requirement that memory access patterns remain cache-coherent. All execution paths are designed to minimize random-access memory reads or writes.

* **Branching Cost**
  The computational penalty incurred by runtime conditionals or mispredicted branches. Minimizing branching is a first-class goal in both excavation logic and suppression handling.

* **Execution Invariants**
    Properties of the system that must hold across all runtime states - for example, immutability of ISP data or non-overlapping suppression bounds. Violation of invariants is considered a **category error**, not a bug.

* **Category Error**
    A structural misuse of a system that violates its foundational assumptions - not just a runtime fault, but an epistemic misclassification. These errors occur when external logic interacts with internal systems in ways the architecture was never meant to permit or defend against. They are uncatchable in production, crash explicitly in devmode, and must be treated as invalid at the level of system design.



---

### **Development, Diagnostics, and Exposure Models**

* **Unsafe System**
    A subsystem that assumes strict internal discipline and does not enforce invariants at runtime. It will operate correctly only when used within its intended execution constraints; violations may cause silent state corruption or undefined behavior. Safety is enforced in development mode, not in production.

* **Volatile System**
    A subsystem whose semantics, interfaces, or invariants are still subject to fundamental change. Volatile systems may appear functional but are not stable across builds or documentation cycles, and should not be targeted by external code or user expectations.

* **Closed System**
    A subsystem deliberately withheld from public access due to safety, volatility, or architectural fragility. Closed systems are undocumented, unsupported, and subject to change without notice; external interaction is forbidden until formal API exposure.

* **DevMode**
  A debug configuration toggle that enables runtime validation of otherwise unchecked invariants. Introduces performance overhead but crashes early when system contracts are violated. Intended for internal builds only.

* **Contributor Window**
  A future period when public contributions will be allowed, contingent on stabilization of closed systems and publication of a validated API. Prior to this, all contributions are rejected by policy.

* **Semantic Volatility**
  The condition of a system or API whose behavior is still changing at the level of meaning, not just surface syntax. Volatile systems are not ready for external use, even if their interfaces appear stable.

