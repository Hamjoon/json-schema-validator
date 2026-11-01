{
    "testCases": [
        {
            "group": "SchemaNode Casting Exception",
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
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        }
                    ],
                    "expected": "ClassCastException (schemaNode is not an ObjectNode and is cast inside validate)",
                    "description": "The validator instance is constructed with a schemaNode that is a TextNode (or any non‑ObjectNode). When validate() attempts to treat the schemaNode as an ObjectNode, a ClassCastException must be thrown."
                }
            ]
        },
        {
            "group": "Instance Node Type Validation Exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "objectNodeWhenArrayExpected"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        }
                    ],
                    "expected": "IllegalArgumentException (node type does not match validator's expected type)",
                    "description": "A concrete subclass of BaseJsonValidator that validates an array (e.g., ItemsValidator) receives an ObjectNode. The validator should detect the type mismatch early and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Missing Schema in ExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContextWithNullSchema"
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        }
                    ],
                    "expected": "IllegalStateException (executionContext does not contain a schema to validate against)",
                    "description": "The ExecutionContext supplied to validate() has a null or missing root JsonSchema. The validator must detect this condition and throw IllegalStateException before any validation logic runs."
                }
            ]
        },
        {
            "group": "Discriminator Configuration Exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContextWithMalformedDiscriminator"
                        },
                        {
                            "parameter": "node",
                            "value": "jsonNodeMissingDiscriminatorProperty"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        }
                    ],
                    "expected": "IllegalArgumentException (discriminator property is missing or malformed)",
                    "description": "The schema contains a discriminator definition, but the discriminator object lacks the required \"propertyName\" field (or the mapping is null). When checkDiscriminatorMatch is invoked, it should throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer Nullifies Context",
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
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "mockCustomizerThatNullifiesContext"
                        }
                    ],
                    "expected": "NullPointerException (executionContext becomes null after customizer runs)",
                    "description": "A non‑null ExecutionContextCustomizer is supplied that sets the ExecutionContext reference to null (or clears its internal state). After the customizer is invoked, the validate method should encounter a null executionContext and throw NullPointerException."
                }
            ]
        }
    ]
}