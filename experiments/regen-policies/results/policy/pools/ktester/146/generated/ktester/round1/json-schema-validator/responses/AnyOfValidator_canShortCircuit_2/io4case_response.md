{
    "group": "AnyOfValidator.canShortCircuit() - additional edge cases",
    "cases": [
        {
            "input": [],
            "expected": "true",
            "description": "canShortCircuit field is null, parent schema returns a validator list containing a validator whose getKeyword() returns null (or empty string) and another validator with a harmless keyword (e.g., \"type\"). Neither keyword matches the short‑circuit blockers, so the method should leave the flag true, cache it and return true."
        },
        {
            "input": [],
            "expected": "false",
            "description": "canShortCircuit field is null, parent schema returns a validator list where the first validator has a non‑blocking keyword (e.g., \"minimum\") and a second validator has the blocking keyword \"unevaluatedItems\". The loop should continue after the first validator and flip the flag to false when the second validator is examined, then cache and return false."
        },
        {
            "input": [],
            "expected": "java.lang.NullPointerException",
            "description": "Parent schema's getValidators() returns null. The method does not guard against a null list, so invoking the for‑each loop should throw a NullPointerException. The test verifies that the method fails fast in this unexpected situation."
        },
        {
            "input": [],
            "expected": "true",
            "description": "canShortCircuit field is initially null and the parent schema has only non‑blocking validators, so the first call caches true. After the first call, the parent schema is mutated to include a validator with keyword \"unevaluatedProperties\". A second call to canShortCircuit() must return the previously cached true value, proving that the cached result is not recomputed even when the underlying schema changes."
        },
        {
            "input": [],
            "expected": "false",
            "description": "canShortCircuit field is initially null and the parent schema contains a blocking validator (keyword \"unevaluatedProperties\"), so the first call caches false. The parent schema is then altered to remove that validator. A subsequent call must still return the cached false value, confirming that once cached the result is immutable for the lifetime of the validator instance."
        }
    ]
}