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

#### Unable to Commit Transaction, Validation Failed

A create or update request returns status 422 with the message "Unable to commit transaction, validation failed". The `errors` array of the response names the type, the property and a token like `must_not_be_empty` or `already_taken` for each violated constraint. Fix the submitted data, or adjust the constraint in the schema if it is too strict.

#### 401 Access Denied for a REST Call

A REST request made by a non-admin user or an anonymous visitor returns status 401 with the message "Access denied". Structr grants access to REST resources only through Resource Access Permissions, and a call without a matching permission for the resource signature and the HTTP method is rejected even if the user may read the objects. The server log contains a line starting with "Found no resource access permission for" that names the signature and the method. Create the permission in the Security area with the flags the caller needs, and make sure the permission itself is visible to the caller.

#### Enum Values Are Rejected

Saving an Enum property fails with the message "Enum values must be separated by commas and can not contain spaces". The format field of an Enum property holds the allowed values as a comma-separated list, and a value with a space in it is rejected. Replace the spaces, for example with underscores, and save again.

#### Event Action Mappings Do Nothing

Clicking a button with an Event Action Mapping has no effect and no request appears in the browser's network tab. Event Action Mapping needs the Structr frontend library, which binds the configured events and sends the requests. Include `<script type="module" defer src="/structr/js/frontend/frontend.js"></script>` in the page, as described in the [Event Action Mapping](/structr/docs/ontology/Building%20Applications/Event%20Action%20Mapping) chapter.

#### Page Returns 404 Although It Exists

A page that is visible in the Pages area returns status 404 in the browser. Structr treats a page that the current user is not allowed to see as not found, so this happens when the visibility flags do not match the visitor. Check that the page is `visibleToPublicUsers` for anonymous visitors or `visibleToAuthenticatedUsers` for logged-in users, and that the page is not restricted to another site. If a page is configured to be shown on error code 404, Structr renders that page instead of the container's error page.
