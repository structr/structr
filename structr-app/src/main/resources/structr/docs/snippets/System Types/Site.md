# Site

Controls which pages are served for which domain. A single Structr instance can host multiple websites – useful for running public and internal sites side by side, serving localized versions under country domains, or operating staging and production on the same server. Key properties include `name`, `hostname`, and an optional `port` for exact matching.

## Details

When a request arrives, Structr checks hostname and port against configured sites and serves only the assigned pages. Pages without site assignment are served everywhere (the default behavior). A page can belong to multiple sites. Sites control page visibility only – files remain accessible regardless of domain. Site configurations are included in deployment exports.
