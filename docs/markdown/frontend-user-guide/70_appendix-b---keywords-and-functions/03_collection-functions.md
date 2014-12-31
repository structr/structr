<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>

<tr><td>each(collection, expression)</td><td>Executes the given expression for each element of the given collection, making the current element available using the <i>data</i> keyword</td><td><code>${each(this.children, print(data.name))}</code></td></tr>
<tr><td>extract(collection, propertyName)</td><td>Extracts a collection of values from a given collection of entities, fetching the value of the property with the given name and putting it into the result list</td><td><code>${extract(this.children, 'name')} => ['child1', 'child2', 'child3']</code></td></tr>
<tr><td>merge(collection1, collection2, collection3, ...)</td><td>Merges all parameters (collections or single elements) into a single collection</td><td><code>${merge(extract(this.children, 'name'), this.name')}</code></td></tr>
<tr><td>sort(collection, propertyKey [, descending])</td><td>Sorts the given collection according to the given property key, with an optional boolean parameter <i>descending</i></td><td><code>${sort(this.children, 'position')}</code></td></tr>
<tr><td>int_sum(collection)</td><td>Calculates the sum of all elements of the given collection and returns it as an integer</td><td><code>${int_sum(extract(this.children, 'count'))}</code></td></tr>
<tr><td>double_sum(collection)</td><td>Calculates the sum of all elements of the given collection and returns it as a double</td><td><code>${int_sum(extract(this.children, 'count'))}</code></td></tr>
<tr><td>first(collection)</td><td>Returns the first element of the given collection</td><td><code>${first(this.children)}</code></td></tr>
<tr><td>last(collection)</td><td>Returns the last element of the given collection</td><td><code>${last(this.children)}</code></td></tr>
<tr><td>nth(collection, index)</td><td>Returns the element with the given index of the given collection</td><td><code>${nth(this.children, 3)}</code></td></tr>
<tr><td>size(collection)</td><td>Returns the size of the given collection</td><td><code>${size(this.children)} => 5</code></td></tr>

</table>
