{
  "newTestCases": [
    {
      "group": "Null discriminator node",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) – no expectations needed"
            },
            {
              "parameter": "discriminator",
              "value": "null"
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
          "expected": "java.lang.NullPointerException is thrown when attempting to call discriminator.get(\"mapping\")",
          "description": "The method does not guard against a null discriminator ObjectNode. Supplying null should result in a NPE at the first dereference."
        }
      ]
    },
    {
      "group": "Discriminator mapping node is not an Object (e.g., TextNode)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field whose value is a TextNode \"notAnObject\""
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"mouse\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema) – no special behavior"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch is invoked with the TextNode mapping; no implicit‑mapping helper is called because the explicit‑mapping check is performed regardless of the node type",
          "description": "Even when the \"mapping\" field exists but is not an object, the validator must still call the explicit‑mapping helper. The helper itself decides how to handle a non‑object mapping."
        }
      ]
    },
    {
      "group": "Context already has a discriminator match before method call",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns true"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode without a \"mapping\" field"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema)"
            }
          ],
          "expected": "checkForImplicitDiscriminatorMappingMatch is still invoked, but the context remains marked as a match (isDiscriminatorMatchFound() stays true). No explicit‑mapping helper is called because there is no \"mapping\" field.",
          "description": "The method does not short‑circuit when the context already reports a match. It should still perform the implicit‑mapping logic, but must not clear the existing match flag."
        }
      ]
    },
    {
      "group": "Empty discriminator property value (\"\"), explicit mapping matches",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false initially"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field that contains a key \"\" (empty string) mapping to a schema reference"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema)"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch is called and marks the context as a match; checkForImplicitDiscriminatorMappingMatch is NOT called afterwards",
          "description": "An empty string is a valid discriminator value. When an explicit mapping contains an empty‑string key that matches, the validator must record the match and skip the implicit fallback."
        }
      ]
    },
    {
      "group": "Null jsonSchema argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode without a \"mapping\" field"
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
          "expected": "java.lang.NullPointerException is thrown when the implicit‑mapping helper receives a null schema",
          "description": "The method forwards the JsonSchema argument to the mapping helpers without null‑checking. Supplying null should cause a NPE inside the helper."
        }
      ]
    }
  ]
}