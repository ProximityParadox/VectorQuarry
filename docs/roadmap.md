## Development Roadmap

The following roadmap outlines current, pending, and future development tasks for *VectorQuarry*. This is a work-in-progress mod with most user-facing systems incomplete. Development is focused on correctness, performance, and system architecture first; user polish and ecosystem compatibility will follow.

This roadmap is not a changelog or feature wish list. Items are listed approximately in dependency order or structural sequencing. Some tasks are gated on others; others will evolve as system constraints or design paradigms shift. Future timelines are not guaranteed.

### Phase 1: Core System Completion

* **Upgrade System**

  * Write and finalize upgrade logic.
  * Ensure upgrades respect energy-per-block invariants.
  * Expose modifiers through the config system.

* **Config Hook-up**

  * Link configuration files to runtime logic.
  * Ensure consistency between config mutations and batch execution behavior.

* **Energy Compatibility Layer**

  * Integrate with major energy systems: **FE**, **RF**, **EU**, etc.
  * Architect modular compatibility to avoid hard dependencies.
  * Performance test energy abstraction overhead.

* **Item System Implementation**

  * Write item output logic for mined blocks.
  * Establish block-to-inventory logic with configurable ejection or piping behavior.
  * Lay groundwork for dupe protection and flow control.

* **Dupe Protection System**

  * Enforce invariants to prevent duplication exploits during item ejection or suppression edge cases.
  * Tied to item system, not standalone.

* **User-Facing Polish**

  * Finalize and implement textures.
  * Complete the **quarry frame animations** and related visual states.
  * Finish the placeholder GUI and expose relevant state introspection.

* **Quarry Size Configuration (Mirror System)**

  * Allow users to define quarry dimensions via an intuitive "mirror block" system (design under review).
  * Ensure correct registration and suppression bounds during dynamic resizing.

* **Performance Pass**

  * Additional optimization rounds are planned.

### Phase 2: Public Release

* **Publish Initial Release**

  * Target platform: **NeoForge**, **Minecraft 1.20.1**.
  * Profiled and validated on real server-scale workloads.

* **Documentation and Initial README Finalization**

  * API documentation (partial if needed).
  * Architectural guide and suppression subsystem explanation.

### Phase 3: Post-Release Development

* **GSI, ISP, SDR API Publication**

  * Harden internal systems for public consumption.
  * Validate safety, document invariants, publish with stable API surface.
  * Accept limited external contributions once stability is guaranteed.

* **Version Porting**

  * Forward port to newer versions (1.20.4+, 1.21, etc.) as needed.
  * Conditional backport to selected older versions if performance architecture remains viable.
  * No general backport support; only to active modding baselines.

* **Cross-Version Performance Harmonization**

  * Analyze JVM and engine-level differences between versions.
  * Optimize CQM/GSI behavior based on profiler deltas per version.

* **Optional Submodules: Waste Material Management**

  * Planned utility mod(s) to handle bulk non-valuable block output from quarries.
  * Low priority, but conceptually important for realism-preserving automation.

---

This roadmap is neither exhaustive nor final. Its structure reflects the system dependencies and architecture-centric priorities of *VectorQuarry*, and will evolve with testing, contributor feedback, and performance validation.
