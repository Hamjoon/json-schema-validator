{
    "tests": [
        {
            "group": "OneOfValidator.canShortCircuit() - keyword edge cases",
            "cases": [
                {
                    "input": [],
                    "expected": true,
                    "description": "First invocation when the parent schema contains a validator whose `getKeyword()` returns `null`. The equality checks ignore null, so the flag stays true and the method returns true."
                },
                {
                    "input": [],
                    "expected": true,
                    "description": "First invocation when the parent schema contains a validator whose `getKeyword()` returns an empty string. No match with the disallowed keywords, therefore the method returns true."
                },
                {
                    "input": [],
                    "expected": true,
                    "description": "First invocation when the parent schema contains a validator with keyword \"UnevaluatedProperties\" (different case). Equality is case‑sensitive, so the method treats it as unrelated and returns true."
                },
                {
                    "input": [],
                    "expected": true,
                    "description": "First invocation when the parent schema contains a validator with keyword \"myunevaluatedProperties\" (keyword is a substring, not an exact match). The method returns true."
                }
            ]
        },
        {
            "group": "OneOfValidator.canShortCircuit() - caching after schema change",
            "cases": [
                {
                    "input": [],
                    "expected": true,
                    "description": "First call on a schema whose validators list does **not** contain disallowed keywords → caches `true`. Afterwards the schema's validators list is mutated to include a validator with keyword \"unevaluatedProperties\". A second call still returns the cached `true` without re‑evaluating the list."
                },
                {
                    "input": [],
                    "expected": false,
                    "description": "First call on a schema that contains a validator with keyword \"unevaluatedItems\" → caches `false`. The list is then cleared of that validator. A second call still returns the cached `false`."
                }
            ]
        },
        {
            "group": "OneOfValidator.canShortCircuit() - null validators collection",
            "cases": [
                {
                    "input": [],
                    "expected": true,
                    "description": "First invocation when `getEvaluationParentSchema().getValidators()` returns `null`. The method should treat the collection as empty, keep the flag `true`, cache it and return `true`."
                }
            ]
        }
    ]
}