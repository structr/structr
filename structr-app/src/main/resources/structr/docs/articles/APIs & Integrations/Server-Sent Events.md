
Server-sent events (SSE) allow Structr to push messages to connected browsers in real time. Unlike traditional request-response patterns where the client polls for updates, SSE maintains an open connection that the server can use to send data whenever something relevant happens.

Common use cases include live notifications, real-time dashboards, progress updates for long-running operations, and collaborative features where multiple users need to see changes immediately.

## How It Works

The browser opens a persistent connection to Structr's EventSource endpoint. Structr keeps track of all connected clients. When your server-side code calls `broadcastEvent()`, Structr sends the message to all connected clients (or a filtered subset based on authentication status). The browser receives the message through its EventSource API and can update the UI accordingly.

This is a one-way channel: server to client. For bidirectional communication, consider WebSockets instead.

> **Important:** When not used over HTTP/2, SSE is limited to a maximum of 6 open connections per browser. This limit applies across all tabs, so opening multiple tabs to the same application can exhaust available connections. Use HTTP/2 in production to avoid this limitation. See the [MDN EventSource documentation](https://developer.mozilla.org/en-US/docs/Web/API/EventSource) for details.

## Configuration

### Enabling the EventSource Servlet

The EventSource servlet is not enabled by default. To activate it:

1. Open the Configuration Interface
2. Navigate to Servlet Settings
3. Add `EventSourceServlet` to the list of enabled servlets
4. Save the configuration
5. Restart the HTTP service

> **Note:** Do not enable this servlet by editing `structr.conf` directly. The setting `http-service.servlets` contains a list of all active servlets. If you add only `EventSourceServlet` to `structr.conf`, all other servlets will be disabled because `structr.conf` overrides defaults rather than extending them. Always use the Configuration Interface for this setting.

### Resource Access

To allow users to connect to the EventSource endpoint, create a Resource Access Permission:

| Setting | Value |
|---------|-------|
| Signature | `_eventSource` |
| Flags | GET for the appropriate user types |

For authenticated users only, grant GET to authenticated users. To allow anonymous connections, grant GET to public users as well.

## Client Setup

In your frontend JavaScript, create an EventSource connection:

```javascript
const source = new EventSource('/structr/EventSource', { 
    withCredentials: true 
});

source.onmessage = function(event) {
    console.log('Received:', event.data);
};

source.onerror = function(event) {
    console.error('EventSource error:', event);
};
```

The `withCredentials: true` option ensures that session cookies are sent with the connection request, allowing Structr to identify authenticated users.

### Handling Different Event Types

The `onmessage` handler only receives events with the type `message`. For custom event types, use `addEventListener()`:

```javascript
const source = new EventSource('/structr/EventSource', { 
    withCredentials: true 
});

// Generic message handler
source.onmessage = function(event) {
    console.log('Message:', event.data);
};

// Custom event type handlers
source.addEventListener('notification', function(event) {
    showNotification(JSON.parse(event.data));
});

source.addEventListener('data-update', function(event) {
    refreshData(JSON.parse(event.data));
});

source.addEventListener('maintenance', function(event) {
    showMaintenanceWarning(JSON.parse(event.data));
});
```

### Connection Management

Browsers automatically reconnect if the connection drops. You can track connection state:

```javascript
source.onopen = function(event) {
    console.log('Connected to EventSource');
};

source.onerror = function(event) {
    if (source.readyState === EventSource.CLOSED) {
        console.log('Connection closed');
    } else if (source.readyState === EventSource.CONNECTING) {
        console.log('Reconnecting...');
    }
};
```

To explicitly close the connection:

```javascript
source.close();
```

## Sending Events

Structr provides two functions for sending server-sent events:

- `broadcastEvent()` - Send to all connected clients (filtered by authentication status)
- `sendEvent()` - Send to specific users or groups

### Broadcasting to All Clients

Use `broadcastEvent()` to send messages to all connected clients.

**Function Signature:**

```
broadcastEvent(eventType, message [, authenticatedUsers [, anonymousUsers]])
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| eventType | String | required | The event type (use `message` for the generic `onmessage` handler) |
| message | String | required | The message content (typically JSON) |
| authenticatedUsers | Boolean | true | Send to authenticated users |
| anonymousUsers | Boolean | false | Send to anonymous users |

**StructrScript:**

```
${broadcastEvent('message', 'Hello world!')}
${broadcastEvent('message', 'For everyone', true, true)}
```

**JavaScript:**

```javascript
$.broadcastEvent('message', 'Hello world!');
$.broadcastEvent('message', 'For everyone', true, true);
```

### Sending to Specific Recipients

Use `sendEvent()` to send messages to specific users or groups. The message is only delivered if the recipient has an open EventSource connection.

**Function Signature:**

```
sendEvent(eventType, message, recipients)
```

| Parameter | Type | Description |
|-----------|------|-------------|
| eventType | String | The event type |
| message | String | The message content |
| recipients | User, Group, or List | A single user, a single group, or a list containing users and groups |

When you specify a group, all members of that group (including nested groups) receive the message.

**StructrScript:**

```
${sendEvent('message', 'Welcome!', find('User', 'name', 'Bob'))}
${sendEvent('notification', 'Team update', find('Group', 'name', 'Editors'))}
```

**JavaScript:**

```javascript
// Send to a specific user
let bob = $.first($.find('User', 'name', 'Bob'));
$.sendEvent('message', 'Welcome!', bob);

// Send to a group
let editors = $.first($.find('Group', 'name', 'Editors'));
$.sendEvent('notification', 'Team update', editors);

// Send to multiple recipients (all admin users)
let admins = $.find('User', { isAdmin: true });
$.sendEvent('announcement', 'Admin meeting in 10 minutes', admins);
```

The function returns `true` if at least one recipient had an open connection and received the message, `false` otherwise.

### Sending JSON Data

For structured data, serialize to JSON:

**JavaScript:**

```javascript
$.broadcastEvent('message', JSON.stringify({
    type: 'notification',
    title: 'New Comment',
    body: 'Someone commented on your post',
    timestamp: new Date().getTime()
}));
```

On the client:

```javascript
source.onmessage = function(event) {
    const data = JSON.parse(event.data);
    if (data.type === 'notification') {
        showNotification(data.title, data.body);
    }
};
```

### Custom Event Types

Use custom event types to separate different kinds of messages:

**JavaScript (server):**

```javascript
// Notification for the UI
$.broadcastEvent('notification', JSON.stringify({
    title: 'New Message',
    body: 'You have a new message from Admin'
}));

// Data update signal
$.broadcastEvent('data-update', JSON.stringify({
    entity: 'Project',
    id: project.id,
    action: 'modified'
}));

// System maintenance warning
$.broadcastEvent('maintenance', JSON.stringify({
    message: 'System maintenance in 10 minutes',
    shutdownTime: new Date().getTime() + 600000
}));
```

Remember: custom event types require `addEventListener()` on the client, not `onmessage`.

### Targeting by Authentication Status

Control who receives broadcast messages:

```javascript
// Only authenticated users (default)
$.broadcastEvent('message', 'For logged-in users only', true, false);

// Only anonymous users
$.broadcastEvent('message', 'For anonymous users only', false, true);

// Everyone
$.broadcastEvent('message', 'For everyone', true, true);
```

## Practical Examples

### Live Notifications

Trigger a notification when a new comment is created. In the `afterCreate` method of your Comment type:

```javascript
{
    let notification = JSON.stringify({
        type: 'new-comment',
        postId: $.this.post.id,
        authorName: $.this.author.name,
        preview: $.this.text.substring(0, 100)
    });
    
    // Notify the post author specifically
    $.sendEvent('notification', notification, $.this.post.author);
}
```

Or broadcast to all authenticated users:

```javascript
{
    let notification = JSON.stringify({
        type: 'new-comment',
        postId: $.this.post.id,
        authorName: $.this.author.name,
        preview: $.this.text.substring(0, 100)
    });
    
    $.broadcastEvent('notification', notification);
}
```

### Progress Updates

For long-running operations, send progress updates:

```javascript
{
    let items = $.find('DataItem', { needsProcessing: true });
    let total = $.size(items);
    let processed = 0;
    
    for (let item of items) {
        // Your processing logic here
        item.needsProcessing = false;
        item.processedDate = $.now;
        
        processed++;
        
        // Send progress update every 10 items
        if (processed % 10 === 0) {
            $.broadcastEvent('progress', JSON.stringify({
                taskId: 'data-processing',
                processed: processed,
                total: total,
                percent: Math.round((processed / total) * 100)
            }));
        }
    }
    
    // Send completion message
    $.broadcastEvent('progress', JSON.stringify({
        taskId: 'data-processing',
        processed: total,
        total: total,
        percent: 100,
        complete: true
    }));
}
```

### Collaborative Editing

Notify other users when someone is editing a document:

```javascript
{
    // Notify all members of the document's team
    $.sendEvent('editing', JSON.stringify({
        documentId: $.this.id,
        documentName: $.this.name,
        userId: $.me.id,
        userName: $.me.name,
        action: 'started'
    }), $.this.team);
}
```

### Team Announcements

Send announcements to specific groups:

```javascript
{
    let engineeringTeam = $.first($.find('Group', 'name', 'Engineering'));
    
    $.sendEvent('announcement', JSON.stringify({
        title: 'Sprint Planning',
        message: 'Sprint planning meeting starts in 15 minutes',
        room: 'Conference Room A'
    }), engineeringTeam);
}
```

## Best Practices

### Use JSON for Message Data

Always serialize structured data as JSON. This makes parsing reliable and allows you to include multiple fields:

```javascript
// Good
$.broadcastEvent('message', JSON.stringify({ action: 'refresh', target: 'projects' }));

// Avoid
$.broadcastEvent('message', 'refresh:projects');
```

### Choose Meaningful Event Types

Use descriptive event types to organize your messages:

- `notification` - User-facing alerts
- `data-update` - Signals that data has changed
- `progress` - Long-running operation updates
- `system` - System-level messages (maintenance, etc.)

### Handle Reconnection Gracefully

Clients may miss messages during reconnection. Design your application to handle this:

- Include timestamps in messages so clients can detect gaps
- Provide a way to fetch missed updates via REST API
- Consider sending a "sync" message when clients reconnect

### Use Targeted Messages for Sensitive Data

`broadcastEvent()` sends to all connected clients matching the authentication filter. For user-specific or sensitive data, use `sendEvent()` with specific recipients instead:

```javascript
// Bad: broadcasts salary info to everyone
$.broadcastEvent('notification', JSON.stringify({ 
    message: 'Your salary has been updated to $75,000' 
}));

// Good: sends only to the specific user
$.sendEvent('notification', JSON.stringify({ 
    message: 'Your salary has been updated to $75,000' 
}), employee);
```

### Consider Message Volume

Broadcasting too frequently can overwhelm clients and waste bandwidth. For high-frequency updates:

- Batch multiple changes into single messages
- Throttle updates (e.g., maximum one update per second)
- Send minimal data and let clients fetch details via REST

## Troubleshooting

### Events Not Received

If clients are not receiving events:

1. Verify the EventSource servlet is enabled in the Configuration Interface under Servlet Settings
2. Check that the Resource Access Permission for `_eventSource` exists and grants GET
3. Confirm the client is using `withCredentials: true`
4. Check the browser's Network tab for the EventSource connection status

### Connection Drops Frequently

EventSource connections can be closed by proxies or load balancers with short timeouts. Configure your infrastructure to allow long-lived connections, or implement reconnection logic on the client.

### Wrong Event Type

If `onmessage` is not firing, verify you are using `message` as the event type. For any other event type, you must use `addEventListener()`.

## Related Topics

- Business Logic - Triggering events from lifecycle methods
- Scheduled Tasks - Sending periodic updates via SSE
- REST Interface - Complementary request-response API
