# Outbound HTTP

Structr can call external HTTP services from any script context. The functions below share one shape:
the URL first, then a body where the verb has one, then the content type, then a single options object.

## Functions

| Function | Signature | Returns |
|----------|-----------|---------|
| `$.GET` | `url [, contentType [, options ]]` | `{ body, status, headers }` |
| `$.HEAD` | `url [, options ]` | `{ body, status, headers }`, body always `null` |
| `$.POST` | `url, body [, contentType [, options ]]` | `{ body, status, headers }` |
| `$.PUT` | `url, body [, contentType [, options ]]` | `{ body, status, headers }` |
| `$.PATCH` | `url, body [, contentType [, options ]]` | `{ body, status, headers }` |
| `$.DELETE` | `url [, body [, contentType [, options ]]]` | `{ body, status, headers }` |
| `$.FETCH` | `url, method [, body [, contentType [, options ]]]` | `{ body, status, headers }` |
| `$.POSTMultiPart` | `url, partsMap [, options ]` | `{ body, status, headers }` |

`status` is an integer. Use `$.FETCH` for a method the others do not cover.

```javascript
{
    let response = $.GET('https://api.example.com/projects', 'application/json', { parseResponse: true });

    if (response.status === 200) {
        return response.body.results;
    }
}
```

## Options

These are accepted by every function:

| Option | Meaning |
|--------|---------|
| `username`, `password` | Basic authentication |
| `preemptive` | Send the credentials with the first request instead of waiting for a `401` |
| `headers` | Additional request headers, merged over `$.addHeader()` |
| `timeout` | Seconds to wait, for both connecting and reading. Without it the defaults `application.httphelper.timeouts.connect` (60 seconds to establish the connection) and `application.httphelper.timeouts.socket` (600 seconds of inactivity between two data packets) apply. |
| `redirects` | Whether to follow redirects |
| `validateCertificates` | Whether to verify TLS certificates |

These are accepted only where they mean something, and are an error elsewhere:

| Option | Where | Meaning |
|--------|-------|---------|
| `parseResponse` | all but `$.HEAD` | Parse the response body as JSON |
| `selector` | `$.GET` | A CSS selector applied to a `text/html` response |
| `binaryResponse` | `$.GET`, `$.POST` | Return the response body as a stream |

An unsupported option is rejected with a message naming the function and the key. This is deliberate: an
option that was accepted and ignored, such as a `timeout` that never applied, would only show up much
later as a call that hung.

## The Character Set Belongs to the Content Type

There is no separate charset argument. Put it where HTTP puts it:

```javascript
$.POST(url, body, 'application/json; charset=ISO-8859-1');
```

## Sending a File

Pass a `File` as the body and its content is streamed from the storage backend, without base64 or a
multipart envelope:

```javascript
$.PUT('https://api.example.com/documents/42', $.first($.find('File', 'name', 'report.pdf')), 'application/pdf');
```

A stream cannot be sent twice, which matters for authentication: basic credentials are normally sent only
after the server answers `401`, and answering means repeating the request. Set `preemptive` when sending a
file to a service that needs credentials, or the call is refused:

```javascript
$.PUT(url, file, 'application/pdf', { username: 'u', password: 'p', preemptive: true });
```

Use `$.POSTMultiPart` when the endpoint expects a form upload with several parts.

## Guidelines

- **Always check `status`.** Nothing about a failed request throws. An HTTP error arrives as its own
  status, so a `404` is `status: 404`, and a request that never reached the server at all is `status: 0`
  with the reason in `error`. Only an unusable call throws, see below.
- **Prefer `parseResponse` over parsing by hand.** It uses the same JSON handling as the rest of Structr.
- **Set a `timeout` for anything a user waits on.** Without one the call waits for the configured defaults,
  60 seconds to connect and 600 seconds for data, which is generous.
- **Keep `validateCertificates` on.** Turning it off disables the check for that call entirely, not just
  for an unknown authority.
- **Omit an argument you do not need.** `$.POST(url, body, { timeout: 5 })` is understood: an options
  object is recognised even when the content type before it is left out.

## Errors

There are three outcomes, and only the last one throws.

| Outcome | Result |
|---------|--------|
| The server answered | `status` is what it sent, `body` may be `null` for a `204` or a bodyless error |
| No answer: DNS, connection refused, TLS, timeout | `status: 0`, `body: null`, `error` describes the failure |
| The call itself is unusable | throws a `FrameworkException` |

```javascript
{
    let response = $.GET('https://api.example.com/thing');

    if (response.status === 0) {
        $.log('service unreachable: ' + response.error);

    } else if (response.status !== 200) {
        $.log('service returned ' + response.status);

    } else {
        return response.body;
    }
}
```

A status of `0` rather than an exception matters in StructrScript, which has no `try`/`catch`: a throw
there could not be handled at all. What still throws is a call that could never be made - a malformed URL
(`400`), a scheme other than http or https (`400`), a URL with no host (`400`), an address in an internal
network (`403`), or a URL outside `application.httphelper.urlwhitelist` (`422`).

## Caveats

- **A response body is text unless you ask otherwise.** For binary data use `binaryResponse` on `$.GET` or
  `binaryResponse` on `$.POST`, which return a stream. A stream can be read once.
- **`$.DELETE` may carry a body**, which is permitted by HTTP but rejected by some servers.
- **`headers` are merged over `$.addHeader()`**, so an option with the same name wins.
- **These functions are not proxied** through the settings used by the crawler; they connect directly.

Signatures changed in 7.0. See the Migration chapter for the previous form and for the automatic rewrite.
