
Structr can connect to MongoDB databases using the `mongodb()` function. This function returns a MongoDB collection object that you can use to query, insert, update, and delete documents using the standard MongoDB Java driver API.

## When to Use MongoDB

MongoDB integration is useful when you need to:

- **Query document databases** that store data in flexible JSON-like structures
- **Integrate with existing MongoDB systems** without migrating data
- **Combine Structr's graph database** with MongoDB's document storage
- **Access analytics or logging data** stored in MongoDB

Unlike JDBC, MongoDB integration requires no driver installation - the MongoDB client library is included with Structr.

## The mongodb() Function

The `mongodb()` function connects to a MongoDB server and returns a collection object.

### Syntax

```javascript
$.mongodb(url, database, collection)
```

| Parameter | Description |
|-----------|-------------|
| `url` | MongoDB connection URL (e.g., `mongodb://localhost:27017`) |
| `database` | Database name |
| `collection` | Collection name |

### Return Value

The function returns a `MongoCollection` object. You can call MongoDB operations directly on this object, such as `find()`, `insertOne()`, `updateOne()`, `deleteOne()`, and others.

### The bson() Function

MongoDB queries and documents must be passed as BSON objects. Use the `$.bson()` function to convert JavaScript objects to BSON:

```javascript
$.bson({ name: 'John', status: 'active' })
```

## Reading Data

### Find All Documents

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'customers');
    let results = collection.find();
    
    for (let doc of results) {
        $.log('Customer: ' + doc.get('name'));
    }
}
```

> **Important:** Results from `find()` are not native JavaScript arrays. Use `for...of` to iterate - methods like `.filter()` or `.map()` are not available.

> **Important:** Documents in the result are not native JavaScript objects. Use `doc.get('fieldName')` instead of `doc.fieldName` to access properties.

### Find with Query

Filter documents using a BSON query:

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'customers');
    let results = collection.find($.bson({ status: 'active' }));
    
    for (let doc of results) {
        $.create('Customer', {
            mongoId: doc.get('_id').toString(),
            name: doc.get('name'),
            email: doc.get('email')
        });
    }
}
```

### Find with Query Operators

MongoDB query operators work as expected:

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'orders');
    
    // Find orders over $100
    let results = collection.find($.bson({ total: { $gt: 100 } }));
    
    for (let doc of results) {
        $.log('Order: ' + doc.get('orderId') + ' - $' + doc.get('total'));
    }
}
```

### Find with Regular Expressions

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'products');
    
    // Find products with names matching a pattern
    let results = collection.find($.bson({ name: { $regex: 'Test[0-9]' } }));
    
    for (let doc of results) {
        $.log('Product: ' + doc.get('name'));
    }
}
```

### Find with Date Comparisons

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'events');
    
    // Find events from 2024 onwards
    let results = collection.find($.bson({ 
        date: { $gte: new Date(2024, 0, 1) } 
    }));
    
    for (let doc of results) {
        $.log('Event: ' + doc.get('name') + ' on ' + doc.get('date'));
    }
}
```

### Query Operators

Common MongoDB query operators:

| Operator | Description | Example |
|----------|-------------|---------|
| `$eq` | Equal | `{ status: { $eq: 'active' } }` |
| `$ne` | Not equal | `{ status: { $ne: 'deleted' } }` |
| `$gt` | Greater than | `{ price: { $gt: 100 } }` |
| `$gte` | Greater than or equal | `{ price: { $gte: 100 } }` |
| `$lt` | Less than | `{ stock: { $lt: 10 } }` |
| `$lte` | Less than or equal | `{ stock: { $lte: 10 } }` |
| `$in` | In array | `{ status: { $in: ['active', 'pending'] } }` |
| `$regex` | Regular expression | `{ name: { $regex: '^Test' } }` |
| `$exists` | Field exists | `{ email: { $exists: true } }` |

For the full list of operators, see the [MongoDB Query Operators documentation](https://docs.mongodb.com/manual/reference/operator/).

## Writing Data

### Insert One Document

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'customers');
    
    collection.insertOne($.bson({
        name: 'John Doe',
        email: 'john@example.com',
        createdAt: new Date()
    }));
}
```

### Insert with Date Fields

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'events');
    
    collection.insertOne($.bson({
        name: 'Conference',
        date: new Date(2024, 6, 15),
        attendees: 100
    }));
}
```

## Updating Data

### Update One Document

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'customers');
    
    collection.updateOne(
        $.bson({ email: 'john@example.com' }),
        $.bson({ $set: { status: 'inactive' } })
    );
}
```

### Update Many Documents

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'orders');
    
    collection.updateMany(
        $.bson({ status: 'pending' }),
        $.bson({ $set: { status: 'cancelled', cancelledAt: new Date() } })
    );
}
```

## Deleting Data

### Delete One Document

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'customers');
    
    collection.deleteOne($.bson({ email: 'john@example.com' }));
}
```

