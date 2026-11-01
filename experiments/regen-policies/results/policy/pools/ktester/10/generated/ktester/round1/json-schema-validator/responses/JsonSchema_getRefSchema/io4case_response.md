{
  "testCases": [
    {
      "group": "Anchor branch – schema resource found",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"myAnchor\""
            },
            {
              "parameter": "schemaLocation.absoluteIri",
              "value": "\"http://example.com/schemas/main.json\""
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "Map containing key \"http://example.com/schemas/main.json#myAnchor\" mapped to a mock JsonSchema instance \"resourceSchema\""
            }
          ],
          "expected": "resourceSchema",
          "description": "When the fragment is an anchor, the method builds the full anchor string using the base IRI from schemaLocation (\"http://example.com/schemas/main.json\") plus \"#\" and the fragment string. It then looks up this key in validationContext.getSchemaResources(). Because the map contains the key, the corresponding JsonSchema (resourceSchema) is returned."
        }
      ]
    },
    {
      "group": "Anchor branch – dynamic anchor fallback",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"dynAnchor\""
            },
            {
              "parameter": "schemaLocation.absoluteIri",
              "value": "\"urn:example:root\""
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "Empty map"
            },
            {
              "parameter": "validationContext.dynamicAnchors",
              "value": "Map containing key \"urn:example:root#dynAnchor\" mapped to a mock JsonSchema instance \"dynamicSchema\""
            }
          ],
          "expected": "dynamicSchema",
          "description": "If the anchor is not found in the static schema resources, the method should query the dynamicAnchors map. When the map contains the constructed anchor, the associated JsonSchema (dynamicSchema) must be returned."
        }
      ]
    },
    {
      "group": "Anchor branch – missing absolute IRI (base empty)",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"plainAnchor\""
            },
            {
              "parameter": "schemaLocation.absoluteIri",
              "value": "null"
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "Map containing key \"#plainAnchor\" mapped to a mock JsonSchema instance \"resourceSchema\""
            }
          ],
          "expected": "resourceSchema",
          "description": "When the schemaLocation has no absolute IRI, the base string should be empty. The constructed anchor becomes \"#plainAnchor\". The method must still locate the schema in schemaResources and return it."
        }
      ]
    },
    {
      "group": "Anchor branch – anchor not found throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"missing\""
            },
            {
              "parameter": "schemaLocation.absoluteIri",
              "value": "\"http://example.org/base.json\""
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "Empty map"
            },
            {
              "parameter": "validationContext.dynamicAnchors",
              "value": "Empty map"
            }
          ],
          "expected": "JsonSchemaException with message containing \"Unable to find anchor http://example.org/base.json#missing\"",
          "description": "If the constructed anchor cannot be resolved in either schemaResources or dynamicAnchors, the method must throw a JsonSchemaException indicating the missing anchor."
        }
      ]
    },
    {
      "group": "Anchor branch – fragment string contains URL‑encoded characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"my%20anchor\""
            },
            {
              "parameter": "schemaLocation.absoluteIri",
              "value": "\"http://example.com/schema.json\""
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "Map containing key \"http://example.com/schema.json#my%20anchor\" mapped to mock JsonSchema \"encodedSchema\""
            }
          ],
          "expected": "encodedSchema",
          "description": "The method does not decode the fragment; it uses the raw string returned by JsonNodePath.toString(). This test confirms that an anchor containing percent‑encoded characters is handled correctly and the exact key is looked up."
        }
      ]
    }
  ]
}