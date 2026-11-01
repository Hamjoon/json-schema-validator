{
    "test_cases": [
        {
            "group": "getNode(Object) – negative array index (Integer)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "-1"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the schemaNode is an ArrayNode, supplying a negative integer index should be considered invalid and cause an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "getNode(Object) – non‑integer numeric type (Long)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "123L"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A Long value is not a supported index type for array access; the method should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "getNode(Object) – floating‑point index (Double)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "1.5"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Floating‑point numbers cannot be used as array indexes; the method should throw IllegalArgumentException when a Double is supplied."
                }
            ]
        },
        {
            "group": "getNode(Object) – invalid URL‑encoded property name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"%ZZ\""
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the supplied String cannot be URL‑decoded (malformed percent‑encoding), the method should propagate an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "getNode(Object) – unsupported argument type (Character)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "'c'"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A Character is not a recognized property name or index type; the method should throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "getNode(Object) – unsupported argument type (Object)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "new Object()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Passing an arbitrary Object as the argument is unsupported and should result in IllegalArgumentException."
                }
            ]
        },
        {
            "group": "getNode(Object) – schemaNode is null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"any\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the JsonSchema instance was constructed with a null schemaNode, any call to getNode should raise NullPointerException."
                }
            ]
        }
    ]
}