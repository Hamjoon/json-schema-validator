{
    "group": "BaseJsonValidator.toString() - edge and special cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "evaluationPathSegments",
                    "value": ["root", "intermediate", null]
                }
            ],
            "expected": null,
            "description": "When the last segment of the evaluation path is null, getName(-1) returns null and toString() should propagate that null value."
        },
        {
            "input": [
                {
                    "parameter": "evaluationPathSegments",
                    "value": []
                },
                {
                    "parameter": "getNameThrows",
                    "value": "IndexOutOfBoundsException"
                }
            ],
            "expected": "java.lang.IndexOutOfBoundsException",
            "description": "If the JsonNodePath implementation throws an IndexOutOfBoundsException for getName(-1) on an empty path, toString() must not catch it and the same exception should be observed by the caller."
        },
        {
            "input": [
                {
                    "parameter": "evaluationPathSegments",
                    "value": ["root", "child", "leaf with spaces"]
                }
            ],
            "expected": "leaf with spaces",
            "description": "Verification that toString() returns the exact last segment even when it contains whitespace characters."
        },
        {
            "input": [
                {
                    "parameter": "evaluationPathSegments",
                    "value": ["根", "子节点", "叶子"]
                }
            ],
            "expected": "叶子",
            "description": "Ensures that Unicode characters in the evaluation path are handled correctly and the last Unicode segment is returned unchanged."
        },
        {
            "input": [
                {
                    "parameter": "evaluationPathSegments",
                    "value": [
                        "seg0","seg1","seg2","seg3","seg4","seg5","seg6","seg7","seg8","seg9",
                        "seg10","seg11","seg12","seg13","seg14","seg15","seg16","seg17","seg18","seg19",
                        "seg20","seg21","seg22","seg23","seg24","seg25","seg26","seg27","seg28","seg29",
                        "seg30","seg31","seg32","seg33","seg34","seg35","seg36","seg37","seg38","seg39",
                        "seg40","seg41","seg42","seg43","seg44","seg45","seg46","seg47","seg48","seg49",
                        "seg50","seg51","seg52","seg53","seg54","seg55","seg56","seg57","seg58","seg59",
                        "seg60","seg61","seg62","seg63","seg64","seg65","seg66","seg67","seg68","seg69",
                        "seg70","seg71","seg72","seg73","seg74","seg75","seg76","seg77","seg78","seg79",
                        "seg80","seg81","seg82","seg83","seg84","seg85","seg86","seg87","seg88","seg89",
                        "seg90","seg91","seg92","seg93","seg94","seg95","seg96","seg97","seg98","seg99"
                    ]
                }
            ],
            "expected": "seg99",
            "description": "Stress test with a large number of path segments (100) to confirm that toString() still correctly returns the final segment without performance degradation or off‑by‑one errors."
        }
    ]
}