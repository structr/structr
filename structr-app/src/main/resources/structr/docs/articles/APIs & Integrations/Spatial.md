
Structr provides support for geographic data. This includes a built-in `Location` type with distance-based queries, geocoding to convert addresses to coordinates, geometry processing for polygons and spatial analysis, and import capabilities for standard geospatial file formats.

> **Note:** The geometry functions require the `geo-transformations` module.

## The Location Type

Structr includes a built-in `Location` type for storing geographic coordinates. This type has two key properties:

| Property | Type | Description |
|----------|------|-------------|
| `latitude` | Double | Latitude coordinate (WGS84) |
| `longitude` | Double | Longitude coordinate (WGS84) |

### Creating Locations

Create Location objects like any other Structr type:

```javascript
{
    // Create a location for Frankfurt
    let frankfurt = $.create('Location', {
        name: 'Frankfurt Office',
        latitude: 50.1109,
        longitude: 8.6821
    });
}
```

You can also extend the Location type or add these properties to your own types. Any type with `latitude` and `longitude` properties can use distance-based queries.

### Distance-Based Queries

The `withinDistance` predicate finds objects within a specified radius of a point. The distance is measured in kilometers.

```javascript
{
    // Find all locations within 25 km of a point
    let nearbyLocations = $.find('Location', $.withinDistance(50.1109, 8.6821, 25));
    
    $.log('Found ' + $.size(nearbyLocations) + ' locations');
}
```

This works with any type that has `latitude` and `longitude` properties:

```javascript
{
    // Find stores within 10 km
    let nearbyStores = $.find('Store', $.withinDistance(customerLat, customerLon, 10));
    
    // Find events within 50 km
    let nearbyEvents = $.find('Event', $.withinDistance(userLat, userLon, 50));
}
```

### Distance Queries via REST API

The REST API supports distance-based queries using request parameters. Any type with `latitude` and `longitude` properties (typically by extending the built-in `Location` type) can be queried this way.

**Using coordinates directly:**

```bash
curl "http://localhost:8082/structr/rest/Hotel?_latlon=50.1167851,8.7265218&_distance=0.1"
```

The `_latlon` parameter specifies the search origin as `latitude,longitude`, and `_distance` specifies the search radius in kilometers.

**Using address components:**

```bash
curl "http://localhost:8082/structr/rest/Store?_country=Germany&_city=Frankfurt&_street=Hauptstraße&_distance=5"
```

**Using combined location string:**

```bash
curl "http://localhost:8082/structr/rest/Restaurant?_location=Germany,Berlin,Unter%20den%20Linden&_distance=2"
```

The `_location` parameter accepts the format `country,city,street`.

**Request Parameters for Distance Search:**

| Parameter | Description |
|-----------|-------------|
| `_latlon` | Search origin as `latitude,longitude` |
| `_distance` | Search radius in kilometers |
| `_location` | Search origin as `country,city,street` |
| `_country` | Country (used with other address fields) |
| `_city` | City (used with other address fields) |
| `_street` | Street (used with other address fields) |
| `_postalCode` | Postal code (used with other address fields) |

When using address-based parameters (`_location` or the individual fields), Structr geocodes the address using the configured provider and searches for objects within the specified radius. Geocoded addresses are cached to minimize API calls.

## Geocoding

Geocoding converts addresses into geographic coordinates. Structr uses geocoding automatically when you use the `distance` parameter in REST queries.

### Configuration

Configure geocoding in the Configuration Interface:

| Setting | Description |
|---------|-------------|
| `geocoding.provider` | Full class name of the provider |
| `geocoding.apikey` | API key (required for Google and Bing) |
| `geocoding.language` | Language for results (e.g., `en`, `de`) |

### Supported Providers

| Provider | Class Name | API Key |
|----------|------------|---------|
| Google Maps | `org.structr.common.geo.GoogleGeoCodingProvider` | Required |
| Bing Maps | `org.structr.common.geo.BingGeoCodingProvider` | Required |
| OpenStreetMap | `org.structr.common.geo.OSMGeoCodingProvider` | Not required |

### Caching

Geocoding results are automatically cached (up to 10,000 entries) to minimize API calls and improve performance. The cache persists for the lifetime of the Structr process.

## Working with Geometries

For more complex geographic data like polygons, boundaries, or routes, create a custom `Geometry` type that stores WKT (Well-Known Text) representations.

