{
    "tests": [
        {
            "group": "BaseJsonValidator.toString – special characters in last path element",
            "cases": [
                {
                    "input": [],
                    "expected": "!@#$%^&*()_+-=[]{}|;':\",.<>/?",
                    "description": "The evaluation path contains several elements and the last element includes a variety of special characters. toString() should return the exact last element without alteration."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – whitespace‑only last element",
            "cases": [
                {
                    "input": [],
                    "expected": "   ",
                    "description": "The evaluation path ends with a name that consists solely of spaces. toString() must return those spaces unchanged, proving it does not trim the result."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – mixed null and valid elements",
            "cases": [
                {
                    "input": [],
                    "expected": "finalNode",
                    "description": "The evaluation path contains a null element before the final one (e.g., [\"root\", null, \"finalNode\"]). toString() should ignore the null earlier element and return the last valid name."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – custom JsonNodePath throws IllegalStateException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalStateException",
                    "description": "A subclass of JsonNodePath is injected that deliberately throws IllegalStateException when getName(-1) is called. toString() must propagate this unchecked exception unchanged."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.toString – evaluation path returns empty string",
            "cases": [
                {
                    "input": [],
                    "expected": "",
                    "description": "The evaluation path’s last element is an empty string (e.g., [\"root\", \"\"]). toString() should return the empty string exactly, confirming that null and empty string are distinguished."
                }
            ]
        }
    ]
}