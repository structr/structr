
Pages in Structr are accessible at URLs that match their names. A page named "about" is available at `/about`, a page named "products" at `/products`. This simplicity is intentional: in Structr, the URL is not just an address – it determines what is displayed and which data is available.

## Why URLs Matter in Structr

In client-side frameworks, URLs are often an afterthought. The application manages its own state, and the URL is updated to reflect it – or sometimes ignored entirely. This leads to applications where the back button breaks, bookmarks don't work, and sharing a link doesn't show the same content.

Structr takes the opposite approach. The URL is the source of truth. When a user navigates to `/projects/a3f8b2c1-...`, Structr resolves that UUID, makes the object available under `current`, and renders the page with that context. No client-side state management, no hydration, no synchronization problems.

This has practical benefits: every application state has a unique, shareable URL. The back button works as expected. Users can bookmark any page, including detail views. And because the server knows exactly what to render from the URL alone, debugging becomes straightforward – you can see the entire application state in the address bar.

## URLs as Entry Points

Because Structr resolves objects directly from URLs, every page can serve as an entry point. Users don't have to navigate through your application to reach a specific record – they can go there directly. This is particularly valuable for applications where users share links, receive notifications with deep links, or return to specific items via bookmarks.

The `current` keyword makes this seamless. You build your detail pages using `current.name`, `current.price`, or any other attribute, and Structr populates them automatically based on the URL. The same page works whether the user clicked through from a list or arrived via a direct link.

## How Structr resolves pages

When a request comes in, Structr determines which page to display based on several factors:

1. **URL Routing**: Structr first checks if any page has a URL route that matches the request path. If a match is found, that page is displayed.

2. **Page Name**: If no route matches, Structr looks for a page whose name matches the URL path.

3. **Visibility and Permissions**: The page must be visible to the current user. For public users, `visibleToPublicUsers` must be enabled. For authenticated users, either `visibleToAuthenticatedUsers` or specific permissions must grant access.

If multiple pages have the same name and the same permissions, Structr cannot distinguish between them and only one will be displayed.

### Pages vs. static files

It is important to understand the difference between dynamic pages and static files when it comes to URL resolution.

For dynamic pages (Page nodes with their tree of Template and DOM elements), Structr is flexible: `/product`, `/product/`, and `/product/index.html` all resolve to the page named `product`. This is the standard behavior unless custom URL routing is configured.

For static files served from the virtual filesystem, Structr resolves paths exactly. A request to `/product` or `/product/` resolves to the folder named `product`, not to a file like `index.html` inside it. Unlike traditional web servers such as Apache or Nginx, Structr does not automatically map directory paths to index files.

This distinction matters when migrating static websites into Structr's virtual filesystem. If your static HTML files use directory-style links like `href="/product/"`, those links will resolve to the folder rather than to an `index.html` file within it. You need to use explicit file references like `href="/product/index.html"` instead.

## The start page

When users navigate to the root URL (`/`), Structr displays a start page based on one of two configurations:

- A page with the lowest `position` value among all visible pages
- A page with "404" configured in `showOnErrorCodes`

If neither configuration exists, Structr returns a standard 404 error. The start page must be visible to public users, otherwise they also receive a 404 error – Structr does not distinguish between non-existent pages and pages without access to avoid leaking information.

## Error pages

You can configure a page to be displayed when specific HTTP errors occur. Set the `showOnErrorCodes` attribute to a comma-separated list of status codes, for example "404" for pages not found or "403" for access denied.

If no error page is configured, Structr returns a standard HTTP error response.

## The current keyword

Structr can automatically resolve objects from URLs and make them available under the `current` keyword. This is one of Structr's core features and enables detail pages without additional configuration.

Note that UUID resolution only works on direct page URLs and partials, not on URL routes. URL routing and UUID resolution are independent mechanisms.

### UUID resolution

When you append a UUID to a page URL, Structr automatically recognizes it and looks up the corresponding object in the database. If the object exists and is visible to the current user, it becomes available under `current`.

For example, navigating to `/products/a3f8b2c1-d4e5-f6a7-b8c9-d0e1f2a3b4c5` makes the Product object with that ID available as `current`. You can then use `${current.name}`, `${current.price}`, and other attributes in your template expressions.

