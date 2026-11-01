{
    "groups": [
        {
            "group": "hasTypeValidator_invalidTypeValue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": 123 }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the \"type\" keyword is a number (invalid type definition), getTypeValidator() should throw IllegalArgumentException, causing hasTypeValidator() to propagate the exception."
                }
            ]
        },
        {
            "group": "hasTypeValidator_unresolvableRef",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"$ref\": \"#/definitions/missing\" }"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "A $ref that points to a non‑existent definition should cause schema resolution to fail and throw a JsonSchemaException when hasTypeValidator() accesses the TypeValidator."
                }
            ]
        },
        {
            "group": "hasTypeValidator_circularRef",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"$ref\": \"#/definitions/self\", \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } } }"
                        }
                    ],
                    "expected": "StackOverflowError",
                    "description": "A schema that references itself directly creates a circular reference; attempting to resolve the TypeValidator should eventually overflow the stack."
                }
            ]
        },
        {
            "group": "hasTypeValidator_malformedSchemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the root schema node is null (e.g., schema parsing returned null), calling hasTypeValidator() should result in a NullPointerException when getTypeValidator() dereferences the node."
                }
            ]
        },
        {
            "group": "hasTypeValidator_typeArrayWithInvalidElement",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": [\"string\", 5, \"null\"] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the \"type\" array contains a non‑string element (the number 5), the TypeValidator creation should fail with IllegalArgumentException."
                }
            ]
        },
        {
            "group": "hasTypeValidator_unsupportedKeywordCombination",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"anyOf\": [ { \"type\": \"string\" } ] }"
                        }
                    ],
                    "expected": "UnsupportedOperationException",
                    "description": "If the implementation does not allow a top‑level \"type\" together with \"anyOf\" (or similar combinators) for TypeValidator creation, it may throw UnsupportedOperationException."
                }
            ]
        }
    ]
}