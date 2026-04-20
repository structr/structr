By default, no URL violations are allowed. When using plain string parameters (i.e. accepting arbitrary user input) via the path for URL Routing or GET methods, you may need to explicitly allow certain URL violations via the configuration key `httpservice.uricompliance.allowedviolations`. Relevant options include:

- `AMBIGUOUS_EMPTY_SEGMENT` - allows empty path segments
- `AMBIGUOUS_PATH_SEPARATOR` - allows `%2f` (`/`) within user-provided values
- `AMBIGUOUS_PATH_ENCODING` - allows `%25` (`%`) within user-provided values

For URL Routing, the first violation can be circumvented by adding a static element to each path segment. The latter two violations can be avoided using a Base64UrlString parameter, since it produces URL-safe output by design.

When calling methods via GET and passing named parameters via path segments, the first violation can not be avoided when empty values are supposed to be valid. The latter two violations can be avoided by manually using URL-safe transport encodings like base64url for parameter values.
