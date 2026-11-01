{
    "group": "walkAtNode - basic parameter validation",
    "cases": [
        {
            "input": [
                { "parameter": "executionContext", "value": "null" },
                { "parameter": "node", "value": "null" },
                { "parameter": "rootNode", "value": "null" },
                { "parameter": "instanceLocation", "value": "null" },
                { "parameter": "validate", "value": "false" }
            ],
            "expected": "NullPointerException thrown because executionContext is required",
            "description": "Verify that the method rejects a null ExecutionContext before any other processing."
        },
        {
            "input": [
                { "parameter": "executionContext", "value": "validExecutionContext" },
                { "parameter": "node", "value": "null" },
                { "parameter": "rootNode", "value": "validRootNode" },
                { "parameter": "instanceLocation", "value": "validInstanceLocation" },
                { "parameter": "validate", "value": "true" }
            ],
            "expected": "ValidationResult with isValid = false and a message indicating that the instance node is missing",
            "description": "When the node to be validated is null, walkAtNode should produce a ValidationResult describing the missing node."
        },
        {
            "input": [
                { "parameter": "executionContext", "value": "validExecutionContext" },
                { "parameter": "node", "value": "validObjectNode" },
                { "parameter": "rootNode", "value": "null" },
                { "parameter": "instanceLocation", "value": "validInstanceLocation" },
                { "parameter": "validate", "value": "true" }
            ],
            "expected": "ValidationResult with isValid = false and a message about missing root node for reference resolution",
            "description": "If rootNode is null while validation is requested, the method should fail early because reference resolution cannot be performed."
        },
        {
            "input": [
                { "parameter": "executionContext", "value": "validExecutionContext" },
                { "parameter": "node", "value": "validObjectNode" },
                { "parameter": "rootNode", "value": "validRootNode" },
                { "parameter": "instanceLocation", "value": "null" },
                { "parameter": "validate", "value": "true" }
            ],
            "expected": "ValidationResult with isValid = false and a message indicating that instanceLocation is required",
            "description": "When instanceLocation is null, the walk cannot compute the JSON Pointer for error messages, so it should return an error."
        }
    ]
}