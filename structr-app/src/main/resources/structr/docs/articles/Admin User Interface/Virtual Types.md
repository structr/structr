
The Virtual Types area is where you create and configure virtual types, declarative mappings that transform objects of a schema type into a different shape without modifying the underlying schema. You can rename properties, filter objects and apply transformation functions in both directions. Virtual types are consumed by the ODS and ODT exporters of the ODF module and, as a temporary mapping the importer creates itself, by the CSV Import Wizard; the REST interface does not serve them as endpoints. By default, this area is hidden in the burger menu.

![Virtual Types](/structr/docs/virtual-types.png)

Note: This area appears empty until you create your first virtual type.

## Secondary Menu

### Create Virtual Type

On the left, two input fields let you enter the Name and Source Type for a new virtual type. Both fields are required. Click the Create button to create it.

### Pager

Navigation controls for browsing through large numbers of virtual types.

### Filter

Two input fields on the right let you filter the list by Name and Source Type.

## Left Sidebar

The sidebar shows a list of all virtual types with the following information:

- Position – The sort order
- Name – The virtual type name
- Source Type – The type that provides the source data

Each entry has a context menu with Edit and Delete options.

## Main Area

When you select a virtual type, the main area shows an editor for its configuration. In the top right corner, a preview link built from the virtual type name is displayed. The REST interface does not resolve virtual type names, so this link returns an error, or the untransformed objects of a schema type if one with the same name exists.

### Virtual Type Settings

The upper section contains settings for the virtual type itself:

- Position – Controls the sort order in the list
- Name – The name of the virtual type
- Source Type – The type that provides the source data
- Filter Expression – An optional script expression that filters which source objects are included
- Visible to Public Users – Checkbox for public visibility
- Visible to Authenticated Users – Checkbox for authenticated user visibility

### Virtual Properties Table

Below the settings, a table shows all virtual properties defined for this type. The columns are:

- Actions – Edit and delete buttons
- Position – Sort order of the property
- Source Name – The property name on the source type
- Target Name – The property name in the virtual type output
- Input Function – Optional transformation script applied in the input direction, when a value is written through the virtual type
- Output Function – Optional transformation script applied in the output direction, when an object is read through the virtual type
- Public Users – Visibility flag
- Authenticated Users – Visibility flag

### Create Virtual Property

A button below the table lets you add new virtual properties.

## Related Topics

- Virtual Types – Detailed documentation on the transformation semantics, the ODS and ODT exporters and scripting
- Importing Data – The CSV Import Wizard, whose mapping table becomes a temporary virtual type