This is useful for populating forms with data. Create a form that uses `current` to fill its input fields, then call the page with the object UUID appended to load that object's data into the form.

### Resolving by other attributes

By default, Structr only resolves objects by UUID. To enable resolution by other attributes, configure `htmlservlet.resolveproperties` in structr.conf. The format is a comma-separated list of `Type.attribute` entries:

```
htmlservlet.resolveproperties = Product.name, Article.title, Project.urlPath
```

With this configuration, navigating to `/products/my-product-name` resolves the Product with that name and makes it available as `current`.

## URL Routing

By default, Structr automatically maps pages to URLs based on their name. URL Routing extends this by allowing you to define custom routing schemes with typed parameters that Structr validates and makes available in the page. This gives you full control over your URL structure beyond the built-in automatic routing.

### How it works

A page can have multiple routes. Structr evaluates URL routes before checking page names, so custom routes take priority over the default name-based resolution. If a route matches, the corresponding page is rendered and the matched parameters are available in template expressions and scripts.

Parameters are optional. If a path segment is missing, the parameter value is `null`. If a value does not match the expected type (for example `"abc"` for an Integer parameter), the page is still rendered but the parameter value is `null` and Structr logs an error.

Multiple routes can point to the same page, allowing a single page to serve different URL patterns. For example, a product page could be reachable via both `/product/{id}` and `/shop/{category}/{id}`.

### Defining routes

In the URL Routing tab of a page, you define path expressions using placeholders following the pattern `/<page>/<param1>/<param2>/.../<paramN>` that allow URL parameters to be mapped to a page and multiple parameters.

The parameters are then available in the page context using their placeholder names. In StructrScript, parameters are accessed with single braces `${paramName}`, while JavaScript blocks use double braces `${{ ... }}`.

> **Note:** Do not use parameter names that are also used as data keys in repeaters, as they will not work.

### Parameter types and validation

For each placeholder, you can select a type that determines how Structr validates and converts the input value. The available types are:

- String
- Integer
- Long
- Double
- Float
- Date
- Boolean

#### String

Any input is accepted as-is and returned without conversion. This is the default type. If an unknown type is configured, Structr logs a warning and falls back to this behavior.

#### Integer and Long

The input is first parsed as a Double, then converted to the target type. This means decimal input is accepted but truncated to the integer part without rounding (e.g. `3.9` becomes `3`, `3.1` becomes `3`). If the input is not a valid number, the parameter value will be `null` and Structr logs a warning:

`WARN o.s.w.entity.path.PagePathParameter - Exception while converting input for PagePathParameter with path aLong: For input string: "123ssdfgsdgf"`

#### Float and Double

The input is parsed as a Double. For Float, the value is then narrowed to float precision, which may cause rounding. Float has approximately 7 significant digits of precision, Double has approximately 15. If the input is not a valid number, the parameter value will be `null` and Structr logs a warning.

#### Boolean

The input is converted using Java's `Boolean.valueOf()`, which returns `true` only if the input string is `"true"` (case-insensitive). Any other value, including `"1"`, `"yes"`, or `"on"`, results in `false`.

#### Date

The input is parsed as an ISO 8601 date string. The following formats are supported:

- `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` (e.g. `2026-02-14T12:34:56.000+01:00`)
- `yyyy-MM-dd'T'HH:mm:ssXXX` (e.g. `2026-02-14T12:34:56+01:00`)
- `yyyy-MM-dd'T'HH:mm:ssZ` (e.g. `2026-02-14T12:34:56Z`)
- `yyyy-MM-dd'T'HH:mm:ss.SSSZ` (e.g. `2026-02-14T12:34:56.000Z`)

If none of the supported formats can parse the input, the parameter value will be `null`. No exception is thrown.


### Examples

#### A single parameter

Path: `/project/{name}`

In the page `project`, access the value of the first URL parameter as follows:

In StructrScript:

`${name}`

In JavaScript:

```
${{
    const projectName = $.name;
}}
```

#### Multiple parameters

Path: `/blog/{lang}/{title}`

In the page `blog`, access the value of the first and second URL parameters as follows:

In StructrScript:

`${lang}`

`${title}`

In JavaScript:

```
${{
    const language = $.lang;
    const title    = $.title;
}}
```

#### Blog-style URLs

