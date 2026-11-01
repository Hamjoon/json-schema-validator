{
    "group": "Pre‑walk listener returns true – validator executed",
    "cases": [
        {
            "input": [
                { "parameter": "executionContext", "value": "mockExecutionContext" },
                { "parameter": "node", "value": "mockNode" },
                { "parameter": "rootNode", "value": "mockRootNode" },
                { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                { "parameter": "shouldValidateSchema", "value": true }
            ],
            "expected": "The validator’s walk() method is invoked. If it returns a non‑empty Set, those ValidationMessage objects are present in the Set returned by JsonSchema.walk(). Post‑walk listeners are invoked exactly once after the validator execution.",
            "description": "Pre‑walk listener signals SKIP = false, therefore the validator is executed. The test supplies a validator that returns a Set containing a single ValidationMessage. The test asserts that the returned error Set contains that message and that post‑walk listeners have been called."
        },
        {
            "input": [
                { "parameter": "executionContext", "value": "mockExecutionContext" },
                { "parameter": "node", "value": "mockNode" },
                { "parameter": "rootNode", "value": "mockRootNode" },
                { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                { "parameter": "shouldValidateSchema", "value": false }
            ],
            "expected": "The validator’s walk() method is invoked with shouldValidateSchema = false. Returned errors are added to the final Set. Post‑walk listeners are invoked.",
            "description": "Same as the previous case but the boolean flag is false, ensuring the flag is correctly propagated to the validator."
        }
    ]
}