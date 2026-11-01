{
    "test_cases": [
        {
            "group": "Construction with null SchemaLocation (should throw NullPointerException)",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "null" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
                    ],
                    "expected": "NullPointerException",
                    "description": "The factory must reject a null SchemaLocation because the schema's identifier is mandatory."
                }
            ]
        },
        {
            "group": "Construction with null EvaluationPath (should throw NullPointerException)",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schema.json\")" },
                        { "parameter": "evaluationPath", "value": "null" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
                    ],
                    "expected": "NullPointerException",
                    "description": "The factory must validate that the evaluationPath argument is non‑null; a null value should cause an NPE."
                }
            ]
        },
        {
            "group": "Construction with null parent and suppressSubSchemaRetrieval = true",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schema.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"string\\\"}\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "true" }
                    ],
                    "expected": "A non‑null JsonSchema instance whose findAncestor() returns null and whose internal suppressSubSchemaRetrieval flag is true.",
                    "description": "Verifies that the factory works when no parent is supplied but the suppress flag is true."
                }
            ]
        },
        {
            "group": "Construction with non‑object schema node (array) and suppressSubSchemaRetrieval = false",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/array-schema.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"[\\\"a\\\",\\\"b\\\"]\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
                    ],
                    "expected": "A non‑null JsonSchema instance; getValidators() returns an empty list because array nodes do not contain validation keywords.",
                    "description": "Ensures the factory can handle a schema definition that is a JSON array rather than an object."
                }
            ]
        },
        {
            "group": "Construction with boolean schema node (true) and suppressSubSchemaRetrieval = false",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/bool-schema.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"true\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
                    ],
                    "expected": "A non‑null JsonSchema instance; getValidators() returns an empty list because a boolean schema does not define explicit keywords.",
                    "description": "Validates that the factory accepts a boolean schema node (true) and creates a schema object without error."
                }
            ]
        },
        {
            "group": "Construction with parent schema and suppressSubSchemaRetrieval = false",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/child-schema.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$.child\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"integer\\\"}\")" },
                        { "parameter": "parent", "value": "JsonSchema.from(new ValidationContext(), new SchemaLocation(\"classpath:/parent-schema.json\"), new JsonNodePath(\"$\"), JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\"), null, false)" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
                    ],
                    "expected": "A non‑null JsonSchema instance whose findAncestor() returns the supplied parent schema and whose internal suppressSubSchemaRetrieval flag is false.",
                    "description": "Confirms that the suppress flag is independent of the presence of a parent; when false the flag should be recorded as false."
                }
            ]
        },
        {
            "group": "Construction with schema node containing $ref and suppressSubSchemaRetrieval = false (reference resolution possible)",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/ref-schema.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"$ref\\\":\\\"#/definitions/name\\\",\\\"definitions\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"}}}\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
                    ],
                    "expected": "A non‑null JsonSchema instance; subsequent call to getRefSchema(JsonNodePath.of(\"#/definitions/name\")) returns a JsonSchema whose getTypeValidator() reports type \"string\".",
                    "description": "Ensures that when sub‑schema retrieval is not suppressed, a schema containing a $ref can resolve the referenced definition."
                }
            ]
        },
        {
            "group": "Construction with schema node containing $ref and suppressSubSchemaRetrieval = true (reference resolution disabled)",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/ref-schema-suppressed.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"$ref\\\":\\\"#/definitions/name\\\",\\\"definitions\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"}}}\")" },
                        { "parameter": "parent", "value": "null" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": "true" }
                    ],
                    "expected": "A non‑null JsonSchema instance; calling getRefSchema(JsonNodePath.of(\"#/definitions/name\")) returns null because sub‑schema retrieval is suppressed.",
                    "description": "Verifies that when the suppress flag is true, the factory does not eagerly resolve $ref fragments."
                }
            ]
        }
    ]
}