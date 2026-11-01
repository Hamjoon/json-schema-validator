{
    "groups": [
        {
            "group": "Empty fragment – returns null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with zero segments []"
                        }
                    ],
                    "expected": "null (the method returns null because the loop is never entered)",
                    "description": "When the supplied JsonNodePath contains no segments, the for‑loop is skipped, subSchema remains null and the method returns null. This tests the boundary condition of an empty pointer."
                }
            ]
        },
        {
            "group": "Primitive node segment – returns JsonSchema wrapping primitive",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single string segment ['default']"
                        }
                    ],
                    "expected": "JsonSchema instance whose underlying node is a TextNode with value \"default value\" (i.e., a primitive schema node)",
                    "description": "The fragment points to a property whose schema node is a primitive (e.g., a string constant). The method should still create a JsonSchema for that node; the test verifies that the returned schema wraps a primitive JsonNode rather than an object or array."
                }
            ]
        },
        {
            "group": "Numeric segment on non‑array parent – fallback to external schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with segments ['properties', 'title', 0]"
                        }
                    ],
                    "expected": "JsonSchema instance representing the sub‑schema located at '/externalTitle' in the external document fetched by fetchSubSchemaNode",
                    "description": "The numeric segment (0) is applied to a parent node that is an object (\"title\"), so getNode(0) returns null. The method therefore falls back to document.findSchemaResourceRoot().fetchSubSchemaNode(...), which returns a different schema document containing the requested fragment. The test confirms that the returned schema originates from the external document."
                }
            ]
        },
        {
            "group": "Large numeric index beyond array bounds – exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single numeric segment [999]"
                        }
                    ],
                    "expected": "JsonSchemaException with message containing \"Unable to find subschema\" and the fragment string",
                    "description": "The fragment points to an array index that does not exist. getNode(999) returns null, fetchSubSchemaNode also returns null, causing the method to throw a JsonSchemaException. This validates proper error handling for out‑of‑range numeric segments."
                }
            ]
        },
        {
            "group": "Non‑String/Number segment (Boolean) – treated as string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single Boolean segment [true]"
                        }
                    ],
                    "expected": "JsonSchema instance representing the sub‑schema located at '/true' of the root document",
                    "description": "When the segment is neither a Number nor a String (e.g., a Boolean), the code falls into the else‑branch and uses segment.toString() as the key. The test ensures that a Boolean segment is correctly converted to the string \"true\" and that the corresponding sub‑schema is returned."
                }
            ]
        }
    ]
}