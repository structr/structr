Every database object in Structr has at least the following default properties, so we can easily access the `name` property of the repeated images.

* id
* name
* createdDate
* lastModifiedDate

Special (or dynamic) entities of course have more properties. You can even chain those property references through multiple layers, i.e. use something like

    ${image.owner.name}
