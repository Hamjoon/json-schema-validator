{
  "testCases": [
    {
      "group": "Keyword found in current schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "type"
            }
          ],
          "expected": true,
          "description": "The evaluation parent schema contains a validator whose keyword equals the supplied keyword. The method should return true immediately without inspecting the evaluation path."
        }
      ]
    },
    {
      "group": "Keyword not found, evaluation path element is \"properties\"",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "minimum"
            }
          ],
          "expected": false,
          "description": "No validator in the current schema (or any ancestor) matches the keyword. The evaluation path's last element is \"properties\", causing the method to return false as soon as it detects a change in instance location."
        }
      ]
    },
    {
      "group": "Keyword not found, evaluation path element is \"items\"",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "maxLength"
            }
          ],
          "expected": false,
          "description": "Similar to the previous case but the last element of the evaluation path is \"items\". The method should return false because a change in instance location has been detected."
        }
      ]
    },
    {
      "group": "Keyword not found, evaluation path element is other value, continue to parent and find match",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "pattern"
            }
          ],
          "expected": true,
          "description": "The current schema does not contain the keyword and its evaluation path element is neither \"properties\" nor \"items\" (e.g., \"anyOf\"). The method climbs to the parent schema where a validator with the matching keyword exists, thus returning true."
        }
      ]
    },
    {
      "group": "Keyword not found, no parent schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "keyword",
              "value": "exclusiveMaximum"
            }
          ],
          "expected": false,
          "description": "The evaluation parent schema (and all its ancestors) contain no validator with the given keyword, and the evaluation path never contains \"properties\" or \"items\". The loop exhausts all schemas and the method returns false."
        }
      ]
    }
  ]
}