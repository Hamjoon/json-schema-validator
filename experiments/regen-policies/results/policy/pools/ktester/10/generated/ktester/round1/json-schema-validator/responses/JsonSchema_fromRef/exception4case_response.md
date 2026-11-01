{
    "test_cases": [
        {
            "group": "fromRef - null validationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithNullValidationContext"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "JsonNodePath.of(\"/any\")"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The parent schema's internal ValidationContext is null. The method dereferences validationContext to copy its configuration, which should result in a NullPointerException."
                }
            ]
        },
        {
            "group": "fromRef - null config in validationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithNullConfig"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "JsonNodePath.of(\"/props\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The parent schema's ValidationContext returns a null config object. The constructor of ValidationContext expects a non‑null config, so fromRef should throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "fromRef - null schemaReferences collection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithNullSchemaReferences"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "JsonNodePath.of(\"/defs\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The parent schema's ValidationContext returns null for getSchemaReferences(). The new ValidationContext constructor validates that the map is non‑null and should raise an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "fromRef - null schemaResources collection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithNullSchemaResources"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "JsonNodePath.of(\"/resources\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The parent schema's ValidationContext returns null for getSchemaResources(). The constructor of ValidationContext requires a non‑null map, leading to an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "fromRef - null dynamicAnchors collection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "refEvaluationParentSchema",
                            "value": "parentSchemaWithNullDynamicAnchors"
                        },
                        {
                            "parameter": "refEvaluationPath",
                            "value": "JsonNodePath.of(\"/anchors\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The parent schema's ValidationContext returns null for getDynamicAnchors(). ValidationContext expects a non‑null set, so fromRef should throw an IllegalArgumentException."
                }
            ]
        }
    ]
}