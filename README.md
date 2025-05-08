# VectorQuarry

**VectorQuarry** is a high-performance, block-breaking quarry mod for modern Minecraft versions, designed as a rigorous reimagining of the *BuildCraft* quarry ethos. Faithful to the principle of real physical excavation, yet engineered to scale across thousands of instances with minimal server overhead.

VectorQuarry is an attempt to reconcile physical excavation with modern tick-scale performance -eschewing virtualization in favor of deterministic, server-friendly terrain manipulation.

VectorQuarry is under active development. It is not feature-complete, and the current repository is intended for developers, performance testers, and architecture reviewers, not end users. Most user-facing features (GUI, config UIs, intermod compatibility, redstone control, etc.) are incomplete or placeholder

Note: This repository does not contain runnable quarry logic. Critical internal systems (GSI, ISP) are withheld; runtime execution is structurally disabled.

## Versioning Philosophy

VectorQuarry does **not** currently adhere to semver or guarantee stability of public or internal APIs. All interfaces, configurations, and behavioral patterns are subject to change without notice.

However, one invariant *will not be relaxed*:

> **Porting will never degrade core performance or system coherence.**

If future Minecraft versions introduce breaking changes that would force performance regression, spatial inconsistency, or abstraction leakage, porting to that version will be suspended or canceled. Partial rewrites may be considered, but never at the cost of violating the mod's foundational constraints.

Until a public API is formally released, all consumers should treat all systems as volatile and unsupported.

## Repository Overview

This repository is intended for architecture reviewers, profiling engineers, and contributors evaluating core design constraints. If you are here to:

- **Understand the motivation behind the mod** - read `concept.md`
- **Learn how core systems interrelate** - read `architecture.md`
- **See what runtime logic is planned or scaffolded** - read `roadmap.md`
- **Get definitions for internal terms and execution models** - read `glossary.md`
- **Understand what contributions are allowed, when, and why** - read `contribution.md`
- **Preview closed-system constraints and API exposure policy** - read `internal-api.md`
- **Explore future upgrade mechanics and balancing invariants** - read `upgrades.md`

Note: This codebase is intentionally incomplete. Subsystems like GSI (Global Suppression Index) and ISP (Immutable State Pool) are structurally required but not present in this repository. Without them, quarry logic will not run. To understand why please consult: `internal-api.md`

We welcome critique and architectural feedback. Runtime testing, issue reports, and patches are not currently accepted.

