# Scheduled Tasks

Some tasks need to run automatically at regular intervals: cleaning up temporary data, sending scheduled reports, synchronizing with external systems, or performing routine maintenance. Structr's CronService executes global schema methods on a schedule you define, without requiring external tools like system cron or task schedulers.

## How It Works

The CronService runs in the background and monitors configured schedules. When a scheduled time is reached, it executes the corresponding global schema method. To use scheduled tasks, you need two things: a global schema method that performs the work, and a cron expression that defines when it runs.

Scheduled tasks start running only after Structr has fully started. If a scheduled time passes during startup or while Structr is shut down, that execution is skipped - Structr does not retroactively run missed tasks.

## Configuring Tasks

Register your tasks in `structr.conf` using the `CronService.tasks` setting. This accepts a whitespace-separated list of global schema method names:

```properties
CronService.tasks = cleanupExpiredSessions dailyReport weeklyMaintenance
```

For each task, define a cron expression that determines when it runs:

```properties
cleanupExpiredSessions.cronExpression = 0 0 * * * *
dailyReport.cronExpression = 0 0 8 * * *
weeklyMaintenance.cronExpression = 0 0 3 * * 0
```

Note that `structr.conf` only contains settings that differ from Structr's defaults. The CronService is active by default, so you only need to add your task configuration - no additional setup is required.

## Applying Configuration Changes

The CronService reads its configuration only at startup. When you add, edit, or remove a scheduled task in `structr.conf`, you must restart the CronService for the changes to take effect.

To restart the CronService:

1. Open the Configuration Interface
2. Navigate to the Services tab
3. Find "CronService" in the list
4. Click "Restart"

Alternatively, restart Structr entirely. Simply saving changes to `structr.conf` is not sufficient - the service must be restarted.

> **Note:** Forgetting to restart the CronService is a common reason why newly configured tasks do not run. If your task is not executing at the expected time, verify that you restarted the service after changing the configuration.

## Execution Context

Scheduled tasks run as the superuser in a privileged context. This means:

- The task has full access to all data and operations
- `$.me` refers to the superuser (displayed as `SuperUser(superadmin, 00000000000000000000000000000000)` in logs)
- `$.this` is not available since there is no "current object" - the method is not attached to an instance

The superuser object has only a name and ID, no additional attributes. If your task needs user-specific information, query for the relevant user objects explicitly rather than relying on `$.me`.

Since tasks run with full privileges, they bypass all permission checks. This is intentional - maintenance tasks typically need to access and modify data across the entire system. However, it also means you should be careful about what your scheduled tasks do.

## Creating a Scheduled Task

A scheduled task is simply a global schema method. Create it like any other method:

1. Open the Schema area
2. Select "Global Schema Methods"
3. Create a new method with a descriptive name (this name goes into `CronService.tasks`)
4. Write the method logic

The method runs without parameters and any return value is ignored. Use logging to track what the task does.

### Example: Cleanup Expired Sessions

This example deletes sessions that have been inactive for more than 24 hours:

```javascript
{
    let cutoff = $.date_add($.now, 'P-1D');
    let expiredSessions = $.find('Session', { lastActivity: $.predicate.lt(cutoff) });
    
    $.log('Cleanup: Found ' + $.size(expiredSessions) + ' expired sessions');
    
    for (let session of expiredSessions) {
        $.delete(session);
    }
    
    $.log('Cleanup: Deleted expired sessions');
}
```

### Example: Daily Summary

This example logs a daily summary of new registrations:

```javascript
{
    let yesterday = $.date_add($.now, 'P-1D');
    let newUsers = $.find('User', { createdDate: $.predicate.gte(yesterday) });
    
    $.log('Daily Summary: ' + $.size(newUsers) + ' new users registered in the last 24 hours');
    
    if ($.size(newUsers) > 0) {
        for (let user of newUsers) {
            $.log('  - ' + user.name + ' (' + user.eMail + ')');
        }
    }
}
```

## Testing

You can test a scheduled task before configuring it in the CronService. Since scheduled tasks are regular global schema methods, you can execute them manually using the Run button in the Schema area. This lets you verify that the method works correctly before scheduling it for automatic execution.

When testing, keep in mind that the manual execution also runs in a privileged context, so the behavior should be identical to scheduled execution.

## Logging and Debugging

The CronService does not automatically log when a task starts or completes. If you want to track executions, add logging statements to your method:

