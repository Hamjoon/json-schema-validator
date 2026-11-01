{
    "group": "OneOfValidator.canShortCircuit()",
    "cases": [
        {
            "input": [],
            "expected": true,
            "description": "First invocation when `canShortCircuit` is null and the parent schema's validators list does **not** contain any validator with keyword \"unevaluatedProperties\" or \"unevaluatedItems\". The loop never flips the flag, so the method should return true and cache the result."
        },
        {
            "input": [],
            "expected": false,
            "description": "First invocation when `canShortCircuit` is null and the parent schema's validators list contains a validator whose `getKeyword()` returns \"unevaluatedProperties\". The presence of this keyword forces `canShortCircuit` to be set to false, which is then cached and returned."
        },
        {
            "input": [],
            "expected": false,
            "description": "First invocation when `canShortCircuit` is null and the parent schema's validators list contains a validator whose `getKeyword()` returns \"unevaluatedItems\". The method should detect the keyword, set the flag to false, cache it and return false."
        },
        {
            "input": [],
            "expected": false,
            "description": "First invocation when the validators list contains **multiple** validators, one of which has the keyword \"unevaluatedProperties\" (or \"unevaluatedItems\"). Even though other validators have unrelated keywords, the presence of the disallowed keyword makes the method return false."
        },
        {
            "input": [],
            "expected": true,
            "description": "Second invocation after a previous call has already cached `canShortCircuit` as true. The internal field is no longer null, so the method should bypass the loop and directly return the cached true value."
        },
        {
            "input": [],
            "expected": false,
            "description": "Second invocation after a previous call has already cached `canShortCircuit` as false (e.g., because a disallowed keyword was found earlier). The method should return the cached false without re‑evaluating the validators."
        },
        {
            "input": [],
            "expected": true,
            "description": "First invocation when the parent schema's validators list is empty. No keywords are examined, the flag stays true, and the method returns true."
        }
    ]
}