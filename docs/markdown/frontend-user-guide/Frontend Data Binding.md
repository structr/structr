If you want to be able to use the "frontend editing mode" of Structr, you have to do the following:

* Go to the "Edit Mode Binding" tab in the properties dialog of the parent node
* Enter "${this.id}" into the "Element ID" field
* For each data element (e.g. a paragraph that displays the name of your entity), go to the "Edit Mode Binding" tab and enter the following:
    * in the "Attribute Key" field, enter the name of the attribute, e.g. "name"
    * in the "Raw Value" field, enter the template expression for the given value, e.g. "${this.name}"
* Add the "Edit Button" widget to the parent element
