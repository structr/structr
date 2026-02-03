
Structr can connect to message brokers to send and receive messages asynchronously. This enables event-driven architectures, real-time data pipelines, and integration with external systems through industry-standard messaging protocols.

## When to Use Message Brokers

Message brokers are useful when you need to:

- **Decouple systems** - Send data to other services without waiting for a response
- **Process events asynchronously** - Handle incoming events in the background
- **Integrate with IoT devices** - Receive sensor data or send commands via MQTT
- **Build data pipelines** - Stream data to analytics systems via Kafka or Pulsar
- **Enable real-time communication** - React to events from external systems immediately

If you only need to push updates to browsers, Server-Sent Events may be simpler. Message brokers are for system-to-system communication.

## Supported Brokers

Structr supports three message broker protocols:

| Broker | Protocol | Typical Use Case |
|--------|----------|------------------|
| MQTT | Lightweight publish/subscribe | IoT, sensors, mobile apps |
| Kafka | Distributed streaming | High-throughput data pipelines, event sourcing |
| Pulsar | Cloud-native messaging | Multi-tenant messaging, geo-replication |

All three use the same programming model in Structr: create a client, configure subscribers, and process incoming messages with callbacks.

## Core Concepts

### Message Clients

A message client represents a connection to a broker. In Structr, clients are database objects - you create them like any other data object, either through the Admin UI or via `$.create()` in scripts. Each broker type has its own client type (`MQTTClient`, `KafkaClient`, `PulsarClient`) with broker-specific configuration properties, but they all share the same interface for sending messages and managing subscriptions.

When you enable a client, Structr establishes and maintains the connection in the background. The connection persists independently of HTTP requests or user sessions.

### Message Subscribers

A `MessageSubscriber` is a database object that defines what happens when a message arrives. You create subscribers and link them to one or more clients. Each subscriber has:

- **topic** - Which topic to listen to (use `*` for all topics)
- **callback** - Code that runs when a message arrives (stored as a string property)
- **clients** - Which client(s) this subscriber is connected to (a relationship to MessageClient objects)

When a message arrives on a matching topic, Structr executes the callback code with two special variables available:

- `$.topic` - The topic the message was published to
- `$.message` - The message content (typically a string or JSON)

### The Basic Pattern

Message broker integration in Structr works through database objects. Clients and subscribers are regular Structr objects that you create, configure, and link - just like any other data in your application. This means you can create them through the Admin UI or programmatically via scripts.

**Setting up via Admin UI:**

1. Open the Data area in the Admin UI
2. Select the client type (`MQTTClient`, `KafkaClient`, or `PulsarClient`)
3. Create a new object and fill in the connection properties
4. Create a `MessageSubscriber` object with a topic and callback
5. Link the subscriber to the client by setting the `clients` property
6. Enable the client by checking `isEnabled` (MQTT) or `enabled` (Kafka/Pulsar)

**Setting up via Script:**

The same steps work programmatically using `$.create()`. This is useful when you need to create clients dynamically or as part of an application setup routine.

Once the client is enabled, Structr maintains the connection in the background. Incoming messages automatically trigger the callbacks of linked subscribers. The connection persists across requests - you configure it once, and it keeps running until you disable or delete the client.

## MQTT

MQTT (Message Queuing Telemetry Transport) is a lightweight protocol designed for constrained devices and low-bandwidth networks. It's the standard for IoT applications.

### MQTTClient Properties

| Property | Type | Description |
|----------|------|-------------|
| `mainBrokerURL` | String | Broker URL (required), e.g., `ws://localhost:15675/ws` |
| `fallbackBrokerURLs` | String[] | Alternative broker URLs for failover |
| `username` | String | Authentication username |
| `password` | String | Authentication password |
| `qos` | Integer | Quality of Service level (0, 1, or 2), default: 0 |
| `isEnabled` | Boolean | Set to `true` to connect |
| `isConnected` | Boolean | Connection status (read-only) |

### Setting Up an MQTT Client

You can create the client and subscriber objects in the Data area of the Admin UI, or programmatically as shown below:

```javascript
// Create the MQTT client
let client = $.create('MQTTClient', {
    name: 'IoT Gateway',
    mainBrokerURL: 'ws://localhost:15675/ws',
    username: 'guest',
    password: 'guest',
    qos: 1
});

// Create a subscriber for temperature readings
let subscriber = $.create('MessageSubscriber', {
    topic: 'sensors/temperature',
    callback: `{
        let data = JSON.parse($.message);
        $.log('Temperature reading: ' + data.value + '°C from ' + data.sensorId);
        
        // Store the reading
        $.create('TemperatureReading', {
            sensorId: data.sensorId,
            value: data.value,
            timestamp: $.now
        });
    }`
});

// Link subscriber to client
subscriber.clients = [client];

// Enable the connection
client.isEnabled = true;
```

When creating via the Admin UI, you fill in the same properties in the object editor. The `callback` property accepts StructrScript or JavaScript code as a string. After linking the subscriber to the client and enabling `isEnabled`, the connection activates immediately.

