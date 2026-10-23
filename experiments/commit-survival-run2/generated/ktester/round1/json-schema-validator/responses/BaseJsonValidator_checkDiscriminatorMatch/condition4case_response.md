{
  "testCases": [
    {
      "group": "discriminatorPropertyValue is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false }"
            },
            {
              "parameter": "discriminator",
              "value": "null (not used)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "null"
            },
            {
              "parameter": "jsonSchema",
              "value": "null (not used)"
            }
          ],
          "expected": "currentDiscriminatorContext.markMatch() is invoked; no other methods are called",
          "description": "When the discriminator property value is null the method should immediately mark a match and return without inspecting the discriminator mapping."
        }
      ]
    },
    {
      "group": "discriminator mapping is absent → implicit mapping used",
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
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": "checkForImplicitDiscriminatorMappingMatch(currentDiscriminatorContext, \"cat\", mockSchema) is invoked",
          "description": "If the discriminator node does not contain a \"mapping\" property, the validator must fall back to the implicit mapping logic."
        }
      ]
    },
    {
      "group": "explicit mapping present and match found",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): true after explicit check }"
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
            "checkForExplicitDiscriminatorMappingMatch is invoked",
            "currentDiscriminatorContext.isDiscriminatorMatchFound() returns true",
            "noImplicit check is performed"
          ],
          "description": "When an explicit mapping exists and the explicit check reports a match, the method must not invoke the implicit mapping logic."
        }
      ]
    },
    {
      "group": "explicit mapping present, no explicit match, implicit fallback allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): false after explicit check }"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field (non‑null JsonNode)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"bird\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": [
            "checkForExplicitDiscriminatorMappingMatch is invoked",
            "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
            "noExplicitDiscriminatorKeyOverride(discriminatorMapping, jsonSchema) returns true",
            "checkForImplicitDiscriminatorMappingMatch is invoked"
          ],
          "description": "If the explicit mapping does not yield a match and the schema does not override the explicit key, the validator must fall back to the implicit mapping."
        }
      ]
    },
    {
      "group": "explicit mapping present, no explicit match, implicit fallback NOT allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): false after explicit check }"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field (non‑null JsonNode)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"fish\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockSchema"
            }
          ],
          "expected": [
            "checkForExplicitDiscriminatorMappingMatch is invoked",
            "currentDiscriminatorContext.isDiscriminatorMatchFound() returns false",
            "noExplicitDiscriminatorKeyOverride(discriminatorMapping, jsonSchema) returns false",
            "checkForImplicitDiscriminatorMappingMatch is NOT invoked"
          ],
          "description": "When the explicit mapping fails and the schema explicitly overrides the discriminator key, the validator must stop after the explicit check and must not attempt an implicit match."
        }
      ]
    }
  ]
}