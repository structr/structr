# Mailbox

Configures an email account for automatic fetching from IMAP or POP3 servers. The MailService periodically checks all mailboxes and stores new messages as EMailMessage objects. Key properties include `host`, `mailProtocol` (imaps or pop3), `port`, `user`, `password`, and `folders` to monitor.

## Details

The service fetches messages newest first, detects duplicates via Message-ID, and extracts attachments as File objects. Use `overrideMailEntityType` to specify a custom subtype for incoming emails, enabling lifecycle methods for automatic processing. You can trigger immediate fetching with FetchMailsCommand or list available folders with FetchFoldersCommand.
