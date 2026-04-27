This IntelliJ workspace spans multiple repositories.

- `CinderStride` in `../CinderStride` is the main app and the default project focus.
- `Dirigo` is a separate dependency repo used by `CinderStride`.
- Do not assume a task belongs to `Dirigo` just because the current working directory is this repo.
- When the request is ambiguous, verify whether the intended change belongs in `CinderStride`, `Dirigo`, or both.
