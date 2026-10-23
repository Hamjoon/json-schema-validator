{
    "group": "OneOfValidator.walk - shouldValidateSchema = true, executionContext null",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                },
                {
                    "parameter": "node",
                    "value": "mockJsonNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "mockRootNode"
                },
                {
                    "parameter": "at",
                    "value": "\"/anyPath\""
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": "true"
                }
            ],
            "expected": "NullPointerException (or any unchecked exception propagated from validate when executionContext is null)",
            "description": "When shouldValidateSchema is true and the ExecutionContext argument is null, walk() forwards the call to validate(). The validator is expected to require a non‑null ExecutionContext, causing a NullPointerException (or similar) which must propagate."
        }
    ]
}