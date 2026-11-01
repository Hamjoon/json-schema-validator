{
    "group": "getValidators - when read throws runtime exception",
    "cases": [
        {
            "input": [],
            "expected": "IllegalStateException is propagated from the read() method",
            "description": "A JsonSchema spy is configured so that the private read(JsonNode) method throws an IllegalStateException. The first call to getValidators() should propagate this exception without wrapping it."
        }
    ]
}