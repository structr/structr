# Virtual Types

Virtual Types create custom REST endpoints that transform and filter data from existing types. They allow you to expose different views of your data without modifying your schema or duplicating data in the database.

## How Virtual Types Work

Each Virtual Type is based on a Source Type. When you request the Virtual Type's REST endpoint, Structr retrieves instances of the Source Type, applies an optional filter expression, and transforms each object according to your Virtual Property definitions. The transformed data is returned directly - nothing is stored in the database.

The REST endpoint is automatically created at:

```
/structr/rest/{VirtualTypeName}
```

For example, a Virtual Type named `PublicProject` creates an endpoint at `/structr/rest/PublicProject`.

## Accessing Virtual Types

You access Virtual Types through the REST API just like regular types:

**curl:**

```bash
curl http://localhost:8082/structr/rest/PublicProject \
  -H "X-User: admin" \
  -H "X-Password: admin"
```

**JavaScript:**

```javascript
const response = await fetch('/structr/rest/PublicProject', {
    headers: {
        'Content-Type': 'application/json'
    }
});

const data = await response.json();
```

The response has the same structure as any other REST endpoint:

```json
{
    "result": [
        {
            "id": "a3f8b2c1-d4e5-f6a7-b8c9-d0e1f2a3b4c5",
            "type": "PublicProject",
            "name": "Project Alpha",
            "status": "active",
            "managerName": "Jane Smith"
        }
    ],
    "result_count": 1,
    "page_count": 1,
    "query_time": "0.000127127",
    "serialization_time": "0.001092944"
}
```

Notice that the `type` field shows the Virtual Type name, and properties like `managerName` can be computed values that don't exist on the source type.

## Configuration

Virtual Types are configured in the Admin UI under the Virtual Types area. Each Virtual Type has:

### Source Type

The Source Type provides the data that the Virtual Type transforms. All instances of this type are candidates for inclusion in the Virtual Type's output.

### Filter Expression

The Filter Expression is a script that determines which source objects appear in the output. Only objects for which the expression returns true are included.

For example, to expose only active projects:

```javascript
$.this.status === 'active'
```

Or to expose only projects visible to the current user:

```javascript
$.this.visibleToAuthenticatedUsers === true
```

### Virtual Properties

Virtual Properties define which properties appear in the output and how they are transformed. Each Virtual Property has:

| Setting | Description |
|---------|-------------|
| Source Name | The property name on the Source Type |
| Target Name | The property name in the Virtual Type output |
| Input Function | Transforms data during input operations (e.g., CSV import) |
| Output Function | Transforms data during output operations (REST responses) |

If you only specify Source Name and Target Name, the property value is passed through unchanged. Use Output Functions when you need to transform or compute values.

## Output Functions

Output Functions transform property values when data is read through the REST endpoint. The function receives the source object as `$.this` and returns the transformed value.

### Renaming Properties

To expose a property under a different name, create a Virtual Property with the desired Target Name and a simple Output Function:

```javascript
$.this.internalProjectCode
```

This exposes the internal `internalProjectCode` property as whatever Target Name you specify.

### Computed Properties

Output Functions can compute values that don't exist on the source type:

**Full name from parts:**

```javascript
$.this.firstName + ' ' + $.this.lastName
```

**Age from birth date:**

```javascript
Math.floor((Date.now() - new Date($.this.birthDate).getTime()) / (365.25 * 24 * 60 * 60 * 1000))
```

**Related data:**

```javascript
$.this.manager ? $.this.manager.name : 'Unassigned'
```

### Formatting Values

Output Functions can format values for display:

**Date formatting:**

```javascript
$.date_format($.this.createdDate, 'yyyy-MM-dd')
```

**Currency formatting:**

```javascript
'$' + $.this.amount.toFixed(2)
```

## Input Functions

Input Functions transform data during write operations, primarily used for CSV import. When you import data through a Virtual Type, Input Functions convert the incoming values before they are stored.

For example, to parse a date string during import:

```javascript
$.parse_date($.input, 'MM/dd/yyyy')
```

The incoming value is available as `$.input`.

## Use Cases

### API Facades

Create simplified views for external consumers without exposing your internal data model. You can flatten nested structures, rename properties to match external specifications, or hide internal fields:

A Virtual Type `PublicProject` might expose only `name`, `status`, and `description` from a `Project` type that has many more internal properties.

### Filtered Endpoints

Create endpoints that return subsets of data without requiring query parameters:

- `ActiveProject` - only projects with status "active"
- `MyTasks` - only tasks assigned to the current user (using `$.me` in the filter)
- `RecentOrders` - only orders from the last 30 days

### Computed Views

Expose calculated values without storing them:

- Project progress percentage calculated from completed vs. total tasks
- User display names combined from first and last name
- Aggregated totals from related objects

### CSV Import Mapping

Virtual Types serve as the mapping layer for CSV imports. Each column maps to a Virtual Property, and Input Functions handle data conversion. For details, see the Data Creation & Import chapter.

## Limitations

Virtual Types transform data at read time. They do not support:

- Creating new objects (POST requests)
- Updating objects (PUT/PATCH requests)
- Deleting objects (DELETE requests)

For write operations, use the source type's REST endpoint directly.

## Related Topics

- Overview - REST API fundamentals and data format
- Data Access - Reading and writing data through the REST API
- Authentication - Securing REST endpoints with Resource Access Permissions
- Data Creation & Import - Using Virtual Types for CSV import transformations
- Admin User Interface / Virtual Types - The interface for creating and configuring Virtual Types
