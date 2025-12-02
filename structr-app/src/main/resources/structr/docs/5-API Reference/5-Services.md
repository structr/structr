# Services
## MailService
A service that queries mailboxes and stores the emails in the database.
### Settings

|Name|Description|
|---|---|
|mail.maxemails|Maximum number of (new) emails that are fetched and created in one go|
|mail.updateinterval|Update interval in milliseconds|
|mail.attachmentbasepath|path in Structr's virtual filesystem where attachments are downloaded to|


### Types
`Mailbox`, `EMailMessage`

