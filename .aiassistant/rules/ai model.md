# AI Critical Constraints

## Protected Files & Constants
- **File:** `airepository.kt`
- **Constraint:** STRICTLY FORBIDDEN to modify the `model name` or any version-related constants.
- **Reason:** The model version is hardcoded for stability and must only be changed manually by a human.

## Modification Rules
1. If you need to suggest a logic change in `airepository.kt`, do so, but keep the `model` string exactly as it is.
2. Never "refactor" or "update" the model name even if you think a newer version is available.
3. If a task requires changing the model, ask for explicit human permission in the chat first.

## Code Style for Kotlin
- Maintain existing architecture in `airepository.kt`.
- Use functional approach for data mapping.