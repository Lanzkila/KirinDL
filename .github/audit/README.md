# KirinDL Audit — Assistant Handoff

> **Read this file first when continuing KirinDL work in a new ChatGPT conversation.**
>
> This folder is the persistent handoff note for the assistant. The user should be able to say:
> **"Baca folder .github/audit dulu."**
> The assistant must then read this README, check the latest GitHub Actions audit run, and continue using the current repository state rather than relying on the previous chat's memory.

## Repository

- Repository: `Lanzkila/KirinDL`
- Default branch: `main`
- Audit workflow: `.github/workflows/kirindl-update-audit.yml`
- Assistant trigger file: `.github/audit/assistant-trigger.txt`
- This handoff file: `.github/audit/README.md`

## What this workflow is for

This is a repository-health and Android regression-check workflow intended to be run and inspected by the ChatGPT assistant while working on KirinDL. It is not just a workflow for the user to press manually.

It has three ways to start:

1. **Automatic:** every push to `main` starts the audit.
2. **User manual:** open GitHub Actions → **KirinDL Update Audit** → **Run workflow**. The workflow also has `workflow_dispatch`.
3. **Assistant-triggered:** the connected GitHub tools do not currently expose a direct `workflow_dispatch` action. To start a separate audit, the assistant updates `.github/audit/assistant-trigger.txt` with a new unique `request_id` and commits that change to `main`. The push starts the same audit workflow. Use a clear commit message such as `ci: assistant starts KirinDL audit`.

Do not claim the assistant can dispatch directly unless the available GitHub connection actually provides that capability. The trigger-file commit is the supported assistant route currently tested.

## Checks performed

The workflow currently checks:

- Checkout, JDK 21, Android SDK, Python 3.11 and Gradle environment setup.
- Tracked JSON, XML and Python files for syntax/parse errors.
- Required Android/Gradle project files are present.
- The moved website pages exist under `pages/` and preserve the public permalinks:
  - `/support.html`
  - `/contact.html`
  - `/privacy.html`
- The moved favicon is under `pages-assets/favicon.png`, with no duplicate at repo root.
- Local website links, sitemap entries and moved-page output routes resolve.
- The moved trigger files exist under `.github/triggers/`, old root trigger files are absent, and active workflows reference the new paths.
- GitHub Actions workflow YAML parses successfully.
- Kotlin formatting via `:app:ktfmtCheck`.
- Android Lint via `:app:lintGenericDebug`.
- Unit tests via `:app:testGenericDebugUnitTest`.
- Android debug compilation via `:app:assembleGenericDebug`.

The workflow produces a Markdown summary and uploads diagnostic logs/reports as a GitHub Actions artifact for 14 days.

### Limits

- This is automated validation, not a guarantee that no runtime bugs exist.
- Formatting, lint, unit tests and compilation do not replace a human/assistant review of the changed code and its intended behavior.
- No signed release, GitHub Release publication, or APK publishing should be triggered by this audit.
- The audit builds a debug variant only; it does not install the app on a physical phone.
- The workflow's concurrency group cancels an older audit on the same ref when a newer push starts another one. Always inspect the newest run and its final conclusion.

## How the assistant must use the audit

When asked to continue KirinDL work:

1. Read this file and `assistant-trigger.txt`.
2. Check the latest run for `.github/workflows/kirindl-update-audit.yml` on GitHub Actions. Do not rely on the status snapshot below if a newer run exists.
3. If the user asks to audit, or a code change needs verification, start an assistant-triggered audit by updating `request_id` in `assistant-trigger.txt` to a unique value and committing it to `main`.
4. Follow the run until it completes when the tools allow. Inspect job steps, summary, uploaded artifact and logs for any failure or warning.
5. If the audit reports a problem, inspect the relevant source and logs, fix only the related issue, and re-run the audit. Do not silently skip failed checks.
6. Report clearly which checks passed, failed, were cancelled or were not run. Never describe an in-progress or cancelled run as passed.
7. Keep this README updated after meaningful changes to the audit workflow, trigger method, test coverage or current audit state.

## Current setup and history

- `a47a3a4` — added `.github/workflows/kirindl-update-audit.yml`.
- `c4540eb` — added the assistant trigger file and verified that changing/committing it starts a new **KirinDL Update Audit** run.
- Audit run `43832397678` (first workflow run): cancelled after the assistant-trigger commit started a newer run. Its structure/website checks, YAML validation and Kotlin format check had passed; lint was cancelled and unit tests/build were skipped.
- Audit run `43832990431` (second run, assistant-triggered): at the last recorded check, checkout, JDK, Android SDK, Python, Gradle setup, repository/website checks and YAML validation had passed; Kotlin formatting was still running. Its final result must be checked live before relying on it.

Useful links:
- [Audit workflow file](../workflows/kirindl-update-audit.yml)
- [Assistant trigger file](assistant-trigger.txt)
- [GitHub Actions audit runs](https://github.com/Lanzkila/KirinDL/actions/workflows/kirindl-update-audit.yml)
- [Repository](https://github.com/Lanzkila/KirinDL)

## New-chat instruction

When the user says **"Baca folder audit dulu"**, do not ask them to repeat the context. Open this README, inspect the live workflow and latest audit run, then summarize the current state and proceed with their KirinDL request.
