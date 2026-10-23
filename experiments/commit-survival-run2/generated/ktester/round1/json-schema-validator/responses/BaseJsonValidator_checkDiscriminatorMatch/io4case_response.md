{
  "testCases": [
    {
      "group": "discriminator mapping field is null → treated as absent",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): false }"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with \"mapping\" field set to null"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"lion\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": "checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, \"lion\", mockSchema) is invoked; explicit check is NOT invoked because mapping == null is treated as absent",
          "description": "When the discriminator node contains a \"mapping\" property whose value is null, the method should behave as if the mapping were missing and fall back to the implicit discriminator mapping logic."
        }
      ]
    },
    {
      "group": "discriminator node is null → NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false }"
            },
            {
              "parameter": "discriminator",
              "value": "null"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"tiger\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": "NullPointerException is thrown when discriminator.get(\"mapping\") is invoked",
          "description": "The method does not guard against a null discriminator node; passing null should result in a NullPointerException, confirming the current contract."
        }
      ]
    },
    {
      "group": "jsonSchema is null with implicit mapping path → NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): false }"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode without a \"mapping\" field"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"elephant\""
            },
            {
              "parameter": "jsonSchema",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown inside checkForImplicitDiscriminatorMappingMatch because jsonSchema is null",
          "description": "When the discriminator mapping is absent the method delegates to the implicit mapping logic; passing a null JsonSchema should cause that delegate to throw a NullPointerException, verifying that the method does not perform its own null‑check."
        }
      ]
    },
    {
      "group": "discriminator mapping is non‑object (array) → explicit check invoked, fallback allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): false after explicit check }"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with \"mapping\" field that is a JSON array node"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"rabbit\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": [
            "checkForExplicitDiscriminatorMappingMatch is invoked with the array node",
            "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
            "noExplicitDiscriminatorKeyOverride(discriminatorMapping, mockSchema) returns true",
            "checkForImplicitDiscriminatorMappingMatch is invoked"
          ],
          "description": "Even if the \"mapping\" node is not an object (e.g., an array), the method still calls the explicit‑mapping helper. When that helper reports no match and the schema does not override the explicit key, the implicit mapping is tried."
        }
      ]
    },
    {
      "group": "currentDiscriminatorContext already marked match before call (no effect)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: true, isDiscriminatorMatchFound(): true }"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field (non‑null JsonNode)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"dog\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": [
            "checkForExplicitDiscriminatorMappingMatch is still invoked",
            "currentDiscriminatorContext.isDiscriminatorMatchFound() may stay true",
            "no implicit mapping is performed because explicit check already found a match"
          ],
          "description": "The method does not short‑circuit based on the context's existing match state; it still performs the explicit‑mapping check. This test confirms that the pre‑existing match flag does not prevent the method's normal flow."
        }
      ]
    }
  ]
}