### Delete Many Documents

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'logs');
    
    // Delete logs older than 30 days
    let cutoff = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
    collection.deleteMany($.bson({ timestamp: { $lt: cutoff } }));
}
```

## Examples

### Importing MongoDB Data into Structr

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'crm', 'contacts');
    let results = collection.find($.bson({ active: true }));
    
    let count = 0;
    for (let doc of results) {
        let mongoId = doc.get('_id').toString();
        
        // Check if already imported
        let existing = $.first($.find('Contact', 'mongoId', mongoId));
        
        if (!existing) {
            $.create('Contact', {
                mongoId: mongoId,
                name: doc.get('name'),
                email: doc.get('email'),
                phone: doc.get('phone'),
                importedAt: $.now
            });
            count++;
        }
    }
    
    $.log('Imported ' + count + ' new contacts');
}
```

### Insert and Query

```javascript
{
    let collection = $.mongodb('mongodb://localhost:27017', 'testDatabase', 'testCollection');
    
    // Insert a record
    collection.insertOne($.bson({
        name: 'Test4',
        createdAt: new Date()
    }));
    
    // Query all records with that name
    let results = collection.find($.bson({ name: 'Test4' }));
    
    for (let doc of results) {
        $.log('Found: ' + doc.get('name') + ' created at ' + doc.get('createdAt'));
    }
}
```

### Scheduled Sync

```javascript
// Global schema method: syncFromMongo
// Cron expression: 0 */15 * * * * (every 15 minutes)
{
    let collection = $.mongodb('mongodb://analytics.example.com:27017', 'events', 'pageviews');
    
    // Get last sync time
    let syncStatus = $.first($.find('SyncStatus', 'name', 'mongo_pageviews'));
    let since = syncStatus ? syncStatus.lastRun : new Date(0);
    
    let results = collection.find($.bson({ 
        timestamp: { $gt: since } 
    }));
    
    let count = 0;
    for (let doc of results) {
        $.create('PageView', {
            path: doc.get('path'),
            userId: doc.get('userId'),
            timestamp: doc.get('timestamp')
        });
        count++;
    }
    
    // Update sync status
    if (!syncStatus) {
        syncStatus = $.create('SyncStatus', { name: 'mongo_pageviews' });
    }
    syncStatus.lastRun = $.now;
    
    $.log('Synced ' + count + ' pageviews from MongoDB');
}
```

## Available Collection Methods

The returned collection object exposes all methods from the MongoDB Java Driver's `MongoCollection` class. Common methods include:

| Method | Description |
|--------|-------------|
| `find()` | Find all documents |
| `find(query)` | Find documents matching query |
| `insertOne(document)` | Insert one document |
| `insertMany(documents)` | Insert multiple documents |
| `updateOne(query, update)` | Update first matching document |
| `updateMany(query, update)` | Update all matching documents |
| `deleteOne(query)` | Delete first matching document |
| `deleteMany(query)` | Delete all matching documents |
| `countDocuments()` | Count all documents |
| `countDocuments(query)` | Count matching documents |

For the complete API, see the [MongoDB Java Driver documentation](https://mongodb.github.io/mongo-java-driver/4.2/apidocs/mongodb-driver-sync/com/mongodb/client/MongoCollection.html).

## Connection URL

The MongoDB connection URL follows the standard MongoDB connection string format:

```
mongodb://[username:password@]host[:port][/database][?options]
```

Examples:

| Scenario | URL |
|----------|-----|
| Local, default port | `mongodb://localhost:27017` |
| Local, short form | `mongodb://localhost` |
| With authentication | `mongodb://user:pass@localhost:27017` |
| Remote server | `mongodb://mongo.example.com:27017` |
| Replica set | `mongodb://host1:27017,host2:27017,host3:27017/?replicaSet=mySet` |

## Important Notes

### Results Are Not Native JavaScript

Results from `find()` behave differently than native JavaScript:

```javascript
// This does NOT work:
let results = collection.find();
let filtered = results.filter(d => d.status === 'active');  // Error!
let name = results[0].name;  // Error!

// This works:
for (let doc of results) {
    let name = doc.get('name');  // Use .get() for properties
}
```

### Always Use bson() for Queries

Pass all query and document objects through `$.bson()`:

```javascript
// This does NOT work:
collection.find({ name: 'John' });  // Error!

// This works:
collection.find($.bson({ name: 'John' }));
```

### Convert ObjectIds to Strings

MongoDB's `_id` field is an ObjectId. Convert it to a string when storing in Structr:

```javascript
let mongoId = doc.get('_id').toString();
```

## Error Handling

```javascript
{
    try {
        let collection = $.mongodb('mongodb://localhost:27017', 'mydb', 'customers');
        let results = collection.find();
        
        for (let doc of results) {
            $.create('Customer', { 
                name: doc.get('name') 
            });
        }
        
    } catch (e) {
        $.log('MongoDB error: ' + e.message);
    }
}
```

## Testing with Docker

To quickly set up a local MongoDB instance for testing:

```bash
docker run -d -p 27017:27017 mongo
```

This starts MongoDB on the default port, accessible at `mongodb://localhost:27017`.

## Related Topics

- JDBC - Connecting to SQL databases
- Scheduled Tasks - Running MongoDB operations on a schedule
- Business Logic - Processing imported data in schema methods
