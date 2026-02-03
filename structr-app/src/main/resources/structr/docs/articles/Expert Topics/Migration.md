# Migration Guide

This chapter covers breaking changes and migration steps when upgrading between major Structr versions.

> **Important:** Always create a full backup before upgrading Structr.

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
