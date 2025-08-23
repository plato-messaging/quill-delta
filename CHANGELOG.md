## 2.0.0

### Breaking changes

- `Delta` is now an extension of `ArrayList<Op>`
  - `Delta::getOps` & `OpList` no longer exist. `OpList.Iterator` is replaced by `Delta.Iterator`
  - JSON serialization of `Delta` now results in a array of operations with no embedding in an `ops` document
