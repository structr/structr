### Structr UI Features

Structr UI is a frontend for Neo4j-based projects. It can be used either stand-alone as a Content Management system, or in combination with a custom backend.

All content elements are stored in a graph structure and rendered top-down from a page node, including dynamic content.

#### Page Editor
- Modify pages by drag and drop
- In-page content editing
- Shared components (elements used on multiple pages)
- Add images and files (script, CSS) by Drag'n'Drop
- Collaborative editing
- Content editor (CodeMirror)
- Output escaping for
    - text/plain
    - text/css
    - text/javascript
    - text/html
- Output converter for markup languages:
    - Markdown
    - Textile
    - Mediawiki
    - Tracwiki
    - Confluence

#### Widgets
- Create page structure by Drag'n'Drop
- Exchange widget code
- Editable widget source code

#### CRUD UI
- Create, read, update, delete
- Dynamic frontend creation based on schema definition
- CSV export

#### OAuth Integration
- OAuth 2.0 support, with pre-defined clients for
    - Facebook,
    - LinkedIn,
    - GitHub,
    - Google
- OAuth 1.0a support for
    - Twitter (using Twitter4j)
- Extensible OAuth client API (using Apache Oltu)

#### Dynamic Data Integration
- HTML markup creation from database results
- Iterate over query results to 
- Data query types:
    - REST
    - Cypher
    - XPath
- Remote Cypher queries (experimental)
- Access to query results by `${object.attr}` notation
- Access to page context and data objects:
    - request: Request parameters
    - size: Size of a collection
    - link: Linked node
    - now: Current date
    - me: Currently logged-in user
    - this: Current object (in results loop)
    - page: Current page
    - parent: Parent node
    - owner: Owner
- Paging context:
    - result_size: Overall result size (unfiltered)
    - page_size: Page size
    - page_count: Number of pages
    - page_no: Current page number
- Built-in functions for content output:
    - md5
    - upper, lower, abbr, capitalize, titleize
    - urlencode
    - if, not, and
    - empty, equal
    - add, subt, mult, quot, round
    - min, max
    - date_format, number_format
    - GET
- Content syndication (display content from remote URLs)
    
#### Dynamic Edit Mode
- Auto-generated edit UI from specific node attributes
- Query limiting to single result (detail mode)
- Hide elements in edit/non-edit mode
- Auto-refresh pages after action

#### Schema
- All content objects are nodes in a Neo4j graph database
- Page elements connected by relationships
- Server-side implementation of W3C DOM API
- Content Management-specific entities
    - Pages
    - HTML5 DOM elements
    - Content (text)
    - Folders, Files, Images
    - Blog Posts, Comments
    - News Items
    - Mail Templates
    - Tags

#### Importer
- Import complete web pages
- Import HTML code
- Downloads all linked media
    - CSS
    - JavaScript
    - Images
- Creates all necessary elements in the database

#### Image Handling
- Base64 importer (POST base64 string to create image)
- Background thumbnail auto-generation
- Dynamic thumbnail properties
    - Define dimensions
    - Crop factor
- Image upload by Drag'n'Drop

#### Websocket Interface
- Real-time interaction
- Event broadcasting to multiple clients
- Real-time partial page update (experimental)

#### Infrastructure
- Integrated web server
- URL rewriting (UrlRewrite)
- Caching
    - Time-based (fixed expiration time)
    - Expiry-based (re-render only if page has changed)
    - Timestamp-based (sends 304)
- Proper HTTP error codes and expiration headers
