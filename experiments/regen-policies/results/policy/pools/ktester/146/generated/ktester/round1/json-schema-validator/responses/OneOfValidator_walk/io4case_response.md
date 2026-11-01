{
    "testCases": [
        {
            "group": "Delegate to child schemas when shouldValidateSchema is false and node is null",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": null },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "Empty Set; each child JsonSchema in the schemas list receives a walk call with shouldValidateSchema=false (node is null)",
                    "description": "When shouldValidateSchema is false the validator must skip its own validation regardless of node value and delegate to every child schema, passing shouldValidateSchema=false. The returned set remains empty because OneOfValidator never adds messages in the else‑branch."
                }
            ]
        },
        {
            "group": "Do not invoke child schemas when shouldValidateSchema is true and node is non‑null",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNonNullNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "Set containing the ValidationMessage(s) returned by validate(..., true); verify that no child JsonSchema.walk(...) is invoked",
                    "description": "When both shouldValidateSchema is true and node is non‑null, OneOfValidator must execute its own validation path and must NOT forward the call to its child schemas. The result set is exactly what the protected validate method returns."
                }
            ]
        },
        {
            "group": "Validate returns empty set when shouldValidateSchema is true and node is non‑null",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNonNullNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "Empty Set (validate method returns an empty Set); child schemas are not called",
                    "description": "Even when validation is performed, the protected validate method may legitimately return no messages (e.g., the instance satisfies oneOf). walk must forward that empty Set unchanged and must not invoke child schemas."
                }
            ]
        },
        {
            "group": "Multiple child schemas receive walk call with shouldValidateSchema false",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNonNullNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "Empty Set; verify that each of the three child JsonSchema instances receives exactly one walk call with shouldValidateSchema=false",
                    "description": "When shouldValidateSchema is false, OneOfValidator must iterate over its entire schemas list, calling walk on every child schema with the same node/root/instanceLocation but with shouldValidateSchema set to false. The validator itself adds no messages."
                }
            ]
        }
    ]
}