{
    "test_cases": [
        {
            "group": "putAnnotation - null customizer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with a fresh, empty AnnotationMap"
                        },
                        {
                            "parameter": "customizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the method attempts to invoke accept on a null Consumer",
                    "description": "Verify that passing a null Consumer causes an NPE, confirming the method does not guard against a null customizer."
                }
            ]
        },
        {
            "group": "putAnnotation - executionContext with null annotation map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext whose getAnnotations() returns null"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> {}"
                        }
                    ],
                    "expected": "NullPointerException is thrown when attempting to call put on a null AnnotationMap",
                    "description": "Ensure that if the ExecutionContext provides a null AnnotationMap, the method fails with NPE at the put step."
                }
            ]
        },
        {
            "group": "putAnnotation - annotation map throws exception on put",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext whose AnnotationMap is a custom Map that throws UnsupportedOperationException on put"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> {}"
                        }
                    ],
                    "expected": "UnsupportedOperationException propagates out of putAnnotation and no annotation is stored",
                    "description": "Validate that any runtime exception thrown by the underlying AnnotationMap during put is not swallowed."
                }
            ]
        },
        {
            "group": "putAnnotation - builder missing required fields leads to IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with a fresh, empty AnnotationMap"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> builder.withEvaluationPath(null).withSchemaLocation(null).withKeyword(null)"
                        }
                    ],
                    "expected": "IllegalStateException (or NullPointerException depending on builder implementation) is thrown when builder.build() is invoked because required fields are null",
                    "description": "Check that the builder validates required fields and throws an exception if they are cleared/overridden with null values."
                }
            ]
        }
    ]
}