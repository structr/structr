# Migration Guide

This chapter covers breaking changes and migration steps when upgrading between major Structr versions.

> **Important:** Always create a full backup before upgrading Structr.

## Migrating to Structr 7.x

Version 7 changes the signatures of the outbound HTTP functions. All optional arguments moved into a
single trailing options object, so a call no longer has to pass positional nulls to reach the last one,
and an option that is not supported by a function is rejected instead of being silently ignored.

### Outbound HTTP Function Signatures

| Function | Signature |
|----------|-----------|
| `$.GET` | `url [, contentType [, options ]]` |
| `$.HEAD` | `url [, options ]` |
| `$.POST` | `url, body [, contentType [, options ]]` |
| `$.PUT` | `url, body [, contentType [, options ]]` |
| `$.PATCH` | `url, body [, contentType [, options ]]` |
| `$.DELETE` | `url [, body [, contentType [, options ]]]` |
| `$.FETCH` | `url, method [, body [, contentType [, options ]]]` |
| `$.POST_multi_part` | `url, partsMap [, options ]` |

```javascript
// Old (6.x): charset, then credentials, as positional arguments
$.POST(url, body, 'application/json', 'UTF-8', 'user', 'secret');

// New (7.x): the charset belongs to the content type, everything else is an option
$.POST(url, body, 'application/json; charset=UTF-8', { username: 'user', password: 'secret' });
```

`username`, `password`, `preemptive`, `headers`, `timeout`, `redirects` and `validateCertificates` are
accepted by every function. `selector`, `binaryResponse` and `parseResponse` are accepted
only where they mean something; passing one elsewhere is an error naming the function and the key.

Most calls can be rewritten automatically. See `application.migration.mode` and the `migrate` maintenance
command, which report every call that still uses the old form and can rewrite the unambiguous ones.

### Binary Responses Are Streams

`$.POST` previously returned the response body as a byte array when the content type was
`application/octet-stream`. Streaming the response is now requested by the `binaryResponse` option, the
same key `$.GET` uses, and the result is an `InputStream` for both. The stream removes the 2 GB limit
that the array imposed.

```javascript
// Old (6.x): a byte array
const bytes = $.POST(url, body, 'application/octet-stream').body;

// New (7.x): a stream, which can be passed straight to setContent()
const stream = $.POST(url, body, 'application/octet-stream', { binaryResponse: true }).body;
$.get_or_create('File', { name: 'result.bin' }).setContent(stream);
```

**This change cannot be detected automatically.** The call text is unchanged, so the migration report
will not flag it: any code that indexes into the result, measures its length or stores it as a byte array
has to be adjusted by hand.

### Failed Requests No Longer Throw

A request that reached the server has always returned its status. A request that did NOT reach it - an
unreachable host, a refused connection, a TLS failure, a timeout - used to throw a `FrameworkException`
with status 422. It now returns a response with `status: 0` and an `error` field describing the failure.

```javascript
// Old (6.x): only a try/catch could survive an unreachable service, and StructrScript could not
try { $.GET(url); } catch (e) { /* ... */ }

// New (7.x): check the status, which StructrScript can do as well
let response = $.GET(url);
if (response.status === 0) { $.log('unreachable: ' + response.error); }
```

Two related corrections come with it. An error response WITHOUT a body, such as a bodyless `404` or a
`204`, used to be reported as a thrown 422 because the empty body was read unchecked; it now returns the
status the server sent. And a rejected URL used to be re-wrapped as that same 422; its own status now
survives.

Calls that cannot be made at all still throw: a malformed URL, a scheme other than http or https, a URL
without a host, an internal network address, or a URL outside the outgoing whitelist.

**Code that relies on a `catch` to detect an unreachable service will no longer enter it.** Check
`status === 0` instead.

### Binary Request Bodies

A `File` passed as the request body is now sent as its content rather than as its string representation,
so an upload no longer needs base64 or a multipart envelope. See the Filesystem chapter.

