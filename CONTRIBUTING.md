# Contributing to UsefulAllays

Thanks for considering a contribution.

## Basic rules

- Keep the plugin Paper-focused unless a compatibility layer is explicitly planned.
- Keep gameplay values configurable where it makes sense.
- Do not add hard dependencies unless they are required.
- Avoid NMS unless there is no clean API solution.
- Keep code readable and split features into services/listeners instead of bloating the main class.

## Branch naming

Examples:

```text
feature/allay-trust-system
fix/teleport-world-change
cleanup/config-loading
```

## Pull requests

A good pull request should include:

- what changed
- why it changed
- how it was tested
- any config or permission changes

## Testing checklist

Before opening a PR, please test at least:

- plugin startup
- `/usefulallays reload`
- claiming an unclaimed Allay
- interacting with an owned Allay
- teleporting with a claimed Allay loaded
- server restart with a claimed Allay loaded
