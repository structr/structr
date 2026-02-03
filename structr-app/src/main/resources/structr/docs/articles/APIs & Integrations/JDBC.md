
Structr can query external SQL databases directly using the `jdbc()` function. This allows you to import data from MySQL, PostgreSQL, Oracle, SQL Server, or any other database with a JDBC driver, without setting up intermediate services or ETL pipelines.

## When to Use JDBC

JDBC integration is useful when you need to:

- **Import data** from existing SQL databases into Structr
- **Synchronize data** between Structr and legacy systems
- **Query external databases** without migrating data
- **Build dashboards** that combine Structr data with external sources

For ongoing synchronization, combine JDBC queries with scheduled tasks. For one-time imports, run the query manually or through a schema method.

## Prerequisites

JDBC drivers are not included with Structr. Before using the `jdbc()` function, you must install the appropriate driver for your database.

### Installing a JDBC Driver

1. Download the JDBC driver JAR for your database:
   - MySQL: [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)
   - PostgreSQL: [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/download/)
   - SQL Server: [Microsoft JDBC Driver](https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server)
   - Oracle: [Oracle JDBC Driver](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html)

2. Copy the JAR file to Structr's `lib` directory:

```bash
cp mysql-connector-java-8.0.33.jar /opt/structr/lib/
```

3. Restart Structr to load the driver

## The jdbc() Function

The `jdbc()` function executes an SQL statement against an external database and returns any results.

### Syntax

```javascript
$.jdbc(url, query)
$.jdbc(url, query, username, password)
```

| Parameter | Description |
|-----------|-------------|
| `url` | JDBC connection URL including host, port, and database name |
| `query` | SQL statement to execute |
| `username` | Optional: Database username (can also be included in URL) |
| `password` | Optional: Database password (can also be included in URL) |

### Return Value

For `SELECT` statements, the function returns an array of objects. Each object represents a row, with properties matching the column names.

```javascript
[
    { id: 1, name: "Alice", email: "alice@example.com" },
    { id: 2, name: "Bob", email: "bob@example.com" }
]
```

For `INSERT`, `UPDATE`, and `DELETE` statements, the function executes the statement but returns an empty result.

## Connection URLs

JDBC connection URLs follow a standard format but vary slightly by database:

| Database | URL Format |
|----------|------------|
| MySQL | `jdbc:mysql://host:3306/database` |
| PostgreSQL | `jdbc:postgresql://host:5432/database` |
| SQL Server | `jdbc:sqlserver://host:1433;databaseName=database` |
| Oracle | `jdbc:oracle:thin:@host:1521:database` |
| MariaDB | `jdbc:mariadb://host:3306/database` |

### Authentication

You can provide credentials either as separate parameters or in the URL:

```javascript
// Credentials as parameters (recommended)
let result = $.jdbc("jdbc:mysql://localhost:3306/mydb", "SELECT * FROM users", "admin", "secret");

// Credentials in URL
let result = $.jdbc("jdbc:mysql://localhost:3306/mydb?user=admin&password=secret", "SELECT * FROM users");
```

## Examples

### Importing from MySQL

```javascript
{
    let url = "jdbc:mysql://localhost:3306/legacy_crm";
    let query = "SELECT id, name, email, created_at FROM customers WHERE active = 1";
    
    let rows = $.jdbc(url, query, "reader", "secret");
    
    for (let row of rows) {
        $.create('Customer', {
            externalId: row.id,
            name: row.name,
            eMail: row.email,
            importedAt: $.now
        });
    }
    
    $.log('Imported ' + $.size(rows) + ' customers');
}
```

### Querying PostgreSQL

```javascript
{
    let url = "jdbc:postgresql://db.example.com:5432/analytics";
    let query = "SELECT product_id, SUM(quantity) as total FROM orders GROUP BY product_id";
    
    let rows = $.jdbc(url, query, "readonly", "secret");
    
    for (let row of rows) {
        let product = $.first($.find('Product', 'externalId', row.product_id));
        if (product) {
            product.totalOrders = row.total;
        }
    }
}
```

### Querying SQL Server

```javascript
{
    let url = "jdbc:sqlserver://sqlserver.example.com:1433;databaseName=inventory";
    let query = "SELECT sku, stock_level, warehouse FROM inventory WHERE stock_level < 10";
    
    let rows = $.jdbc(url, query, "reader", "secret");
    
    // Process low-stock items
    for (let row of rows) {
        $.create('LowStockAlert', {
            sku: row.sku,
            currentStock: row.stock_level,
            warehouse: row.warehouse,
            alertDate: $.now
        });
    }
}
```

### Writing to External Databases

The `jdbc()` function can also execute `INSERT`, `UPDATE`, and `DELETE` statements:

```javascript
{
    let url = "jdbc:mysql://localhost:3306/external_system";
    
    // Insert a record
    $.jdbc(url, "INSERT INTO sync_log (source, timestamp, status) VALUES ('structr', NOW(), 'completed')", "writer", "secret");
    
    // Update records
    $.jdbc(url, "UPDATE orders SET synced = 1 WHERE synced = 0", "writer", "secret");
    
    // Delete old records
    $.jdbc(url, "DELETE FROM temp_data WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)", "writer", "secret");
}
```

Write operations execute successfully but don't return affected row counts. If you need confirmation, query the data afterward or use database-specific techniques like `SELECT LAST_INSERT_ID()`.

### Scheduled Synchronization

