# CronService

Executes global schema methods on a configurable schedule. Tasks run as superuser.

## Settings

| Setting | Description |
|---------|-------------|
| `CronService.tasks` | Whitespace-separated list of method names to schedule |
| `<methodName>.cronExpression` | Six-field cron expression: `<s> <m> <h> <dom> <mon> <dow>` |
| `CronService.allowparallelexecution` | Allow concurrent execution of the same task (default: false) |

