{
    "group": "Delegation with various JsonNode types",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNodeFactory.instance.textNode(\"any string\")"
                }
            ],
            "expected": "Set<ValidationMessage> returned by the overloaded validate method (stubbed to contain a single message \"text‑node\"). The top‑level validate must forward the call unchanged for a text node.",
            "description": "Verify that a plain text JsonNode is delegated correctly regardless of its type."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNodeFactory.instance.numberNode(42)"
                }
            ],
            "expected": "Set<ValidationMessage> returned by the overloaded validate method (stubbed to contain a single message \"number‑node\").",
            "description": "Validate that a numeric JsonNode (int) is passed through the delegation chain without alteration."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNodeFactory.instance.booleanNode(true)"
                }
            ],
            "expected": "Set<ValidationMessage> returned by the overloaded validate method (stubbed to contain a single message \"boolean‑node\").",
            "description": "Check that a boolean JsonNode is handled the same way as other primitive nodes."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNodeFactory.instance.arrayNode()"
                }
            ],
            "expected": "Set<ValidationMessage> returned by the overloaded validate method (stubbed to contain a single message \"empty‑array\").",
            "description": "Ensure that an empty array node is delegated correctly."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNodeFactory.instance.objectNode().set(\"inner\", JsonNodeFactory.instance.textNode(\"value\"))"
                }
            ],
            "expected": "Set<ValidationMessage> returned by the overloaded validate method (stubbed to contain a single message \"object‑with‑inner\").",
            "description": "Confirm that a complex object node (with nested fields) is passed unchanged to the overloaded validator."
        }
    ]
}