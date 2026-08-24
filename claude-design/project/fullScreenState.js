// CANONICAL SOURCE for the FullScreenState pattern (mascot + heading + optional body + optional actions).
// Reference copy for humans/Claude Code, not an executed import — DC screens can't share arbitrary
// child markup, so each usage pastes this pattern verbatim under a
// "// CANONICAL SOURCE: fullScreenState.js — do not edit here" comment.
// RULE: change the pattern here first, then re-paste into every usage in the same session.
// Claude Code task: make this a real composable (e.g. a FullScreenState() function/component
// taking mascotState/heading/body/actions) instead of copy-pasted markup once the app has a
// real component layer.

export const FULL_SCREEN_STATE_MARKUP_NOTES = `
Layout: flex column, centered (align-items:center, justify-content:center), 32dp padding (24dp
  when actions follow — see Retry/Offline).
Mascot: primaryContainer rounded-square (64dp small size), eyes/mouth per current MascotState
  (Idle default; Sleepy for "nothing here yet" states; Thinking for in-progress states).
Heading: Poppins 500, 20-22px, onSurface, centered.
Body (optional): Inter 400, 14px, onSurfaceVariant, centered.
Actions (optional): PrimaryButton and/or SecondaryButton below the body, per screen.

Known usages:
  - Home.dc.html: Day-one Home empty state (Idle), Empty diary state (Sleepy), Progress/Profile
    stub tabs (Idle).
  - PhotoLogging.dc.html (Session 2): RetryScreen (photo instead of mascot), ManualSearchScreen
    (Sleepy, ends in a field instead of buttons), OfflineScreen (Sleepy, two buttons).
`;
