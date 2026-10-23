{
    "group": "Null InputStream handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "null"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "throws JsonSchemaException (cause is NullPointerException)",
            "description": "The method should not accept a null InputStream; it must throw a JsonSchemaException wrapping the NullPointerException that occurs when jsonMapper tries to read from a null stream."
        }
    ]
}