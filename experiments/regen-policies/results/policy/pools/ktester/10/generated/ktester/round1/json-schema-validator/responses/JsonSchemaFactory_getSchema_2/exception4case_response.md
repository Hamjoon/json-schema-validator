{
    "group": "JsonSchemaFactory.getSchema(InputStream) - exception edge cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "new ByteArrayInputStream(\"{\\\"$ref\\\":\\\"classpath:/missing-schema.json\\\"}\".getBytes(StandardCharsets.UTF_8))"
                }
            ],
            "expected": "JsonSchemaException (or RuntimeException) indicating that the referenced resource cannot be found",
            "description": "When the schema contains a $ref to a non‑existent classpath resource, the factory should fail with a JsonSchemaException."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "new ByteArrayInputStream(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-99/schema#\\\",\\\"type\\\":\\\"object\\\"}\".getBytes(StandardCharsets.UTF_8))"
                }
            ],
            "expected": "JsonSchemaException (or IllegalArgumentException) because the $schema URI does not correspond to a known meta‑schema",
            "description": "The method must reject schemas that declare an unknown or unsupported draft version."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "new ByteArrayInputStream(\"{\\\"$id\\\":\\\"ht!tp://[invalid]\\\",\\\"type\\\":\\\"object\\\"}\".getBytes(StandardCharsets.UTF_8))"
                }
            ],
            "expected": "IllegalArgumentException (or JsonSchemaException) due to invalid URI syntax in $id",
            "description": "A malformed $id value should cause the factory to throw an exception when it attempts to create a SchemaLocation."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "new ByteArrayInputStream(\"{\\\"$ref\\\":\\\"#\\\"}\".getBytes(StandardCharsets.UTF_8))"
                }
            ],
            "expected": "JsonSchemaException (or StackOverflowError) indicating detection of a circular $ref",
            "description": "A schema that references itself should trigger circular reference handling and result in an exception."
        }
    ]
}