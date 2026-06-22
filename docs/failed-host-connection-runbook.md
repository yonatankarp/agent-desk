# Failed Local Host Connection Runbook

This runbook diagnoses local-network host connection failures without exposing
private LAN details in public artifacts.

## Local-Only Flow

1. Confirm a local host profile exists outside tracked project source or in an
   ignored `agent-desk.host*.properties` file.
2. Confirm the profile uses a public-safe `hostAlias`, such as `host:primary`.
3. Run the local reachability smoke:

   ```bash
   ./gradlew :cli:run --args='host-smoke --host-config agent-desk.host.properties'
   ```

4. Record only the public-safe diagnostic state and failure category.
5. If the command reports a failure, follow the matching branch below.
6. Do not paste the real profile, endpoint, port, token, raw exception, local
   path, screenshot, or runtime id into public issues or pull requests.

## Expected Public Output

Reachable:

```text
Host reachability: host=host:primary state=reachable.
```

Missing configuration:

```text
Host reachability: host=not-configured state=not-configured failure=missing-configuration.
```

Unreachable:

```text
Host reachability: host=host:primary state=unreachable failure=network-unavailable.
```

Timeout:

```text
Host reachability: host=host:primary state=timed-out failure=timeout.
```

Authentication rejected:

```text
Host reachability: host=host:primary state=rejected failure=authentication-rejected.
```

Unsupported host mode:

```text
Host reachability: host=host:primary state=unsupported-host-mode failure=unsupported-host-mode.
```

Unsafe detail redacted:

```text
Host reachability: host=host:primary state=unsafe-private-detail-redacted failure=unsafe-private-detail-redacted private-detail=redacted.
```

## Troubleshooting Branches

### Missing Configuration

- Check that the local profile path passed to `--host-config` exists.
- Check that `hostAlias` and `hostEndpoint` are present in the local profile.
- Use the checked-in placeholder template only as a shape reference, not as a
  real connection profile.

Safe public evidence: command name, `not-configured`, and
`missing-configuration`.

Keep local: real profile path, endpoint value, host name, port, and profile
contents.

### Unreachable Host

- Confirm the host process is running on the same local network.
- Confirm the operator machine is on the expected network.
- Check local firewall or network isolation rules.
- Re-run the smoke after the local service is known to be listening.

Safe public evidence: public alias, `unreachable`, and
`network-unavailable`.

Keep local: host name, address, port, firewall output, network names, and raw
socket errors.

### Timeout

- Check whether the host process is slow, overloaded, or blocked by network
  filtering.
- Retry from the same local environment before changing product code.
- Prefer a later lab fixture for repeatable CI evidence instead of relying on a
  private local timeout.

Safe public evidence: public alias, `timed-out`, and `timeout`.

Keep local: timing logs that include private endpoints, raw process output, and
network traces.

### Auth Rejection

- Confirm local auth material is configured and current.
- Confirm the host recognizes the operator profile.
- If auth is pending or expired, resolve it locally before trying read-only
  observation or action-capable modes.

Safe public evidence: public alias, auth state, and
`authentication-rejected`.

Keep local: tokens, headers, pairing codes, account ids, credential references,
and raw provider responses.

### Unsupported Runtime Mode

- Confirm the selected host mode is one Agent Desk supports for the current
  milestone.
- Fall back to diagnostic-only mode before attempting live observation.
- Do not add live control behavior as a shortcut; mutating live actions remain
  out of scope until the action proposal flow is accepted.

Safe public evidence: public alias, `unsupported-host-mode`, and the public
mode name.

Keep local: private host capability payloads and raw runtime feature flags.

### Unsafe Detail Redacted

- Treat this as a privacy-boundary warning.
- Inspect the local profile and mapper path for values that would leak private
  details.
- Replace public comments or artifacts with the redacted diagnostic only.

Safe public evidence: public alias, `unsafe-private-detail-redacted`, and
`private-detail=redacted`.

Keep local: the original unsafe value and any raw parser or adapter output.

## Public Evidence Checklist

Safe to share:

- issue, PR, commit, workflow, and check names
- public-safe host alias
- diagnostic state and failure category
- sanitized command names and local check names
- synthetic fixture or lab output

Keep local:

- hostnames, addresses, ports, URLs, and socket paths
- tokens, cookies, authorization headers, one-time codes, and credential refs
- raw runtime ids, process ids, session ids, channel ids, and message ids
- raw transcripts, prompts, logs, stack traces, and private screenshots
- local filesystem paths and network names
