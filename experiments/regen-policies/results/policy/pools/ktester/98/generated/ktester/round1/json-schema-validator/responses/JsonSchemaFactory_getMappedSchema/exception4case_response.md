{
    "group": "Null arguments validation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "null"
                },
                {
                    "parameter": "config",
                    "value": "defaultConfig"
                }
            ],
            "expected": "NullPointerException (or IllegalArgumentException) thrown before entering the try‑with‑resources block because schemaUri is null and the method attempts to call schemaUri.getAbsoluteIri().",
            "description": "Verifies that passing a null SchemaLocation causes an immediate NPE rather than being wrapped in JsonSchemaException."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "file:/tmp/simple-schema.json"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException thrown when config.getPathType() is invoked inside the method.",
            "description": "Ensures that a null SchemaValidatorsConfig leads to an NPE (not wrapped) because the method dereferences config to create JsonNodePath."
        }
    ]
}