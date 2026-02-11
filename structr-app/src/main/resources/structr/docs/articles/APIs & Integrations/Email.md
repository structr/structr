
Structr provides email functionality for both sending and receiving messages. You can send simple emails with a single function call, compose complex messages with attachments and custom headers, and automatically fetch incoming mail from IMAP or POP3 mailboxes.

## Quick Start

To send your first email:

1. Configure your SMTP server in the Configuration Interface under SMTP Settings (host, port, user, password)
2. Call `sendPlaintextMail()` or `sendHtmlMail()`:

```javascript
$.sendPlaintextMail(
    'sender@example.com', 'Sender Name',
    'recipient@example.com', 'Recipient Name',
    'Subject Line',
    'Email body text'
);
```

That's it. For multiple recipients, attachments, or custom headers, see the Advanced Email API section below.

## Sending Emails

### SMTP Configuration

Before sending emails, configure your SMTP server in the Configuration Interface under SMTP Settings:

| Setting | Description |
|---------|-------------|
| `smtp.host` | SMTP server hostname |
| `smtp.port` | SMTP server port (typically 587 for TLS, 465 for SSL) |
| `smtp.user` | SMTP username for authentication |
| `smtp.password` | SMTP password |
| `smtp.tls.enabled` | Enable TLS encryption |
| `smtp.tls.required` | Require TLS (fail if not available) |

### Multiple SMTP Configurations

You can define multiple SMTP configurations for different purposes (transactional emails, marketing, different departments). Add a prefix to each setting:

```properties
# Default configuration
smtp.host = mail.example.com
smtp.port = 587
smtp.user = default@example.com
smtp.password = secret
smtp.tls.enabled = true
smtp.tls.required = true

# Marketing configuration
marketing.smtp.host = marketing-mail.example.com
marketing.smtp.port = 587
marketing.smtp.user = marketing@example.com
marketing.smtp.password = secret
marketing.smtp.tls.enabled = true
marketing.smtp.tls.required = true
```

Select a configuration in your code with `mailSelectConfig()` before sending.

### Basic Email Functions

For simple emails, use the one-line functions:

**sendHtmlMail:**

```javascript
$.sendHtmlMail(
    'info@example.com',           // fromAddress
    'Example Company',            // fromName
    'user@domain.com',            // toAddress
    'John Doe',                   // toName
    'Welcome to Our Service',     // subject
    '<h1>Welcome!</h1><p>Thank you for signing up.</p>',  // htmlContent
    'Welcome! Thank you for signing up.'                   // textContent
);
```

**sendPlaintextMail:**

```javascript
$.sendPlaintextMail(
    'info@example.com',           // fromAddress
    'Example Company',            // fromName
    'user@domain.com',            // toAddress
    'John Doe',                   // toName
    'Your Order Confirmation',    // subject
    'Your order #12345 has been confirmed.'  // content
);
```

**With attachments:**

```javascript
let invoice = $.first($.find('File', 'name', 'invoice.pdf'));

$.sendHtmlMail(
    'billing@example.com',
    'Billing Department',
    'customer@domain.com',
    'Customer Name',
    'Your Invoice',
    '<p>Please find your invoice attached.</p>',
    'Please find your invoice attached.',
    [invoice]  // attachments must be a list
);
```

### Advanced Email API

For complex emails with multiple recipients, custom headers, or dynamic content, use the Advanced Mail API. This follows a builder pattern: start with `mailBegin()`, configure the message, then send with `mailSend()`.

**Basic example:**

```javascript
$.mailBegin('support@example.com', 'Support Team', 'Re: Your Question', '<p>Thank you for contacting us.</p>', 'Thank you for contacting us.');
$.mailAddTo('customer@domain.com', 'Customer Name');
$.mailSend();
```

**Complete example with all features:**

```javascript
// Start a new email
$.mailBegin('newsletter@example.com', 'Newsletter');

// Set content
$.mailSetSubject('Monthly Newsletter - January 2026');
$.mailSetHtmlContent('<h1>Newsletter</h1><p>This month\'s updates...</p>');
$.mailSetTextContent('Newsletter\n\nThis month\'s updates...');

// Add recipients
$.mailAddTo('subscriber1@example.com', 'Subscriber One');
$.mailAddTo('subscriber2@example.com', 'Subscriber Two');
$.mailAddCc('marketing@example.com', 'Marketing Team');
$.mailAddBcc('archive@example.com');

// Set reply-to address
$.mailAddReplyTo('feedback@example.com', 'Feedback');

// Add custom headers
$.mailAddHeader('X-Campaign-ID', 'newsletter-2026-01');
$.mailAddHeader('X-Mailer', 'Structr');

// Add attachments
let attachment = $.first($.find('File', 'name', 'report.pdf'));
$.mailAddAttachment(attachment, 'January-Report.pdf');  // optional custom filename

// Send and get message ID
let messageId = $.mailSend();

if ($.mailHasError()) {
    $.log('Failed to send email: ' + $.mailGetError());
} else {
    $.log('Email sent with ID: ' + messageId);
}
```

