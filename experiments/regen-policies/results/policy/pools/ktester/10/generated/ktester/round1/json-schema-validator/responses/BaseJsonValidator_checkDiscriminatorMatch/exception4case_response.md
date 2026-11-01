{
    "testCases": [
        {
            "group": "nullDiscriminatorArgument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "MockDiscriminatorContext (matchFound=false)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "null"
                        },
                        {
                            "parameter": "discriminatorPropertyValue",
                            "value": "\"anyValue\""
                        },
                        {
                            "parameter": "jsonSchema",
                            "value": "MockJsonSchema (any schema)"
                        }
                    ],
                    "expected": "NullPointerException is thrown because discriminator is null and the method attempts discriminator.get(\"mapping\")",
                    "description": "Passing a null discriminator ObjectNode should cause an NPE before any mapping logic is evaluated."
                }
            ]
        },
        {
            "group": "nullJsonSchemaArgument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "MockDiscriminatorContext (matchFound=false)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode without a \"mapping\" field"
                        },
                        {
                            "parameter": "discriminatorPropertyValue",
                            "value": "\"cat\""
                        },
                        {
                            "parameter": "jsonSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown when the method forwards the null jsonSchema to checkForImplicitDiscriminatorMappingMatch",
                    "description": "If the JsonSchema argument is null, the implicit mapping helper receives a null reference and throws NPE."
                }
            ]
        },
        {
            "group": "nonObjectMappingType",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "MockDiscriminatorContext (matchFound=false)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with \"mapping\": [\"#/components/schemas/Dog\"] (array instead of object)"
                        },
                        {
                            "parameter": "discriminatorPropertyValue",
                            "value": "\"dog\""
                        },
                        {
                            "parameter": "jsonSchema",
                            "value": "MockJsonSchema (any schema)"
                        }
                    ],
                    "expected": "IllegalArgumentException (or ClassCastException) is thrown because checkForExplicitDiscriminatorMappingMatch expects an object node for the mapping but receives an array node",
                    "description": "Providing a mapping field whose value is not a JSON object should cause the explicit‑mapping helper to fail with a type‑mismatch exception."
                }
            ]
        },
        {
            "group": "nullMappingFieldValue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "MockDiscriminatorContext (matchFound=false)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with \"mapping\": null"
                        },
                        {
                            "parameter": "discriminatorPropertyValue",
                            "value": "\"cat\""
                        },
                        {
                            "parameter": "jsonSchema",
                            "value": "MockJsonSchema (any schema)"
                        }
                    ],
                    "expected": "NullPointerException is thrown when checkForExplicitDiscriminatorMappingMatch attempts to iterate over a null mapping node",
                    "description": "When the \"mapping\" field exists but its value is JSON null, the method treats it as a non‑null JsonNode and later dereferences it, causing NPE."
                }
            ]
        },
        {
            "group": "implicitMappingResolutionFailure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "MockDiscriminatorContext (matchFound=false)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode without a \"mapping\" field"
                        },
                        {
                            "parameter": "discriminatorPropertyValue",
                            "value": "\"unknownAnimal\""
                        },
                        {
                            "parameter": "jsonSchema",
                            "value": "MockJsonSchema where implicit mapping for \"unknownAnimal\" does NOT resolve to any subschema (throws SchemaException)"
                        }
                    ],
                    "expected": "SchemaException (or RuntimeException) is thrown by checkForImplicitDiscriminatorMappingMatch because the implicit discriminator reference cannot be resolved",
                    "description": "If the implicit discriminator mapping points to a non‑existent subschema, the helper method throws an exception that propagates out of checkDiscriminatorMatch."
                }
            ]
        },
        {
            "group": "explicitMappingResolutionFailure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "MockDiscriminatorContext (matchFound=false)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with \"mapping\": { \"dog\": \"#/components/schemas/NonExistingDog\" }"
                        },
                        {
                            "parameter": "discriminatorPropertyValue",
                            "value": "\"dog\""
                        },
                        {
                            "parameter": "jsonSchema",
                            "value": "MockJsonSchema where the reference \"#/components/schemas/NonExistingDog\" cannot be resolved (throws SchemaException)"
                        }
                    ],
                    "expected": "SchemaException (or IllegalArgumentException) is thrown by checkForExplicitDiscriminatorMappingMatch when it tries to load a non‑existent subschema",
                    "description": "An explicit mapping that points to an undefined schema reference should cause the explicit‑mapping helper to raise an exception."
                }
            ]
        }
    ]
}