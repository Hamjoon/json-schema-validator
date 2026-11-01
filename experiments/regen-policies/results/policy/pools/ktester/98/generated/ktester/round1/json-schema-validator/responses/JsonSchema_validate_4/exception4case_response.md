{
    "test_cases": [
        {
            "group": "Customizer Exception Propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"name\", \"Test\")"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { throw new RuntimeException(\"custom error\"); }"
                        }
                    ],
                    "expected": "Throws RuntimeException with message \"custom error\"",
                    "description": "The customizer lambda throws a RuntimeException; the validate method should not catch it and must propagate the exception to the caller."
                }
            ]
        },
        {
            "group": "Malformed $ref Decoding Error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "Throws IllegalArgumentException (from URLDecoder.decode) due to malformed percent‑encoding in $ref",
                    "description": "The schema contains a $ref value like \"%ZZ\" which is not a valid percent‑encoded string. When validate() attempts to resolve the reference it calls URLDecoder.decode, which throws IllegalArgumentException. The method should surface this exception."
                }
            ]
        },
        {
            "group": "Unresolvable $ref Leads to JsonSchemaException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"refProp\": {} }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or subclass) indicating that the referenced schema could not be found",
                    "description": "The schema contains a $ref pointing to a non‑existent definition (e.g., \"#/definitions/Unknown\"). Validation attempts to resolve it and should raise a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Circular $ref Without Recursive Anchor",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"self\": {} }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "Throws StackOverflowError or JsonSchemaException indicating infinite $ref recursion",
                    "description": "The schema defines two subschemas that reference each other via $ref but does not set \"$recursiveAnchor\": true. Validation should detect the recursion and either throw a StackOverflowError or a JsonSchemaException describing the problem."
                }
            ]
        },
        {
            "group": "Invalid Validator Type Configuration",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"field\": \"value\" }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "Throws IllegalStateException (or IllegalArgumentException) because the schema specifies an unknown validator type",
                    "description": "The schema contains a validator entry with an unsupported \"type\" value (e.g., \"unknownType\"). When the validator factory attempts to instantiate it, an IllegalStateException should be thrown."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer Sets Null Attribute Key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"name\": \"Alice\" }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> ctx.setAttribute(null, \"value\")"
                        }
                    ],
                    "expected": "Throws NullPointerException",
                    "description": "The customizer attempts to store an attribute with a null key, which the ExecutionContext implementation rejects by throwing NullPointerException. The validate method should let this exception propagate."
                }
            ]
        }
    ]
}