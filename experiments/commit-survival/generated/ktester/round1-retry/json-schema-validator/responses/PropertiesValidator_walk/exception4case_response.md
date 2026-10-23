{
    "group": "testWalkApplyDefaultsStrategyNull",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "nonNullObjectNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "null"
                },
                {
                    "parameter": "at",
                    "value": "\"/\""
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": "true"
                }
            ],
            "expected": "NullPointerException",
            "description": "applyDefaultsStrategy field is deliberately set to null (via reflection) and node is a non‑null ObjectNode. The method attempts to call applyDefaultsStrategy.shouldApplyPropertyDefaults() leading to NPE."
        }
    ]
}