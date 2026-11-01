{
    "group": "Unsupported propertyOrIndex type – Long",
    "cases": [
        {
            "input": [
                {
                    "parameter": "node",
                    "value": "{\"key\":\"value\"}"
                },
                {
                    "parameter": "propertyOrIndex",
                    "value": "1234567890123L"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Passing a java.lang.Long as propertyOrIndex is not supported; JsonNodes.get should throw IllegalArgumentException."
        }
    ]
}