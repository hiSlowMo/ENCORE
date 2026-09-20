# 2026-09-19: Check-in Window Contract Tests

Added a test-only contract based on the existing documented inclusive interval:
`show start - 2 hours <= now <= show end` (`PROJECT_MEMORY.md` and the 2026-05-17
development log). Production Java code and administrator force check-in behavior
are unchanged.

Seven instants cover one nanosecond before/at/after opening, show start, and one
nanosecond before/at/after the end. Each instant exercises bound and unbound
ordinary check-in plus the schedule list's availability flag: 21 invocations.
Rejection asserts no ticket transition and no dashboard refresh publication.
The fixtures use explicit expected values and a fixed clock, not implementation
constants or a copied production predicate as their oracle.

## Controlled mutation evidence

The same test bytes were applied to five separate local backend copies of
`9017c9dcdf18c59436921a7456c81081efda06dc`, without changing the original checkout:

| Copy | Result |
| --- | --- |
| Original service | 21 passed |
| Change opening lead time from 2 hours to 1 | 6 assertion failures; no test errors |
| Change opening lead time from 2 hours to 3 | 3 assertion failures; no test errors |
| Exclude the opening instant in verify and availability | 3 assertion failures; no test errors |
| Exclude the ending instant in verify and availability | 3 assertion failures; no test errors |

These four errors were deliberately injected, not discovered defects in the
original service. Mutation runs prove sensitivity only to these declared changes;
the separate copies and raw logs remain outside the repository. They are not
included in the normal green test suite.

This is an in-process service contract with Mockito collaborators. It does not
exercise HTTP, database locks/transactions, a real host hook, openEuler or a live
user. It does not add general Java support to AET. At the initial local handoff,
independent review was deferred because the review agent had reached its usage
limit. A subsequent independent Astra review completed the source/test,
requirement-provenance, mutation-evidence and retained-hash checks with no
blocking findings in the declared scope. The reviewer did not rerun Java or the
mutants, and this agent review is not maintainer approval. The earlier pending
status is historical, not the current review state.

The normal `mvn test` run on this test-only branch passed 158 tests across 18
classes, with zero failures, errors or skips (Windows, Java 21.0.4 targeting Java
17, Maven 3.9.10). These results are separate from the nickname branch's 140 tests
and describe the initial single-branch validation, not a combined total.

Subsequently, the two published PR heads were merged in a new local clone and
validated at `f863af4958595cb2667aa67c7686bdd6ac50d5d4`: frontend 92 tests, frontend
type check/build, and backend 161 tests across 19 classes all passed without
failures or skips. The combined tree is
`3ee886f1d87920e1d376fa9d94d3b126bd781db0`. This is a separate local integration
run, not the sum of the earlier counts and not a remote CI run; its fixed source
identity and scope are also recorded in the PR descriptions.
