# Functions

Structr provides a comprehensive library of built-in functions that can be used in template expressions, schema methods, and scripting contexts. These functions cover data manipulation, string processing, mathematical operations, date handling, and much more.

## Overview

Functions in Structr are available in different scripting contexts:

- **StructrScript**: Simple functional language for templates and expressions
- **JavaScript**: Full JavaScript environment with Structr bindings
- **Schema Methods**: Custom business logic implementation
- **Flow Elements**: Visual programming components

## Function Categories

### Data Query Functions

#### find()
Retrieve objects from the database.

```javascript
// Find all objects of a type
find('Project')

// Find with property filter
find('Project', 'status', 'ACTIVE')

// Find with multiple conditions
find('Project', and(
  equal('status', 'ACTIVE'),
  gte('priority', 5)
))
```

#### first()
Get the first element from a collection.

```javascript
// Get first project
first(find('Project'))

// Get first active project
first(find('Project', 'status', 'ACTIVE'))
```

#### get()
Retrieve a single object by ID.

```javascript
// Get specific object
get('123e4567-e89b-12d3-a456-426614174000')

// Get current user
get(me.id)
```

### Collection Functions

#### filter()
Filter collections based on conditions.

```javascript
// Filter high-priority projects
filter(find('Project'), gte(data.priority, 8))

// Filter by complex condition
filter(find('Task'), and(
  equal(data.status, 'TODO'),
  lt(data.dueDate, now())
))
```

#### sort()
Sort collections by property values.

```javascript
// Sort by name
sort(find('Project'), 'name')

// Sort by multiple criteria
sort(find('Project'), 'priority', 'name')
```

#### slice()
Extract a portion of a collection.

```javascript
// Get first 10 items
slice(find('Project'), 0, 10)

// Get items 11-20 (pagination)
slice(find('Project'), 10, 10)
```

#### size()
Get the size of a collection.

```javascript
// Count all projects
size(find('Project'))

// Count active tasks
size(find('Task', 'status', 'ACTIVE'))
```

#### extract()
Extract specific property values from collection items.

```javascript
// Get all project names
extract(find('Project'), 'name')

// Get all user emails
extract(find('User'), 'email')
```

### String Functions

#### concat()
Concatenate strings.

```javascript
// Simple concatenation
concat('Hello', ' ', 'World')

// Concatenate with variables
concat('Project: ', project.name)
```

#### join()
Join collection elements into a string.

```javascript
// Join names with comma
join(extract(find('User'), 'name'), ', ')

// Join with custom separator
join(['apple', 'banana', 'cherry'], ' | ')
```

#### capitalize()
Capitalize the first letter of a string.

```javascript
// Capitalize name
capitalize(user.name)
```

#### upper()
Convert string to uppercase.

```javascript
// Uppercase status
upper(task.status)
```

#### lower()
Convert string to lowercase.

```javascript
// Lowercase email
lower(user.email)
```

#### contains()
Check if string contains substring.

```javascript
// Check if title contains word
contains(lower(project.title), 'urgent')
```

### Mathematical Functions

#### add()
Add numbers.

```javascript
// Simple addition
add(5, 3)  // Returns 8

// Add multiple numbers
add(1, 2, 3, 4, 5)  // Returns 15
```

#### subtract()
Subtract numbers.

```javascript
// Simple subtraction
subtract(10, 3)  // Returns 7

// Calculate remaining budget
subtract(project.budget, project.spent)
```

#### multiply()
Multiply numbers.

```javascript
// Simple multiplication
multiply(5, 4)  // Returns 20

// Calculate total cost
multiply(item.quantity, item.price)
```

#### divide()
Divide numbers.

```javascript
// Simple division
divide(20, 4)  // Returns 5

// Calculate average
divide(sum(extract(tasks, 'hours')), size(tasks))
```

#### sum()
Sum all values in collection.

```javascript
// Sum numbers
sum([1, 2, 3, 4, 5])  // Returns 15

// Sum project costs
sum(extract(projects, 'cost'))
```

#### avg()
Calculate average of collection.

```javascript
// Average of numbers
avg([1, 2, 3, 4, 5])  // Returns 3

// Average project rating
avg(extract(projects, 'rating'))
```

### Date and Time Functions

#### now()
Get current date and time.

```javascript
// Current timestamp
now()

// Set creation date
set(this, 'createdDate', now())
```

#### date()
Format date for display.

```javascript
// Format with pattern
date(project.startDate, 'yyyy-MM-dd')

// Long format
date(now(), 'MMMM dd, yyyy')
```

