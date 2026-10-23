{
    "groups": [
        {
            "group": "keywordWalkListenerRunner is null – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/null‑runner\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown when keywordWalkListenerRunner.runPreWalkListeners is invoked",
                    "description": "The JsonSchema instance has its keywordWalkListenerRunner field set to null. Calling walk should attempt to invoke runPreWalkListeners, resulting in a NullPointerException before any walker execution."
                }
            ]
        },
        {
            "group": "jsonWalker is null in validators map – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/null‑walker\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown when jsonWalker.walk is called",
                    "description": "The validators map returned by getValidators() contains a key with a null JsonSchemaWalker value. When pre‑walk listeners return true, the method tries to invoke walk on the null walker, causing a NullPointerException."
                }
            ]
        },
        {
            "group": "validators map is null – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/null‑validators\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown when attempting to iterate over a null validators map",
                    "description": "The JsonSchema instance is stubbed so that getValidators() returns null. The walk method calls getValidators().forEach, which throws a NullPointerException because the map reference is null."
                }
            ]
        },
        {
            "group": "jsonWalker returns null – NullPointerException on addAll",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/walker‑null‑return\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown when validationMessages.addAll receives a null collection",
                    "description": "The validator map contains a JsonSchemaWalker whose walk method returns null instead of a Set<ValidationMessage>. After a true pre‑walk result, walk attempts validationMessages.addAll(null), which throws a NullPointerException."
                }
            ]
        }
    ]
}