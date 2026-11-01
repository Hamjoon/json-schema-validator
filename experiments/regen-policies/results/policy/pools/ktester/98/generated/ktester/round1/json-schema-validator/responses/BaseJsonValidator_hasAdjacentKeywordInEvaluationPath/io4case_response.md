{
  "testCases": [
    {
      "group": "No match across multiple ancestors",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "minimum"
            }
          ],
          "expected": false,
          "description": "Three ancestor schemas each contain several validators, but none of them have the keyword \"minimum\". The method must walk the whole chain and finally return false."
        }
      ]
    },
    {
      "group": "Keyword matches validator in current schema only",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "type"
            }
          ],
          "expected": false,
          "description": "The validator's own schema (the one being evaluated) holds a validator with keyword \"type\" while all evaluation‑parent schemas lack it. Since the method only inspects parent schemas, it should return false."
        }
      ]
    },
    {
      "group": "Validator list contains null element",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "maxItems"
            }
          ],
          "expected": "NullPointerException",
          "description": "One of the entries returned by getValidators() on the evaluation‑parent schema is null. When the loop reaches this entry, calling validator.getKeyword() triggers a NullPointerException."
        }
      ]
    },
    {
      "group": "Schema validators collection is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "minLength"
            }
          ],
          "expected": "NullPointerException",
          "description": "The evaluation‑parent schema's getValidators() method returns null. The enhanced‑for loop attempts to iterate over a null collection, causing a NullPointerException."
        }
      ]
    },
    {
      "group": "Empty keyword matches empty validator keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": ""
            }
          ],
          "expected": true,
          "description": "A validator in the nearest evaluation‑parent schema returns an empty string from getKeyword(). The equals check succeeds, so the method returns true."
        }
      ]
    },
    {
      "group": "Case‑sensitivity mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "MAXLENGTH"
            }
          ],
          "expected": false,
          "description": "A validator exists with keyword \"maxLength\" (lower‑case \"m\"). Because String.equals is case‑sensitive, the supplied upper‑case keyword does not match and the method returns false."
        }
      ]
    },
    {
      "group": "Keyword with surrounding whitespace does not match",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": " pattern "
            }
          ],
          "expected": false,
          "description": "The parent schema contains a validator whose keyword is \"pattern\" (no spaces). The supplied keyword includes leading and trailing spaces, so equals fails and the method returns false."
        }
      ]
    }
  ]
}