### Using Different SMTP Configurations

Select a named configuration before sending:

```javascript
$.mailBegin('marketing@example.com', 'Marketing');
$.mailSelectConfig('marketing');  // Use marketing SMTP settings
$.mailAddTo('customer@example.com');
$.mailSetSubject('Special Offer');
$.mailSetHtmlContent('<p>Check out our latest deals!</p>');
$.mailSend();
```

To reset to the default configuration:

```javascript
$.mailSelectConfig('');  // Empty string resets to default
```

### Dynamic SMTP Configuration

For runtime-configurable SMTP settings (e.g., from database or user input):

```javascript
$.mailBegin('sender@example.com', 'Sender');
$.mailSetManualConfig(
    'smtp.provider.com',  // host
    587,                  // port
    'username',           // user
    'password',           // password
    true,                 // useTLS
    true                  // requireTLS
);
$.mailAddTo('recipient@example.com');
$.mailSetSubject('Test');
$.mailSetTextContent('Test message');
$.mailSend();

// Reset manual config for next email
$.mailResetManualConfig();
```

Configuration priority: manual config > selected config > default config.

### Saving Outgoing Messages

Outgoing emails are not saved by default. To keep a record of sent emails, explicitly enable saving with `mailSaveOutgoingMessage(true)` before calling `mailSend()`. Structr then stores the message as an `EMailMessage` object:

```javascript
$.mailBegin('support@example.com', 'Support');
$.mailAddTo('customer@example.com');
$.mailSetSubject('Ticket #12345 Update');
$.mailSetHtmlContent('<p>Your ticket has been updated.</p>');

// Enable saving before sending
$.mailSaveOutgoingMessage(true);

$.mailSend();

// Retrieve the saved message
let sentMessage = $.mailGetLastOutgoingMessage();
$.log('Saved message ID: ' + sentMessage.id);
```

Saved messages include all recipients, content, headers, and attachments. Attachments are copied to the file system under the configured attachment path.

### Replying to Messages

To create a proper reply that mail clients can thread correctly:

```javascript
// Get the original message
let originalMessage = $.first($.find('EMailMessage', 'id', originalId));

$.mailBegin('support@example.com', 'Support');
$.mailAddTo(originalMessage.fromMail);
$.mailSetSubject('Re: ' + originalMessage.subject);
$.mailSetHtmlContent('<p>Thank you for your message.</p>');

// Set In-Reply-To header for threading
$.mailSetInReplyTo(originalMessage.messageId);

$.mailSend();
```

### Error Handling

Always check for errors after sending:

```javascript
$.mailBegin('sender@example.com', 'Sender');
$.mailAddTo('recipient@example.com');
$.mailSetSubject('Test');
$.mailSetTextContent('Test message');
$.mailSend();

if ($.mailHasError()) {
    let error = $.mailGetError();
    $.log('Email failed: ' + error);
    // Handle error (retry, notify admin, etc.)
} else {
    $.log('Email sent successfully');
}
```

Common errors include authentication failures, connection timeouts, and invalid recipient addresses.

### Sender Address Requirements

Most SMTP providers require the sender address to match your authenticated account. If you use a shared SMTP server, the from address must typically be your account email.

For example, if your SMTP account is `user@example.com`, sending from `other@example.com` will likely fail with an error like:

```
550 5.7.1 User not authorized to send on behalf of <other@example.com>
```

This also applies to Structr's built-in mail templates for password reset and registration confirmation. By default, these emails are sent using the address configured in structr.conf under `smtp.user` (if it contains a valid email address). If not, the sender defaults to `structr-mail-daemon@localhost`, which is typically rejected by external mail providers. Configure the correct sender addresses in the Mail Templates area of the Admin UI.

## Receiving Emails

Structr can automatically fetch emails from IMAP or POP3 mailboxes and store them as `EMailMessage` objects in the database. The MailService runs in the background and periodically checks all configured mailboxes.

### MailService Configuration

Configure the MailService in the Configuration Interface:

| Setting | Default | Description |
|---------|---------|-------------|
| `mail.maxemails` | 25 | Maximum number of emails to fetch per mailbox per check |
| `mail.updateinterval` | 30000 | Interval between checks in milliseconds (default: 30 seconds) |
| `mail.attachmentbasepath` | /mail/attachments | Base path for storing email attachments |

### Creating a Mailbox

Create a `Mailbox` object to configure an email account for fetching:

