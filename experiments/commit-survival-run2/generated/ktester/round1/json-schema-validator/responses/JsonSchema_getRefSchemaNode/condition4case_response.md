{
  "testCases": [
    {
      "group": "JSON Pointer starting with \"#/\" – normal path",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "#/properties/name"
            }
          ],
          "expected": "Returns the JsonNode located at \"/properties/name\" (non‑missing node). The method should not invoke handleNullNode.",
          "description": "The reference begins with \"#/\". The leading \"#\" is stripped, leaving \"/properties/name\" which starts with \"/\". After URL decoding the pointer the code calls node.at(\"/properties/name\"). The mocked node.at returns a non‑missing JsonNode, so the method returns that node directly."
        }
      ]
    },
    {
      "group": "JSON Pointer starting with \"/\" – missing node triggers fallback",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/definitions/address"
            }
          ],
          "expected": "Returns the fallback JsonNode produced by handleNullNode(ref, schema).",
          "description": "The reference starts directly with \"/\". After decoding the pointer the code calls node.at(\"/definitions/address\"). The mocked node.at returns a MissingNode, causing the method to call handleNullNode and return its result."
        }
      ]
    },
    {
      "group": "Fragment reference starting with \"#\" (non‑empty) – meta‑schema resolves",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "#foo"
            }
          ],
          "expected": "Returns the JsonNode supplied by metaSchema.getNodeByFragmentRef(ref, originalNode).",
          "description": "The reference does not start with \"#/\" nor with \"/\" but matches the first part of the else‑if (starts with \"#\" and length > 1). The meta‑schema is asked for the fragment node and returns a non‑null node, which is then returned by the method."
        }
      ]
    },
    {
      "group": "Fragment reference starting with \"urn:\" – meta‑schema cannot resolve, fallback used",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "urn:example:schema"
            }
          ],
          "expected": "Returns the fallback JsonNode produced by handleNullNode(ref, schema).",
          "description": "The reference starts with \"urn:\" and length > 4, satisfying the else‑if condition. metaSchema.getNodeByFragmentRef returns null, so the method calls handleNullNode and returns its result."
        }
      ]
    },
    {
      "group": "Reference with no special prefix – returns original schema node",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "simple"
            }
          ],
          "expected": "Returns the original schema node (the node obtained from findAncestor().getSchemaNode()).",
          "description": "The reference does not match any of the conditional prefixes, so none of the branches are taken and the method returns the original node unchanged."
        }
      ]
    },
    {
      "group": "JSON Pointer with URL‑encoded characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "#/encoded%20path"
            }
          ],
          "expected": "Returns the JsonNode located at \"/encoded path\" after URL decoding.",
          "description": "The reference begins with \"#/\". After stripping the leading \"#\" the pointer is \"/encoded%20path\". URLDecoder.decode converts it to \"/encoded path\". node.at(\"/encoded path\") returns a non‑missing node, which is returned directly."
        }
      ]
    }
  ]
}