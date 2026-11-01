{
    "testCases": [
        {
            "group": "Normal behavior - valid inputs",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/person.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance representing the supplied schemaNode",
                    "description": "Creates a JsonSchema from a valid schema node with no parent schema; verifies that a non‑null JsonSchema is returned."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/address.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$.address\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}})" },
                        { "parameter": "parentSchema", "value": "new JsonSchemaFactory().getSchema(\"{\\\"type\\\":\\\"object\\\"}\")" }
                    ],
                    "expected": "JsonSchema instance whose parent is the supplied parentSchema",
                    "description": "Creates a child schema with a non‑null parent; checks that the returned schema retains a reference to the parent."
                }
            ]
        },
        {
            "group": "Null handling - parameters that may be null",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "null" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/nullContext.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"type\":\"string\"})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null ValidationContext should cause a NullPointerException because the method forwards it to doCreate which expects a non‑null context."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "null" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"type\":\"integer\"})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance (schemaLocation defaults to an empty location)",
                    "description": "When schemaLocation is null the factory should treat it as an empty location and still create a schema."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/nullNode.json\")" },
                        { "parameter": "evaluationPath", "value": "null" },
                        { "parameter": "schemaNode", "value": "objectNode({\"type\":\"boolean\"})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance (evaluationPath defaults to root)",
                    "description": "A null evaluationPath should be interpreted as the root path and not cause failure."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/nullSchemaNode.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "null" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A null schemaNode is invalid; the method should throw IllegalArgumentException before delegating to doCreate."
                }
            ]
        },
        {
            "group": "Branch coverage based on schema node content",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/enum.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"enum\":[\"A\",\"B\",\"C\"]})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance containing an EnumValidator",
                    "description": "When the schema node contains an \"enum\" keyword, doCreate should follow the branch that creates an EnumValidator."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/ifThenElse.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"if\":{\"properties\":{\"age\":{\"minimum\":18}}},\"then\":{\"required\":[\"driverLicense\"]},\"else\":{\"required\":[\"guardianConsent\"]}})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance containing ConditionalValidator (if/then/else)",
                    "description": "Schema node with \"if\", \"then\", and \"else\" triggers the conditional validation branch."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/allOf.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"allOf\":[{\"type\":\"string\"},{\"maxLength\":5}]})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance containing AllOfValidator",
                    "description": "Presence of \"allOf\" array should cause the factory to create an AllOfValidator branch."
                },
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/unsupportedKeyword.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"unknownKeyword\":123})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance without a validator for the unknown keyword (no exception)",
                    "description": "When the schema contains an unrecognised keyword the factory should ignore it and continue; this tests the default/else branch."
                }
            ]
        },
        {
            "group": "Suppress sub‑schema retrieval flag (internal branch)",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "new ValidationContext()" },
                        { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schemas/ref.json\")" },
                        { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                        { "parameter": "schemaNode", "value": "objectNode({\"$ref\":\"#/definitions/address\"})" },
                        { "parameter": "parentSchema", "value": "null" }
                    ],
                    "expected": "JsonSchema instance that resolves the $ref (sub‑schema retrieval not suppressed)",
                    "description": "Even though create() passes suppressSubSchemaRetrieval = false, this case verifies that the $ref branch is taken and the referenced schema is loaded."
                }
            ]
        }
    ]
}