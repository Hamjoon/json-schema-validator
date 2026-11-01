{
  "testCases": [
    {
      "group": "Discriminator property value is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where markIgnore() is verified"
            },
            {
              "parameter": "discriminator",
              "value": "any ObjectNode (not used)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "null"
            },
            {
              "parameter": "jsonSchema",
              "value": "any JsonSchema (not used)"
            }
          ],
          "expected": "currentDiscriminatorContext.markIgnore() is called exactly once and method returns without invoking any other helper methods",
          "description": "When the discriminator property value is null the validator must ignore the discriminator and exit early."
        }
      ]
    },
    {
      "group": "Implicit discriminator mapping (no explicit mapping node)",
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
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema)"
            }
          ],
          "expected": "checkForImplicitDiscriminatorMappingMatch is invoked with the given context, value and schema; no explicit‑mapping helper is called",
          "description": "If the discriminator object does not contain a \"mapping\" property the validator must fall back to the implicit‑mapping logic."
        }
      ]
    },
    {
      "group": "Explicit discriminator mapping match found",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false initially and will be set to true by the explicit‑mapping helper"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field containing a mapping that matches the discriminatorPropertyValue"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"dog\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema)"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch is called and it marks the context as a match; the implicit‑mapping helper is NOT called afterwards",
          "description": "When an explicit mapping exists and matches the discriminator value the validator must record the match and skip any implicit fallback."
        }
      ]
    },
    {
      "group": "Explicit mapping not found, implicit fallback allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false before and after explicit check"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field that does NOT contain the discriminatorPropertyValue"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"bird\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema) crafted so that noExplicitDiscriminatorKeyOverride(mapping, schema) returns true"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch is called, finds no match; then noExplicitDiscriminatorKeyOverride returns true, so checkForImplicitDiscriminatorMappingMatch is called",
          "description": "If an explicit mapping is present but does not match and the schema permits an implicit fallback, the validator must invoke the implicit‑mapping logic."
        }
      ]
    },
    {
      "group": "Explicit mapping not found, implicit fallback NOT allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mock(DiscriminatorContext) where isDiscriminatorMatchFound() returns false before and after explicit check"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with a \"mapping\" field that does NOT contain the discriminatorPropertyValue"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"fish\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mock(JsonSchema) crafted so that noExplicitDiscriminatorKeyOverride(mapping, schema) returns false"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch is called, finds no match; noExplicitDiscriminatorKeyOverride returns false, therefore checkForImplicitDiscriminatorMappingMatch is NOT called",
          "description": "When an explicit mapping is present but does not match and the schema explicitly disables implicit fallback, the validator must stop after the explicit check."
        }
      ]
    }
  ]
}