{
    "group": "TypeValidator.getSchemaType() exception scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "validatorInstance",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Calling getSchemaType() on a null reference of TypeValidator should throw NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "schemaTypeViaReflection",
                    "value": "\"invalid-type\""
                }
            ],
            "expected": "ClassCastException",
            "description": "When the private field `schemaType` is forcibly set (via reflection) to a value that is not a JsonType (e.g., a String), invoking getSchemaType() should result in a ClassCastException because the returned object cannot be cast to JsonType."
        }
    ]
}