
Structr can fetch and store content from RSS and Atom feeds. Create a `DataFeed` object with a feed URL, and Structr retrieves entries and stores them as `FeedItem` objects. You can configure retention limits and add custom processing logic when new items arrive.

## Quick Start

To subscribe to a feed:

```javascript
{
    let feed = $.create('DataFeed', {
        name: 'Tech News',
        url: 'https://example.com/feed.xml'
    });
}
```

When a DataFeed is created, Structr immediately fetches the feed and creates `FeedItem` objects for each entry. Access the items via the `items` property:

```javascript
{
    let feed = $.first($.find('DataFeed', 'name', 'Tech News'));
    
    for (let item of feed.items) {
        $.log(item.name + ' - ' + item.pubDate);
    }
}
```

## DataFeed Properties

| Property | Type | Description |
|----------|------|-------------|
| `url` | String | Feed URL (required) |
| `name` | String | Display name for the feed |
| `description` | String | Feed description (populated automatically from feed metadata) |
| `feedType` | String | Feed format (e.g., `rss_2.0`, `atom_1.0` - populated automatically) |
| `updateInterval` | Long | Milliseconds between updates (used by `updateIfDue()`) |
| `lastUpdated` | Date | Timestamp of the last successful fetch |
| `maxItems` | Integer | Maximum number of items to retain |
| `maxAge` | Long | Maximum age of items in milliseconds |
| `items` | List | Collection of FeedItem objects |

## FeedItem Properties

Each feed entry is stored as a `FeedItem` with these properties:

| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Entry title |
| `url` | String | Link to the original content |
| `author` | String | Author name |
| `description` | String | Entry summary or excerpt |
| `pubDate` | Date | Publication date |
| `updatedDate` | Date | Last modification date |
| `comments` | String | URL to comments |
| `contents` | List | Full content blocks (FeedItemContent objects) |
| `enclosures` | List | Attached media (FeedItemEnclosure objects) |
| `feed` | DataFeed | Reference to the parent feed |

## FeedItemContent Properties

Some feeds include full content in addition to the description. These are stored as `FeedItemContent` objects:

| Property | Type | Description |
|----------|------|-------------|
| `value` | String | The content text or HTML |
| `mode` | String | Content mode (e.g., `escaped`, `xml`) |
| `itemType` | String | MIME type of the content |
| `item` | FeedItem | Reference to the parent item |

## FeedItemEnclosure Properties

Feeds often include media attachments like images, audio files, or videos. These are stored as `FeedItemEnclosure` objects:

| Property | Type | Description |
|----------|------|-------------|
| `url` | String | URL to the media file |
| `enclosureType` | String | MIME type (e.g., `image/jpeg`, `audio/mpeg`) |
| `enclosureLength` | Long | File size in bytes |
| `item` | FeedItem | Reference to the parent item |

## Updating Feeds

### Manual Update

Trigger an immediate update with `updateFeed()`:

```javascript
{
    let feed = $.first($.find('DataFeed', 'name', 'News Feed'));
    feed.updateFeed();
}
```

### Conditional Update

The `updateIfDue()` method checks whether enough time has passed since `lastUpdated` based on `updateInterval`. If an update is due, it fetches new entries:

```javascript
{
    let feed = $.first($.find('DataFeed', 'name', 'News Feed'));
    feed.updateIfDue();
}
```

This is useful when called from a scheduled task that runs more frequently than individual feed intervals.

### Automatic Updates via CronService

Structr includes a built-in `UpdateFeedTask` that periodically checks all feeds. To enable it, configure the CronService in `structr.conf`:

```properties
#### Specifying the feed update task for the CronService
CronService.tasks = org.structr.feed.cron.UpdateFeedTask

#### Setting up the execution interval in cron time format
# In this example the web feed will be updated every 5 minutes
org.structr.feed.cron.UpdateFeedTask.cronExpression = 5 * * * * *
```

After changing the configuration:

1. Stop the Structr instance
2. Edit `structr.conf` with the settings above
3. Restart the instance

The UpdateFeedTask calls `updateIfDue()` on each DataFeed. Configure `updateInterval` on individual feeds to control how often they actually fetch new content:

```javascript
{
    $.create('DataFeed', {
        name: 'Hourly News',
        url: 'https://example.com/news.xml',
        updateInterval: 3600000  // Only fetch if last update was more than 1 hour ago
    });
}
```

Even if the CronService runs every 5 minutes, a feed with `updateInterval` set to one hour will only fetch when at least one hour has passed since `lastUpdated`.

## Retention Control

By default, Structr keeps all feed items indefinitely. Use `maxItems` and `maxAge` to automatically remove old entries. Cleanup runs automatically after each feed update.

