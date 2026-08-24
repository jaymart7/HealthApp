// CANONICAL SOURCE for the AppScaffold pattern (bottom nav + docked FAB + quick-action sheet), per A6.
// This is a reference copy for humans/Claude Code, not an executed import — DCs cannot import
// arbitrary child content into a shared component. As of Session 3, Home.dc.html is the single
// app shell and owns the only live copy of this markup (all four tabs live inside it — Progress
// and Profile are stub content inside the same scaffold, not separate files). If a future session
// splits tabs back into separate top-level DCs, paste this markup verbatim into each under a
// "// CANONICAL SOURCE: appScaffold.js — do not edit here" comment.
// RULE: change the pattern here first, then re-paste into every live copy in the same session.
// Never hand-edit one copy in isolation — see COMPONENTS.md Concerns.
//
// Values below use theme.js token names; substitute `t.<token>` for the live theme object in context.

export const APP_SCAFFOLD_MARKUP_NOTES = `
BOTTOM NAV BAR
  container: height 80dp + bottom safe-area inset (24dp) = 104dp total, background t.surfaceContainer
  4 tabs, fixed order: Home(home) / Food(restaurant) / Progress(trending_up) / Profile(person)
  selected tab: icon+label onSecondaryContainer, pill indicator background secondaryContainer
  unselected tab: icon+label onSurfaceVariant, outlined icon
  labels always visible. tapping active tab again scrolls that screen to top.

DOCKED FAB
  position: absolute, right 16dp, docked so it overlaps the nav bar's top edge (not centered/free-floating)
  container primaryContainer, icon/label onPrimaryContainer, shadow Level 3 (only real shadow in the app)
  extended (icon + "Log") at rest / scrolling up; icon-only collapsed on scroll-down
  onClick opens the quick-action sheet

QUICK-ACTION SHEET
  modal bottom sheet, scrim color scrim @ 32% opacity covering the full screen
  sheet: surfaceContainerLow background, 28dp top corners, drag handle (onSurfaceVariant, 32x4px pill)
  three rows: Log food / Log weight / Add photo
  "Log food" routes into the photo capture flow (Session 2); the flow shows neither nav bar nor FAB
`;
