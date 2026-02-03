# Built-in Analytics

Structr includes a built-in analytics system that allows you to build custom analytics and audit functionality into your application. You can record events like page views, user actions, or business transactions, and later query them with filtering, aggregation, and time-based analysis.

This feature is similar to tools like Google Analytics, but runs entirely within your Structr instance. All data stays on your server, giving you full control over what you track and how you analyze it.

## Overview

Event tracking consists of two parts:

- The `logEvent()` function records events from your application code
- The `/structr/rest/log` endpoint queries and analyzes recorded events

Events are stored as LogEvent entities in the database with the following properties:

| Property | Description |
|----------|-------------|
| action | The type of event (e.g., "VIEW", "CLICK", "PURCHASE") |
| message | Additional details about the event |
| subject | Who triggered the event (typically a user ID) |
| object | What the event relates to (typically a content ID) |
| timestamp | When the event occurred (set automatically) |

## Recording Events

You can record events in two ways: using the `logEvent()` function from your application code, or by posting directly to the REST endpoint.

### Using logEvent()

**StructrScript:**
```
${logEvent('VIEW', 'User viewed article', me.id, article.id)}
```

**JavaScript:**
```javascript
$.logEvent('VIEW', 'User viewed article', $.me.id, article.id);
```

The parameters are:

1. action (required) – The event type
2. message (required) – A description or additional data
3. subject (optional) – Who triggered the event
4. object (optional) – What the event relates to

### JavaScript Object Syntax

In JavaScript, you can also pass a single object:

```javascript
$.logEvent({
    action: 'PURCHASE',
    message: 'Order completed',
    subject: $.me.id,
    object: order.id
});
```

### Using the REST API

You can also create events via POST request, which is useful for external systems or JavaScript frontends:

```
POST /structr/rest/log
Content-Type: application/json

{
    "action": "VIEW",
    "message": "User viewed article",
    "subject": "user-uuid-here",
    "object": "article-uuid-here"
}
```

When using the REST API, `subject`, `object`, and `action` are required. The timestamp is set automatically.

### Common Patterns

Track page views in a page's `onRender` method:

```javascript
$.logEvent('VIEW', request.path, $.me?.id, thisPage.id);
```

Track user actions in event handlers:

```javascript
$.logEvent('DOWNLOAD', file.name, $.me.id, file.id);
```

Track business events in lifecycle methods:

```javascript
// In Order.afterCreate
$.logEvent('ORDER_CREATED', 'New order: ' + this.total, this.customer.id, this.id);
```

## Querying Events

The `/structr/rest/log` endpoint provides flexible querying capabilities.

### Query Parameters

| Parameter | Description |
|-----------|-------------|
| `subject` | Filter by subject ID |
| `object` | Filter by object ID |
| `action` | Filter by action type |
| `timestamp` | Filter by time range using `[start TO end]` syntax |
| `aggregate` | Group by time using SimpleDateFormat pattern |
| `histogram` | Extract and count values from messages using regex |
| `filters` | Filter messages by regex patterns (separated by `::`) |
| `multiplier` | Extract numeric multiplier from message using regex |
| `correlate` | Filter based on related events (see Correlation section) |

### Overview Query

Without parameters, the endpoint returns a summary of all recorded events:

```
GET /structr/rest/log
```

Response:
```json
{
    "result": [{
        "actions": "VIEW, CLICK, PURCHASE",
        "entryCount": 15423,
        "firstEntry": "2026-01-01T00:00:00+0000",
        "lastEntry": "2026-02-03T14:30:00+0000"
    }]
}
```

### Filtering Events

Filter by subject, object, action, or time range:

```
GET /structr/rest/log?subject=<userId>
GET /structr/rest/log?object=<articleId>
GET /structr/rest/log?action=VIEW
GET /structr/rest/log?subject=<userId>&action=PURCHASE
```

### Time Range Queries

Filter by timestamp using range syntax:

```
GET /structr/rest/log?timestamp=[2026-01-01T00:00:00+0000 TO 2026-01-31T23:59:59+0000]
```

### Aggregation

The `aggregate` parameter groups events by time intervals. It accepts a Java SimpleDateFormat pattern that defines the grouping granularity:

| Pattern | Groups by |
|---------|-----------|
| `yyyy` | Year |
| `yyyy-MM` | Month |
| `yyyy-MM-dd` | Day |
| `yyyy-MM-dd HH` | Hour |
| `yyyy-MM-dd HH:mm` | Minute |

Example – count events per day:

```
GET /structr/rest/log?action=VIEW&aggregate=yyyy-MM-dd
```

You can add custom aggregation patterns as additional query parameters. Each pattern is a regex that matches against the message field:

```
GET /structr/rest/log?action=VIEW&aggregate=yyyy-MM-dd&category=category:(.*)&premium=premium:true
```

This groups by day and counts how many messages match each pattern. The response includes a `total` count plus counts for each named pattern.

### Multiplier

