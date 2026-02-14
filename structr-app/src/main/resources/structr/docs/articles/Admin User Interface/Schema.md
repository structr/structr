# Schema

The Schema area is the visual editor for designing your data model. Types appear as boxes on a canvas, and relationships appear as connecting lines between them. You can drag types to arrange them, click to edit their properties, and draw connections between types to create relationships.

![Schema Overview](/structr/docs/schema_type-created_Project.png)

## The Canvas

The main area displays your data model as a graph. Each type appears as a box showing the type name. Hover over a type to reveal the pencil icon (edit) and delete icon. Connection points at the top and bottom of each box let you create relationships by dragging from one type to another – drag from the source type's connector to the target type's connector, and Structr opens the relationship configuration dialog.

### Navigating Large Schemas

Use the mouse wheel to zoom in and out. Click and drag on empty canvas space to pan. For applications with many types, these controls help you focus on the part of the schema relevant to your current task.

### Relationship Colors

Relationship lines are color-coded:

- **Green** – Normal relationships
- **Orange** – Relationships configured for permission propagation (see User Management for details on graph-based permission resolution)

### Schema and Data Are Loosely Coupled

The schema and your data are loosely coupled. If you delete a type from the schema, the type definition and its relationships are removed, but the data objects of that type remain in the database. You can recreate the type later and the data becomes accessible again. This flexibility is useful during development but means you need to manage data cleanup separately from schema changes.

### Editing Types and Relationships

Click the pencil icon on a type box to open the Edit Type dialog. Click on a relationship line to open the Edit Relationship dialog. Both dialogs provide access to all configuration options – properties, methods, views, and more. For details on these options, see the Data Model chapter.

## Secondary Menu

The menu bar above the canvas provides tools for managing your schema.

### Create Type

The green button opens the Create Type dialog where you enter a name and select traits for the new type. After creation, the Edit Type dialog opens automatically so you can add properties and configure the type further.

### Snapshots

The Snapshots menu lets you save and restore schema states. A snapshot captures your entire schema definition at a point in time.

- **Create Snapshot** – Saves the current schema state with a name you provide
- **Restore Snapshot** – Replaces the current schema with a previously saved snapshot
- **Delete Snapshot** – Removes a saved snapshot

Snapshots are useful before making significant schema changes, allowing you to roll back if needed.

### User Defined Functions

Opens a table listing all global schema methods. This is a legacy location – the same methods are more conveniently accessible in the Code area under Global Methods.

### Display Menu

Controls the visual appearance of the schema editor.

#### Type Visibility

Opens a dialog where you show or hide types on the canvas. Types are grouped into categories: Custom Types, User/Group Types, File Types, HTML Types, Flow Types, Schema Types, and Other Types. Each type has a checkbox to toggle its visibility.

This is essential for focusing on specific parts of the schema. In a typical application, you work primarily with your custom types and rarely need to see the built-in HTML or Flow types.

#### Display Options

Two toggles control what information appears on the canvas:

- **Relationship Labels** – Shows or hides the relationship names on connecting lines
- **Inheritance Arrows** – Shows or hides arrows indicating trait inheritance

#### Edge Style

Controls how relationship lines are drawn: Flowchart, Bezier, State Machine, or Straight. Choose whatever makes your schema most readable – Flowchart works well for hierarchical schemas, while Straight lines are cleaner for simpler models.

#### Layouts

Schema layouts save the visual arrangement of types on the canvas. If you've organized a complex schema to make it readable, you can save that layout and restore it later. You can also export layouts to share with team members or import layouts they've created.

- **Save Current Layout** – Saves the current arrangement
- **Restore Layout** – Loads a previously saved layout
- **Export Layout** – Downloads the layout as a file
- **Import Layout** – Loads a layout from a file
- **Delete Layout** – Removes a saved layout

#### Reset Layout / Reset Zoom

Reset Layout returns all types to their default positions. Reset Zoom returns to the default zoom level.

#### Apply Automatic Layout

An experimental feature that arranges types on the canvas automatically. Results vary depending on schema complexity.

## Admin Menu

The Admin menu provides database maintenance functions.

### Indexing – Nodes

- **Rebuild Index** – Recreates indexes for all or selected node types. Run this after adding indexed properties to a type that already has data.
- **Add UUIDs** – Adds UUIDs to nodes that lack one. Use this when importing data from an external Neo4j database.
- **Create Labels** – Creates Neo4j labels based on the type property. Use this when importing data that has type values but is missing the corresponding labels.

### Indexing – Relationships

- **Rebuild Index** – Recreates indexes for relationships.
- **Add UUIDs** – Adds UUIDs to relationships imported from an external database.

### Rebuild All Indexes

Triggers a complete rebuild of all indexes for both nodes and relationships. Use this after importing data or when you suspect index inconsistencies.

### Maintenance

- **Flush Caches** – Clears internal caches. Rarely needed in current versions.
- **Clear Schema** – Removes all custom types and relationships from the schema. Use with extreme caution – this erases your entire data model definition (though not the data itself).

## Settings

The gear icon opens configuration options for the Schema area. These are the same settings available in the Dashboard under UI Settings, filtered to show only schema-relevant options.
