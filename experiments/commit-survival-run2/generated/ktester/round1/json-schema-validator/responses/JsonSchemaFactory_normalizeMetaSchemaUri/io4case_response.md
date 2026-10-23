{
  "test_cases": [
    {
      "group": "normalizeMetaSchemaUri - scheme conversion edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "u",
              "value": "ftp://example.com/resource.txt?download=true#section"
            }
          ],
          "expected": "https://example.com/resource.txt",
          "description": "Converts a non‑http scheme (ftp) to HTTPS, preserving authority and path while stripping query and fragment."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://[2001:db8::1]:8080/abc/def"
            }
          ],
          "expected": "https://[2001:db8::1]:8080/abc/def",
          "description": "Handles IPv6 host literals correctly, keeping the port and path."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com:0"
            }
          ],
          "expected": "https://example.com:0",
          "description": "Accepts port 0 (explicitly specified) and converts scheme to HTTPS."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com:65536/path"
            }
          ],
          "expected": "JsonSchemaException with message \"Wrong MetaSchema URI: http://example.com:65536/path\"",
          "description": "Port number out of valid range triggers URISyntaxException, which is wrapped in JsonSchemaException."
        }
      ]
    },
    {
      "group": "normalizeMetaSchemaUri - authority variations",
      "cases": [
        {
          "input": [
            {
              "parameter": "u",
              "value": "https://user@example.com:443/path?query=1#frag"
            }
          ],
          "expected": "https://user@example.com:443/path",
          "description": "Input already uses HTTPS; method must keep user info, host, port and path while discarding query and fragment."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com?"
            }
          ],
          "expected": "https://example.com",
          "description": "URI with an empty query component; result should be HTTPS without a trailing '?' and without a path."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com#fragmentOnly"
            }
          ],
          "expected": "https://example.com",
          "description": "URI containing only a fragment; fragment must be removed and scheme forced to HTTPS."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com:65535/path"
            }
          ],
          "expected": "https://example.com:65535/path",
          "description": "Maximum valid port number is accepted and preserved in the resulting HTTPS URI."
        }
      ]
    },
    {
      "group": "normalizeMetaSchemaUri - path and encoding handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com/%7Euser"
            }
          ],
          "expected": "https://example.com/%7Euser",
          "description": "Preserves percent‑encoded characters (tilde) in the path."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com/space%20here"
            }
          ],
          "expected": "https://example.com/space%20here",
          "description": "Ensures encoded spaces remain unchanged after conversion."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com/../a/b/.."
            }
          ],
          "expected": "https://example.com/../a/b/..",
          "description": "Method does not perform path normalization; it simply copies the raw path."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": ""
            }
          ],
          "expected": "https:",
          "description": "Empty string is parsed as a relative URI; conversion yields the minimal HTTPS URI with no authority."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "/relative/path"
            }
          ],
          "expected": "https:/relative/path",
          "description": "Relative URI without authority is transformed into an HTTPS URI with an empty authority component."
        }
      ]
    }
  ]
}