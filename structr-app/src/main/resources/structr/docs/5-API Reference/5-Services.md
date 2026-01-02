# Services

## CronService
This service allows you to schedule periodic execution of built-in functions based on a pattern similar to the "cron" daemon on UNIX systems.

### How It Works
Scheduled tasks for the CronService are configured in `structr.conf`. The main configuration key is `CronService.tasks`. It accepts a whitespace-separated list of user-defined function names. These are the tasks that are registered with the CronService.

For each of those tasks, a `cronExpression` entry has to be configured which determines the execution time/date of the task. It consists of seven fields and looks similar to crontab entries in Unix-based operating systems:

```
<methodName>.cronExpression = <s> <m> <h> <dom> <m> <dow>
```

| Field | Explanation | Value Range |
| --- | --- | --- |
| `<methodName>` | name of the user-defined function | any existing user-defined function |
| `<s>` | seconds of the minute | 0-59 |
| `<m>` | minute of the hour | 0-59 |
| `<h>` | hour of the day | 0-23 |
| `<dom>` | day of the month | 0-31 |
| `<m>` | month of the year | 1-12 |
| `<dow>` | day of the week | 0-6 (0 = Sunday) |

There are several supported notations for the fields:

| Notation | Meaning |
| --- | --- |
| * | Execute for every possible value of the field. |
| x | Execute at the given field value. |
| x-y | Execute for value x up to value y of the field. |
| */x | Execute for every multiple of x in the field (in relation to the next bigger field). |

Examples:

| Example | Meaning |
| --- | --- |
| Hours = * | Execute at every full hour. |
| Hours = 12| Execute at 12 o’clock. |
| Hours = 12-16 | Execute at 12, 13, 14, 15, 16 o’clock. |
| Seconds = */15 | In every minute, execute at 0, 15, 30, 45 seconds. |
| Seconds = */23 | Special case: In every minute, execute at 0, 23, 46 seconds. If the unit is not evenly divisible by the given number the last interval is shorter than the others. |

### Notes
- The scheduled functions are executed in the context of an admin user.
- The CronService must be included in the structr.conf setting `configured.services` to be activated.
- When a cronExpression is added, deleted or edited, the CronService has to be restarted for the changes to take effect. This can be done at runtime via the configuration tool or by restarting Structr.
- By default, Structr prevents the same cron task to run run while the previous execution is still running. This can be changed via the setting `CronService.allowparallelexecution` in structr.conf.


## DirectoryWatchService
A service that synchronizes a Folder in Structr's filesystem with a folder in the server's filesystem.

### How It Works
If you set the `mountTarget` property of a Folder node to an existing path on your local filesystem, you can configure Structr to watch the folder's contents and update the metadata whenever a file on disk changes.

The contents of the files are not imported into Structr but remain in the mounted folder. Changes made to the files in Structr are written to the corresponding files.

### Notes
- This will only work on filesystems that support directory watch events.
- The DirectoryWatchService must be included in the structr.conf setting `configured.services` to be activated.


## FtpService
A service that provides access to the Structr filesystem via FTP.

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


## SSHService
A service that provides access to Structr via SSH.