Combine JDBC with scheduled tasks for regular data synchronization:

```javascript
// Global schema method: syncExternalOrders
// Cron expression: 0 */15 * * * * (every 15 minutes)
{
    let lastSync = $.first($.find('SyncStatus', 'name', 'orders'));
    let since = lastSync ? lastSync.lastRun : '1970-01-01';
    
    let query = "SELECT * FROM orders WHERE updated_at > '" + since + "' ORDER BY updated_at";
    let rows = $.jdbc("jdbc:mysql://orders.example.com:3306/shop", query, "sync", "secret");
    
    for (let row of rows) {
        let existing = $.first($.find('Order', 'externalId', row.id));
        
        if (existing) {
            existing.status = row.status;
            existing.updatedAt = $.now;
        } else {
            $.create('Order', {
                externalId: row.id,
                customerEmail: row.customer_email,
                total: row.total,
                status: row.status
            });
        }
    }
    
    // Update sync timestamp
    if (!lastSync) {
        lastSync = $.create('SyncStatus', { name: 'orders' });
    }
    lastSync.lastRun = $.now;
    
    $.log('Synced ' + $.size(rows) + ' orders');
}
```

## Supported Databases

JDBC drivers are loaded automatically based on the connection URL (JDBC 4.0 auto-discovery). The following databases are commonly used with Structr:

| Database | Driver JAR | Example URL |
|----------|-----------|-------------|
| MySQL | mysql-connector-java-x.x.x.jar | `jdbc:mysql://host:3306/db` |
| PostgreSQL | postgresql-x.x.x.jar | `jdbc:postgresql://host:5432/db` |
| SQL Server | mssql-jdbc-x.x.x.jar | `jdbc:sqlserver://host:1433;databaseName=db` |
| Oracle | ojdbc8.jar | `jdbc:oracle:thin:@host:1521:sid` |
| MariaDB | mariadb-java-client-x.x.x.jar | `jdbc:mariadb://host:3306/db` |
| H2 | h2-x.x.x.jar | `jdbc:h2:~/dbfile` |
| SQLite | sqlite-jdbc-x.x.x.jar | `jdbc:sqlite:/path/to/db.sqlite` |

## Error Handling

Wrap JDBC calls in try-catch blocks to handle connection failures and query errors:

```javascript
{
    try {
        let rows = $.jdbc("jdbc:mysql://localhost:3306/mydb", "SELECT * FROM customers", "admin", "secret");
        
        // Process results
        for (let row of rows) {
            $.create('Customer', { name: row.name });
        }
        
    } catch (e) {
        $.log('JDBC error: ' + e.message);
        
        // Optionally notify administrators
        $.sendPlaintextMail(
            'alerts@example.com', 'System',
            'admin@example.com', 'Admin',
            'JDBC Import Failed',
            'Error: ' + e.message
        );
    }
}
```

Common errors:

| Error | Cause |
|-------|-------|
| `No suitable JDBC driver found` | JDBC driver JAR not in `lib` directory, restart Structr after adding |
| `Access denied` | Invalid username or password |
| `Unknown database` | Database name incorrect or doesn't exist |
| `Connection refused` | Database server not reachable (check host, port, firewall) |

## Best Practices

### Use Appropriate Credentials

For read-only operations, create a dedicated database user with minimal permissions:

```sql
-- MySQL example: read-only user
CREATE USER 'structr_reader'@'%' IDENTIFIED BY 'password';
GRANT SELECT ON legacy_db.* TO 'structr_reader'@'%';
```

For write operations, grant only the necessary permissions:

```sql
-- MySQL example: limited write access
CREATE USER 'structr_sync'@'%' IDENTIFIED BY 'password';
GRANT SELECT, INSERT, UPDATE ON external_db.sync_log TO 'structr_sync'@'%';
```

### Limit Result Sets

For large tables, use `LIMIT` or `WHERE` clauses to avoid memory issues:

```javascript
// Bad: fetches entire table
let rows = $.jdbc(url, "SELECT * FROM orders", user, pass);

// Good: fetches only what you need
let rows = $.jdbc(url, "SELECT * FROM orders WHERE created_at > '2024-01-01' LIMIT 1000", user, pass);
```

### Store Connection Details Securely

Don't hardcode credentials in your scripts. Use a dedicated configuration type:

```javascript
{
    let config = $.first($.find('JdbcConfig', 'name', 'legacy_crm'));
    let url = "jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database;
    
    let rows = $.jdbc(url, "SELECT * FROM customers", config.username, config.password);
    // ...
}
```

### Handle Column Name Differences

Map external column names to Structr property names explicitly:

```javascript
for (let row of rows) {
    $.create('Customer', {
        name: row.customer_name,      // External: customer_name → Structr: name
        eMail: row.email_address,     // External: email_address → Structr: eMail
        phone: row.phone_number       // External: phone_number → Structr: phone
    });
}
```

## Limitations

- Large result sets are loaded entirely into memory. For very large imports, paginate with `LIMIT` and `OFFSET`.
- Connection pooling is not supported. Each call opens a new connection. For high-frequency queries, consider caching results.
- Write operations (`INSERT`, `UPDATE`, `DELETE`) execute successfully but don't return affected row counts.

## Related Topics

- Scheduled Tasks - Running JDBC imports on a schedule
- Data Creation & Import - Other import methods including CSV and REST
- Business Logic - Processing imported data in schema methods