### PDF Generation Without wkhtmltopdf

The `pdf()` function and the `PdfServlet` no longer call the external `wkhtmltopdf` binary. The document
is now produced inside the JVM, so nothing has to be installed on the server and PDF generation works
from a cron job or a `doPrivileged` context as well as from a request.

Three things change for existing applications.

**`pdf()` returns a File, not a string.** The old return value was the document's bytes carried in an
ISO-8859-1 string, which every caller then wrote into a file.

```
// Old (6.x)
${ set_content(create('File', 'name', 'report.pdf'), pdf('report'), 'ISO-8859-1') }

// New (7.x)
${ pdf('report', 'report.pdf') }
```

**The wkhtmltopdf parameters are gone.** The second parameter is now the name of the generated file.
Passing an argument string raises an error rather than being ignored, because a silently dropped
`--header-html` produces a document that looks right and is missing its header.

**A detail object still comes from the path, but parameters are now an argument.** The old function
built a URL, so everything travelled in one string. The page path still carries the object the page
renders, which it reads as `current`, while request parameters are passed as an object instead of a
query string. A query string in the path is refused rather than ignored.

```
// Old (6.x)
${ pdf(concat('invoice/', order.id, '?lang=de')) }

// New (7.x)
${ pdf(concat('invoice/', order.id), 'invoice.pdf', { lang: 'de' }) }
```

The page reads those as `${request.lang}`, and it sees exactly the parameters passed and no others, so
the same call produces the same document from a page, a cron job or `doPrivileged`.

**Headers, footers and page numbers move into the print stylesheet.** They used to be separate Structr
pages fetched over HTTP. They are now page level CSS, and no second page is involved:

```css
@page {
    size: A4;
    margin: 25mm 18mm;
    @top-left     { content: element(docheader); }
    @bottom-right { content: "Page " counter(page) " of " counter(pages); }
}
#docheader { position: running(docheader); }
```

The renderer implements CSS 2.1 plus paged media. Flexbox, grid, custom properties and JavaScript have
no effect on paper, so a page whose screen layout relies on them needs a print stylesheet. Declarations
the renderer cannot use are written to the server log rather than dropped in silence, so the log tells
you what a document lost.

Images, stylesheets and fonts are read from the Structr filesystem by path, under the permissions of the
user the page is rendered as. External URLs are not fetched unless `pdf.resources.external.allowed` is
enabled.

## Migrating to Structr 6.x

Version 6 introduces several breaking changes that require manual migration from 5.x.

### Global Schema Methods

Global schema methods have been simplified. The `globalSchemaMethods` namespace no longer exists – functions can now be called directly from the root context.

**StructrScript / JavaScript:**

```javascript
// Old (5.x)
$.globalSchemaMethods.foo()

// New (6.x)
$.foo()
```

**REST API:**

```
# Old (5.x)
/structr/rest/maintenance/globalSchemaMethods/foo

# New (6.x)
/structr/rest/foo
```

> **Action required:** Search your codebase for `/maintenance/globalSchemaMethods` and `$.globalSchemaMethods` and update all occurrences.

### REST API Query Parameter Change

The `_loose` parameter has been renamed to `_inexact`.

```
# Old (5.x)
/structr/rest/foo?_loose=1

# New (6.x)
/structr/rest/foo?_inexact=1
```

### REST API Response Structure

The response body from `$.GET` and `$.POST` requests is now accessible via the `body` property.

```javascript
// Old (5.x)
JSON.parse($.GET(url))

// New (6.x)
JSON.parse($.GET(url).body)
```

### Schema Inheritance

The `extendsClass` property on schema nodes has been replaced with `inheritedTraits`.

```javascript
// Old (5.x)
eq('Location', get(first(find('SchemaNode', 'name', request.type)), 'extendsClass').name)

// New (6.x)
contains(first(find('SchemaNode', 'name', request.type)).inheritedTraits, 'Location')
```