### Creating a Geometry Type

In the Schema area, create a type with these properties:

| Property | Type | Description |
|----------|------|-------------|
| `wkt` | String | WKT representation of the geometry |
| `name` | String | Name or identifier |

Add a schema method `getGeometry` to convert WKT to a geometry object:

```javascript
// Schema method: getGeometry
{
    return $.wktToGeometry($.this.wkt);
}
```

Add a method `contains` to check if a point is inside:

```javascript
// Schema method: contains (parameter: point)
{
    let point = $.retrieve('point');
    let geometry = $.this.getGeometry();
    let pointGeom = $.wktToGeometry('POINT(' + point.latitude + ' ' + point.longitude + ')');
    
    return geometry.contains(pointGeom);
}
```

### Creating Geometries

```javascript
{
    // Create a polygon
    let polygon = $.create('Geometry', {
        name: 'Delivery Zone A',
        wkt: 'POLYGON ((8.6 50.0, 8.8 50.0, 8.8 50.2, 8.6 50.2, 8.6 50.0))'
    });
    
    // Create a line
    let route = $.create('Geometry', {
        name: 'Route 1',
        wkt: 'LINESTRING (8.68 50.11, 8.69 50.12, 8.70 50.13)'
    });
}
```

### Point-in-Polygon Queries

Check if a point is inside a geometry:

```javascript
{
    let point = { latitude: 50.1, longitude: 8.7 };
    
    // Check against a single geometry
    let zone = $.first($.find('Geometry', 'name', 'Delivery Zone A'));
    if (zone.contains(point)) {
        $.log('Point is inside delivery zone');
    }
    
    // Find all geometries containing a point
    let geometries = $.find('Geometry');
    let matching = [];
    
    for (let geom of geometries) {
        if (geom.contains(point)) {
            matching.push(geom);
        }
    }
}
```

## Geometry Functions

Structr provides functions for creating, parsing, and analyzing geometries.

### Creating Geometries

| Function | Description |
|----------|-------------|
| `coordsToPoint(coord)` | Create Point from `[x, y]`, `{x, y}`, or `{latitude, longitude}` |
| `coordsToLineString(coords)` | Create LineString from array of coordinates |
| `coordsToPolygon(coords)` | Create Polygon from array of coordinates |
| `coordsToMultipoint(coords)` | Create MultiPoint from array of coordinates |

```javascript
{
    let point = $.coordsToPoint([8.6821, 50.1109]);
    let point2 = $.coordsToPoint({ latitude: 50.1109, longitude: 8.6821 });
    
    let line = $.coordsToLineString([[8.68, 50.11], [8.69, 50.12], [8.70, 50.13]]);
    
    let polygon = $.coordsToPolygon([
        [8.6, 50.0], [8.8, 50.0], [8.8, 50.2], [8.6, 50.2], [8.6, 50.0]
    ]);
}
```

### Parsing Geometries

| Function | Description |
|----------|-------------|
| `wktToGeometry(wkt)` | Parse WKT string to geometry |
| `wktToPolygons(wkt)` | Extract all polygons from WKT |

```javascript
{
    let point = $.wktToGeometry('POINT (8.6821 50.1109)');
    let polygon = $.wktToGeometry('POLYGON ((8.6 50.0, 8.8 50.0, 8.8 50.2, 8.6 50.2, 8.6 50.0))');
}
```

### Calculations

| Function | Description |
|----------|-------------|
| `distance(point1, point2)` | Geodetic distance in meters |
| `azimuth(point1, point2)` | Bearing in degrees |
| `getCoordinates(geometry)` | Extract coordinates as array |

```javascript
{
    let frankfurt = $.coordsToPoint([8.6821, 50.1109]);
    let berlin = $.coordsToPoint([13.405, 52.52]);
    
    let distanceMeters = $.distance(frankfurt, berlin);
    $.log('Distance: ' + (distanceMeters / 1000).toFixed(1) + ' km');
    
    let bearing = $.azimuth(frankfurt, berlin);
    $.log('Bearing: ' + bearing.toFixed(1) + '°');
}
```

### Coordinate Conversion

| Function | Description |
|----------|-------------|
| `latLonToUtm(lat, lon)` | Convert to UTM string |
| `utmToLatLon(utmString)` | Convert UTM to lat/lon object |
| `convertGeometry(srcCRS, dstCRS, geom)` | Transform coordinate system |

