## Closed Systems and API Disclosure Policy

### Why You Can't See the GSI or ISP in the Repository

Two of the most performance-critical subsystems in *VectorQuarry* - the **Global Suppression Index (GSI)** and the **Immutable State Pool (ISP)** - are currently **not published** in the public source tree. This is intentional and non-negotiable in the current development phase.

These systems are **unsafe** in the precise, technical sense: they make deep assumptions about memory safety, data immutability, and execution ordering that are **not validated at runtime** and **will fail silently** if misused. They are engineered to operate within narrowly defined execution invariants and are *not defensive* against improper use. A small deviation - e.g., mutating a scratchpad block position without first copying - can lead to undefined behavior across later stages of execution, with no stack trace or external indication of failure. Such issues are not bugs in the traditional sense; they are *category errors* - violations of architectural constraints that were never meant to be externally enforced.

Further, these components constitute the locus of *VectorQuarry*'s re-architectural and performance innovations. They are subject to **frequent, fundamental changes**, many of which are incompatible with previous states and assumptions. Publishing unstable or semantically volatile systems prematurely would mislead contributors and destabilize the userbase. Until they reach a level of **stability, safety, and abstraction** suitable for general use, they will remain internal.

Once hardened, a public API may be released with defined invariants and validation layers, but that process will be cautious and incremental.

### On Reverse Engineering

You are **discouraged** from attempting to de-obfuscate or reverse-engineer these systems from the distributed `.jar`. This is not because it is impossible, but because it is dangerous and epistemically misleading. The systems are not designed to be readable or interpretable in their current state, and inferring behavior from implementation details is likely to result in misapplication.

That said, you are **not prohibited** from doing so. If you reverse-engineer these systems, you do so at your own risk. If you footgun yourself via misuse of suppressed systems, you **must not** submit issue reports relating to these internal components. They are unsupported until explicitly documented and published.

However, you **are permitted** to send private fixes or generalizations derived from de-obfuscated analysis. If you identify a concrete bug, safety violation, or optimizable pattern within the GSI, ISP, or their dependent logic, and submit a clean, reproducible patch that improves stability or abstraction, it will be **considered for review**. If accepted, your contributions will be acknowledged and published with the eventual API release.

### Where Is the Suppression Debug Renderer (SDR)?

The **Suppression Debug Renderer (SDR)** is similarly withheld. It relies on **hand-rolled matrix and vertex math** to render arbitrary 3D debug geometry for suppression fields. This renderer bypasses large portions of Minecraft's rendering engine and does not follow standard visual update protocols. It is fragile, volatile, and can cause significant performance issues if used improperly.

Until it is encapsulated and made safe for general rendering use, the SDR will remain closed-source.


## Developer Mode: Epistemic Crash Enforcement

VectorQuarry includes a developer-mode toggle that enables **aggressive internal checks** against architectural misuse. When `DEV_SAFE_MODE=true`, the mod performs runtime validation of state invariants, memory safety boundaries, and suppression field coherence. This mode is explicitly **non-performant** and intended only for internal testing and future contributor debugging.

- Violations trigger **immediate crashes** or loud logs with tracebacks and contextual dump state.
- Devmode is mutually exclusive with production profiling or server-scale deployment.
- These checks are designed to catch category errors early - e.g., improper mutation of scratchpad state or execution order violations.

This system is structurally analogous to runtime assertions in unsafe systems programming. It is not a user-facing feature and is unsupported outside designated internal builds.
