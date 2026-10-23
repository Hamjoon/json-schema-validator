{
    "group": "BaseJsonValidator.getSchemaPath",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": null
                }
            ],
            "expected": "null",
            "description": "When the internal `schemaPath` field is never initialized (remains null), `getSchemaPath()` should return null."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": ""
                }
            ],
            "expected": "\"\"",
            "description": "When `schemaPath` is set to an empty string, the method should return the empty string unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/properties/name"
                }
            ],
            "expected": "\"/properties/name\"",
            "description": "Typical use‑case: `schemaPath` contains a JSON Pointer to a property. The method must return the exact string."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/items/0/properties/age"
                }
            ],
            "expected": "\"/items/0/properties/age\"",
            "description": "Complex path with array index and nested properties – verifies that the method does not alter or truncate the value."
        },
        {
            "input": [
                {
                    "parameter": "schemaPath",
                    "value": "/$defs/Person~1address"
                }
            ],
            "expected": "\"/$defs/Person~1address\"",
            "description": "Path containing escaped characters (`~1` for `/`) to ensure the raw stored value is returned unchanged."
        }
    ]
}