After enabling, the `isConnected` property indicates whether the connection succeeded. In the Admin UI, the client will show a green indicator when connected, red when disconnected.

### Subscribing to Multiple Topics

You can create multiple subscribers for different topics:

```javascript
// Subscribe to all sensor data
$.create('MessageSubscriber', {
    topic: 'sensors/*',
    callback: `{ $.call('processSensorData', { topic: $.topic, message: $.message }); }`,
    clients: [client]
});

// Subscribe to system alerts
$.create('MessageSubscriber', {
    topic: 'alerts/#',
    callback: `{ $.call('handleAlert', { topic: $.topic, message: $.message }); }`,
    clients: [client]
});
```

Use `*` to match a single level, `#` to match multiple levels in MQTT topic hierarchies.

### Publishing Messages

Send messages using the client's `sendMessage` method or the `mqttPublish` function:

```javascript
// Using the method on the client
client.sendMessage('devices/lamp/command', JSON.stringify({ action: 'on', brightness: 80 }));

// Using the global function
$.mqttPublish(client, 'devices/lamp/command', JSON.stringify({ action: 'off' }));
```

### MQTT-Specific Functions

| Function | Description |
|----------|-------------|
| `mqttPublish(client, topic, message)` | Publish a message to a topic |
| `mqttSubscribe(client, topic)` | Subscribe to a topic programmatically |
| `mqttUnsubscribe(client, topic)` | Unsubscribe from a topic |

### Quality of Service Levels

MQTT supports three QoS levels:

| Level | Name | Guarantee |
|-------|------|-----------|
| 0 | At most once | Message may be lost |
| 1 | At least once | Message delivered, may be duplicated |
| 2 | Exactly once | Message delivered exactly once |

Higher QoS levels add overhead. Use QoS 0 for frequent sensor readings where occasional loss is acceptable, QoS 1 or 2 for important commands or events.

## Kafka

Apache Kafka is a distributed streaming platform designed for high-throughput, fault-tolerant messaging. It's commonly used for data pipelines and event sourcing.

### KafkaClient Properties

| Property | Type | Description |
|----------|------|-------------|
| `servers` | String[] | Bootstrap server addresses, e.g., `['localhost:9092']` |
| `groupId` | String | Consumer group ID for coordinated consumption |
| `enabled` | Boolean | Set to `true` to connect |

### Setting Up a Kafka Client

Create the client and subscriber objects in the Data area, or programmatically:

```javascript
// Create the Kafka client
let client = $.create('KafkaClient', {
    name: 'Event Processor',
    servers: ['kafka1.example.com:9092', 'kafka2.example.com:9092'],
    groupId: 'structr-consumers'
});

// Create a subscriber for order events
let subscriber = $.create('MessageSubscriber', {
    topic: 'orders',
    callback: `{
        let order = JSON.parse($.message);
        $.log('New order received: ' + order.orderId);
        
        $.create('Order', {
            externalId: order.orderId,
            customerEmail: order.customer.email,
            totalAmount: order.total,
            status: 'received'
        });
    }`,
    clients: [client]
});

// Enable the connection
client.enabled = true;
```

The `servers` property accepts an array of bootstrap servers. Kafka clients connect to any available server and discover the full cluster topology automatically.

### Publishing to Kafka

```javascript
let client = $.first($.find('KafkaClient', 'name', 'Event Processor'));

client.sendMessage('order-updates', JSON.stringify({
    orderId: order.externalId,
    status: 'shipped',
    trackingNumber: 'ABC123',
    timestamp: new Date().toISOString()
}));
```

### Consumer Groups

The `groupId` property determines how multiple consumers coordinate. Consumers in the same group share the workload - each message is processed by only one consumer in the group. Different groups receive all messages independently.

Use the same `groupId` across multiple Structr instances to distribute processing. Use different group IDs if each instance needs to see all messages.

## Pulsar

Apache Pulsar is a cloud-native messaging platform that combines messaging and streaming. It supports multi-tenancy and geo-replication out of the box.

### PulsarClient Properties

| Property | Type | Description |
|----------|------|-------------|
| `servers` | String[] | Service URLs, e.g., `['pulsar://localhost:6650']` |
| `enabled` | Boolean | Set to `true` to connect |

### Setting Up a Pulsar Client

Create the client and subscriber objects in the Data area, or programmatically:

```javascript
// Create the Pulsar client
let client = $.create('PulsarClient', {
    name: 'Analytics Pipeline',
    servers: ['pulsar://pulsar.example.com:6650']
});

// Create a subscriber for analytics events
let subscriber = $.create('MessageSubscriber', {
    topic: 'analytics/pageviews',
    callback: `{
        let event = JSON.parse($.message);
        
        $.create('PageView', {
            path: event.path,
            userId: event.userId,
            sessionId: event.sessionId,
            timestamp: $.parseDate(event.timestamp, "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        });
    }`,
    clients: [client]
});

// Enable the connection
client.enabled = true;
```

Pulsar clients have minimal configuration. The `servers` property accepts Pulsar service URLs, typically starting with `pulsar://` for unencrypted or `pulsar+ssl://` for TLS connections.

### Publishing to Pulsar

```javascript
let client = $.first($.find('PulsarClient', 'name', 'Analytics Pipeline'));

client.sendMessage('analytics/events', JSON.stringify({
    type: 'conversion',
    userId: user.id,
    product: product.name,
    value: product.price,
    timestamp: new Date().toISOString()
}));
```

## Working with Callbacks

### Callback Context

Inside a callback, you have access to:

| Variable | Description |
|----------|-------------|
| `$.topic` | The topic the message arrived on |
| `$.message` | The message content as a string |
| `$.this` | The MessageSubscriber object |

### Forwarding to Schema Methods

For complex processing, forward messages to a global schema method:

```javascript
// Simple callback that delegates to a method
$.create('MessageSubscriber', {
    topic: '*',
    callback: `{ $.call('handleIncomingMessage', { topic: $.topic, message: $.message }); }`
});
```

Then implement the logic in your schema method where you have full access to error handling, transactions, and other methods:

```javascript
// Global schema method: handleIncomingMessage
{
    let topic = $.arguments.topic;
    let message = $.arguments.message;
    
    try {
        let data = JSON.parse(message);
        
        if (topic.startsWith('sensors/')) {
            processSensorData(topic, data);
        } else if (topic.startsWith('orders/')) {
            processOrderEvent(topic, data);
        } else {
            $.log('Unknown topic: ' + topic);
        }
    } catch (e) {
        $.log('Error processing message: ' + e.message);
        // Store failed message for retry
        $.create('FailedMessage', {
            topic: topic,
            message: message,
            error: e.message,
            timestamp: $.now
        });
    }
}
```

### Error Handling

Callbacks should handle errors gracefully. Unhandled exceptions are logged but don't stop message processing. For critical messages, implement your own retry logic:

```javascript
$.create('MessageSubscriber', {
    topic: 'critical-events',
    callback: `{
        try {
            let event = JSON.parse($.message);
            processEvent(event);
        } catch (e) {
            // Log and store for manual review
            $.log('Failed to process critical event: ' + e.message);
            $.create('FailedEvent', {
                topic: $.topic,
                payload: $.message,
                error: e.message
            });
        }
    }`
});
```

## Managing Connections

### Checking Connection Status

For MQTT clients, check the `isConnected` property:

```javascript
let client = $.first($.find('MQTTClient', 'name', 'IoT Gateway'));

if (!client.isConnected) {
    $.log('MQTT client is disconnected, attempting reconnect...');
    client.isEnabled = false;
    client.isEnabled = true;
}
```

### Disabling and Re-enabling

To temporarily stop processing:

```javascript
// Disable
client.isEnabled = false;  // or client.enabled = false for Kafka/Pulsar

// Re-enable
client.isEnabled = true;
```

Disabling disconnects from the broker. Re-enabling reconnects and resubscribes to all configured topics.

### Cleaning Up

Deleting a client automatically closes the connection and cleans up resources. Subscribers linked only to that client become inactive but are not automatically deleted.

## Best Practices

### Use JSON for Messages

Structure your messages as JSON for easy parsing and forward compatibility:

```javascript
client.sendMessage('events', JSON.stringify({
    type: 'user.created',
    version: 1,
    timestamp: new Date().toISOString(),
    data: {
        userId: user.id,
        email: user.eMail
    }
}));
```

### Keep Callbacks Simple

Callbacks should be short. Delegate complex logic to schema methods:

```javascript
// Good: Simple callback that delegates
callback: `{ $.call('processOrder', { data: $.message }); }`

// Avoid: Complex logic directly in callback
callback: `{ /* 50 lines of processing code */ }`
```

### Handle Connection Failures

Brokers can become unavailable. Design your application to handle disconnections gracefully and log connection issues for monitoring.

### Use Meaningful Topic Names

Organize topics hierarchically for easier subscription management:

```
sensors/temperature/building-a/floor-1
sensors/humidity/building-a/floor-1
orders/created
orders/shipped
orders/delivered
```

### Secure Your Connections

Use authentication (username/password for MQTT) and encrypted connections (TLS) in production. Never store credentials in callbacks - use the client properties.

## Troubleshooting

### Client Won't Connect

1. Verify the broker URL is correct and reachable from the Structr server
2. Check authentication credentials
3. Review the Structr server log for connection errors
4. For MQTT, ensure the WebSocket endpoint is enabled on the broker

### Messages Not Received

1. Verify the subscriber's topic matches the published topic
2. Check that the subscriber is linked to the correct client
3. Ensure the client is enabled and connected
4. Test with topic `*` to receive all messages and verify the connection works

### Callback Errors

1. Check the server log for exception details
2. Verify JSON parsing if the message format is unexpected
3. Test the callback logic in a schema method first

## Related Topics

- Server-Sent Events - Pushing updates to browsers
- Scheduled Tasks - Processing queued messages periodically
- Business Logic - Implementing message handlers as schema methods
