{
    "group": "OneOfValidator.walk - shouldValidateSchema = true",
    "cases": [
        {
            "input": [
                { "parameter": "executionContext", "value": "mockExecutionContext" },
                { "parameter": "node", "value": "mockJsonNode" },
                { "parameter": "rootNode", "value": "mockRootNode" },
                { "parameter": "at", "value": "\"/path\"" },
                { "parameter": "shouldValidateSchema", "value": "true" }
            ],
            "expected": "Set containing the ValidationMessage(s) returned by validate(). If validate() returns an empty set, the result is an empty set.",
            "description": "When shouldValidateSchema is true the method must invoke validate() and return exactly the messages produced by validate(). This case verifies the normal flow with a non‑empty validation result."
        },
        {
            "input": [
                { "parameter": "executionContext", "value": "mockExecutionContext" },
                { "parameter": "node", "value": "mockJsonNode" },
                { "parameter": "rootNode", "value": "mockRootNode" },
                { "parameter": "at", "value": "\"/anotherPath\"" },
                { "parameter": "shouldValidateSchema", "value": "true" }
            ],
            "expected": "Empty Set (no ValidationMessage).",
            "description": "When shouldValidateSchema is true but validate() returns an empty set, walk() must return an empty set. This ensures the method does not add any extra messages."
        }
    ]
}