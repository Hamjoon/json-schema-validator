{
    "test_cases": [
        {
            "group": "Null fragment argument causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException is thrown when the method attempts to call fragment.getNameCount() on a null reference.",
                    "description": "Passing a null JsonNodePath should result in an immediate NullPointerException because the implementation does not perform a null‑check before accessing fragment methods."
                }
            ]
        },
        {
            "group": "Root schema resource is null leading to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing /any"
                        }
                    ],
                    "expected": "NullPointerException is thrown when the method tries to invoke methods on a null document returned by findSchemaResourceRoot().",
                    "description": "If findSchemaResourceRoot() is stubbed to return null, the subsequent call to document.getSchemaLocation() (or document.getEvaluationPath()) will cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Parent schema node is null causing NullPointerException in getNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing /missingNode"
                        }
                    ],
                    "expected": "NullPointerException is thrown when getNode(parentNode, segment) is invoked with parentNode == null.",
                    "description": "When the root schema's getSchemaNode() returns null (e.g., an empty or malformed schema), the loop will call getNode(null, segment) which leads to a NullPointerException inside the protected getNode implementation."
                }
            ]
        },
        {
            "group": "ValidationContext is null causing NullPointerException during $id resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath representing /any"
                        }
                    ],
                    "expected": "NullPointerException is thrown when parent.getValidationContext() returns null and the code attempts to call resolveSchemaId(subSchemaNode).",
                    "description": "If the JsonSchema instance is constructed with a null ValidationContext, the call to parent.getValidationContext().resolveSchemaId(subSchemaNode) will dereference a null object, raising a NullPointerException."
                }
            ]
        }
    ]
}