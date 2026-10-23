{
    "group": "Null and Empty Input",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": null
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null string should cause the factory to reject the request and throw a NullPointerException (the overload forwards to getSchema(String, null) which validates the argument)."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": ""
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "An empty schema string does not represent a valid JSON/YAML document; the method should detect this early and throw an IllegalArgumentException."
        }
    ]
}