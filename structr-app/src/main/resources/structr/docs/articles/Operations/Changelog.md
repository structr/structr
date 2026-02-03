# Changelog

Structr can automatically track all changes to your data, recording who changed what and when. This changelog provides a complete audit trail for compliance requirements, debugging, or building features like activity feeds and undo functionality.

## Overview

The changelog system records five types of events:

| Verb | Description |
|------|-------------|
| `create` | A node was created |
| `delete` | A node was deleted |
| `link` | A relationship was created |
| `unlink` | A relationship was removed |
| `change` | A property value was modified |

Structr provides two perspectives on the changelog data:

- **Entity Changelog** – What happened to a specific object? Use `changelog()` to retrieve all changes made to an entity.
- **User Changelog** – What did a specific user do? Use `userChangelog()` to retrieve all changes made by a user.

Both track the same events, just organized differently.

## Enabling the Changelog

The changelog is disabled by default. Enable it in `structr.conf` or through the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `application.changelog.enabled` | false | Enable entity-centric changelog |
| `application.changelog.user_centric.enabled` | false | Enable user-centric changelog |
| `changelog.path` | changelog/ | Directory where changelog files are stored |

You can enable one or both depending on your needs. Note that enabling the changelog adds overhead to every write operation, as each change must be recorded.

## Storage

Changelog data is stored in files on the filesystem, not in the database. This keeps the database lean and allows the changelog to grow independently. The files are stored in the directory specified by `changelog.path`.

## Querying the Entity Changelog

Use the `changelog()` function to retrieve the history of a specific entity.

### Basic Usage

**JavaScript:**
```javascript
let history = $.changelog(node);
```

**StructrScript:**
```
${changelog(current)}
```

You can also pass a UUID string instead of an entity:

```javascript
let history = $.changelog('abc123-def456-...');
```

### Resolving Related Entities

The second parameter controls whether related entities are resolved:

```javascript
// Without resolving - target contains only the UUID
let history = $.changelog(node, false);

// With resolving - targetObj contains the actual entity (if it still exists)
let history = $.changelog(node, true);
```

### Changelog Entry Structure

Each entry in the returned list contains different fields depending on the verb:

| Field | create | delete | link | unlink | change | Description |
|-------|--------|--------|------|--------|--------|-------------|
| `verb` | ✓ | ✓ | ✓ | ✓ | ✓ | The type of change |
| `time` | ✓ | ✓ | ✓ | ✓ | ✓ | Timestamp (milliseconds since epoch) |
| `userId` | ✓ | ✓ | ✓ | ✓ | ✓ | UUID of the user who made the change |
| `userName` | ✓ | ✓ | ✓ | ✓ | ✓ | Name of the user |
| `target` | ✓ | ✓ | ✓ | ✓ | | UUID of the affected entity |
| `type` | ✓ | ✓ | | | | Type of the created/deleted entity |
| `rel` | | | ✓ | ✓ | | Relationship type |
| `relId` | | | ✓ | ✓ | | Relationship UUID |
| `relDir` | | | ✓ | ✓ | | Direction ("in" or "out") |
| `key` | | | | | ✓ | Property name that was changed |
| `prev` | | | | | ✓ | Previous value (JSON) |
| `val` | | | | | ✓ | New value (JSON) |
| `targetObj` | ✓ | ✓ | ✓ | ✓ | | Resolved entity (if resolve=true) |

## Querying the User Changelog

Use the `userChangelog()` function to retrieve all changes made by a specific user.

**JavaScript:**
```javascript
let userHistory = $.userChangelog(user);
let myHistory = $.userChangelog($.me);
```

**StructrScript:**
```
${userChangelog(me)}
```

The user changelog returns the same entry structure, but without `userId` and `userName` fields (since the user is already known). For `change` entries, the `target` and `targetObj` fields are included to indicate which entity was modified.

## Filtering Results

Both functions support filtering to narrow down the results. Filters are combined with AND logic, except for filters that can have multiple values, which use OR logic within that filter.

### Filter Parameters

| Filter | Applicable Verbs | Description |
|--------|------------------|-------------|
| `timeFrom` | all | Only entries at or after this time |
| `timeTo` | all | Only entries at or before this time |
| `verb` | all | Only entries with matching verb(s) |
| `userId` | all | Only entries by matching user ID(s) |
| `userName` | all | Only entries by matching user name(s) |
| `relType` | link, unlink | Only entries with matching relationship type(s) |
| `relDir` | link, unlink | Only entries with matching direction |
| `target` | create, delete, link, unlink | Only entries involving matching target(s) |
| `key` | change | Only entries changing matching property name(s) |

### Time Filters

Time values can be specified as:

- Milliseconds since epoch (number)
- JavaScript Date object
- ISO format string: `yyyy-MM-dd'T'HH:mm:ssZ`

### JavaScript Filter Syntax

In JavaScript, pass filters as an object. Use arrays for multiple values:

```javascript
// Single filter
let changes = $.changelog(node, false, {verb: 'change'});

// Multiple verbs (OR logic)
let linkEvents = $.changelog(node, false, {verb: ['link', 'unlink']});

// Combined filters (AND logic)
let recentLinks = $.changelog(node, false, {
    verb: ['link', 'unlink'],
    relType: 'OWNS',
    timeFrom: Date.now() - 86400000  // Last 24 hours
});

// Filter by specific property changes
let nameChanges = $.changelog(node, false, {
    verb: 'change',
    key: 'name'
});
```

### StructrScript Filter Syntax

In StructrScript, pass filters as key-value pairs:

```
${changelog(current, false, 'verb', 'change')}
${changelog(current, false, 'verb', 'link', 'verb', 'unlink')}
${changelog(current, false, 'verb', 'change', 'key', 'name', 'timeFrom', now)}
```

## Use Cases

### Activity Feed

Show recent changes to an entity:

```javascript
let recentActivity = $.changelog(document, true, {
    timeTo: Date.now(),
    timeFrom: Date.now() - 7 * 86400000  // Last 7 days
});

for (let entry of recentActivity) {
    $.log`${entry.userName} ${entry.verb}d at ${new Date(entry.time)}`;
}
```

### Audit Trail

Track all modifications by a specific user:

```javascript
let audit = $.userChangelog(suspiciousUser, true, {
    timeFrom: investigationStart,
    timeTo: investigationEnd
});
```

### Property History

Show the history of a specific property:

```javascript
let priceHistory = $.changelog(product, false, {
    verb: 'change',
    key: 'price'
});

for (let entry of priceHistory) {
    $.log`Price changed from ${entry.prev} to ${entry.val}`;
}
```

### Relationship Tracking

Find when relationships were created or removed:

```javascript
let membershipChanges = $.changelog(group, true, {
    verb: ['link', 'unlink'],
    relType: 'HAS_MEMBER'
});
```

## Performance Considerations

- The changelog adds write overhead to every database modification
- Changelog files grow over time and are not automatically pruned
- Consider enabling only the perspective you need (entity or user)
- For high-volume applications, implement a retention policy to archive or delete old changelog files
- Queries with `resolve=true` perform additional database lookups

## Related Topics

- Built-in Analytics – Custom event tracking for application-level analytics
- Logging & Debugging – Server logging and debugging tools
- Security – Access control and permissions
