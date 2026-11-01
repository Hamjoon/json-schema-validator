{
    "tests": [
        {
            "group": "Known keyword – minimum annotation stored",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, schemaNode = { \"minimum\": 5 }, keyword = \"minimum\" }"
                        }
                    ],
                    "expected": "true",
                    "description": "When the validator’s keyword is a known numeric keyword (\"minimum\") and the schema node contains the corresponding value, the method should create a JsonNodeAnnotation, add it to the context and return true."
                }
            ]
        },
        {
            "group": "Keyword present but schemaNode missing expected value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, schemaNode = { }, keyword = \"type\" }"
                        }
                    ],
                    "expected": "false",
                    "description": "The keyword is recognised (\"type\") but the schema node does not contain a \"type\" property; the validator cannot build a meaningful annotation and therefore returns false."
                }
            ]
        },
        {
            "group": "Empty keyword string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, keyword = \"\" }"
                        }
                    ],
                    "expected": "false",
                    "description": "An empty string returned by getKeyword() should be treated as an unknown keyword, causing the method to skip annotation creation and return false."
                }
            ]
        },
        {
            "group": "ExecutionContext with pre‑existing annotations (no duplication)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, annotations = [ { \"keyword\": \"type\" } ], keyword = \"type\" }"
                        }
                    ],
                    "expected": "true",
                    "description": "Even if the context already holds an annotation for the same keyword, collectAnnotations should still add a new annotation (or safely ignore duplication) and return true without throwing."
                }
            ]
        },
        {
            "group": "ExecutionContext with null annotations list (should initialise)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, annotations = null, keyword = \"type\" }"
                        }
                    ],
                    "expected": "true",
                    "description": "When the ExecutionContext’s annotation collection is enabled but its internal list is null, collectAnnotations must initialise the list, store the new annotation and return true."
                }
            ]
        },
        {
            "group": "SuppressSubSchemaRetrieval true – annotation still collected",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, suppressSubSchemaRetrieval = true, keyword = \"format\", schemaNode = { \"format\": \"email\" } }"
                        }
                    ],
                    "expected": "true",
                    "description": "The validator’s `suppressSubSchemaRetrieval` flag does not affect annotation collection; with a known keyword (\"format\") the method should still create the annotation and return true."
                }
            ]
        }
    ]
}