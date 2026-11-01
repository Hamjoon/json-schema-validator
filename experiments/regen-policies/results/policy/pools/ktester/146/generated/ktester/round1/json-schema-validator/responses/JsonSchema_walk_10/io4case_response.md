{
    "tests": [
        {
            "group": "Pre‑walk listener returns false – validator skipped",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "Validator.walk() is never invoked. The returned Set is empty. Post‑walk listeners are still invoked exactly once.",
                    "description": "The pre‑walk listener signals SKIP = true, therefore the validator must be bypassed. The test verifies that no validator interaction occurs, the final error Set is empty and the post‑walk listener is still called."
                }
            ]
        },
        {
            "group": "Validator returns null – no errors added",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "Validator.walk() returns null. The final Set returned by JsonSchema.walk() is empty. Post‑walk listeners are invoked.",
                    "description": "Ensures that a null result from a validator does not cause a NullPointerException and does not contribute any ValidationMessage to the final Set."
                }
            ]
        },
        {
            "group": "Validator returns empty Set – no errors added",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "Validator.walk() returns an empty Set. JsonSchema.walk() returns an empty Set. Post‑walk listeners are invoked exactly once.",
                    "description": "Verifies that an empty result from a validator does not affect the final error collection."
                }
            ]
        },
        {
            "group": "Multiple validators – errors aggregated",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "Both validators are executed (pre‑walk listeners return false). Their non‑empty result Sets are merged; the final Set contains all ValidationMessage objects from both validators. Post‑walk listeners are invoked for each validator.",
                    "description": "Checks that JsonSchema.walk() correctly iterates over all registered validators, aggregates distinct ValidationMessage objects, and still calls post‑walk listeners after each validator."
                }
            ]
        },
        {
            "group": "Validator throws exception – post‑walk still invoked",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "Validator.walk() throws a RuntimeException. The exception propagates out of JsonSchema.walk(). Despite the exception, the post‑walk listener is still executed exactly once before the exception bubbles up.",
                    "description": "Ensures that the finally block guarantees post‑walk listener execution even when a validator fails with an exception, and that the exception is not swallowed."
                }
            ]
        }
    ]
}