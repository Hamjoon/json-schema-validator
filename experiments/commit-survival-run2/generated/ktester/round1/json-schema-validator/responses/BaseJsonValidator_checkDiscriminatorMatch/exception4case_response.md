{
  "test_cases": [
    {
      "group": "null currentDiscriminatorContext – discriminatorPropertyValue null",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
            },
            {
              "parameter": "discriminator",
              "value": "any ObjectNode (not accessed because value is null)"
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
          "expected": "NullPointerException is thrown when currentDiscriminatorContext.markMatch() is invoked",
          "description": "When the discriminator property value is null the method tries to mark a match on the provided context. If the context itself is null a NPE should be raised, confirming that the method does not guard against a null context."
        }
      ]
    },
    {
      "group": "null currentDiscriminatorContext – implicit mapping path",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
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
          "expected": "NullPointerException is thrown when checkForImplicitDiscriminatorMappingMatch tries to use the null currentDiscriminatorContext",
          "description": "With a non‑null discriminator property value and no explicit mapping, the method delegates to the implicit‑mapping helper which accesses the context. Supplying a null context should cause a NPE."
        }
      ]
    },
    {
      "group": "null currentDiscriminatorContext – explicit mapping path",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "null"
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
          "expected": "NullPointerException is thrown when checkForExplicitDiscriminatorMappingMatch attempts to use the null currentDiscriminatorContext",
          "description": "When an explicit mapping exists the method invokes the explicit‑mapping helper which needs the context. Passing a null context should result in a NPE."
        }
      ]
    },
    {
      "group": "jsonSchema null with explicit mapping present",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "mockContext { matchMarked: false, isDiscriminatorMatchFound(): false }"
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
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown inside checkForExplicitDiscriminatorMappingMatch because jsonSchema is null",
          "description": "The explicit‑mapping helper expects a non‑null JsonSchema. Supplying null should cause a NPE, verifying that the outer method does not pre‑validate the schema argument."
        }
      ]
    }
  ]
}