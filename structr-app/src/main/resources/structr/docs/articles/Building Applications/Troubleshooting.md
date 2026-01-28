# Troubleshooting

When something doesn't work as expected, Structr provides several tools to help you identify and resolve the issue. This chapter covers common problems and how to diagnose them.

## Server Log

The server log is your primary tool for diagnosing problems. You can view it in the Dashboard under "Server Log", or directly in the log file on the server.

### Enable Query Logging

If you need to see exactly what database queries Structr is executing, enable query logging in the configuration:
```
log.cypher.debug = true
```

After saving this setting, all Cypher queries are written to the server log. This is useful when you suspect a query is returning unexpected results or causing performance issues. Remember to disable it again after debugging – query logging generates a lot of output.

## Error Messages

When an error occurs, Structr returns an HTTP status code and an error response object:
```json
{
    "code": 422,
    "message": "Unable to commit transaction, validation failed",
    "errors": [
        {
            "type": "Project",
            "property": "name",
            "token": "must_not_be_empty"
        }
    ]
}
```

### Common Status Codes

| Code | Meaning |
| --- | --- |
| 401 | Not authenticated – user needs to log in |
| 403 | Forbidden – user lacks permission for this operation |
| 404 | Not found – object or endpoint doesn't exist |
| 422 | Validation failed – data doesn't meet schema constraints |
| 500 | Server error – check the server log for details |

## Common Problems

### "Access Denied" for Logged-In Users

*[Placeholder – Resource Access Permissions, Visibility Flags]*

### Page Shows No Data

*[Placeholder – Repeater configuration, Function Query syntax, Permissions on data]*

### Lifecycle Method Not Executing

*[Placeholder – Method naming, transaction boundaries]*

### Schema Changes Not Taking Effect

*[Placeholder – Schema recompilation, browser cache]*