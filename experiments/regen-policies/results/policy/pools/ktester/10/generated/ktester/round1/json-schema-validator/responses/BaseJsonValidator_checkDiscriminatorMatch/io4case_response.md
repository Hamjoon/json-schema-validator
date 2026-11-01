{
  "testCases": [
    {
      "group": "nullDiscriminatorPropertyValue",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext (initial state, matchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with any content (e.g., empty object)"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "null"
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema (any schema)"
            }
          ],
          "expected": "currentDiscriminatorContext.markMatch() is called exactly once; after execution currentDiscriminatorContext.isDiscriminatorMatchFound() returns true",
          "description": "When the discriminator property value is null the method should immediately mark a match on the context and return without inspecting the discriminator mapping."
        }
      ]
    },
    {
      "group": "noMappingImplicitMatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext (initial state, matchFound=false)"
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
              "value": "MockJsonSchema where implicit mapping for \"cat\" resolves to a subschema"
            }
          ],
          "expected": "checkForImplicitDiscriminatorMappingMatch is invoked; after execution currentDiscriminatorContext.isDiscriminatorMatchFound() returns true",
          "description": "If the discriminator object does not contain a \"mapping\" field, the method should fall back to the implicit discriminator mapping logic and mark a match when the implicit mapping resolves."
        }
      ]
    },
    {
      "group": "explicitMappingMatchFound",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext (initial state, matchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with \"mapping\": { \"dog\": \"#/components/schemas/Dog\" }"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"dog\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema whose subschema at \"#/components/schemas/Dog\" exists"
            }
          ],
          "expected": "checkForExplicitDiscriminatorMappingMatch finds the key and marks a match; currentDiscriminatorContext.isDiscriminatorMatchFound() returns true and no implicit check is performed",
          "description": "When an explicit mapping is present and the discriminator property value matches a key in that mapping, the method should record a match via the explicit‑mapping logic and skip any implicit mapping."
        }
      ]
    },
    {
      "group": "explicitMappingNoMatchButImplicitAllowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext (initial state, matchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with \"mapping\": { \"bird\": \"#/components/schemas/Bird\" }"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"fish\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema where implicit mapping for \"fish\" resolves to a subschema and no explicit‑key override is defined"
            }
          ],
          "expected": "explicit mapping check does not find a match; noExplicitDiscriminatorKeyOverride returns true; implicit mapping is evaluated and marks a match; currentDiscriminatorContext.isDiscriminatorMatchFound() returns true",
          "description": "If the explicit mapping does not contain the discriminator value but the schema does not forbid an implicit fallback (no explicit‑key override), the method should try the implicit mapping and succeed."
        }
      ]
    },
    {
      "group": "explicitMappingNoMatchAndOverridePreventsImplicit",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext (initial state, matchFound=false)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode with \"mapping\": { \"rabbit\": \"#/components/schemas/Rabbit\" }"
            },
            {
              "parameter": "discriminatorPropertyValue",
              "value": "\"hamster\""
            },
            {
              "parameter": "jsonSchema",
              "value": "MockJsonSchema where an explicit‑key override is defined for \"hamster\" (noImplicit=true) so implicit mapping must be ignored"
            }
          ],
          "expected": "explicit mapping check does not find a match; noExplicitDiscriminatorKeyOverride returns false; implicit mapping is NOT evaluated; currentDiscriminatorContext.isDiscriminatorMatchFound() remains false",
          "description": "When the explicit mapping does not match and the parent schema defines an explicit‑key override that disables implicit fallback, the method must leave the context without a match."
        }
      ]
    }
  ]
}