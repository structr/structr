# Structr UI Features

Structr UI is a frontend for Neo4j-based projects. It can be used either stand-alone as a Content Management system, or in combination with a custom backend.

All content elements are stored in a graph structure and rendered top-down from a page node, including dynamic content.

## Schema

- Content Management-specific entities
    - Page
    - HTML5 DOM elements
    - Content (text)
    - Folders
    - Files
    - Images
    - Blog posts
    - News items
- Server-side implementation of W3C DOM API

## Dynamic Data Integration
- HTML markup creation from dynamic data
- Iterate over query results
- Query types:
    - REST
    - Cypher
    - XPath
- Remote Cypher queries (experimental)
    
## Websocket Interface
- Real-time interaction
- Event broadcasting to multiple clients

## Page Editor
- Create pages
- Add DOM elements
- Shared components (elements used on multiple pages)

## CRUD UI
- Create, read, update, delete
- Dynamic

## OAuth Integration
- OAuth 2.0
    - Facebook
    - LinkedIn
    - GitHub
    - Google
- OAuth 1.0a
    - Twitter
- Extensible OAuth clients API

## Dynamic Edit Mode
- Auto-generated edit UI from specific node attributes
- Query limiting to single result (detail mode)
- Hide elements in edit/non-edit mode
- Auto-refresh pages after action completion

## Importer
- Import complete web pages
- Import HTML code
- Downloads all linked media
    - CSS
    - JavaScript
    - Images
- Creates all necessary elements in the database

## Widgets
- Create page structure by drag and drop
- Exchange widget code
- Editable widget source code

## Infrastructure
- Integrated web server
- URL rewriting
- Caching
  - Time-based (fixed expiration time)
  - Expiry-based (re-render only if page has changed)
  - Timestamp-based (sends 304)
- Proper HTTP error codes and expiration headers
