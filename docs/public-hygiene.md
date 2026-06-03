# Public Hygiene

Run the public hygiene scanner before opening review:

```bash
bash scripts/validate-public-hygiene.sh
```

The scanner reads tracked files with `git grep`, so generated build output and `.git` internals are outside the checked scope. It covers source, docs, scripts, GitHub issue and pull request templates, workflow files, and public project policy files.

The blocked patterns target private absolute paths, webhook URLs, common token formats, private key blocks, raw transcript markers, generated OpenClaw workspace artifacts, and real-looking chat or channel identifiers. Policy examples for these patterns belong in this file, which is allowlisted by the scanner so that docs can explain the rules without weakening checks elsewhere.

Use the built-in fixture smoke when changing scanner behavior:

```bash
bash scripts/validate-public-hygiene.sh --self-test
```

The smoke creates a temporary git repository, verifies that a private-path fixture fails, then verifies that public-safe allowlisted policy examples still pass.
