Invalid control character at: line 41 column 41 (char 1398)
{
  "testCases": [
    {
      "group": "Discriminator property value is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext(markMatchCalled=false, matchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode(empty)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "null"
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema()"
            }
          ],
          "expected": "currentDiscriminatorContext.markMatch() is invoked and method returns immediately; no further checks are performed.",
          "description": "Verifies that when the discriminator property value is null the method marks a match and exits without inspecting the discriminator mapping."
        }
      ]
    },
    {
      "group": "Implicit discriminator mapping (no \"mapping\" field)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext(markMatchCalled=false, matchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode({})   // no \"mapping\" property"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"cat\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema()"
            }
          ],
          "expected": "checkForImplicitDiscriminatorMappingMatch is called with value \"cat\"; no explicit mapping logic is executed.",
          "description": "Ensures that when the discriminator object does not contain a \"mapping\" field the method falls back to the implicit‑mapping routine."
        }
      ]
    },
    {
      "group": "Explicit mapping – match found",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext(markMatchCalled=false, matchFound=false, setMatchFound=true)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode({\"mapping\": {\"dog\": \"#/components/schemas/Dog\"}})"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"dog\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema()"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch is invoked and sets matchFound=true; the subsequent implicit‑mapping check is skipped.",
          "description": "Validates that when an explicit mapping exists and the value matches one of the keys, the explicit‑mapping routine records the match and the method does not attempt implicit mapping."
        }
      ]
    },
    {
      "group": "Explicit mapping – no match, no explicit‑key‑override (implicit fallback)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext(markMatchCalled=false, matchFound=false, setMatchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode({\"mapping\": {\"bird\": \"#/components/schemas/Bird\"}})"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"fish\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema(noExplicitKeyOverride=true)"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch runs but does not find a match; noExplicitDiscriminatorKeyOverride returns true, so checkForImplicitDiscriminatorMappingMatch is invoked.",
          "description": "Tests the path where an explicit mapping is present but the discriminator value does not correspond to any key and the schema indicates that implicit mapping is allowed, causing a fallback to the implicit routine."
        }
      ]
    },
    {
      "group": "Explicit mapping – no match, explicit‑key‑override present (no implicit fallback)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext(markMatchCalled=false, matchFound=false, setMatchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode({\"mapping\": {\"rabbit\": \"#/components/schemas/Rabbit\"}})"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"turtle\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema(noExplicitKeyOverride=false)"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch runs and finds no match; noExplicitDiscriminatorKeyOverride returns false, therefore the implicit‑mapping routine is NOT called.",
          "description": "Ensures that when an explicit mapping exists but the value does not match any key and the schema explicitly disables implicit fallback, the method finishes after the explicit check without invoking the implicit routine."
        }
      ]
    }
  ]
}