# XMPP

Structr can take part in XMPP conversations as a user of an XMPP server. XMPP, also known as Jabber, is an open protocol for chat and presence, and many organizations run their own server for it. Your application can send messages to people, react to messages it receives, take part in chat rooms, and manage the contacts of its account.

Typical uses are notifications that reach people in the chat client they already use, chat bots that answer questions with data from your application, and rooms where your application posts events for a team.

If you want to exchange data with other systems rather than with people, use a message broker instead, see Message Brokers.

## The XMPPClient

An `XMPPClient` object represents an account on an XMPP server. You create it like any other object, in the Data area of the Admin UI or with `$.create()` in a script. When you enable it, Structr logs in to the server with the account and keeps the connection open in the background. If the connection is lost, for example because the server restarts, Structr tries to restore it every five seconds. If the server ends the session on purpose, or the client cannot log in, for example because of a wrong password, Structr does not try again. Enabled clients connect again when Structr starts.

| Property | Type | Description |
|----------|------|-------------|
| `xmppUsername` | String | The user name of the account, the part of the address before the `@` |
| `xmppPassword` | String | The password of the account |
| `xmppService` | String | The XMPP domain of the account, the part of the address after the `@`, e.g. `example.com` |
| `xmppHost` | String | The host name of the server to connect to |
| `xmppPort` | Integer | The port of the server, usually `5222` |
| `presenceMode` | Enum | The presence the client announces: `available`, `chat`, `away`, `xa` (extended away) or `dnd` (do not disturb). Default: `available` |
| `isEnabled` | Boolean | Set to `true` to connect |
| `isConnected` | Boolean | Set by Structr: whether the client is connected. Structr updates it when the connection is lost and when it is restored. |
| `xmppHandle` | String | The user name and host, read-only |

### Encryption

Structr only connects to servers that encrypt the connection with TLS, because the password of the account and every message would otherwise travel in plain text. The server must offer TLS on the connection, and its certificate must be issued for the XMPP domain in `xmppService` and be trusted by the Java runtime of Structr. A certificate from a public certificate authority works without further setup. For a certificate of your own, add it or the certificate of its authority to the trust store of the Java runtime.

### Setting Up a Client

```javascript
let client = $.create('XMPPClient', {
    name: 'Notifications',
    xmppUsername: 'structr',
    xmppPassword: 'secret',
    xmppService: 'example.com',
    xmppHost: 'xmpp.example.com',
    xmppPort: 5222,
    isEnabled: true
});
```

After the client has logged in, it announces its presence, because the server delivers messages to an account only while it is available. Messages that the server kept while the client was offline arrive at that point.

The connection settings take effect when the client connects. To apply changes to them, disable the client and enable it again.

## Receiving Messages

To react to incoming messages, add a method named `onXMPPMessage` to the type `XMPPClient` in the schema. Structr calls it for every message with a text that the client receives, both direct messages and messages in chat rooms the client has joined. The method receives two arguments:

| Argument | Description |
|----------|-------------|
| `$.arguments.sender` | The address of the sender, including the resource, e.g. `alice@example.com/phone`. In a chat room, the address of the room and the nickname of the sender |
| `$.arguments.message` | The text of the message |

`$.this` is the client that received the message, so the method can answer directly:

```javascript
// method onXMPPMessage of the type XMPPClient
{
    if ($.arguments.message.trim() === 'status') {

        let open = $.find('Ticket', { status: 'open' }).length;

        $.this.doSendMessage({ recipient: 'alice@example.com', message: open + ' open tickets' });
    }
}
```

The client also receives the messages that it sends to a chat room, because the room distributes them to all occupants, the sender included. A method that answers messages in a room should therefore ignore its own.

## Sending Messages

The method `doSendMessage` sends a message to an address:

```javascript
client.doSendMessage({ recipient: 'alice@example.com', message: 'The nightly import has finished.' });
```

Like every method called from JavaScript, the methods of the client take their arguments as an object with names.

## Chat Rooms

Chat rooms let several people and applications talk in one place. A room has an address of its own, usually in a separate domain of the server, e.g. `team@conference.example.com`.

