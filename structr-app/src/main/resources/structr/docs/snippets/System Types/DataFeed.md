
Fetches and stores content from RSS and Atom feeds. Structr periodically checks configured feeds and creates FeedItem objects for new entries. Key properties include url for the feed location, updateInterval for automatic refresh timing, lastUpdated for tracking, and maxItems and maxAge for limiting stored entries.

### Details
Feed items include title, author, publication date, content, and any enclosures like images or audio files. The updateIfDue() method checks whether enough time has passed since the last fetch and updates only when necessary. Use cleanUp() to remove old items based on your retention settings. You can extend DataFeed or FeedItem with custom properties and add an onCreate method on FeedItem to process new entries automatically – for example, sending notifications or importing content into your application.