When aggregating, you can extract a numeric value from the message to use as a multiplier instead of counting each event as 1:

```
GET /structr/rest/log?action=PURCHASE&aggregate=yyyy-MM-dd&multiplier=amount:(\d+)
```

If an event message contains `amount:150`, it contributes 150 to the total instead of 1. This is useful for summing values like order amounts or quantities.

### Histograms

The `histogram` parameter extracts values from messages using a regex pattern with a capture group, creating a breakdown by those values:

```
GET /structr/rest/log?action=VIEW&aggregate=yyyy-MM-dd&histogram=category:(.*)
```

This returns counts grouped by both time (from `aggregate`) and by the captured category value. The response shows how many events occurred for each category in each time period.

### Filters

The `filters` parameter applies regex patterns to the message field. Only events where all patterns match are included. Separate multiple patterns with `::`:

```
GET /structr/rest/log?action=VIEW&filters=premium:true::region:EU
```

This returns only VIEW events where the message contains both `premium:true` and `region:EU`.

### Correlation

Correlation allows you to filter events based on the existence of related events. This is useful for questions like "show me all views of articles that were later purchased" or "find users who viewed but did not buy".

The correlation parameter has the format:

```
correlate=ACTION::OPERATOR::PATTERN
```

The components are:

- ACTION – The action type to correlate with (e.g., "PURCHASE")
- OPERATOR – How to match: `and`, `andSubject`, `andObject`, or `not`
- PATTERN – A regex pattern to extract a correlation key from the message

**Example: Find views that led to purchases**

```
GET /structr/rest/log?action=VIEW&correlate=PURCHASE::and::article-(.*)
```

This returns VIEW events only if there is also a PURCHASE event where the pattern `article-(.*)` extracts the same value from the message.

**Operators:**

| Operator | Description |
|----------|-------------|
| `and` | Include event if a correlating event exists |
| `andSubject` | Include event if a correlating event exists with the same subject |
| `andObject` | Include event if a correlating event exists with the same object |
| `not` | Include event only if NO correlating event exists |

**Example: Find users who viewed but did not purchase**

```
GET /structr/rest/log?action=VIEW&correlate=PURCHASE::not::article-(.*)
```

This is an advanced feature that requires careful design of your event messages to include matchable patterns.

## Designing Event Messages

The power of the query features depends on how you structure your event messages. A well-designed message format makes filtering, aggregation, and correlation much easier.

### Key-Value Format

A recommended pattern is to use key-value pairs in your messages:

```javascript
$.logEvent('PURCHASE', 'category:electronics amount:299 region:EU premium:true', $.me.id, order.id);
```

This format allows you to:

- Filter by any attribute: `filters=premium:true`
- Extract values for histograms: `histogram=category:(.*?) `
- Sum amounts: `multiplier=amount:(\d+)`
- Correlate by category: `correlate=VIEW::and::category:(.*?) `

### JSON Format

For complex data, you can store JSON in the message:

```javascript
$.logEvent('PURCHASE', JSON.stringify({
    category: 'electronics',
    amount: 299,
    region: 'EU'
}), $.me.id, order.id);
```

Note that JSON is harder to query with regex patterns, but useful when you need to retrieve and parse the full event data later.

### Consistent Naming

Use consistent action names across your application:

- Use uppercase for action types: `VIEW`, `CLICK`, `PURCHASE`
- Use a prefix for related actions: `FUNNEL_START`, `FUNNEL_STEP`, `FUNNEL_COMPLETE`
- Document your event schema so team members use the same format

## Use Cases

### Page View Analytics

Track which pages are most popular:

```javascript
// In page onRender
$.logEvent('VIEW', thisPage.name, $.me?.id, thisPage.id);
```

Query most viewed pages:
```
GET /structr/rest/log?action=VIEW&aggregate=object
```

### User Activity Tracking

Track what a specific user does:

```
GET /structr/rest/log?subject=<userId>
```

### Conversion Funnels

Track steps in a process:

```javascript
$.logEvent('FUNNEL_STEP', 'cart', $.me.id, session.id);
$.logEvent('FUNNEL_STEP', 'checkout', $.me.id, session.id);
$.logEvent('FUNNEL_STEP', 'payment', $.me.id, session.id);
$.logEvent('FUNNEL_STEP', 'complete', $.me.id, session.id);
```

### Audit Trails

Track who changed what:

```javascript
// In onSave lifecycle method
let mods = $.retrieve('modifications');
$.logEvent('MODIFIED', JSON.stringify(mods.after), $.me.id, this.id);
```

## Performance Considerations

- LogEvent entities are regular database nodes and count toward your database size
- Consider implementing a retention policy to delete old events
- For high-traffic applications, consider batching events or using sampling
- Index the properties you filter on most frequently

## Related Topics

- Monitoring – System-level monitoring and health checks
- Lifecycle Methods – Recording events in onCreate, onSave, onDelete
- Scheduled Tasks – Implementing retention policies or generating reports
