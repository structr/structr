# MailService

Periodically fetches emails from configured Mailbox objects via IMAP or POP3 and stores them as EMailMessage objects. Detects duplicates using Message-ID headers and extracts attachments automatically.

## Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `mail.maxemails` | 25 | Maximum emails to fetch per mailbox per check |
| `mail.updateinterval` | 30000 | Interval between checks in milliseconds |
| `mail.attachmentbasepath` | /mail/attachments | Path for storing email attachments |

## Related Types

`Mailbox`, `EMailMessage`
