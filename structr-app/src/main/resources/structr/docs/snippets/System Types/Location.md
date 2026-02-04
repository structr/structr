# Location

Stores geographic coordinates for spatial queries. Key properties are `latitude` and `longitude` (WGS84). The `withinDistance` predicate finds all locations within a radius – useful for store locators or proximity searches. This works both in StructrScript and via REST API with `_latlon` and `_distance` parameters.

## Details

Address-based queries are automatically geocoded using Google Maps, Bing, or OpenStreetMap, with results cached for performance. Any type with `latitude` and `longitude` properties can use distance queries – extend Location directly or add these properties to your own types like Store or Event. For polygons and complex geometries, use WKT representations with Structr's geometry functions.
