{
  "testCases": [
    {
      "group": "Id matches known VersionFlag draft‑04",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-04/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-04/schema#",
          "description": "The supplied id equals the id of the VersionFlag enum constant for Draft‑04, therefore `found` becomes true and the method returns the original id unchanged."
        }
      ]
    },
    {
      "group": "Id matches known VersionFlag draft‑06",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-06/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-06/schema#",
          "description": "The supplied id equals the id of the VersionFlag enum constant for Draft‑06, causing the method to return the unchanged id."
        }
      ]
    },
    {
      "group": "Id matches known VersionFlag draft‑2019‑09",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/2019-09/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2019-09/schema#",
          "description": "The supplied id equals the id of the VersionFlag enum constant for Draft‑2019‑09, so the method returns it unchanged."
        }
      ]
    },
    {
      "group": "Id matches known VersionFlag draft‑2020‑12",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/2020-12/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2020-12/schema#",
          "description": "The supplied id equals the id of the VersionFlag enum constant for Draft‑2020‑12, therefore no normalisation occurs."
        }
      ]
    },
    {
      "group": "Null id input throws NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null reference causes a NPE when the method attempts to call `contains` or `equals` on it."
        }
      ]
    },
    {
      "group": "Empty string id returns empty string",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": ""
            }
          ],
          "expected": "",
          "description": "An empty string does not match any VersionFlag and does not contain the draft marker, so the method returns it unchanged."
        }
      ]
    },
    {
      "group": "Id contains both draft‑07 and draft‑04, normalises to V7 (order test)",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://example.com/xyz://json-schema.org/draft/draft-04/schema#?ref=//json-schema.org/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The string contains the generic marker and both recognised sub‑paths. Because the method checks for `/draft-07/` before `/draft-04/`, it normalises to the V7 constant."
        }
      ]
    },
    {
      "group": "Id contains draft‑07 without trailing slash – no normalisation",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07schema#",
          "description": "The generic marker is present but the specific sub‑path `/draft-07/` (with a trailing slash) is not, so the method leaves the id unchanged."
        }
      ]
    },
    {
      "group": "Id contains generic marker with uppercase domain – no normalisation",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://JSON-SCHEMA.ORG/draft-07/schema#"
            }
          ],
          "expected": "http://JSON-SCHEMA.ORG/draft-07/schema#",
          "description": "The `contains` check is case‑sensitive; the uppercase domain prevents detection of the draft marker, so the id is returned unchanged."
        }
      ]
    },
    {
      "group": "Id contains generic marker with different scheme (ftp) – normalises",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "ftp://json-schema.org/draft/draft-06/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-06/schema#",
          "description": "Even though the scheme is `ftp`, the substring `://json-schema.org/draft` is present, and the recognised `/draft-06/` segment triggers normalisation to `SchemaId.V6`."
        }
      ]
    }
  ]
}