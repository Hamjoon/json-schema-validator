{
    "group": "walk_shouldValidateSchema_false_noSchemas",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
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
                    "parameter": "instanceLocation",
                    "value": "mockJsonNodePath"
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": false
                }
            ],
            "expected": "Empty Set (no ValidationMessage) and no calls to validate(); also no calls to any schema.walk() because the internal schemas list is empty.",
            "description": "When shouldValidateSchema is false and the validator has no pre‑loaded schemas, walk() must skip validation and simply return an empty Set."
        }
    ]
}