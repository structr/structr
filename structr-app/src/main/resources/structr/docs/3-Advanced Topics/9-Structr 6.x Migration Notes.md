# Structr 6.x Migration Notes

Version 6 has some breaking changes which require manual migration of applications from older versions.

> **Note:** It is always advised to have a full backup before upgrading Structr.

# Structr Migration Guide: 5.x to 6.x

This guide covers the breaking changes and required modifications when upgrading from Structr 5.x to 6.x.

## Global Schema Methods

Global schema methods have been renamed to _user-defined functions_. The `globalSchemaMethods` namespace has been deprecated and should no longer be used - functions can now be called directly from the root context.

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

## REST API Query Parameter Change

The `_loose` parameter has been renamed to `_inexact`.

```
# Old (5.x)
/structr/rest/foo?_loose=1

# New (6.x)
/structr/rest/foo?_inexact=1
```

## REST API Response Structure

The response body from `$.GET` and `$.POST` requests is now accessible via the `body` property.

```javascript
// Old (5.x)
JSON.parse($.GET(url))

// New (6.x)
JSON.parse($.GET(url).body)
```

## Schema Inheritance

The `extendsClass` property on schema nodes has been replaced with `inheritedTraits`.

```javascript
// Old (5.x)
eq('Location', get(first(find('SchemaNode', 'name', request.type)), 'extendsClass').name)

// New (6.x)
contains(first(find('SchemaNode', 'name', request.type)).inheritedTraits, 'Location')
```

To get the first inherited trait:
```javascript
// Old: schemaNode.extendsClass
// New: $.first(schemaNode.inheritedTraits)
```

## JavaScript Function Return Behavior

JavaScript functions now return their result directly by default. The previous behavior required an explicit `return` statement.

**Option 1:** Restore the old behavior globally by adding this to `structr.conf`:
```
application.scripting.js.wrapinmainfunction = true
```

**Option 2:** Remove `return` statements from functions that now produce errors due to this change.

## Custom Indices

Custom indices are dropped during the upgrade to 6.0.

> **Action required:** After upgrading, recreate all custom indices manually.

## Upload Servlet Changes

The upload servlet has several new security and behavioral changes:

| Aspect | 5.x Behavior | 6.x Behavior |
|--------|--------------|--------------|
| Default upload folder | Root or configurable | `/._structr_uploads` |
| Empty folder setting | Allowed | Enforced non-empty (prevents root uploads) |
| `parent`/`parentId` usage | No restriction | Requires write permission on target folder |
| `uploadFolderPath` attribute | Unrestricted | Authenticated users only; must be under default upload folder |
| Upload to home folder | Required explicit path | Omit `uploadFolderPath` |

> **Note:** Even administrators cannot use `uploadFolderPath` to target folders outside the default upload folder hierarchy.

## Repeaters: No More REST Queries

REST queries are no longer allowed for repeaters. You must migrate them to function queries or flows.

**How to identify affected repeaters:** Check your server logs for messages about REST queries being used in repeaters.

**Migration options:**
1. Convert REST queries to function queries
2. Convert REST queries to flows

## Migration Checklist

- [ ] Replace all `$.globalSchemaMethods.xyz()` calls with `$.xyz()`
- [ ] Update REST URLs: remove `/maintenance/globalSchemaMethods/`
- [ ] Replace `_loose` with `_inexact` in REST query parameters
- [ ] Update `$.GET`/`$.POST` calls to use `.body` for response content
- [ ] Replace `extendsClass` with `inheritedTraits` in schema queries
- [ ] Review JavaScript functions for return statement compatibility
- [ ] Recreate custom indices after upgrade
- [ ] Review and update upload handling code for new security requirements
- [ ] Migrate repeater REST queries to function queries or flows
