# rerout

Official Dart SDK for the [Rerout](https://rerout.co) branded-link API.

```yaml
dependencies:
  rerout: ^0.1.0
```

Requires Dart `^3.9.0`.

## Usage

```dart
import 'package:rerout/rerout.dart';

final rerout = Rerout.initialize(apiKey: 'rrk_...');

final result = await rerout.links.create(
  const CreateLinkRequest(
    targetUrl: 'https://example.com/q4-sale',
    domainHostname: 'go.brand.com',
    code: 'q4',
  ),
);

switch (result) {
  case Success(:final data):
    print('Short URL: ${data.shortUrl}');
  case Error(:final error):
    print('Failed: ${error.message}');
}
```

Every call returns a `Result<T>` — either `Success<T>(data)` or
`Error<T>(ReroutException)`. No thrown exceptions in normal control flow.

## API

### Construction

```dart
Rerout.initialize(
  apiKey: 'rrk_...',                       // required
  baseUrl: 'https://api.rerout.co',        // optional
  dio: customDio,                          // optional — bring your own Dio
);
```

### Links

```dart
rerout.links.create(CreateLinkRequest(...));
rerout.links.list(cursor: 0, limit: 50);
rerout.links.get('q4');
rerout.links.update('q4', UpdateLinkRequest(isActive: false));
rerout.links.delete('q4');
rerout.links.stats('q4', days: 30);
```

### Project

```dart
rerout.project.stats(days: 7);
rerout.project.me();
```

### QR codes

```dart
final url = rerout.qr.url(
  'q4',
  options: const QrOptions(size: 12, ecc: 'H'),
);
final svgResult = await rerout.qr.svg('q4');
```

### Webhook signatures

```dart
final ok = ReroutWebhookSignature.verify(
  rawBody: request.body,
  signatureHeader: request.headers['x-rerout-signature']!,
  secret: const String.fromEnvironment('REROUT_WEBHOOK_SECRET'),
);
```

Default 5-minute tolerance; pass `toleranceSeconds: 0` to disable the time check.

## Error handling

`ReroutException` carries:

- `code` — stable identifier (`bad_target_url`, `rate_limited`, `not_found`, …)
- `statusCode` — HTTP status, or 0 for network failures
- `message` — human-readable
- `path`, `timestamp` — when supplied by the API
- `isServerError`, `isRateLimited` — convenience flags

Synthetic codes used when the server gave no JSON body:
`network_error`, `timeout`, `unexpected_response`, `unauthorized`,
`forbidden`, `not_found`, `rate_limited`, `server_error`, `client_error`.

## Local development

```bash
dart pub get
dart analyze
dart test
```

## License

MIT — see [LICENSE](../LICENSE).