Path: `/{category}/{year}/{title}` – for blog-style URLs

Structr uses the value of the first path segment (`category`) to resolve the target page by name.

In StructrScript:

```
${
    log('Year: ', year, ', Category: ', category, ', Title: ', title)
}
```

In JavaScript:

```
${{
    const year        = $.year;
    const category    = $.category;
    const title       = $.title;
}}
```

### Use cases

URL Routing is particularly useful for:

- **SEO-friendly URLs** — using human-readable object names or slugs instead of UUIDs (e.g. `/product/ergonomic-keyboard` instead of `/product?id=a3f8...`)
- **Multilingual sites** — including the language code as a path segment (e.g. `/en/about`, `/de/about`) to serve localized content from a single page
- **Detail pages** — passing identifiers via the URL so a single page template can render different content (e.g. `/user/{username}`, `/order/{orderId}`)
- **Hierarchical content** — modeling category and subcategory structures directly in the URL (e.g. `/docs/{section}/{topic}`)
- **Clean API-style endpoints** — combining URL Routing with page methods to create RESTful-style interfaces served by Structr pages

## Building navigation

This section covers different ways to implement navigation in your application.

### Links between pages

Navigation between pages works like in any other web application: you use standard HTML links with the `href` attribute.

```html
<a href="/about">About Us</a>
<a href="/products">Products</a>
```

If you need links that automatically update when a page is renamed, you can retrieve the page object via scripting and use its `name` attribute as the link target. This is uncommon – most applications use simple string-based links.

### Navigation after actions

Event Action Mappings can navigate to another page after an action completes. This is commonly used to redirect users to a detail page after creating a new object. You specify the target page name in the follow-up action configuration.

For details on configuring navigation in Event Action Mappings, see the Event Action Mapping chapter.

### Dynamic navigation menus

A common pattern in Structr is to generate navigation menus automatically. You implement the menu as a repeater that iterates over pages and creates a link for each one.

To control which pages appear in the menu, you can use visibility settings to include only pages visible to the current user, or add a custom attribute to the Page type (for example `showInMenu`) and filter by it.

```html
<nav>
    <ul>
        <li data-structr-meta-function-query="find('Page', equals('showInMenu', true))" data-structr-meta-data-key="page">
            <a href="/${page.name}">${page.name}</a>
        </li>
    </ul>
</nav>
```

### Request parameters

Request parameters from the URL query string are available via `$.request` in any scripting context.

For example, with the URL `/products?category=electronics&sort=price`:

```javascript
$.request.category  // "electronics"
$.request.sort      // "price"
```

You can use request parameters in template expressions, show/hide conditions, function queries, and any other scripting context.

### Redirects and periodic reloads

The Load/Update Mode settings on the General tab of a page control automatic redirects and periodic reloads. You can configure the page to redirect to another URL when it loads, or to refresh at regular intervals.



## Partials

Every element in a page is directly accessible via its UUID. This allows you to render individual elements independently from their page, which is useful for AJAX requests, dynamic updates, and partial reloads.

### Rendering partials

To render a partial, simply use the element's UUID as the URL:

```
/a3f8b2c1-d4e5-f6a7-b8c9-d0e1f2a3b4c5
```

Structr returns only the HTML of that element and its children. The content type is determined by any content or template elements contained in the partial.

### Organizing partials

You can organize partials in two ways: create a separate page for each partial, or collect all partials in a single page. Since partials are addressed directly by UUID, their location does not matter. Keeping them in a single page can simplify maintenance.

### Partials and the current keyword

UUID resolution for the `current` keyword also works with partials. Append an object UUID to the partial URL to make that object available under `current` when the partial renders.

When a URL contains two UUIDs, Structr resolves the first one as the partial and the second one as the detail object:

```
/a3f8b2c1-d4e5-f6a7-b8c9-d0e1f2a3b4c5/b4c5d6e7-f8a9-b0c1-d2e3-f4a5b6c7d8e9
```

In this example, the first UUID addresses the partial and the second UUID is resolved as the `current` object.

### Partial reloads

Instead of reloading the entire page, you can update individual elements independently. Configure this via Event Action Mapping by specifying the target element either by its CSS ID or by linking it directly in the mapping configuration.

For details on configuring partial reloads, see the Event Action Mapping chapter.
