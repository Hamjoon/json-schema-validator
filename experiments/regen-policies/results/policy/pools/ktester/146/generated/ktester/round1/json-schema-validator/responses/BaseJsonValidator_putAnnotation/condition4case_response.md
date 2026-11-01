{
  "testCases": [
    {
      "group": "putAnnotation - default behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with a fresh, empty AnnotationMap"
            },
            {
              "parameter": "customizer",
              "value": "consumer that invokes accept(builder) but does not modify the builder"
            }
          ],
          "expected": "AnnotationMap contains exactly one JsonNodeAnnotation whose evaluationPath equals validator.evaluationPath, schemaLocation equals validator.schemaLocation, and keyword equals validator.getKeyword(); no additional attributes are present.",
          "description": "Verify that putAnnotation creates an annotation with the default fields when the customizer does nothing."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with a fresh, empty AnnotationMap"
            },
            {
              "parameter": "customizer",
              "value": "consumer that calls builder.withAttribute(\"customKey\", \"customValue\") before returning"
            }
          ],
          "expected": "AnnotationMap contains exactly one JsonNodeAnnotation whose evaluationPath, schemaLocation and keyword are set as above and that also holds a custom attribute 'customKey' with value 'customValue'.",
          "description": "Ensure that a customizer can enrich the builder and that the resulting annotation reflects those extra attributes."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with a fresh, empty AnnotationMap"
            },
            {
              "parameter": "customizer",
              "value": "consumer that throws new RuntimeException(\"boom\") when accept(builder) is invoked"
            }
          ],
          "expected": "RuntimeException with message 'boom' propagates out of putAnnotation and the AnnotationMap remains empty (no partial annotation stored).",
          "description": "Validate that an exception thrown by the customizer propagates and does not leave a partially built annotation in the context."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "customizer",
              "value": "any consumer (e.g., builder -> {})"
            }
          ],
          "expected": "NullPointerException is thrown because executionContext is dereferenced.",
          "description": "Check that passing a null ExecutionContext results in an NPE, confirming the method does not guard against null arguments."
        }
      ]
    }
  ]
}