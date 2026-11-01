{
    "group": "JsonSchema#getId - null reference",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaInstance",
                    "value": null
                }
            ],
            "expected": "NullPointerException",
            "description": "Calling getId() on a null JsonSchema reference should throw a NullPointerException, as the instance method cannot be invoked without an object."
        }
    ]
}