### Limiting by Count

Keep only the most recent entries:

```javascript
{
    $.create('DataFeed', {
        name: 'Headlines',
        url: 'https://example.com/headlines.xml',
        maxItems: 50  // Keep only the 50 most recent items
    });
}
```

### Limiting by Age

Remove entries older than a specified duration:

```javascript
{
    $.create('DataFeed', {
        name: 'Daily Digest',
        url: 'https://example.com/daily.xml',
        maxAge: 604800000  // Keep items for 7 days (7 * 24 * 60 * 60 * 1000)
    });
}
```

### Manual Cleanup

You can also trigger cleanup manually:

```javascript
{
    let feed = $.first($.find('DataFeed', 'name', 'Active Feed'));
    feed.cleanUp();
}
```

## DataFeed Methods

| Method | Description |
|--------|-------------|
| `updateFeed()` | Fetches new entries from the remote feed URL and runs cleanup afterward |
| `updateIfDue()` | Checks if an update is due based on `lastUpdated` and `updateInterval`, and fetches new items if necessary |
| `cleanUp()` | Removes old feed items based on the configured `maxItems` and `maxAge` properties |

## Processing New Items

To automatically process incoming feed items, add an `onCreate` method to the `FeedItem` type. This is useful for setting visibility, creating notifications, or triggering other actions.

### Making Items Visible

By default, newly created FeedItem objects are not visible to public or authenticated users. Set the visibility flags in the `onCreate` method:

```javascript
// onCreate method on FeedItem
{
    $.this.visibleToPublicUsers = true;
    $.this.visibleToAuthenticatedUsers = true;
}
```

### Custom Processing

You can extend the `onCreate` method with additional logic:

```javascript
// onCreate method on FeedItem
{
    // Make visible
    $.this.visibleToPublicUsers = true;
    $.this.visibleToAuthenticatedUsers = true;
    
    // Create a notification for items from a specific feed
    if ($.this.feed.name === 'Critical Alerts') {
        $.create('Notification', {
            title: 'Alert: ' + $.this.name,
            message: $.this.description,
            sourceUrl: $.this.url
        });
    }
}
```

## Examples

### News Aggregator

Collect news from multiple sources:

```javascript
{
    let sources = [
        { name: 'Tech News', url: 'https://technews.example.com/feed.xml' },
        { name: 'Business', url: 'https://business.example.com/rss' },
        { name: 'Science', url: 'https://science.example.com/atom.xml' }
    ];
    
    for (let source of sources) {
        $.create('DataFeed', {
            name: source.name,
            url: source.url,
            updateInterval: 1800000,  // 30 minutes
            maxItems: 100
        });
    }
}
```

### Finding Podcast Episodes

Extract audio files from a podcast feed:

```javascript
{
    let feed = $.first($.find('DataFeed', 'name', 'My Podcast'));
    
    let episodes = [];
    for (let item of feed.items) {
        let audioEnclosure = null;
        
        for (let enc of item.enclosures) {
            if (enc.enclosureType === 'audio/mpeg') {
                audioEnclosure = enc;
                break;
            }
        }
        
        episodes.push({
            title: item.name,
            published: item.pubDate,
            description: item.description,
            audioUrl: audioEnclosure ? audioEnclosure.url : null,
            fileSize: audioEnclosure ? audioEnclosure.enclosureLength : null
        });
    }
    
    return episodes;
}
```

### Recent Items Across All Feeds

Get items from the last 24 hours across all feeds:

```javascript
{
    let yesterday = new Date(Date.now() - 86400000);
    let feeds = $.find('DataFeed');
    let recentItems = [];
    
    for (let feed of feeds) {
        for (let item of feed.items) {
            if (item.pubDate && item.pubDate.getTime() > yesterday.getTime()) {
                recentItems.push({
                    feedName: feed.name,
                    title: item.name,
                    url: item.url,
                    published: item.pubDate
                });
            }
        }
    }
    
    return recentItems;
}
```

## Duplicate Detection

Structr detects duplicate entries using the item's URL. When fetching a feed, items with URLs that already exist in the feed's item list are skipped. This prevents duplicate entries even if the feed is fetched multiple times.

## Supported Feed Formats

Structr uses the ROME library to parse feeds and supports:

- RSS 0.90, 0.91, 0.92, 0.93, 0.94, 1.0, 2.0
- Atom 0.3, 1.0

The feed format is detected automatically and stored in the `feedType` property.

## Related Topics

- Scheduled Tasks - Running feed updates on a schedule
- Business Logic - Processing feed items in lifecycle methods
