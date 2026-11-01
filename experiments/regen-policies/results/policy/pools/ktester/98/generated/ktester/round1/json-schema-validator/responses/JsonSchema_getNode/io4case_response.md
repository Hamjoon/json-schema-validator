{
    "testCases": [
        {
            "group": "getNode(Object) – array index (Integer)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "0"
                        }
                    ],
                    "expected": "\"first\"",
                    "description": "When the schemaNode is an ArrayNode ['first','second'], getNode(0) should return the JsonNode representing the first element."
                },
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "1"
                        }
                    ],
                    "expected": "\"second\"",
                    "description": "When the schemaNode is an ArrayNode ['first','second'], getNode(1) should return the JsonNode representing the second element."
                },
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "2"
                        }
                    ],
                    "expected": "null",
                    "description": "When the schemaNode is an ArrayNode of size 2, getNode(2) (out‑of‑bounds) should return null."
                }
            ]
        },
        {
            "group": "getNode(Object) – array index supplied as String",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"0\""
                        }
                    ],
                    "expected": "null",
                    "description": "When the schemaNode is an ArrayNode, a String argument (even if numeric) is not treated as an index; getNode(\"0\") should therefore return null."
                }
            ]
        },
        {
            "group": "getNode(Object) – null argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing null as propertyOrIndex should result in a NullPointerException (the method does not accept null)."
                }
            ]
        },
        {
            "group": "getNode(Object) – unsupported argument type (Boolean)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "true"
                        }
                    ],
                    "expected": "null",
                    "description": "When a Boolean is supplied, getNode should ignore the type and return null."
                }
            ]
        },
        {
            "group": "getNode(Object) – schemaNode is a value node (not container)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"any\""
                        }
                    ],
                    "expected": "null",
                    "description": "If the underlying schemaNode is a primitive/value node (e.g., a text node), getNode should always return null regardless of the argument."
                }
            ]
        },
        {
            "group": "getNode(Object) – URL‑decoded property name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "propertyOrIndex",
                            "value": "\"a%20b\""
                        }
                    ],
                    "expected": "\"space key\"",
                    "description": "When the schemaNode contains a field with a space in its name (\"a b\"), supplying the URL‑encoded string \"a%20b\" should be decoded and the corresponding JsonNode returned."
                }
            ]
        }
    ]
}