#### days_between()
Calculate days between dates.

```javascript
// Project duration
days_between(project.startDate, project.endDate)

// Days until deadline
days_between(now(), task.dueDate)
```

### Logical Functions

#### and()
Logical AND operation.

```javascript
// Multiple conditions
and(
  equal(project.status, 'ACTIVE'),
  gte(project.priority, 5),
  not(empty(project.assignedTeam))
)
```

#### or()
Logical OR operation.

```javascript
// Alternative conditions
or(
  equal(user.role, 'ADMIN'),
  equal(user.role, 'MANAGER')
)

// Default value
or(user.displayName, user.email, 'Unknown User')
```

#### not()
Logical NOT operation.

```javascript
// Negate condition
not(empty(project.tasks))
```

#### if()
Conditional expression.

```javascript
// Simple conditional
if(gte(project.priority, 8), 'High Priority', 'Normal Priority')

// Nested conditionals
if(equal(task.status, 'COMPLETED'), 
   'Completed',
   if(equal(task.status, 'IN_PROGRESS'), 
      'In Progress', 
      'To Do'))
```

#### equal()
Test equality.

```javascript
// String equality
equal(user.status, 'ACTIVE')

// Number equality
equal(task.priority, 1)
```

#### lt(), lte(), gt(), gte()
Numerical comparisons.

```javascript
// Less than
lt(task.progress, 100)

// Greater than or equal
gte(team.size, minTeamSize)
```

### Utility Functions

#### coalesce()
Return first non-null value.

```javascript
// Provide fallback values
coalesce(user.displayName, user.firstName, user.email, 'Anonymous')
```

#### is_null()
Check if value is null.

```javascript
// Check for missing data
is_null(project.assignedUser)
```

#### to_json()
Convert object to JSON string.

```javascript
// Serialize for storage
to_json(configuration)

// Debug output
to_json(project)
```

#### log()
Write to log file.

```javascript
// Debug logging
log('Processing project: ', project.name)

// Error logging
log('ERROR: Failed to process user ', user.id)
```

#### error()
Throw error with message.

```javascript
// Validation error
if(lt(project.budget, 0), 
   error('Project', 'budget', 'Budget cannot be negative'))
```

## Function Chaining

Functions can be chained together for complex operations:

```javascript
// Complex data processing
join(
  extract(
    sort(
      filter(
        find('Project'), 
        equal(data.status, 'ACTIVE')
      ), 
      'priority'
    ), 
    'name'
  ), 
  ', '
)
```

## Context-Specific Functions

### Template Functions

#### render()
Render child elements.

```javascript
// In template elements
render(children)
```

#### include()
Include shared component.

```javascript
// Include navigation
include('Navigation')
```

### Schema Method Functions

#### set()
Set property value.

```javascript
// Set single property
set(this, 'lastModified', now())

// Set multiple properties
set(this, {
  'status': 'COMPLETED',
  'completedDate': now(),
  'completedBy': me
})
```

#### create()
Create new object.

```javascript
// Create related object
create('Task', {
  'title': 'New Task',
  'project': this,
  'createdBy': me
})
```

## Performance Considerations

### Efficient Queries

```javascript
// Good: Use indexed properties
find('User', 'email', 'user@example.com')

// Avoid: Filter after loading all
filter(find('User'), equal(data.email, 'user@example.com'))

// Good: Use paging
find('Project', page(1, 10))
```

## Custom Function Development

### Schema Methods

Create reusable functions as schema methods:

```javascript
// Method: calculateProjectProgress
let completedTasks = filter(this.tasks, equal(data.status, 'COMPLETED'));
let totalTasks = this.tasks;
if (empty(totalTasks)) return 0;
return round(multiply(divide(size(completedTasks), size(totalTasks)), 100));
```

### Global Schema Methods

Create utility functions available everywhere:

```javascript
// Global method: formatCurrency
let amount = arguments[0];
let currency = arguments[1] || 'USD';
return concat(currency, ' ', number(amount, '#,##0.00'));
```

## Next Steps

After mastering Structr functions:

1. **Advanced Scripting**: Learn complex business logic implementation
2. **Performance Optimization**: Optimize function usage for large datasets
3. **Custom Extensions**: Develop custom functions for specific needs
4. **Integration Patterns**: Use functions for external system integration
5. **Testing Strategies**: Develop testing approaches for function-heavy code

Structr's function library provides the building blocks for sophisticated data processing, business logic, and user interaction patterns in your applications.