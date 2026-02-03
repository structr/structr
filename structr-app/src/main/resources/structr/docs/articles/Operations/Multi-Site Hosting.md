# Multi-Site Hosting

A single Structr instance can serve multiple websites under different domains. This is useful when you want to run a public website and an internal application side by side, serve localized versions of your site under country-specific domains, or operate staging and production environments on the same server.

Structr uses Site objects to control which pages are served for which domain. You can think of this as a built-in reverse proxy: when a request arrives, Structr checks the hostname and port against your configured sites and serves only the pages assigned to the matching site.

Pages not assigned to any site are served for all requests, which is the default behavior when you don't use this feature. Sites control page visibility only — files are not affected and remain accessible regardless of the requesting domain.

## Creating a Site

Site is a built-in type in Structr. To create a site:

1. Open the Data area in the Admin UI
2. Select the `Site` type
3. Create a new Site object with the following properties:

| Property | Description |
|----------|-------------|
| `name` | A descriptive name for the site (e.g., "Production Website") |
| `hostname` | The domain name this site responds to (e.g., `example.com`) |
| `port` | Optional port number. If omitted, the site matches any port. |

## Assigning Pages to Sites

Since there is no dedicated UI for managing site assignments, you configure the relationship between pages and sites in the Data area:

1. Open the Data area in the Admin UI
2. Select either the `Site` type and edit the `pages` property, or select the `Page` type and edit the `sites` property
3. Add or remove the relationship as needed

A page can be assigned to multiple sites if it should appear on more than one domain.

## Request Matching

When Structr receives an HTTP request, it determines which pages to serve based on the following rules:

1. If the page is not assigned to any site, it is visible for all requests
2. If the page is assigned to one or more sites, Structr checks whether the request's hostname and port match any of those sites
3. A site matches if the hostname equals the request's hostname AND either the site has no port defined or the port matches the request's port

This means you can create a site with only a hostname to match all ports, or specify a port for exact matching.

## Example Configuration

Consider a Structr instance accessible via three domains:

- `www.example.com` (port 443) – public website
- `admin.example.com` (port 443) – internal admin area  
- `staging.example.com` (port 8443) – staging environment

You would create three sites:

| Site Name | Hostname | Port |
|-----------|----------|------|
| Public | www.example.com | (empty) |
| Admin | admin.example.com | (empty) |
| Staging | staging.example.com | 8443 |

Then assign your pages accordingly:

- Public marketing pages → Public site
- Admin dashboard pages → Admin site
- Test versions of pages → Staging site
- Shared components (e.g., error pages) → No site assignment (visible everywhere)

## Deployment

Sites are included in application deployment exports. When you import an application, the site configurations are restored along with the page assignments.

If you deploy to an environment with different domain names (e.g., from staging to production), you may need to update the hostname properties after import.

## Related Topics

- Pages - Creating and managing pages
- Application Lifecycle - Exporting and importing applications
