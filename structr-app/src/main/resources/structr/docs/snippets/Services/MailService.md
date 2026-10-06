# MailService

Fetches emails from Mailbox objects via IMAP or POP3 and stores them as EMailMessage objects. Detects duplicates using Message-ID headers and extracts attachments automatically.

The service never fetches on its own. A mailbox is fetched when `fetchMails()` is called on it; to fetch on a schedule, register a method that calls it with the CronService. Every fetch is logged at info level, with a short status of the service.

## Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `mail.maxemails` | 25 | Maximum emails to fetch per mailbox per fetch; with IMAP, a larger backlog is fetched over several runs |
| `mail.attachmentbasepath` | /mail/attachments | Path for storing email attachments |
| `mail.connecttimeout` | 30000 | How long to wait for a server to accept a connection, in milliseconds |
| `mail.readtimeout` | 60000 | How long to wait for a server to answer once connected, in milliseconds |
| `mail.maxconcurrentfetches` | 4 | How many mailboxes are fetched at the same time |

## Related Types

`Mailbox`, `EMailMessage`
