{
    "group": "BaseJsonValidator.getNodeFieldType - exception scenarios",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "The validator's parent schema is null. Calling getParentSchema() returns null, so the subsequent call to getSchemaNode() triggers a NullPointerException."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "The parent schema exists but its schemaNode is null. The call to getParentSchema().getSchemaNode() returns null, and invoking .get(\"type\") on a null reference throws a NullPointerException."
        },
        {
            "input": [],
            "expected": "RuntimeException",
            "description": "The parent schema's schemaNode is a mock that throws a RuntimeException when its get(\"type\") method is invoked. This simulates an unexpected failure inside the JSON node handling."
        }
    ]
}