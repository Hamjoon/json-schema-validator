{
    "test_cases": [
        {
            "group": "Failure due to malformed $ref causing UnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
                        { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
                        { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
                        {
                            "parameter": "schemaNode",
                            "value": "mocked ObjectNode containing { \"$ref\": \"http://example.com/invalid%ZZschema.json\" }"
                        },
                        { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": false }
                    ],
                    "expected": "UnsupportedEncodingException",
                    "description": "The schemaNode contains a $ref with an invalid percent‑encoding sequence. During construction the $ref is URL‑decoded, which throws UnsupportedEncodingException."
                }
            ]
        },
        {
            "group": "Failure due to $ref referencing non‑existent schema causing IOException",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
                        { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
                        { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
                        {
                            "parameter": "schemaNode",
                            "value": "mocked ObjectNode containing { \"$ref\": \"http://nonexistent.example.com/schema.json\" }"
                        },
                        { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": false }
                    ],
                    "expected": "IOException",
                    "description": "The $ref points to an external URI that cannot be resolved. The constructor attempts to load the referenced schema and propagates the resulting IOException."
                }
            ]
        },
        {
            "group": "Failure due to invalid schema node type causing IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
                        { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
                        { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
                        {
                            "parameter": "schemaNode",
                            "value": "mocked IntNode (numeric value 123) – a primitive node that cannot represent a schema object"
                        },
                        { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": false }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The constructor expects the schemaNode to be an ObjectNode, ArrayNode or a BooleanNode representing a valid JSON‑Schema. Supplying a scalar numeric node triggers an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Failure due to circular reference detection causing JsonSchemaException",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
                        { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
                        { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
                        {
                            "parameter": "schemaNode",
                            "value": "mocked ObjectNode containing { \"$ref\": \"#\" } – a self‑referencing schema"
                        },
                        { "parameter": "parent", "value": "mocked parent JsonSchema instance (which also contains the same $ref)" },
                        { "parameter": "suppressSubSchemaRetrieval", "value": false }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "A schema that directly or indirectly references itself creates a circular reference. The constructor detects this condition and throws a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Failure when suppressSubSchemaRetrieval is false but parent is null and schema contains $ref",
            "cases": [
                {
                    "input": [
                        { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
                        { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
                        { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
                        {
                            "parameter": "schemaNode",
                            "value": "mocked ObjectNode containing { \"$ref\": \"#/definitions/foo\" } – requires parent for resolution"
                        },
                        { "parameter": "parent", "value": null },
                        { "parameter": "suppressSubSchemaRetrieval", "value": false }
                    ],
                    "expected": "IllegalStateException",
                    "description": "When a $ref needs to be resolved against a parent schema but the parent argument is null, the constructor cannot resolve the reference and throws IllegalStateException."
                }
            ]
        }
    ]
}