```javascript
{
    // Lat/Lon to UTM
    let utm = $.latLonToUtm(53.855, 8.0817);
    // Result: "32U 439596 5967780"
    
    // UTM to Lat/Lon
    let coords = $.utmToLatLon('32U 439596 5967780');
    // Result: { latitude: 53.855, longitude: 8.0817 }
    
    // Transform between coordinate systems
    let wgs84Point = $.wktToGeometry('POINT (8.6821 50.1109)');
    let utmPoint = $.convertGeometry('EPSG:4326', 'EPSG:32632', wgs84Point);
}
```

## File Import

### GPX Import

The `importGpx` function parses GPS track files:

```javascript
{
    let file = $.first($.find('File', 'name', 'track.gpx'));
    let gpxData = $.importGpx($.getContent(file, 'utf-8'));
    
    // Process waypoints
    if (gpxData.waypoints) {
        for (let wp of gpxData.waypoints) {
            $.create('Waypoint', {
                name: wp.name,
                latitude: wp.latitude,
                longitude: wp.longitude,
                altitude: wp.altitude
            });
        }
    }
    
    // Process tracks
    if (gpxData.tracks) {
        for (let track of gpxData.tracks) {
            let points = [];
            for (let segment of track.segments) {
                for (let point of segment.points) {
                    points.push([point.longitude, point.latitude]);
                }
            }
            
            $.create('Route', {
                name: track.name,
                wkt: $.coordsToLineString(points).toString()
            });
        }
    }
}
```

### Shapefile Import

The `readShapefile` function reads ESRI Shapefiles:

```javascript
{
    let result = $.readShapefile('/data/regions.shp');
    
    $.log('Fields: ' + result.fields.join(', '));
    
    for (let item of result.geometries) {
        $.create('Region', {
            name: item.metadata.NAME,
            wkt: item.wkt,
            population: item.metadata.POPULATION
        });
    }
}
```

The function automatically reads the associated `.dbf` file for attributes and `.prj` file for coordinate reference system, transforming coordinates to WGS84.

## Map Layers

For applications with multiple geometry sources (e.g., different Shapefiles), organize geometries into layers:

```javascript
{
    // Create a map layer
    let layer = $.create('MapLayer', {
        name: 'Administrative Boundaries',
        description: 'Country and state boundaries'
    });
    
    // Import shapefile into layer
    let result = $.readShapefile('/data/boundaries.shp');
    
    for (let item of result.geometries) {
        $.create('Geometry', {
            mapLayer: layer,
            name: item.metadata.NAME,
            wkt: item.wkt
        });
    }
}
```

## Examples

### Store Locator

```javascript
// Schema method on Store: findNearby (parameters: latitude, longitude, radiusKm)
{
    let lat = $.retrieve('latitude');
    let lon = $.retrieve('longitude');
    let radius = $.retrieve('radiusKm');
    
    let stores = $.find('Store', $.withinDistance(lat, lon, radius));
    let customerPoint = $.coordsToPoint([lon, lat]);
    
    let result = [];
    for (let store of stores) {
        let storePoint = $.coordsToPoint([store.longitude, store.latitude]);
        let dist = $.distance(customerPoint, storePoint);
        
        result.push({
            store: store,
            distanceKm: (dist / 1000).toFixed(1)
        });
    }
    
    // Sort by distance
    result.sort((a, b) => a.distanceKm - b.distanceKm);
    
    return result;
}
```

### Geofencing

```javascript
// Global schema method: checkDeliveryZone (parameters: latitude, longitude)
{
    let lat = $.retrieve('latitude');
    let lon = $.retrieve('longitude');
    let point = $.wktToGeometry('POINT(' + lat + ' ' + lon + ')');
    
    let zones = $.find('DeliveryZone');
    
    for (let zone of zones) {
        let polygon = $.wktToGeometry(zone.wkt);
        if (polygon.contains(point)) {
            return {
                inZone: true,
                zoneName: zone.name,
                deliveryFee: zone.deliveryFee
            };
        }
    }
    
    return { inZone: false };
}
```

## Related Topics

- Building a Spatial Index - Tutorial for optimizing point-in-polygon queries
- REST API - Distance queries with `_latlon`, `_distance`, and address parameters
- Schema - Creating custom types for geographic data
- Scheduled Tasks - Batch geocoding and index building
