{
  "test_cases": [
    {
      "group": "checkDiscriminatorMatch – null currentDiscriminatorContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (no \"mapping\" field)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockJsonSchema (non‑null)"
            }
          ],
          "expected": "NullPointerException is thrown because the method attempts to use a null DiscriminatorContext inside checkForImplicitDiscriminatorMappingMatch.",
          "description": "When the discriminator property value is non‑null but the DiscriminatorContext argument is null, the method eventually calls checkForImplicitDiscriminatorMappingMatch which dereferences the context, resulting in a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\": {\"cat\": \"#/components/schemas/Cat\"}}"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "mockJsonSchema"
            }
          ],
          "expected": "NullPointerException is thrown when checkForExplicitDiscriminatorMappingMatch tries to interact with the null DiscriminatorContext.",
          "description": "Even though a mapping node exists, the explicit‑mapping helper receives a null context and throws a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (any value, not used because property value is null)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "null"
            },
            {
              "parameter": "jsonSchema",
              "value": "mockJsonSchema"
            }
          ],
          "expected": "NullPointerException is thrown when the method attempts to invoke currentDiscriminatorContext.markIgnore() in the early‑return branch.",
          "description": "If the discriminator property value is null, the method returns early after calling markIgnore(); a null context causes a NullPointerException at that call."
        }
      ]
    },
    {
      "group": "checkDiscriminatorMatch – null jsonSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockDiscriminatorContext (no match flag set)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (no \"mapping\" field)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown inside checkForImplicitDiscriminatorMappingMatch because the JsonSchema argument is null.",
          "description": "When there is no explicit mapping and the schema argument is null, the implicit‑discriminator helper receives a null schema and throws a NullPointerException."
        },
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockDiscriminatorContext (isDiscriminatorMatchFound() returns false after explicit check)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\": {\"cat\": \"#/components/schemas/Cat\"}}"
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
          "expected": "NullPointerException is thrown either in checkForExplicitDiscriminatorMappingMatch or in noExplicitDiscriminatorKeyOverride when they receive a null JsonSchema.",
          "description": "With an explicit mapping present but a null JsonSchema, the method proceeds to the explicit‑mapping branch; any internal use of the schema (e.g., to resolve references) results in a NullPointerException."
        }
      ]
    }
  ]
}