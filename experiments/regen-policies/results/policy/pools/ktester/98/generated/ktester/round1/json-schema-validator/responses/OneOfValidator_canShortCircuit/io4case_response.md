{
    "testCases": [
        {
            "group": "canShortCircuit_emptyValidatorList",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[]"
                        }
                    ],
                    "expected": true,
                    "description": "When the parent schema's validator list is empty, the method should compute and return `true` because no disallowed keywords are present."
                }
            ]
        },
        {
            "group": "canShortCircuit_nonTargetValidators",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"type\", \"minimum\", \"maxLength\"]"
                        }
                    ],
                    "expected": true,
                    "description": "When the validator list contains only keywords unrelated to `unevaluatedProperties` or `unevaluatedItems`, the method should return `true`."
                },
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"type\", \"unevaluatedProperties\", \"maximum\"]"
                        }
                    ],
                    "expected": false,
                    "description": "When the list contains a mix of unrelated keywords and one `unevaluatedProperties` keyword, the presence of the disallowed keyword forces the result to `false`."
                },
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"unevaluatedItems\", \"format\", \"pattern\"]"
                        }
                    ],
                    "expected": false,
                    "description": "When the list contains a mix of unrelated keywords and one `unevaluatedItems` keyword, the method should return `false`."
                }
            ]
        },
        {
            "group": "canShortCircuit_duplicateDisallowedKeywords",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"unevaluatedProperties\", \"unevaluatedProperties\", \"unevaluatedItems\"]"
                        }
                    ],
                    "expected": false,
                    "description": "Even if the disallowed keywords appear multiple times, the method should still return `false` after the first match."
                }
            ]
        },
        {
            "group": "canShortCircuit_cachingBehaviorAfterFirstCall",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"type\", \"minimum\"]"
                        },
                        {
                            "parameter": "preSetCanShortCircuit",
                            "value": "null"
                        }
                    ],
                    "expected": true,
                    "description": "First invocation computes `true` (no disallowed keywords) and caches it; a subsequent call should return the cached `true` without re‑examining the validator list."
                },
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"unevaluatedItems\", \"type\"]"
                        },
                        {
                            "parameter": "preSetCanShortCircuit",
                            "value": "null"
                        }
                    ],
                    "expected": false,
                    "description": "First invocation computes `false` because a disallowed keyword is present and caches it; a second call must return the cached `false` without re‑checking the list."
                },
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"type\", \"minimum\"]"
                        },
                        {
                            "parameter": "preSetCanShortCircuit",
                            "value": "true"
                        }
                    ],
                    "expected": true,
                    "description": "When `canShortCircuit` is already cached as `true`, the method should immediately return `true` and must not invoke `getKeyword()` on any validator (the test can verify this by using a mock that throws if called)."
                },
                {
                    "input": [
                        {
                            "parameter": "parentValidators",
                            "value": "[\"unevaluatedProperties\", \"type\"]"
                        },
                        {
                            "parameter": "preSetCanShortCircuit",
                            "value": "false"
                        }
                    ],
                    "expected": false,
                    "description": "When `canShortCircuit` is already cached as `false`, the method should immediately return `false` without evaluating the validator list."
                }
            ]
        }
    ]
}