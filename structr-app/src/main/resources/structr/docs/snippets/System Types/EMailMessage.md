# EMailMessage

Stores email messages fetched from mailboxes or saved from outgoing mail. Key properties include `subject`, `from`, `fromMail`, `to`, `cc`, `bcc`, `content` (plain text), `htmlContent`, `sentDate`, `receivedDate`, `messageId`, `inReplyTo` for threading, `header` (all headers as JSON), `mailbox`, and `attachedFiles`.

## Details

Attachments are stored as linked File objects in a date-based folder structure. Add an `onCreate` method to process incoming emails automatically – for example, creating support tickets. You can extend EMailMessage with custom properties and configure the subtype on the Mailbox via `overrideMailEntityType`.
