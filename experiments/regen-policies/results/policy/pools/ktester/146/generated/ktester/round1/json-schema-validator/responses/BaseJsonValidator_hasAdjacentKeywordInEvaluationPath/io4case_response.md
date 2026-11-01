{
    "testCases": [
        {
            "group": "Keyword null input leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null keyword causes a NPE when the method calls `keyword.equals(...)`. The test verifies that the method does not swallow the exception."
                }
            ]
        },
        {
            "group": "Keyword found in grandparent schema after traversing multiple ancestors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "minimum"
                        }
                    ],
                    "expected": true,
                    "description": "The current schema and its immediate parent contain no validator with the keyword. Their evaluation paths end with elements that are not \"properties\" or \"items\" (e.g., \"anyOf\" and \"oneOf\"). The grand‑parent schema does contain a validator whose keyword equals \"minimum\", so the method should climb two levels and return true."
                }
            ]
        },
        {
            "group": "Keyword found in ancestor but early \"properties\" element stops search",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "maxItems"
                        }
                    ],
                    "expected": false,
                    "description": "The current schema does not contain the keyword. Its evaluation path’s last element is \"anyOf\" (so the search continues). The immediate parent schema’s evaluation path ends with \"properties\" – indicating a change in instance location – therefore the method must return false without checking the grand‑parent where the keyword actually exists."
                }
            ]
        },
        {
            "group": "Keyword not found, multiple ancestors, no matching keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "exclusiveMinimum"
                        }
                    ],
                    "expected": false,
                    "description": "None of the schemas in the hierarchy (current, parent, grand‑parent, etc.) contain a validator with the supplied keyword, and none of the evaluation‑path elements are \"properties\" or \"items\". The loop exhausts all ancestors and the method returns false."
                }
            ]
        },
        {
            "group": "Current schema has no validators, keyword found in parent",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "pattern"
                        }
                    ],
                    "expected": true,
                    "description": "The current schema’s `getValidators()` returns an empty collection, so the method immediately checks the evaluation path (which is not \"properties\"/\"items\") and proceeds to the parent schema where a validator with keyword \"pattern\" exists. The method should return true."
                }
            ]
        },
        {
            "group": "Evaluation path element is unrelated (e.g., \"additionalProperties\"), continue search and find match",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "format"
                        }
                    ],
                    "expected": true,
                    "description": "The current schema does not contain the keyword and its evaluation path ends with \"additionalProperties\" – a value that is neither \"properties\" nor \"items\". The method therefore continues to the parent schema, which does contain a validator with keyword \"format\", and returns true."
                }
            ]
        }
    ]
}