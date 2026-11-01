{
    "groups": [
        {
            "group": "Construction with invalid SchemaLocation (empty string) should throw IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The factory must reject a SchemaLocation that does not contain a valid URI (empty string)."
                }
            ]
        },
        {
            "group": "Construction with numeric schema node should throw IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/numeric-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonMapperFactory.getInstance().readTree(\"123\")"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A schema node that is a numeric literal is not a valid JSON Schema definition; the factory should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Construction with malformed $ref fragment should throw IllegalArgumentException on resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/bad-ref-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "new JsonNodePath(\"$\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"$ref\\\":\\\"#/definitions/@@invalid@@\\\",\\\"definitions\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"}}}\")"
                        },
                        {
                            "parameter": "parent",
                            "value": "null"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the $ref value contains an illegal fragment, attempting to resolve it (e.g., via getRefSchema) should result in an IllegalArgumentException."
                }
            ]
        }
    ]
}