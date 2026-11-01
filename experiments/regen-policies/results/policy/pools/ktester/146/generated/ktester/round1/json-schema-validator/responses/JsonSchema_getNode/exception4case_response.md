{
    "test_cases": [
        {
            "group": "getNode(Object) – Null schemaNode triggers NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"any\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the JsonSchema instance has a null internal schemaNode, invoking getNode(Object) should throw a NullPointerException regardless of the supplied propertyOrIndex."
                }
            ]
        },
        {
            "group": "getNode(Object) – non‑numeric String index on ArrayNode throws NumberFormatException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"abc\""
                        }
                    ],
                    "expected": "NumberFormatException",
                    "description": "If the underlying schemaNode is an ArrayNode and the propertyOrIndex is a String that cannot be parsed into an integer, the method attempts to convert it and propagates a NumberFormatException."
                }
            ]
        }
    ]
}