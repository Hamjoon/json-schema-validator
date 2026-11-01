{
  "test_cases": [
    {
      "group": "putAnnotation - overriding core fields",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with an empty AnnotationMap"
            },
            {
              "parameter": "customizer",
              "value": "consumer that calls builder.withEvaluationPath(\"/override\") and builder.withSchemaLocation(new SchemaLocation(\"/overrideSchema\")) and builder.withKeyword(\"overrideKeyword\")"
            }
          ],
          "expected": "AnnotationMap contains exactly one JsonNodeAnnotation whose evaluationPath is \"/override\", schemaLocation is \"/overrideSchema\", and keyword is \"overrideKeyword\" (the values set by the customizer). No other attributes are present.",
          "description": "Verify that the customizer can replace the default evaluationPath, schemaLocation and keyword supplied by the validator."
        }
      ]
    },
    {
      "group": "putAnnotation - existing annotation replacement",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext whose AnnotationMap already contains an annotation with key equal to the builder's default key and value = JsonNodeAnnotation with keyword \"oldKeyword\""
            },
            {
              "parameter": "customizer",
              "value": "consumer that adds attribute \"newKey\":\"newValue\""
            }
          ],
          "expected": "AnnotationMap still contains a single entry for the same key; the old annotation is replaced by the new one that has the original evaluationPath, schemaLocation, keyword (from validator) and the added attribute \"newKey\":\"newValue\".",
          "description": "Ensure that putAnnotation overwrites an existing annotation for the same location instead of creating a duplicate."
        }
      ]
    },
    {
      "group": "putAnnotation - customizer invoked exactly once",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with empty AnnotationMap"
            },
            {
              "parameter": "customizer",
              "value": "a spy consumer that records the number of invocations; it simply calls builder.withAttribute(\"count\",\"1\")"
            }
          ],
          "expected": "The spy reports exactly one invocation of accept(builder); AnnotationMap contains one annotation with the added attribute.",
          "description": "Validate that putAnnotation calls the supplied Consumer exactly once."
        }
      ]
    },
    {
      "group": "putAnnotation - customizer adds null attribute value",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with empty AnnotationMap"
            },
            {
              "parameter": "customizer",
              "value": "consumer that calls builder.withAttribute(\"nullKey\", null)"
            }
          ],
          "expected": "AnnotationMap contains one JsonNodeAnnotation that includes the attribute \"nullKey\" with a null value (or the attribute is omitted if the builder discards nulls, but no exception is thrown).",
          "description": "Check how the builder/annotation handles a null attribute value supplied by the customizer."
        }
      ]
    },
    {
      "group": "putAnnotation - executionContext with pre‑populated map",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext whose AnnotationMap already holds two unrelated annotations"
            },
            {
              "parameter": "customizer",
              "value": "consumer that adds attribute \"extra\":\"value\""
            }
          ],
          "expected": "AnnotationMap size becomes three (the two original entries remain unchanged and a new annotation is added). The new annotation has the expected default fields plus the extra attribute.",
          "description": "Confirm that putAnnotation adds a new entry without disturbing existing unrelated annotations."
        }
      ]
    }
  ]
}