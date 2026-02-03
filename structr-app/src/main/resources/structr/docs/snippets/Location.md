# Location

Stores geographic coordinates for distance-based queries and spatial operations. Key properties include `latitude` and `longitude` for WGS84 coordinates. The `withinDistance` predicate finds all locations within a specified radius in kilometers, enabling use cases like store locators or proximity searches. Distance queries work both in StructrScript with `$.find('Location', $.withinDistance(lat, lon, radius))` and via REST API using `_latlon` and `_distance` parameters.

## Geocoding and Geometry

Address-based parameters like `_country`, `_city`, and `_street` are automatically geocoded using configurable providers (Google Maps, Bing Maps, or OpenStreetMap) with automatic caching of up to 10,000 results. Any custom type with `latitude` and `longitude` properties can use distance-based queries, so you can either extend Location directly or add these properties to your own types like Store, Event, or Customer. For complex geographic data like polygons, boundaries, or routes, create custom types storing WKT (Well-Known Text) representations and use geometry functions for point-in-polygon queries, distance calculations, and coordinate transformations.
