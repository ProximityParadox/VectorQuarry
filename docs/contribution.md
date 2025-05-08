# Contributing to VectorQuarry

VectorQuarry is a systems-first mod with strong architectural invariants and non-public internal subsystems. While we welcome future contributions, the project is currently not in a state where outside pull requests or runtime testing are viable. This document outlines the project's contribution model - what is acceptable, what is out of scope, and when engagement will be possible.

---

## Current Status: Contributions Disabled

As of now, **external contributions are not accepted**, and **the mod is not runnable** outside of internal development environments. Two critical subsystems - the **Global Suppression Index (GSI)** and **Immutable State Pool (ISP)** - are not present in the public repository. Without them, no quarry logic will execute, and most runtime paths will fail silently or throw irrecoverable errors.

**Do not attempt to fork, build, or run this repository unless explicitly invited to do so.**

---

## Philosophy of Contribution

VectorQuarry is not a grab-bag of features. It is a carefully constrained response to a structural failure in the Minecraft modding ecosystem: the false dichotomy between realism and performance. Every system is designed with minimal coupling, strict memory locality, and batch-oriented processing as first-class concerns.

Contributions to this project, when allowed, will be reviewed for:

- **Architectural coherence**: Does the contribution preserve system invariants?
- **Computational discipline**: Are performance, memory, and execution order explicitly considered?
- **Thematic alignment**: Does it maintain VectorQuarry's commitment to physical realism and spatial determinism?

If your instinct is to "just make it work," this is not your mod. If you understand that certain tradeoffs are non-negotiable, even if inconvenient, then your future contributions will be welcome.

---

## When Contributions Will Be Accepted

Contribution windows will open **after the following conditions are met**:

1. The GSI and ISP subsystems are stabilized, documented, and partially exposed.
2. A minimal runnable build of the mod is published.
3. Public APIs are frozen or versioned with formal invariants.

These milestones will be tagged in the repository and announced in the `roadmap.md` file.

---

## Future Contributions: What Will Be Accepted

Once the internals are published and execution is possible, contributions in the following categories will be considered:

- Performance improvements backed by profiling evidence.
- New upgrade types that respect the energy-per-block invariant.
- Configuration system extensions (e.g. richer balancing knobs).
- UX or visualization improvements (e.g. debug renderers, config UIs).
- Platform integration (e.g. NeoForge energy API adapters).
- Documentation improvements, tutorials, and architectural explanations.

**All contributions must align with the mod's design ethos.** If your patch breaks spatial determinism or introduces implicit state, it will be rejected regardless of user benefit.

---

## Future Contributions: What Will Be Rejected

Certain categories of contributions are incompatible with VectorQuarry's foundational principles and will be rejected without discussion:

- "Magic blocks" that teleport, duplicate, or conjure items.
- Digital miners or abstractionist block harvesting systems.
- Wireless item, energy, or upgrade transfer.
- Tick-based per-block logic or uncoalesced update paths.
- Features that bypass terrain excavation or override physical constraints.

---

## Bug Reports and Issue Policy

Until the mod is runnable, please **do not open issues** related to:

- Missing class errors, crashes, or null pointers caused by absent subsystems.
- GUI elements failing to appear or operate.
- Runtime behavior not matching documentation (it doesn't run yet).

However, we **welcome**:

- Architectural critiques.
- Conceptual questions about system tradeoffs.
- Feedback on documentation clarity or structural assumptions.
- Reports on typos, malformed configuration comments, or doc inconsistencies.

---

## Contact

For design discussions, conceptual critiques, or questions about future contribution windows, please contact **Nicholas** at:  
**Discord: ``**

Do not ask for support in running the mod. You cannot.

---

## License Reminder

All code in this repository is licensed under MPL.  
Closed systems like the GSI and ISP are internal IP and not covered by any public relicense until formally released.

---

This document will evolve as the mod approaches public stability. Until then, thank you for reading, and please respect the architectural boundaries.