### JavaScript Function Return Behavior

JavaScript functions now return their result directly by default.

**Option 1:** Restore old behavior globally:
```
application.scripting.js.wrapinmainfunction = true
```

**Option 2:** Remove unnecessary `return` statements from functions.

### JavaScript Strict Mode

Identifiers must be declared before use. Assigning to undeclared variables throws a ReferenceError.

```javascript
// ❌ Not allowed
foo = 1;
for (foo of array) {}

// ✅ Correct
let foo = 1;
for (let foo of array) {}
```

### Custom Indices

Custom indices are dropped during the upgrade to 6.0.

> **Action required:** Recreate all custom indices manually after upgrading.

### Upload Servlet Changes

| Aspect | 5.x Behavior | 6.x Behavior |
|--------|--------------|--------------|
| Default upload folder | Root or configurable | `/._structr_uploads` |
| Empty folder setting | Allowed | Enforced non-empty |
| `uploadFolderPath` | Unrestricted | Authenticated users only |

### Repeaters: No REST Queries

REST queries are no longer allowed for repeaters. Migrate them to function queries or flows.

### Migration Checklist for 6.x

- [ ] Replace `$.globalSchemaMethods.xyz()` with `$.xyz()`
- [ ] Update REST URLs: remove `/maintenance/globalSchemaMethods/`
- [ ] Replace `_loose` with `_inexact`
- [ ] Update `$.GET`/`$.POST` calls to use `.body`
- [ ] Replace `extendsClass` with `inheritedTraits`
- [ ] Review JavaScript functions for return statement compatibility
- [ ] Declare all JavaScript variables properly
- [ ] Recreate custom indices after upgrade
- [ ] Review upload handling code
- [ ] Migrate repeater REST queries to function queries

---

## Migrating to Structr 4.x

All versions starting with the 4.0 release include breaking changes which require migration of applications built with Structr versions prior to 4.0 (1.x, 2.x and 3.x).

### GraalVM Migration

