# ChatBase Full App UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give all 15 ChatBase pages and the application shell one cohesive, responsive light operations-workbench design while preserving all existing behavior.

**Architecture:** Keep Vue 3 single-file components and current routes/APIs. Define a compact semantic theme in `web/src/styles/anime.css`, use it in the application shell and shared components, then migrate page-specific layouts in route groups. Preserve existing script logic and use template/CSS changes for the redesign.

**Tech Stack:** Vue 3, TypeScript, Vite, CSS, lucide-vue-next.

---

## File map

- `web/src/styles/anime.css`: global semantic colors, typography, spacing, focus, motion, base controls, and app shell layout styles.
- `web/src/App.vue`: app navigation shell, responsive menu state, navigation grouping, user footer; auth and route logic stays intact.
- `web/src/components/{GlowCard,StatCard,LoadingSpinner,ProgressBar,KeywordCloud,UserProfile,AvatarCropper}.vue`: shared visual primitives and user dialogs.
- Page SFCs under `web/src/pages/`: route-specific layouts and scoped styles; retain each existing `<script setup>` behavior.
- `web/src/main.ts`: only if the global style import order must include a new stylesheet; do not change route definitions.

## Task 1: Establish semantic theme and responsive app shell

**Files:**
- Modify: `web/src/styles/anime.css`
- Modify: `web/src/App.vue`

- [x] **Step 1: Replace decorative global theme values with semantic tokens**

Set the following values in `:root` in `anime.css`, and map legacy `--anime-*` names to these values while page migration is in progress:

```css
:root {
  --ui-canvas: #f3f5f4;
  --ui-surface: #ffffff;
  --ui-surface-muted: #f7f9f8;
  --ui-text: #172321;
  --ui-text-secondary: #53615d;
  --ui-text-muted: #75827e;
  --ui-border: #dce4e0;
  --ui-primary: #087f70;
  --ui-primary-hover: #06685d;
  --ui-primary-soft: #e5f4f0;
  --ui-accent: #c57a25;
  --ui-danger: #bd3c45;
  --ui-success: #21845b;
  --ui-focus: #087f70;
  --ui-radius-sm: 6px;
  --ui-radius-md: 10px;
  --ui-radius-lg: 14px;
  --ui-shadow-card: 0 2px 8px rgb(19 48 41 / 6%);
}
```

Remove the body gradient, pink scrollbar treatment, star/petal decoration styles, and perpetual floating decoration. Add a `prefers-reduced-motion` rule that disables nonessential transitions and animations.

- [x] **Step 2: Restructure the existing app shell without changing auth behavior**

In `App.vue`, group current links under “工作台”, “内容”, and “管理”; retain all existing route targets and the `v-if="isAdmin"` guards. Replace the current decorative background elements with a plain application canvas. Keep `syncAuthState`, `loadUserProfile`, `handleLogout`, route watchers, and `UserProfile` event wiring unchanged.

- [x] **Step 3: Add a narrow-screen navigation toggle**

Add a button with `aria-expanded` and `aria-controls` to `App.vue`; the button toggles a Vue `ref` controlling the existing sidebar. Close the sidebar after a navigation link is selected. At widths below 900px, use a fixed overlay drawer and backdrop; at desktop widths, keep the sidebar visible. Keep the toggle operable by keyboard and give it a visible focus ring.

- [x] **Step 4: Define shared shell geometry**

Set desktop sidebar width to 248px, page horizontal padding to `clamp(16px, 2.4vw, 36px)`, and a 1480px content maximum. At mobile widths, remove the desktop left offset and ensure the drawer does not reduce the content viewport while closed.

## Task 2: Normalize shared components and common controls

**Files:**
- Modify: `web/src/components/GlowCard.vue`
- Modify: `web/src/components/StatCard.vue`
- Modify: `web/src/components/LoadingSpinner.vue`
- Modify: `web/src/components/ProgressBar.vue`
- Modify: `web/src/components/KeywordCloud.vue`
- Modify: `web/src/components/UserProfile.vue`
- Modify: `web/src/components/AvatarCropper.vue`
- Modify: `web/src/styles/anime.css`

- [x] **Step 1: Normalize buttons, inputs, selects, tables, and modal surfaces**

In `anime.css`, make the existing shared selectors use semantic tokens. Primary buttons use `--ui-primary`; destructive buttons use `--ui-danger`; disabled controls retain readable text; form focus uses a 2px `--ui-focus` outline. Tables use a muted header surface and row separators, with no pink drop shadow. Modal panels use `--ui-surface`, `--ui-border`, `--ui-radius-lg`, and `--ui-shadow-card`.

- [x] **Step 2: Migrate shared component scoped styles**

Replace literal pink/purple gradients and hard-coded text colors in the listed component styles with the semantic tokens. Preserve props, emitted events, crop/resize behavior, progress values, and keyword sizing. Verify that the shared controls still expose their current slot and prop APIs.

## Task 3: Redesign overview and analytics pages

**Files:**
- Modify: `web/src/pages/DashboardPage.vue`
- Modify: `web/src/pages/StatisticsPage.vue`

- [x] **Step 1: Give both pages one title and action pattern**

Use a page heading with title, one-line description, and right-aligned page actions. Keep the dashboard refresh action, statistics range controls, aggregation action, and keyword sync action in their current handlers.

- [x] **Step 2: Rebuild the dashboard information hierarchy**