| Method | Arguments | Description |
|--------|-----------|-------------|
| `doJoinChat` | `chatRoom`, `nickname`, `password` | Joins the room with the given nickname, and does nothing if the client is in the room already. The password is only needed for protected rooms. |
| `doSendChatMessage` | `chatRoom`, `message`, `password` | Sends a message to the room. If the client has not joined the room yet, it joins first, with its user name and host as nickname. |

```javascript
client.doJoinChat({ chatRoom: 'team@conference.example.com', nickname: 'Structr' });
client.doSendChatMessage({ chatRoom: 'team@conference.example.com', message: 'Deployment finished.' });
```

Joining a room waits for the answer of the server, so `doJoinChat` reports an error to your script if the server refuses, for example because of a wrong password.

When a lost connection is restored, the client joins its rooms again. After a restart of Structr, or when you disable and enable the client, it is no longer in any room and receives no room messages until it joins again. `doSendChatMessage` joins on its own, but a client that only listens to a room has to join it again. Since `doJoinChat` does nothing for a room the client is in already, you can call it regularly, for example in a scheduled method.

## Contacts and Subscriptions

In XMPP, an account sees whether another account is available only if it has subscribed to it, and the other account has to confirm the subscription. This is how both sides of a chat end up in each other's contact list.

| Method | Description |
|--------|-------------|
| `doSubscribe` | Asks the account in `recipient` for a subscription |
| `doUnsubscribe` | Ends the subscription to the account in `recipient` |
| `doConfirmSubscription` | Confirms the subscription request of the account in `recipient` |
| `doDenySubscription` | Rejects the subscription request of the account in `recipient`, or ends its existing subscription |

To answer requests that other accounts send to your client, add a method named `onXMPPPresence` to the type `XMPPClient`. Structr calls it for subscription requests and answers with two arguments:

| Argument | Description |
|----------|-------------|
| `$.arguments.sender` | The address of the other account, without resource, e.g. `alice@example.com` |
| `$.arguments.type` | `subscribe` for a request, `subscribed` and `unsubscribed` for the answer to a request of your client, `unsubscribe` when the other account ends its subscription |

```javascript
// method onXMPPPresence of the type XMPPClient
{
    if ($.arguments.type === 'subscribe') {

        if ($.arguments.sender.endsWith('@example.com')) {

            $.this.doConfirmSubscription({ recipient: $.arguments.sender });

        } else {

            $.this.doDenySubscription({ recipient: $.arguments.sender });
        }
    }
}
```

Without an `onXMPPPresence` method, Structr rejects every subscription request, so that it does not stay pending. If the method fails with an error, the request stays pending, and the server delivers it again the next time the client logs in.

Structr does not pass on the presences that announce whether a contact is available, because every status change of every contact and of every occupant of a joined room would run your method.

## Delivery

Structr sends messages, room messages and subscription stanzas after the transaction that sends them has committed. If the transaction fails and is rolled back, nothing is sent, so other people never receive messages about changes that did not happen. If Structr repeats the transaction, for example a REST request whose transaction the database terminated to resolve a deadlock, the stanza is still sent only once.

Since the stanza leaves Structr after the commit, an error during sending does not reach your script. Structr logs it instead. If the client is enabled but not connected, the methods report an error to your script right away. A disabled client sends nothing and reports no error.

Structr runs `onXMPPMessage` for each incoming message in a transaction of its own. XMPP has no confirmation of received messages, so a message whose method fails is lost, and Structr logs the error. While Structr is not connected, a server that supports offline storage keeps the direct messages for the account and delivers them when the client connects again.

When the client joins a chat room, the server usually sends the most recent messages of the room as history, and `onXMPPMessage` runs for them as well. Each time the client joins a room again, your method can therefore receive messages it has already processed. Write it so that processing a message twice has the same effect as processing it once.

## Related Topics

- Message Brokers - Exchanging data with other systems over MQTT, Kafka and Pulsar
- Business Logic - Implementing the methods `onXMPPMessage` and `onXMPPPresence`
