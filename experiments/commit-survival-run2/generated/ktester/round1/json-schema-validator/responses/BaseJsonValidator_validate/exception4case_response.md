{
    "group": "Invalid Schema Node",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "validJsonNode"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The validator's `schemaNode` field is set to a JSON array (e.g., `[\"type\",\"string\"]`) while the validator expects an object node. The `validate` method should detect the wrong schema node type and throw an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "validJsonNode"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The validator's `schemaNode` field is a scalar value (e.g., a text node \"string\") instead of an object. The method must reject this configuration with an IllegalArgumentException."
        }
    ]
}