Place today’s conversations, tokens, and success rate in the first row. Put cumulative and 30-day metrics in a compact secondary strip. Place bot status and operational totals in the main column and all six existing quick links in a narrower side column. Keep all current labels, field sources, loading/error/empty states, bot status wording, and computed value formatting; do not add chart data.

- [x] **Step 3: Restyle statistics cards and charts**

Keep current chart components/data and controls. Apply the semantic palette consistently to series, legends, chart surfaces, headings, success/failure labels, and empty states. Retain chart tooltip and filter behavior.

## Task 4: Redesign group and private-chat workspaces

**Files:**
- Modify: `web/src/pages/ImGroupsPage.vue`
- Modify: `web/src/pages/ImSingleChatPage.vue`

- [x] **Step 1: Standardize IM page toolbars**

Place platform/scope filters, list search, and refresh in shared wrapping toolbar patterns. Keep conversation-dependent binding controls beside the selected conversation and use the same spacing and control styles there. Keep current loading flags and handler calls.

- [x] **Step 2: Restyle the group list and message detail split**

Keep the existing list/detail split and pagination. Use a neutral list surface, a clear selected-row background, compact message metadata, and a readable message body. At 800px and below, stack the panels and preserve scrollable message history.

- [x] **Step 3: Restyle private-chat node pairing and approvals**

Keep pairing code creation/copy, node refresh, approve/reject/rename/revoke actions, and conversation binding unchanged. Show connection and approval states with both text and semantic status color. Keep action buttons reachable at 360px width.

## Task 5: Redesign knowledge, app, bot, and FAQ pages

**Files:**
- Modify: `web/src/pages/KnowledgePage.vue`
- Modify: `web/src/pages/AppPage.vue`
- Modify: `web/src/pages/BotManagePage.vue`
- Modify: `web/src/pages/FaqPage.vue`

- [x] **Step 1: Standardize page headings, filters, and primary actions**

Use the common heading/toolbar hierarchy on all four pages. Keep create, edit, delete, upload, sync, verify, and default-app handlers unchanged.

- [x] **Step 2: Restyle knowledge categories, KB cards, document table, and FAQ cards**

Use the same surfaces, borders, spacing, and action hierarchy. Retain category ownership affordances and existing tab selection. Wrap the document table in a horizontally scrollable container on narrow screens.

- [x] **Step 3: Restyle app and bot configuration forms/lists**

Make labels, validation errors, secret/config fields, and status chips consistent. Keep callback-configured WeCom distinct from connected bots, and preserve every existing form field and save handler.

## Task 6: Redesign login, chat, and feedback experiences

**Files:**
- Modify: `web/src/pages/LoginPage.vue`
- Modify: `web/src/pages/ChatPage.vue`
- Modify: `web/src/pages/FeedbackPage.vue`

- [x] **Step 1: Restyle login while preserving the login flow**

Use a centered, high-contrast sign-in panel with brand mark, labeled username/password fields, visible error feedback, and a full-width primary submit button on mobile. Keep the existing request, token storage, and redirect behavior.

- [x] **Step 2: Restyle chat session list, transcript, and composer**

Keep session creation, selection, title editing, history loading, file/URL attachments, sending, and feedback handlers. Give the transcript a readable max width, distinguish user/assistant messages through alignment and surface tone, and keep the composer visible on narrow screens.

- [x] **Step 3: Restyle feedback form and submission states**

Keep all existing fields, rating selection, validation, and submit/reset actions. Use consistent field spacing and announce validation/submission errors as text.

## Task 7: Redesign administrator and review pages

**Files:**
- Modify: `web/src/pages/FeedbackManagePage.vue`
- Modify: `web/src/pages/AdminAppsPage.vue`
- Modify: `web/src/pages/AdminKbsPage.vue`
- Modify: `web/src/pages/UserManagePage.vue`

- [x] **Step 1: Standardize admin data tables and pagination**

Use consistent table header, row hover, status chips, search/filter spacing, and pagination. Keep existing API calls, permission guards, sorting/filtering, and current-page behavior.

- [x] **Step 2: Restyle admin detail panels and mutation forms**

Apply consistent form sections, error messages, modal footers, and destructive action styling to user role/status/delete, app/KB administration, and feedback reply/status flows. Preserve every field and action handler.

- [x] **Step 3: Verify admin-only navigation remains role-gated**

Keep `isAdmin` checks in `App.vue` and route guards in `main.ts`; do not change admin authorization logic.

## Task 8: Integrate, review all routes, and build

**Files:**
- Review: all files listed above
- Modify: only additional page-local styles required to remove remaining legacy pink/purple surfaces or fix overflow

- [x] **Step 1: Scan for remaining legacy visual tokens**

Search `web/src/pages` and `web/src/components` for `anime-pink`, `anime-purple`, hard-coded pink/purple gradients, and per-page radius/shadow mismatches. Replace presentation-only references with the semantic theme; leave platform brand marks and data-dependent status colors intact.

- [x] **Step 2: Review responsive layout rules**

Check desktop, tablet, and 360px mobile widths. Confirm sidebar drawer, IM split panes, tables, dialogs, chat composer, and page toolbars remain usable without document-level horizontal overflow.

- [x] **Step 3: Run the production build**

Run from `web/`:

```powershell
npm run build
```

Expected: `vue-tsc && vite build` exits with code 0. Review any TypeScript errors or CSS build errors before completion.

## Workspace safety

This checkout already contains unrelated staged and unstaged work, including edits to many of the same frontend files. Keep all current index and working-tree changes. Do not reset, stash, clean, or commit page files as a group; make only the UI edits needed for this plan and leave integration/commit decisions to the user.
