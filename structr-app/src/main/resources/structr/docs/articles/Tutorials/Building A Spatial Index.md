# Building a Spatial Index

This tutorial shows how to build a quadtree-based spatial index for efficient point-in-polygon queries. When your application has many geometries, checking each one individually becomes slow. A spatial index narrows down candidates quickly by organizing geometries into a hierarchical grid.

## Prerequisites

This tutorial assumes you have:

- A working Structr instance with the `geo-transformations` module
- A `Geometry` type with a `wkt` property (see the Spatial Data article)
- Geometries imported into your database (e.g., from Shapefiles)

## How It Works

A quadtree divides space into four quadrants recursively. Each node in the tree represents a bounding box. When you search for geometries containing a point, the index quickly eliminates large portions of space that can't possibly contain the point, returning only a small set of candidate geometries to check.

## Creating the SpatialIndex Type

In the Schema area, create a `SpatialIndex` type with these properties:

| Property | Type | Description |
|----------|------|-------------|
| `x1` | Double | Left edge of bounding box |
| `y1` | Double | Bottom edge of bounding box |
| `x2` | Double | Right edge of bounding box |
| `y2` | Double | Top edge of bounding box |
| `level` | Integer | Depth in the quadtree (0 = root) |
| `isRoot` | Boolean | True for the root node |

Create these relationships:

| Relationship | Target Type | Cardinality | Description |
|--------------|-------------|-------------|-------------|
| `parent` | SpatialIndex | Many-to-One | Parent node |
| `children` | SpatialIndex | One-to-Many | Child quadrants |
| `geometries` | Geometry | Many-to-Many | Geometries in this cell |

## Schema Methods

Add the following methods to the `SpatialIndex` type.

### getGeometry

Returns the bounding box as a polygon geometry for intersection tests.

```javascript
{
    let points = [];
    points.push($.this.x1 + ' ' + $.this.y1);
    points.push($.this.x2 + ' ' + $.this.y1);
    points.push($.this.x2 + ' ' + $.this.y2);
    points.push($.this.x1 + ' ' + $.this.y2);
    points.push($.this.x1 + ' ' + $.this.y1);
    
    return $.wktToGeometry('POLYGON ((' + points.join(', ') + '))');
}
```

### createChildren

Creates four child quadrants. The `level` parameter controls recursion depth.

```javascript
{
    let level = $.retrieve('level');
    let halfWidth = ($.this.x2 - $.this.x1) / 2.0;
    let halfHeight = ($.this.y2 - $.this.y1) / 2.0;
    
    if (level <= 7) {  // Maximum depth
        let cx = $.this.x1 + halfWidth;
        let cy = $.this.y1 + halfHeight;
        
        // Create four quadrants
        $.getOrCreate('SpatialIndex', { parent: $.this, x1: $.this.x1, y1: $.this.y1, x2: cx, y2: cy, level: level });
        $.getOrCreate('SpatialIndex', { parent: $.this, x1: cx, y1: $.this.y1, x2: $.this.x2, y2: cy, level: level });
        $.getOrCreate('SpatialIndex', { parent: $.this, x1: cx, y1: cy, x2: $.this.x2, y2: $.this.y2, level: level });
        $.getOrCreate('SpatialIndex', { parent: $.this, x1: $.this.x1, y1: cy, x2: cx, y2: $.this.y2, level: level });
    }
}
```

The maximum depth of 7 creates cells roughly 0.06° × 0.08° at the deepest level (assuming a root covering Germany). Adjust this value based on your data density.

### storeGeometry

Adds a geometry to the index by finding all leaf nodes that intersect with it. It takes the geometry object and the Geometry entity as parameters.

```javascript
{
    let geometryNode = $.retrieve('geometryNode');
    let geometry = $.retrieve('geometry');
    let bbox = $.this.getGeometry();
    
    if (bbox.intersects(geometry)) {
        $.this.createChildren({ level: $.this.level + 1 });
        
        if ($.size($.this.children) > 0) {
            for (let child of $.this.children) {
                child.storeGeometry({ geometry: geometry, geometryNode: geometryNode });
            }
        } else {
            $.createRelationship($.this, geometryNode, 'GEOMETRY');
        }
    }
}
```

### getGeometriesForPolygon

Finds all geometries that might contain or intersect with a given polygon (or point).

```javascript
{
    let polygon = $.retrieve('polygon');
    let result = [];
    
    let search = function(root, level) {
        let bbox = root.getGeometry();
        if (bbox.intersects(polygon)) {
            let withinChildren = false;
            
            for (let child of root.children) {
                withinChildren |= search(child, level + 1);
            }
            
            if (!withinChildren) {
                result = $.mergeUnique(result, root.geometries);
            }
            return true;
        }
        return false;
    };
    
    search($.this, 0);
    return result;
}
```

## Building the Index

Create a global schema method or maintenance script to populate the index:

```javascript
{
    // Create or get the root node
    // Bounding box should cover your entire dataset
    let index = $.getOrCreate('SpatialIndex', {
        isRoot: true,
        x1: 47.0,   // Southern boundary (latitude)
        y1: 5.0,    // Western boundary (longitude)
        x2: 55.0,   // Northern boundary
        y2: 15.0,   // Eastern boundary
        level: 0
    });
    
    // Index all geometries
    let geometries = $.find('Geometry');
    let count = 0;
    
    for (let geom of geometries) {
        let geometry = $.wktToGeometry(geom.wkt);
        index.storeGeometry({ geometry: geometry, geometryNode: geom });
        count++;
        
        if (count % 100 === 0) {
            $.log('Indexed ' + count + ' geometries...');
        }
    }
    
    $.log('Indexing complete. Total: ' + count + ' geometries.');
}
```

For large datasets, run this as a scheduled task during off-peak hours.

## Querying the Index

Once built, use the index to find geometries efficiently:

```javascript
// Global schema method: findPolygon (parameters: latitude, longitude)
{
    let latitude = $.retrieve('latitude');
    let longitude = $.retrieve('longitude');
    
    $.assert(!$.empty(latitude), 422, 'Missing latitude parameter.');
    $.assert(!$.empty(longitude), 422, 'Missing longitude parameter.');
    
    let point = $.wktToGeometry('POINT(' + latitude + ' ' + longitude + ')');
    let index = $.first($.find('SpatialIndex', { isRoot: true }));
    
    $.assert(!$.empty(index), 400, 'Spatial index not found. Run the indexing script first.');
    
    // Get candidates from the index
    let candidates = index.getGeometriesForPolygon({ polygon: point });
    
    // Check each candidate for actual containment
    for (let geom of candidates) {
        let polygon = $.wktToGeometry(geom.wkt);
        if (polygon.contains(point)) {
            return {
                id: geom.id,
                name: geom.name,
                wkt: geom.wkt
            };
        }
    }
    
    return null;
}
```

## Performance Considerations

The root bounding box should tightly fit your data. A box that's too large wastes index levels on empty space.

Deeper trees create smaller cells, reducing candidates but increasing index size. For most datasets, a depth of 6-8 works well.

When you add new geometries, call `storeGeometry` on the root node. For bulk updates, consider rebuilding the index entirely.

Each index node is a database object. A depth-7 tree can have up to 21,845 nodes, though most will be pruned if your data doesn't fill the entire bounding box.

## Related Topics

- Spatial Data - Core geographic functionality
- Scheduled Tasks - Automating index builds