```javascript
$.create('Mailbox', {
    name: 'Support Inbox',
    host: 'imap.example.com',
    mailProtocol: 'imaps',        // 'imaps' for IMAP over SSL, 'pop3' for POP3
    port: 993,                     // Optional, uses protocol default if not set
    user: 'support@example.com',
    password: 'secret',
    folders: ['INBOX', 'Support']  // Folders to monitor
});
```

| Property | Description |
|----------|-------------|
| `host` | Mail server hostname |
| `mailProtocol` | `imaps` (IMAP over SSL) or `pop3` |
| `port` | Server port (optional, defaults to protocol standard) |
| `user` | Account username |
| `password` | Account password |
| `folders` | Array of folder names to fetch from |
| `overrideMailEntityType` | Custom type extending EMailMessage (optional) |

### How Mail Fetching Works

The MailService automatically:

1. Connects to each configured mailbox at the configured interval
2. Fetches messages from the specified folders (newest first)
3. Checks for duplicates using the Message-ID header
4. Creates `EMailMessage` objects for new messages
5. Extracts and stores attachments as File objects

Duplicate detection first tries to match by `messageId`. If no Message-ID header exists, it falls back to matching by subject, from, to, and dates.

### EMailMessage Properties

Fetched emails are stored with these properties:

| Property | Description |
|----------|-------------|
| `subject` | Email subject |
| `from` | Sender display string (name and address) |
| `fromMail` | Sender email address only |
| `to` | Recipients (To:) |
| `cc` | Carbon copy recipients |
| `bcc` | Blind carbon copy recipients |
| `content` | Plain text content |
| `htmlContent` | HTML content |
| `folder` | Source folder name |
| `sentDate` | When the email was sent |
| `receivedDate` | When the email was received |
| `messageId` | Unique message identifier |
| `inReplyTo` | Message-ID of the parent message (for threading) |
| `header` | JSON string containing all headers |
| `mailbox` | Reference to the source Mailbox |
| `attachedFiles` | List of attached File objects |

### Listing Available Folders

To discover which folders are available on a mail server, call the method `getAvailableFoldersOnServer`:

```javascript
let mailbox = $.first($.find('Mailbox', 'name', 'Support Inbox'));
let folders = mailbox.getAvailableFoldersOnServer();

for (let folder of folders) {
    $.log('Available folder: ' + folder);
}
```

### Manual Mail Fetching

While the MailService fetches automatically, you can trigger an immediate fetch:

```javascript
let mailbox = $.first($.find('Mailbox', 'name', 'Support Inbox'));
mailbox.fetchMails();
```

### Custom Email Types

To add custom properties or methods to incoming emails, create a type that extends `EMailMessage` and configure it on the mailbox:

```javascript
// Assuming you have a custom type 'SupportTicketMail' extending EMailMessage
let mailbox = $.first($.find('Mailbox', 'name', 'Support Inbox'));
mailbox.overrideMailEntityType = 'SupportTicketMail';
```

New emails will be created as your custom type, allowing you to add lifecycle methods like `onCreate` for automatic processing.

### Processing Incoming Emails

To automatically process incoming emails, create an `onCreate` method on `EMailMessage` (or your custom type):

```javascript
// onCreate method on EMailMessage or custom subtype
{
    $.log('New email received: ' + $.this.subject);
    
    // Example: Create a support ticket from the email
    if ($.this.mailbox.name === 'Support Inbox') {
        $.create('SupportTicket', {
            title: $.this.subject,
            description: $.this.content,
            customerEmail: $.this.fromMail,
            sourceEmail: $.this
        });
    }
}
```

### Attachment Storage

Email attachments are automatically extracted and stored as File objects. The storage path follows this structure:

```
{mail.attachmentbasepath}/{year}/{month}/{day}/{mailbox-uuid}/
```

For example: `/mail/attachments/2026/2/2/a1b2c3d4-...`

Attachments are linked to their email via the `attachedFiles` property.

## Best Practices

### Sending

- Always check `mailHasError()` after sending and handle failures appropriately
- Use `mailSaveOutgoingMessage()` for important emails to maintain a record
- Set `mailSetInReplyTo()` when replying to maintain proper threading
- Provide both HTML and plain text content for maximum compatibility

### Receiving

- Set a reasonable `mail.maxemails` value to avoid overwhelming the system
- Use `overrideMailEntityType` to add custom processing logic
- Monitor the server log for connection or authentication errors
- Consider the `mail.updateinterval` based on how quickly you need to process incoming mail

### Security

- Store SMTP and mailbox passwords securely
- Use TLS/SSL for all mail connections
- Be cautious with attachments from unknown senders

## Related Topics

- Scheduled Tasks - Triggering email-related tasks on a schedule
- Business Logic - Processing emails in lifecycle methods
- Files - Working with email attachments
