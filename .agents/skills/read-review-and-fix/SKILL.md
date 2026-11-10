---
name: read-review-and-fix
description: Review unresolved comments on the current GitHub pull request, validate each suggestion, add missing regression tests before fixes, and implement and verify valid changes.
---

# Review and fix pull request feedback

Use this skill to work through unresolved review conversations on the current GitHub pull request. Make each decision against the code and its surrounding behavior, preserve the repository's test conventions, and report the outcome of every suggestion.

## Workflow

1. Use `gh` to identify the pull request for the current branch. Confirm the repository and pull request before proceeding. If there is no current pull request or the target is ambiguous, report that and stop rather than selecting an unrelated PR.
2. Retrieve all unresolved review conversations. Keep comments grouped by conversation so that context, follow-up messages, and the reviewer's final intent remain together.
3. For each suggestion, inspect the relevant implementation, callers, related tests, and any necessary surrounding behavior. Evaluate suggestions independently, while accounting for dependencies between them.
4. Decide whether each suggestion is valid for the intended behavior. Record a concise rationale for accepted and rejected suggestions. Do not implement suggestions that are incorrect, already satisfied, or outside the intended behavior.
5. For every valid suggestion, decide whether an automated test could have detected the issue before review. If so, check whether the existing test suite already covers that behavior. Treat a missing or inadequate test as a test gap. Add a focused regression test using the repository's existing test framework, fixtures, and conventions. The test must assert the externally relevant behavior or invariant that the suggestion identifies; avoid tests that merely mirror implementation details.
6. Before changing production code for a testable issue, run the new regression test against the current implementation and confirm that it fails for the expected reason. This establishes that the test detects the reported issue. Then implement the valid fix. If the regression test does not fail as expected, refine or remove the ineffective test before proceeding. If the test cannot be run in the available environment, explain why and do not claim that its expected failure was verified.
7. Implement other valid fixes that do not have a meaningful automated test, and make reasonable test updates for any existing tests affected by the change.
8. Run the relevant regression tests and other focused tests for the affected behavior. Report commands and outcomes accurately; distinguish tests that passed from tests that could not be run.
9. Summarize the result by conversation or suggestion, including accepted fixes, rejected suggestions with rationale, regression tests added, and verification results.

## Boundaries

- Do not push commits or changes to the remote.
- Do not reply to, resolve, or otherwise mutate GitHub review conversations unless the user explicitly authorizes that action.
- Keep changes limited to valid review feedback and the tests needed to cover it.
