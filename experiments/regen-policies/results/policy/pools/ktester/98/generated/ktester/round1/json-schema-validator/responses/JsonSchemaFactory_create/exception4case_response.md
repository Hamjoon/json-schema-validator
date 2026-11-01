{
    "testCases": [
        {
            "group": "Null evaluationPath",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "null"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"object\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'evaluationPath must not be null'",
                    "description": "The factory requires a non‑null JsonNodePath; passing null should be rejected."
                }
            ]
        },
        {
            "group": "Root schema node as number (invalid root type)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/number-root.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.numberNode(42)"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Root schema must be an object or boolean'",
                    "description": "A numeric node cannot be used as the top‑level schema; the factory must reject it."
                }
            ]
        },
        {
            "group": "Invalid 'type' keyword value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/invalid-type.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"unknownType\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Unsupported type value'",
                    "description": "The 'type' keyword must be one of the defined JSON‑Schema types; an unknown value should cause an exception."
                }
            ]
        },
        {
            "group": "Invalid $id URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/invalid-id.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$id\", \"http://[invalid-uri]\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Invalid $id URI'",
                    "description": "A malformed URI in the $id keyword should be detected and cause an exception."
                }
            ]
        },
        {
            "group": "Malformed $ref URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/malformed-ref.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$ref\", \"ht!tp://bad-uri\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Invalid $ref URI'",
                    "description": "When $ref contains a syntactically invalid URI the factory must fail fast."
                }
            ]
        },
        {
            "group": "Unknown meta‑schema identifier",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/unknown-meta.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$schema\", \"http://example.com/unknown-draft\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Unsupported meta‑schema'",
                    "description": "A $schema value that does not map to a known draft should cause an exception during creation."
                }
            ]
        },
        {
            "group": "$ref with sibling keywords (illegal combination)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/ref-with-siblings.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$ref\", \"#/definitions/foo\").put(\"type\", \"string\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing '$ref must not be combined with other keywords'",
                    "description": "According to the spec, a $ref object cannot contain other validation keywords; the factory should enforce this rule."
                }
            ]
        },
        {
            "group": "Malformed SchemaLocation URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"ht!tp://[bad-uri]\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"object\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Invalid schemaLocation URI'",
                    "description": "Creating a SchemaLocation with a syntactically invalid URI should be rejected before schema creation."
                }
            ]
        },
        {
            "group": "Excessive recursion depth detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/deep-recursion.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectNode().put(\"$ref\", \"classpath:/schemas/deep-recursion.json\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) with message containing 'Maximum recursion depth exceeded'",
                    "description": "When a schema references itself directly without a placeholder, the factory should detect the recursion limit and throw a clear exception rather than causing a StackOverflowError."
                }
            ]
        }
    ]
}