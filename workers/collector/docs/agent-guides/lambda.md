# Python Lambda guidance

Read when changing handlers, dependencies, runtime, packaging, or deployment.
These local summaries capture the required AWS guidance. Fetch the linked AWS
pages only to resolve uncertainty or verify requirements affected by the task;
do not fetch them for unrelated changes.

## Handler requirements

Follow the [AWS Python handler guide](https://docs.aws.amazon.com/ko_kr/lambda/latest/dg/python-handler.html).

- Use a synchronous `def handler(event, context)` entry point; configure its
  importable module/function path explicitly. A Python `async def` is not a valid
  Lambda handler. Return values, if any, must be JSON serializable; asynchronous
  invocation discards them.
- Keep handlers thin: validate input, delegate collection logic, and translate
  outcomes to the trigger's contract. Keep domain logic independently testable.
- Initialize reusable SDK clients and database resources outside the handler.
  Handle idle connections safely. Never retain invocation-specific or sensitive
  state globally; `/tmp` is a disposable cache, not durable job storage.
- Read operational configuration from environment variables. Minimize dependency
  complexity and package size. Use public APIs and avoid recursive invocation.
- Make processing idempotent and validate events before side effects.

## AWS build and deployment requirements

Follow the [AWS Python packaging guide](https://docs.aws.amazon.com/ko_kr/lambda/latest/dg/python-package.html).

- For ZIP delivery, stage dependencies and the importable handler/package at the
  archive root. Do not accidentally wrap them in `src/` or a build directory.
- Package all runtime dependencies, including Boto3 when used, with compatible
  transitive versions. Bundled libraries override layers and runtime libraries;
  avoid partial SDK dependency overrides.
- Native dependencies, including a PostgreSQL driver's native components, must
  match Lambda's Linux environment, Python version, and selected CPU architecture.
  Use compatible Linux wheels or build on the matching Amazon Linux environment;
  do not ship Windows binaries.
- Exclude `__pycache__` and local environments. Set readable file permissions
  (644 for ordinary files; 755 for directories and executables).
- Check current upload and uncompressed-size limits, including layers, before
  deployment. Record runtime, architecture, handler, and artifact location.

The current scaffold specifies Python 3.12 in `.python-version`, Python >=3.12 in
`pyproject.toml`, and the `uv_build` backend. Keep development, dependency
resolution, build, and deployed runtime aligned. The console script is not a
Lambda handler configuration. Do not change package format, runtime, deployment
tooling, or introduce layers merely because an AWS example uses them.
