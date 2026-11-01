{
    "test_cases": [
        {
            "group": "Null Fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null JsonNodePath causes a NullPointerException when the method attempts to call fragment.getPathType()."
                }
            ]
        },
        {
            "group": "Null SchemaLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"anyAnchor\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the owning JsonSchema's schemaLocation is null, the call to this.getSchemaLocation().getAbsoluteIri() throws a NullPointerException before any anchor lookup."
                }
            ]
        },
        {
            "group": "Null ValidationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"anyAnchor\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the JsonSchema's validationContext is null, accessing validationContext.getSchemaResources() results in a NullPointerException."
                }
            ]
        },
        {
            "group": "Null SchemaResources Map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"anyAnchor\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If validationContext.getSchemaResources() returns null, the subsequent .get(anchor) call throws a NullPointerException."
                }
            ]
        },
        {
            "group": "Null DynamicAnchors Map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"anyAnchor\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When validationContext.getDynamicAnchors() returns null, the fallback lookup result = this.validationContext.getDynamicAnchors().get(anchor) throws a NullPointerException."
                }
            ]
        }
    ]
}