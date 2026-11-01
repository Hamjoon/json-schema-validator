{
    "tests": [
        {
            "group": "hasTypeValidator - real schema with explicit type keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"string\" }"
                        }
                    ],
                    "expected": true,
                    "description": "A JsonSchema is created from a JSON schema that contains a top‑level \"type\" keyword. The internal TypeValidator should be instantiated, therefore hasTypeValidator() must return true."
                }
            ]
        },
        {
            "group": "hasTypeValidator - real schema without any type keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"properties\": { \"name\": { \"type\": \"string\" } } }"
                        }
                    ],
                    "expected": false,
                    "description": "The schema defines properties but does not declare a top‑level \"type\". JsonSchema should not create a TypeValidator for the root schema, so hasTypeValidator() should return false."
                }
            ]
        },
        {
            "group": "hasTypeValidator - schema with $ref pointing to a schema that has a type",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "referencedSchemaJson",
                            "value": "{ \"type\": \"integer\" }"
                        },
                        {
                            "parameter": "rootSchemaJson",
                            "value": "{ \"$ref\": \"#/definitions/ref\" , \"definitions\": { \"ref\": { \"type\": \"integer\" } } }"
                        }
                    ],
                    "expected": true,
                    "description": "The root schema only contains a $ref that resolves to a definition with a \"type\" keyword. After resolution, the root JsonSchema should have a TypeValidator, so hasTypeValidator() returns true."
                }
            ]
        },
        {
            "group": "hasTypeValidator - schema with $ref pointing to a schema without a type",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "referencedSchemaJson",
                            "value": "{ \"properties\": { \"age\": { \"type\": \"number\" } } }"
                        },
                        {
                            "parameter": "rootSchemaJson",
                            "value": "{ \"$ref\": \"#/definitions/ref\" , \"definitions\": { \"ref\": { \"properties\": { \"age\": { \"type\": \"number\" } } } } }"
                        }
                    ],
                    "expected": false,
                    "description": "The $ref resolves to a definition that does not declare a top‑level \"type\" (only nested property types). The root JsonSchema therefore lacks a TypeValidator and hasTypeValidator() must return false."
                }
            ]
        },
        {
            "group": "hasTypeValidator - schema with empty type array",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": [] }"
                        }
                    ],
                    "expected": false,
                    "description": "An empty array for the \"type\" keyword means no allowed types. The library treats this as the absence of a usable TypeValidator, so hasTypeValidator() should return false."
                }
            ]
        },
        {
            "group": "hasTypeValidator - schema with multiple types (array with values)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": [\"string\", \"null\"] }"
                        }
                    ],
                    "expected": true,
                    "description": "When \"type\" is an array containing at least one valid type, the JsonSchema creates a composite TypeValidator. Hence hasTypeValidator() returns true."
                }
            ]
        }
    ]
}