{
    "testCases": [
        {
            "group": "nullFragment_throwsNullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the supplied JsonNodePath is null, the method attempts to call fragment.getNameCount() and fragment.getElement(x), which results in a NullPointerException. This verifies that the method does not guard against a null argument."
                }
            ]
        },
        {
            "group": "unsupportedSegmentType_throwsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/properties/active\" where the segment at index 1 is a Boolean true (e.g., fragment.getElement(1) returns Boolean.TRUE)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The getNode(parentNode, segment) implementation is expected to reject non‑String/Number segment types and throw IllegalArgumentException. By providing a Boolean segment, the branch `JsonNode subSchemaNode = getNode(parentNode, segment);` triggers the exception, which should propagate out of getSubSchema."
                }
            ]
        },
        {
            "group": "nullValidationContext_throwsNullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/properties/name\""
                        },
                        {
                            "parameter": "validationContext",
                            "value": "null (the JsonSchema instance is constructed with a null ValidationContext)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the schema's ValidationContext is null, the call `parent.getValidationContext().resolveSchemaId(subSchemaNode)` results in a NullPointerException. This case ensures that the method does not internally check for a null ValidationContext."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNodeThrowsRuntimeException_propagates",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing pointer \"/nonexistent\" (fragment not present in the current document)"
                        },
                        {
                            "parameter": "fetchSubSchemaNodeBehavior",
                            "value": "throws IllegalStateException(\"fetch failed\")"
                        }
                    ],
                    "expected": "IllegalStateException with message \"fetch failed\"",
                    "description": "When the fragment cannot be resolved, the method falls back to `document.findSchemaResourceRoot().fetchSubSchemaNode(this.validationContext)`. If that call throws a runtime exception (e.g., IllegalStateException), the exception should propagate unchanged, bypassing the InvalidSchemaRefException logic."
                }
            ]
        }
    ]
}