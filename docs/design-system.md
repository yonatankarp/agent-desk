# Design system

All client visual styling lives in the `:design` Compose Multiplatform module. The
desktop and mobile clients consume it; **clients must not hand-roll palettes, type, or
spacing.** This keeps status, color, and layout meaning consistent across surfaces (a
Vision principle: shared domain semantics across desktop, mobile, and backend).

## What `:design` owns

- **Tokens** (`com.yonatankarp.agentdesk.design.token`): `AgentDeskColors` (light + dark
  surfaces/text), `StatusColors` (per `StatusTone` `text`/`rail`/`pillBg`),
  `AgentDeskSpacing` (spacing, line width, radii, rail width), `AgentDeskElevation` (shared
  tonal/shadow depth), and `AgentDeskTypography` (bundled Inter for UI, JetBrains Mono for IDs/timestamps; both OFL — see
  `design/THIRD_PARTY_LICENSES/`).
- **Theme** (`com.yonatankarp.agentdesk.design.theme`): `AgentDeskTheme { }` provides the
  tokens via composition locals and a Material3 scheme. Access through the
  `AgentDeskTheme` object (`.colors`, `.status`, `.spacing`, `.elevation`, `.typography`,
  `.statusRole(tone)`).
- **Components** (`com.yonatankarp.agentdesk.design.component`): `Panel`, `ActionRow`
  (status rail + monospace id + pill), `StatusPill`, `EventRow`, `EvidenceItem`,
  `SummaryChip`, `ThemeModeControl`.

## Light / dark

`ThemeMode` is `System | Light | Dark` (default `System`, following the OS). Both clients
expose a `ThemeModeControl` toggle. Desktop persists the choice to
`~/.agent-desk/theme` via `DesktopThemeModeStore`; mobile is in-memory per session
(durable mobile persistence is a follow-up). The CLI has no theme mode — the terminal
owns its background — but it applies the same `StatusTone` color semantics as ANSI
(truecolor), disabled under `NO_COLOR` or when stdout is not a TTY.

## Guardrails

- `:design` depends only on `:app`/`:core`; a Konsist test forbids any dependency on
  client/runtime modules.
- Token contrast is pinned: `AgentDeskContrastTest` keeps muted/secondary text and status
  pill text at WCAG AA, and `StatusColorsTest` keeps the light Attention rail visually
  distinct from the Blocked rail.
