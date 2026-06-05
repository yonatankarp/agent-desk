# Public Hygiene

Run the public hygiene scanner before opening review:

```bash
bash scripts/validate-public-hygiene.sh
```

The scanner reads tracked files with `git grep`, so generated build output and `.git` internals are outside the checked scope. It covers source, docs, scripts, GitHub issue and pull request templates, workflow files, and public project policy files.

The blocked patterns target private absolute paths, webhook URLs, common token formats, private key blocks, raw transcript markers, generated OpenClaw workspace artifacts, and real-looking chat or channel identifiers. Private absolute paths include generic Linux, macOS, and Windows user-profile roots. Use placeholders or environment variables for examples instead, such as `/home/<user>/agent-desk/events.ndjson`, `/Users/<user>/agent-desk/events.ndjson`, `C:\Users\<user>\agent-desk\events.ndjson`, `${HOME}/agent-desk-events.ndjson`, or `%USERPROFILE%\agent-desk-events.ndjson`.

Policy examples in this file must use placeholders that do not match blocked patterns, so the documentation is scanned by the same public hygiene checks as other tracked public files.

Use the built-in fixture smoke when changing scanner behavior:

```bash
bash scripts/validate-public-hygiene.sh --self-test
```

The smoke creates a temporary git repository, verifies that the private-path fixture matrix fails, then verifies that public-safe placeholder examples still pass and blocked examples fail even inside this policy document.