With version 4.0, the required Java Runtime changed from standard JVMs (OpenJDK, Oracle JDK) to [GraalVM](https://graalvm.org). GraalVM brings full ECMAScript support, better performance, and polyglot scripting capabilities.

#### Installing GraalVM

Each Structr version supports the stable GraalVM version current at the time of release. The following example shows installation on Linux:

```bash
wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
tar xvzf graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
sudo mv graalvm-ce-java11-22.1.0 /usr/lib/jvm
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/graalvm-ce-java11-22.1.0/bin/java 2210
sudo update-alternatives --auto java
```

### Migration of Script Expressions

#### Predicates in find() and search()

All predicates in `find()` and `search()` expressions need the `$.predicate` prefix. The easiest way to migrate is to export the application using deployment export and search all files for these predicates:

```
$.and
$.or
$.not
$.equals
$.contains
$.empty
$.range
$.within_distance
$.sort
$.page
```

**Examples:**

```javascript
// Old (3.x)
$.find('File', 'size', $.range(null, 100), $.page(1, 10));

// New (4.x+)
$.find('File', 'size', $.predicate.range(null, 100), $.predicate.page(1, 10));
```

```javascript
// Old (3.x)
$.find('User', $.sort('createdDate'));

// New (4.x+)
$.find('User', $.predicate.sort('createdDate'));
```

Some predicates also exist as regular functions (`$.sort()`, `$.empty()`) or keywords (`$.page`). When used outside of `find()`, they don't need changes:

```javascript
// No change needed - sort() used outside find()
$.sort($.find('User'), 'createdDate');
```

### Resource Access Permissions

Resource Permissions (formerly "Resource Access Grants") have been made more flexible. Rights management now also applies to permission nodes themselves, requiring users to have read access to the permission object to use it.

#### Manual Migration

1. Log in as admin
2. Navigate to Security → Resource Permissions
3. Enable "Show only used grants"
4. Migrate permissions:
   - If the permission has active flags for "Public Users": set `visibleToPublicUsers = true`
   - If the permission has active flags for "Authenticated Users": set `visibleToAuthenticatedUsers = true`
   - If both flags apply: split into two permissions with identical signatures

For many permissions, enable "Show visibility flags in Resource Permissions table" in Dashboard → UI Settings.

#### Semi-automatic Migration via Deployment

When importing a deployment export from a pre-4.0 version into 4.x+, Structr runs automatic migration using this heuristic:

- Public Users flags → `visibleToPublicUsers = true`
- Authenticated Users flags → `visibleToAuthenticatedUsers = true`
- If both flags are set, a warning is issued to split the grant (since `visibleToPublicUsers = true` also makes the object visible to authenticated users)

### Scripting Considerations

#### Date Comparisons

Use the `getTime()` function when comparing dates to avoid issues with GraalVM ProxyDate entities:

```javascript
{
    return $.me.createdDate.getTime() <= $.now.getTime();
}
```

#### Conditional Chaining Limitation

Conditional chaining on ProxyObjects with function members can cause errors:

```javascript
{
    const obj = {
        method1: () => "works"
    };

    // Works
    obj.method1?.();

    // Works, call doesn't get executed
    obj.method2?.();

    const proxyObject = $.retrieve('passedObject');

    // Does NOT work - throws unsupported message exception
    proxyObject.myMethod?.();
}
```

### REST Request Parameters

Starting with 4.0, REST request parameters must be prefixed with underscore to prevent name collisions with property names:

```
# Old
/structr/rest/Project?page=1&pageSize=10&sort=name

# New
/structr/rest/Project?_page=1&_pageSize=10&_sort=name
```

**Full list of affected parameters:**

| Parameter | Parameter | Parameter |
|-----------|-----------|-----------|
| `page` | `pageSize` | `sort` |
| `order` | `loose` | `locale` |
| `latlon` | `location` | `state` |
| `house` | `country` | `postalCode` |
| `city` | `street` | `distance` |
| `outputNestingDepth` | `debugLoggingEnabled` | `forceResultCount` |
| `disableSoftLimit` | `parallelizeJsonOutput` | `batchSize` |

Legacy mode can be enabled with `application.legacy.requestparameters.enabled = true` but is discouraged for new projects.

### Neo4j Upgrade

Neo4j 4.x is recommended for Structr 4.x, though Neo4j 3.5 is still supported. If upgrading Neo4j, consult the [Neo4j changelog](https://neo4j.com/docs/cypher-manual/current/deprecations-additions-removals-compatibility/#cypher-compatibility).

#### Cypher Parameter Syntax

The old parameter syntax `{param}` was deprecated in Neo4j 3.0 and removed in Neo4j 4.0. Use `$param` instead. For compatibility, you can prefix queries with `CYPHER 3.5`.

#### Database Name Configuration

If migrating from Neo4j versions prior to 4, the default database may be named `graph.db` instead of `neo4j`. Configure the database name in structr.conf:

```
YOUR_DB_NAME.database.connection.url = bolt://localhost:7687
YOUR_DB_NAME.database.connection.name = YOUR_DB_NAME
YOUR_DB_NAME.database.connection.password = your_neo4j_password
YOUR_DB_NAME.database.connection.databasename = graph.db
YOUR_DB_NAME.database.driver = org.structr.bolt.BoltDatabaseService
```

### Migration Checklist for 4.x

- [ ] Install GraalVM as Java runtime
- [ ] Add `$.predicate` prefix to all find/search predicates
- [ ] Update Resource Permissions with visibility flags
- [ ] Split Resource Permissions that have both public and authenticated flags
- [ ] Prefix REST parameters with underscore
- [ ] Update date comparisons to use `getTime()`
- [ ] Review code for conditional chaining on ProxyObjects
- [ ] Update Neo4j configuration if upgrading database
- [ ] Update Cypher parameter syntax if using Neo4j 4.x
