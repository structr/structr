# Page

Represents a complete web page. Structr renders pages on the server, so browsers receive ready-to-display HTML rather than JavaScript that builds the page client-side. Key properties include `name` (also determines the URL), `contentType` for output format, `position` for start page selection, `showOnErrorCodes` for error pages, and `sites` for multi-site hosting.

## Details

Pages support template expressions for dynamic content, repeaters for collections, partial reloads without full page refresh, and show/hide conditions. Permissions control both data access and what renders – you can make entire page sections visible only to certain users. URL Routing lets you define custom paths with typed parameters that Structr validates automatically.
