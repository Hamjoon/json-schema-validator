{
  "groups": [
    {
      "group": "Null DiscriminatorContext argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with no \"mapping\" field (valid instance)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"anyValue\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema) – not used"
            }
          ],
          "expected": "java.lang.NullPointerException is thrown when the method tries to invoke currentDiscriminatorContext.markIgnore() or currentDiscriminatorContext.isDiscriminatorMatchFound()",
          "description": "The method does not validate the DiscriminatorContext argument. Supplying null should cause an NPE on the first dereference of the context."
        }
      ]
    },
    {
      "group": "Null JsonSchema with explicit mapping present",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field (any JsonNode)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"dog\""
            },
            {
              "parameter": "jsonSchema",
              "value": "null"
            }
          ],
          "expected": "java.lang.NullPointerException is thrown inside checkForExplicitDiscriminatorMappingMatch because jsonSchema argument is null",
          "description": "When an explicit \"mapping\" node exists the method forwards the jsonSchema to the explicit‑mapping helper without null‑checking. Providing a null JsonSchema should cause the helper to throw an NPE."
        }
      ]
    }
  ]
}