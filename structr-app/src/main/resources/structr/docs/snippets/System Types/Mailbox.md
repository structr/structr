# Mailbox

Configures an email account to fetch from IMAP or POP3 servers. Calling `fetchMails()` on a mailbox fetches it in the background and stores new messages as EMailMessage objects; nothing is fetched automatically. Key properties include `host`, `mailProtocol` (imaps or pop3), `port`, `user`, `password`, and `folders` to fetch.

## Details

A fetch reads the messages that arrived since the last one (IMAP; POP3 reads the newest), detects duplicates via Message-ID within the mailbox, and extracts attachments as File objects. The properties `lastFetchStarted`, `lastFetchSucceeded`, `lastFetchError` and `lastFetchCount` show the outcome of the last fetch. Use `overrideMailEntityType` to specify a custom subtype for incoming emails, enabling lifecycle methods for automatic processing. To fetch regularly, call `fetchMails()` from a method registered with the CronService. You can list the folders available on the server with `getAvailableFoldersOnServer()`.
