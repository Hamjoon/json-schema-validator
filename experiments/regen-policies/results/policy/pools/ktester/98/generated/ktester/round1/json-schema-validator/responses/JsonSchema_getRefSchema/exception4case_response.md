{
    "test_cases": [
        {
            "group": "Null schemaLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/foo\""
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (thrown when getSchemaLocation() is dereferenced)",
                    "description": "The JsonSchema instance has a null schemaLocation. When getRefSchema is called with an anchor fragment the method attempts to call this.getSchemaLocation().getAbsoluteIri(), causing a NullPointerException."
                }
            ]
        },
        {
            "group": "Null validationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/bar\""
                        },
                        {
                            "parameter": "validationContext",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (thrown when accessing validationContext.getSchemaResources())",
                    "description": "The JsonSchema instance has a null validationContext. The method tries to read validationContext.getSchemaResources() and validationContext.getDynamicAnchors(), which results in a NullPointerException."
                }
            ]
        },
        {
            "group": "Null schemaResources map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/missing\""
                        },
                        {
                            "parameter": "validationContext.schemaResources",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (thrown when invoking get(anchor) on a null map)",
                    "description": "validationContext.getSchemaResources() returns null. The method attempts to call .get(anchor) on this null reference, causing a NullPointerException before checking dynamicAnchors."
                }
            ]
        },
        {
            "group": "Null dynamicAnchors map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/missing\""
                        },
                        {
                            "parameter": "validationContext.schemaResources",
                            "value": "{} (empty map)"
                        },
                        {
                            "parameter": "validationContext.dynamicAnchors",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (thrown when invoking get(anchor) on a null dynamicAnchors map)",
                    "description": "The schemaResources map does not contain the anchor and validationContext.getDynamicAnchors() returns null. The method attempts to call .get(anchor) on the null map, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "Fragment toString throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "Mocked JsonNodePath where getPathType() returns PathType.ANCHOR and toString() throws RuntimeException(\"toString failure\")"
                        }
                    ],
                    "expected": "RuntimeException with message \"toString failure\"",
                    "description": "When the fragment is an anchor, getRefSchema concatenates the result of fragment.toString(). If toString() itself throws an exception, that exception propagates out of getRefSchema."
                }
            ]
        },
        {
            "group": "Fragment getPathType throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "Mocked JsonNodePath where getPathType() throws IllegalStateException(\"path type failure\")"
                        }
                    ],
                    "expected": "IllegalStateException with message \"path type failure\"",
                    "description": "If fragment.getPathType() throws an exception, the method does not catch it, so the exception propagates to the caller."
                }
            ]
        }
    ]
}