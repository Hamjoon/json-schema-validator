{
    "testCases": [
        {
            "group": "nullCurrentDiscriminatorContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "null"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "empty ObjectNode"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (any valid schemaNode)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "NullPointerException (thrown when method tries to call getDiscriminatorForPath on a null context)",
                    "description": "Passing a null DiscriminatorContext should cause a NullPointerException because the method dereferences the context without a null‑check."
                }
            ]
        },
        {
            "group": "nullDiscriminatorNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns no discriminator for the schema location)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "null"
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
                    "expected": "NullPointerException (thrown when method attempts discriminator.get(\"mapping\"))",
                    "description": "If the supplied discriminator ObjectNode is null, the first call to discriminator.get(...) will raise a NullPointerException."
                }
            ]
        },
        {
            "group": "nonObjectMappingInRootDiscriminator",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location with a non‑object \"mapping\" field, e.g. a text node \"foo\")"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with \"mapping\" set to a TextNode \"foo\""
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains a valid \"discriminator\" with no \"mapping\" field)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "ClassCastException (thrown when casting discriminator.get(\"mapping\") to ObjectNode)",
                    "description": "When the root discriminator’s \"mapping\" field exists but is not an ObjectNode, the cast to ObjectNode fails, resulting in a ClassCastException."
                }
            ]
        },
        {
            "group": "nonObjectMappingInSchemaDiscriminator",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (returns a discriminator for the schema location with an empty ObjectNode mapping)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "ObjectNode with empty \"mapping\" ObjectNode"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode contains \"discriminator\" whose \"mapping\" field is a TextNode \"invalid\")"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "ClassCastException (thrown when casting discriminatorOnSchema.get(\"mapping\") to ObjectNode)",
                    "description": "If the nested schema’s discriminator defines a \"mapping\" that is not an ObjectNode, the cast to ObjectNode triggers a ClassCastException."
                }
            ]
        },
        {
            "group": "nullSchemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "currentDiscriminatorContext",
                            "value": "mockContext (any valid context)"
                        },
                        {
                            "parameter": "discriminator",
                            "value": "empty ObjectNode"
                        },
                        {
                            "parameter": "schema",
                            "value": "mockSchema (schemaNode == null)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath \"/instance\""
                        }
                    ],
                    "expected": "NullPointerException (thrown when accessing schema.schemaNode.get(\"discriminator\"))",
                    "description": "When the supplied JsonSchema has a null schemaNode, the method attempts to call get on a null reference, causing a NullPointerException."
                }
            ]
        }
    ]
}