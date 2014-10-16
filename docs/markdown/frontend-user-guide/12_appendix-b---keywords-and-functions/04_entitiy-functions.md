<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>
<tr><td>get(entity, propertyKey)</td><td>Returns the value of the property with the given name from the given entity. In the following example, the result is the same as the result of <code>this.name</code>, but you can use the get() function to fetch values from other function's results, i.e. <code>${get(find('User', 'name', 'tester'), 'eMail')}</code></td><td><code>${get(this, 'name')}.</code></td></tr>
<tr><td>set(entity, propertyKey, value)</td><td>Sets the value of the property with the given name to the given value</td><td><code>${set(this, 'name', 'New Name')}</code></td></tr>
<tr><td>find(type, key, value [, key2, value2 [, key3, value3]])</td><td>Finds and returns a collection of entities of the given type, matching the given (key, value) pairs (3 at most)</td><td><code>${find('User', 'name', 'tester')}</code></td></tr>
<tr><td>create(type, key, value [, key2, value2 [, key3, value3]])</td><td>Creates an entity of the given type with the given (key, value) pairs</td><td><code>${create('User', 'name', 'tester)}</code></td></tr>
<tr><td>delete(entity)</td><td>Deletes the given entity</td><td><code>${delete(this)}</code></td></tr>
</table>
