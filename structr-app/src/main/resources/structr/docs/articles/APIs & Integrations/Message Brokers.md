
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
| `cleanSession` | Boolean | Start every connection with a new session at the broker, default: false |
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
// Subscribe to temperature readings
$.create('MessageSubscriber', {
    topic: 'sensors/temperature',
    callback: `{ $.call('processSensorData', { topic: $.topic, message: $.message }); }`,
    clients: [client]
});

// Subscribe to system alerts
$.create('MessageSubscriber', {
    topic: 'alerts',
    callback: `{ $.call('handleAlert', { topic: $.topic, message: $.message }); }`,
    clients: [client]
});
```

Structr matches the subscriber topic against the incoming topic as a whole: a subscriber receives a message when its topic equals the message topic exactly, or when its topic is the single value `*`, which receives every message the client sees. MQTT-style wildcards such as `sensors/+` or `alerts/#` are not interpreted, so create one subscriber per topic or use `*` and branch on `$.topic` in the callback.

### Managing Broker Subscriptions

An MQTT client has the methods `subscribeTopic` and `unsubscribeTopic`, which subscribe the client to a topic at the broker and remove that subscription again. Both take the topic as the argument `topic` and only reach the broker while the client is enabled and connected. The functions `mqttSubscribe` and `mqttUnsubscribe` do the same. Messages on such a topic reach the subscribers with that topic or with `*`:

```javascript
client.subscribeTopic({ topic: 'devices/lamp/status' });
client.unsubscribeTopic({ topic: 'devices/lamp/status' });
```

Kafka and Pulsar clients subscribe to the topics of their subscribers on their own, for them these methods do nothing.

### Publishing Messages

Send messages using the client's `sendMessage` method or the `mqttPublish` function. Like every method called from JavaScript, `sendMessage` takes its arguments as an object, here with the names `topic` and `message`:

```javascript
// Using the method on the client
client.sendMessage({ topic: 'devices/lamp/command', message: JSON.stringify({ action: 'on', brightness: 80 }) });

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

The QoS level describes the transfer between the broker and Structr. It does not extend to your callback: Structr acknowledges a message only after its callbacks have committed, so with QoS 2 a message can still be processed twice if Structr stops before the acknowledgement reaches the broker. See Delivery Guarantees below.

### Persistent Sessions

By default, the broker keeps the session of an MQTT client when it disconnects, because `cleanSession` is `false`. The session contains the subscriptions and the messages with QoS 1 or 2 that arrive while the client is disconnected or that Structr has not acknowledged yet, and the broker delivers them when the client connects again, also after a restart of Structr. The broker identifies the session by the client ID, which is the UUID of the `MQTTClient` object.

Set `cleanSession` to `true` if your application only cares about messages that arrive while it is connected, for example live sensor readings. The broker then discards the session on every connect, and messages sent while Structr was disconnected are lost. Messages with QoS 0 are never kept for a disconnected client, regardless of this setting. Changing `cleanSession` reconnects an enabled client.

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

client.sendMessage({
    topic: 'order-updates',
    message: JSON.stringify({
        orderId: order.externalId,
        status: 'shipped',
        trackingNumber: 'ABC123',
        timestamp: new Date().toISOString()
    })
});
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

client.sendMessage({
    topic: 'analytics/events',
    message: JSON.stringify({
        type: 'conversion',
        userId: user.id,
        product: product.name,
        value: product.price,
        timestamp: new Date().toISOString()
    })
});
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

## Delivery Guarantees

Structr combines a message broker with a database, and there is no shared commit between the two. A message can therefore not be processed exactly once in every situation. Where the broker keeps unconfirmed messages, Structr processes incoming messages at least once, and it sends outgoing messages only after the transaction that sent them has committed.

### Incoming Messages

Structr runs the callbacks of all subscribers for a message in one transaction. If the callback of one subscriber fails, Structr logs the error and the work of the other subscribers is still committed.

If the database terminates the transaction, for example to resolve a deadlock, MQTT and Kafka clients run the callbacks again, with up to ten attempts in total. Pulsar clients reject the message instead, and the broker delivers it again after a delay.

Structr confirms a message to the broker only after the transaction has committed or has failed for good. MQTT clients acknowledge the message, Kafka clients commit the offsets after each batch of records, and Pulsar clients acknowledge the message. If Structr shuts down before that point, the broker delivers the message again after the restart. For MQTT, this requires QoS 1 or 2 and a persistent session, see Persistent Sessions above.

The consequence is that a callback can receive the same message twice: once before a shutdown that interrupted the confirmation, and once after the restart. Write callbacks so that processing a message twice has the same effect as processing it once, for example by storing a message ID from the payload with a uniqueness constraint, or by checking whether the object the message describes already exists.

A callback that fails with an error counts as processed, because delivering the message again would fail the same way. The same applies to a transaction that MQTT or Kafka clients could not complete in ten attempts. Structr logs the error and continues with the next message. If you need to retry such messages, store them yourself as shown in Error Handling above.

### Outgoing Messages

When you call `sendMessage` or `mqttPublish` in a transaction, Structr sends the message after the transaction has committed. If the transaction fails and is rolled back, the message is not sent, so other systems never receive messages about changes that did not happen. If Structr repeats the transaction, for example a REST request whose transaction the database terminated to resolve a deadlock, the message is still sent only once.

Since the message leaves Structr after the commit, an error during sending, such as a broker that has become unavailable, does not reach your script. Structr logs it instead. If the server stops between the commit and the sending, the message is lost. Only a missing connection of an MQTT client is reported to the script immediately, as an error.

Subscribers of the sending client receive the message immediately, in the same transaction as the sender. If the client also receives the topic from the broker, its subscribers get the message a second time when the broker delivers it.

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

Messages that arrive while a client is disabled are kept by the broker where the protocol allows it. A Kafka client continues from the last committed offset of its consumer group, and a Pulsar client finds the messages in its subscription. An MQTT client receives them only with a persistent session and QoS 1 or 2, see Persistent Sessions. A message that a Kafka client has already fetched when you disable it is still processed.

### Cleaning Up

Deleting a client automatically closes the connection and cleans up resources. Subscribers linked only to that client become inactive but are not automatically deleted.

## Best Practices

### Use JSON for Messages

Structure your messages as JSON for easy parsing and forward compatibility:

```javascript
client.sendMessage({
    topic: 'events',
    message: JSON.stringify({
        type: 'user.created',
        version: 1,
        timestamp: new Date().toISOString(),
        data: {
            userId: user.id,
            email: user.eMail
        }
    })
});
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
- XMPP - Chat and presence for messages between your application and people
- Scheduled Tasks - Processing queued messages periodically
- Business Logic - Implementing message handlers as schema methods
