{
    "test_cases": [
        {
            "group": "hasTypeValidator_invalidTypeKeyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonSchema",
                            "value": "JsonSchema instance created via JsonSchemaFactory.fromJsonNode with schema node {\"type\":123}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "The \"type\" keyword must be a string or an array of strings. Providing a numeric value causes the internal TypeValidator construction to throw a JsonSchemaException, which propagates out of hasTypeValidator()."
                }
            ]
        },
        {
            "group": "hasTypeValidator_emptyTypeArray",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonSchema",
                            "value": "JsonSchema instance created via JsonSchemaFactory.fromJsonNode with schema node {\"type\":[]}"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "An empty array for the \"type\" keyword is not a valid definition. The validator creation fails and throws a JsonSchemaException, which should be observed when hasTypeValidator() is invoked."
                }
            ]
        },
        {
            "group": "hasTypeValidator_nullReference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling hasTypeValidator() on a null JsonSchema reference should raise a NullPointerException, as the instance method cannot be invoked on a null object."
                }
            ]
        }
    ]
}