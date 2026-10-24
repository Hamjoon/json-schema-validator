# Run 2 status

Completed through addendum 3.

Denominators: Dev 185, KTester 131, Ouédraogo 0, Ouédraogo (adjusted) 22, Ouédraogo (adjusted 2) 41. Final survivors: {'dev': 4, 'ktester': 0, 'ouedraogo': 0, 'ouedraogo-adj': 0, 'ouedraogo-adj2': 0}.

1145 survival records and 905 coverage records. Executable line sets match across all five groups at all 181 indices. Previous group evidence and report values are unchanged. Exactly 25 new model calls, no retries.

The dependency renderer now emits type parameters only when present, preserves declaration modifiers (including implicit public/static modifiers of interface member types), and preserves varargs as Type... instead of arrays. All five dependency blocks passed the mandatory pre-call gate; their type order, membership, omitted types, caps, API order, and prompt constraints match addendum 2. Only the requested header and varargs formatting changed.

The earlier adjusted group retains its documented renderer limitation; its records were not changed. See addendum3-summary.json, addendum3-audit.json, and docs/handover-part9.md. No push.