```javascript
{
    $.log('Starting scheduled task: cleanupExpiredSessions');
    
    // ... task logic ...
    
    $.log('Completed scheduled task: cleanupExpiredSessions');
}
```

Log output appears in the server log, which you can view in the Dashboard under "Server Log" or directly in the log file on the server.

### Identifying Cron Log Entries

Log entries from scheduled tasks show the thread name in brackets. Cron tasks run on threads named `Thread-NN`:

```
2026-02-02 08:00:00.123 [Thread-96] INFO  org.structr.core.script.Scripting - Starting scheduled task: cleanupExpiredSessions
```

This helps you distinguish scheduled task output from other log entries.

## Error Handling

When a scheduled task throws an exception, Structr logs the error and continues with the next scheduled execution. The failed task is not retried immediately - it simply runs again at the next scheduled time.

Error log entries look like this:

```
2026-02-02 11:41:10.664 [Thread-96] WARN  org.structr.core.script.Scripting - myCronJob[static]:myCronJob:2:8: TypeError: Cannot read property 'this' of undefined
2026-02-02 11:41:10.666 [Thread-96] WARN  org.structr.cron.CronService - Exception while executing cron task myCronJob: FrameworkException(422): Server-side scripting error (TypeError: Cannot read property 'this' of undefined)
```

The log shows both the script error (with line and column number) and the CronService wrapper exception. Use this information to debug failing tasks.

If you need more sophisticated error handling - such as sending notifications when tasks fail - implement it within the task itself using try-catch blocks.

## Parallel Execution

By default, Structr prevents a scheduled task from starting while a previous execution is still running. If a task is still running when its next scheduled time arrives, Structr logs a warning and skips that execution:

```
2026-02-02 11:45:10.664 [CronService] WARN  org.structr.cron.CronService - Prevented parallel execution of 'myCronJob' - if this happens regularly you should consider adjusting the cronExpression!
```

If you see this warning regularly, your task is taking longer than the interval between runs. You should either optimize the task to run faster, or increase the interval in the cron expression.

If your use case requires parallel execution, enable it in `structr.conf`:

```properties
cronservice.allowparallelexecution = true
```

Use this setting with caution. Parallel executions of the same task can lead to race conditions or duplicate processing if your method is not designed for it. For example, a cleanup task that deletes old records might process the same records twice if two instances run simultaneously.

## Cron Expression Syntax

A cron expression consists of six fields that specify when the task should run:

```
<seconds> <minutes> <hours> <day-of-month> <month> <day-of-week>
```

| Field | Allowed Values |
|-------|----------------|
| Seconds | 0–59 |
| Minutes | 0–59 |
| Hours | 0–23 |
| Day of month | 1–31 |
| Month | 1–12 |
| Day of week | 0–6 (0 = Sunday) |

Each field supports several notations:

| Notation | Meaning | Example |
|----------|---------|---------|
| `*` | Every possible value | `* * * * * *` runs every second |
| `x` | At the specific value | `0 30 * * * *` runs at minute 30 |
| `x-y` | Range from x to y | `0 0 9-17 * * *` runs hourly from 9 AM to 5 PM |
| `*/x` | Every multiple of x | `0 */15 * * * *` runs every 15 minutes |
| `x,y,z` | At specific values | `0 0 8,12,18 * * *` runs at 8 AM, noon, and 6 PM |

### Common Patterns

| Expression | Schedule |
|------------|----------|
| `0 0 * * * *` | Every hour at minute 0 |
| `0 */15 * * * *` | Every 15 minutes |
| `0 0 0 * * *` | Every day at midnight |
| `0 0 8 * * 1-5` | Every weekday at 8 AM |
| `0 0 3 * * 0` | Every Sunday at 3 AM |
| `0 0 0 1 * *` | First day of every month at midnight |

## Complete Configuration Example

This example configures three scheduled tasks: a cleanup that runs every hour, a daily report at 8 AM, and weekly maintenance on Sundays at 3 AM:

```properties
# Register tasks (global schema method names)
CronService.tasks = cleanupExpiredSessions dailyReport weeklyMaintenance

# Every hour
cleanupExpiredSessions.cronExpression = 0 0 * * * *

# Every day at 8 AM
dailyReport.cronExpression = 0 0 8 * * *

# Every Sunday at 3 AM
weeklyMaintenance.cronExpression = 0 0 3 * * 0
```

## Related Topics

- Business Logic - Creating global schema methods that scheduled tasks can execute
- Configuration - Managing Structr settings in structr.conf
- Logging - Viewing and configuring server logs
