{
    "testCases": [
        {
            "group": "noDiscriminatorOnSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (no discriminator for schema location, register called)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "empty ObjectNode"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode without \"discriminator\" field)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "no exception; context.registerDiscriminator called with schemaLocation and unchanged discriminator",
                    "description": "When the schema does not contain a \"discriminator\" keyword the method should simply register the supplied discriminator without modifying it."
                }
            ]
        },
        {
            "group": "propertyNameRedefinition",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with empty \"mapping\""
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains {\"discriminator\": {\"propertyName\": \"type\"}})"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "JsonSchemaException with message containing \"attempts redefining the discriminator property\"",
                    "description": "If a nested schema defines a discriminator that also defines a \"propertyName\", the method must reject it because the root discriminator already defines the property name."
                }
            ]
        },
        {
            "group": "mappingOnlyOnSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location, its \"mapping\" field is null)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with no \"mapping\" field"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains {\"discriminator\": {\"mapping\": {\"dog\": \"#/components/schemas/Dog\"}}})"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "no exception; discriminator now has a \"mapping\" field equal to the schema's mapping object",
                    "description": "When the root discriminator has no mapping but the nested schema provides one, the method should copy the nested mapping to the root discriminator."
                }
            ]
        },
        {
            "group": "mergeMappingsNoConflict",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location with mapping {\"cat\": \"#/schemas/Cat\"})"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with mapping {\"cat\": \"#/schemas/Cat\"}"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains {\"discriminator\": {\"mapping\": {\"dog\": \"#/schemas/Dog\"}}})"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "no exception; discriminator.mapping now contains both \"cat\" and \"dog\" entries",
                    "description": "When both the root and nested discriminators have a mapping and there is no key overlap, the method should merge the mappings."
                }
            ]
        },
        {
            "group": "mergeMappingsWithSameValue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location with mapping {\"cat\": \"#/schemas/Cat\"})"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with mapping {\"cat\": \"#/schemas/Cat\"}"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains {\"discriminator\": {\"mapping\": {\"cat\": \"#/schemas/Cat\"}}})"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "no exception; discriminator.mapping unchanged (still contains the original entry)",
                    "description": "If the nested discriminator defines a mapping entry that already exists in the root with the identical value, the method should silently accept it."
                }
            ]
        },
        {
            "group": "mergeMappingsWithConflict",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location with mapping {\"cat\": \"#/schemas/Cat\"})"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with mapping {\"cat\": \"#/schemas/Cat\"}"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains {\"discriminator\": {\"mapping\": {\"cat\": \"#/schemas/AnotherCat\"}}})"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "JsonSchemaException with message containing \"discriminator mapping redefinition from cat\"",
                    "description": "When the nested discriminator tries to redefine an existing mapping key with a different value, the method must throw an exception indicating the conflict."
                }
            ]
        }
    ]
}