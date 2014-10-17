You can define relations between custom node types by connecting them by an arrow, starting at the dot at the bottom of a schema node and stop at the dot at the bottom of another one, or - if you want to self-relation - of the same node.

When you have successfully established a relation, you should enter relationship type (typically in all-uppercase/underscore notation, see [https://gist.github.com/nigelsmall/9366313](https://gist.github.com/nigelsmall/9366313)) and set the cardinality to reflect your data model.

* Relationship Type: Mandatory identifier to tag relationships with a common semantic
* Cardinality: Set to 1:*, *:1 or *:* (default)

For each relation defined between node types, a property will be automatically created for both, source and target node type. The default name will be auto-generated based on the source and target node type and the relationship type.

You can overwrite the default name by entering a new, custom name into the name column.
