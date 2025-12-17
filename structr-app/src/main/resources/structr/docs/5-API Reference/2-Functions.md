# Built-in Functions

## Access Control Functions

### addToGroup()
Adds the given user as a member of the given group.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|group|group to add to|no|
|principal|user or group to add to the given group|no|

#### Signatures

```
addToGroup(group, user)
```


### copyPermissions()
Copies the security configuration of an entity to another entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|sourceNode|source node to copy permissions from|no|
|targetNode|target node to copy permissions to|no|
|syncPermissions|synchronize permissions between source and target nodes|yes|


If the `syncPermissions` parameter is set to `true`, the permissions of existing security relationships are aligned between source and target nodes. If it is not set (or omitted) the function just adds the permissions to the existing permissions.
#### Notes
- This function **only** changes target node permissions that are also present on the source node.

#### Signatures

```
copyPermissions(source, target [, overwrite ])
```


### grant()
Grants the given permissions on the given node to the given principal.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|principal|User or Group node|no|
|node|node to grant permissions|no|
|permissions|comma seperated permission string of `read`, `write`, `delete`, `accessControl`|no|


This function creates or modifies the security relationship between the first two parameters.
Valid values for the permission list are `read`, `write`, `delete` and `accessControl`.
The permissions are passed in as a comma-separated list (see the examples below).
The return value is the empty string. See also `revoke()` and `isAllowed()`.
#### Signatures

```
grant(user, node, permissions)
```

#### Examples
##### Example 1 (StructrScript)
```
${grant(me, node1, 'read')}
${grant(me, node2, 'read, write')}
${grant(me, node3, 'read, write, delete')}
${grant(me, node4, 'read, write, delete, accessControl')}

```
##### Example 2 (JavaScript)
```
${{ $.grant($.me, node1, 'read') }}
${{ $.grant($.me, node2, 'read, write') }}
${{ $.grant($.me, node3, 'read, write, delete') }}
${{ $.grant($.me, node4, 'read, write, delete, accessControl') }}

```

### isAllowed()
Returns true if the given principal has the given permissions on the given node.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|principal|principal to check permissions for|no|
|node|node to check permissions on|no|
|permissions|string with comma-separated list of permissions (`read`, `write`, `delete` and `accessControl`)|no|


Valid values for the permission list are `read`, `write`, `delete` and `accessControl`. The permissions are passed in as a comma-separated list (see example). See also `grant()` and `revoke()`.
#### Signatures

```
isAllowed(user, node, permissions)
```

#### Examples
##### 1. (StructrScript) Check if the current user has `read` and `write` permissions on a group
```
${isAllowed(me, group1, 'read, write')}
```

### isInGroup()
Returns true if the given user is in the given group.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|group|group to check membership|no|
|principal|principal whose membership will be checked|no|
|checkHierarchy|set to `false` to only check direct membership|yes|


If the optional parameter `checkHierarchy` is set to `false`, only a direct group membership is checked. Otherwise, the full group hierarchy will be checked.
#### Signatures

```
isInGroup(group, user [, checkHierarchy = false ])
```


### removeFromGroup()
Removes the given user from the given group.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|group|target group|no|
|principal|principal to remove from group|yes|

#### Signatures

```
removeFromGroup(group, user)
```

#### Examples
##### Example 1 (StructrScript)
```
${removeFromGroup(first(find('Group', 'name', 'Admins')), me)}
```
##### Example 2 (JavaScript)
```
${{ $.removeFromGroup($.first($.find('Group', 'name', 'Admins')), $.me)} }}
```

### revoke()
Revokes the given permissions on the given entity from a user.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|principal|User or Group node|no|
|node|node to revoke permissions|no|
|permissions|comma seperated permission string of `read`, `write`, `delete`, `accessControl`|no|


This function modifies the security relationship between the first two parameters.
Valid values for the permission list are `read`, `write`, `delete` and `accessControl`.
The permissions are passed in as a comma-separated list (see the examples below).
The return value is the empty string. See also `grant()` and `isAllowed()`.
#### Signatures

```
revoke(user, node, permissions)
```

#### Examples
##### Example 1 (StructrScript)
```
${revoke(me, node1, 'read')}
${revoke(me, node2, 'read, write')}
${revoke(me, node3, 'read, write, delete')}
${revoke(me, node4, 'read, write, delete, accessControl')}

```
##### Example 2 (JavaScript)
```
${{ $.revoke($.me, node1, 'read') }}
${{ $.revoke($.me, node2, 'read, write') }}
${{ $.revoke($.me, node3, 'read, write, delete') }}
${{ $.revoke($.me, node4, 'read, write, delete, accessControl') }}

```

## Collection Functions

### all()
Evaluates a StructrScript expression for every element of a collection and returns `true` if the expression evaluates to `true` for **all** of the elements.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|expression|expression to evaluate for each element|no|


Inside the expression function, the keyword `data` refers to the current element. See also: `any()` and `none()`.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.reduce()`).

#### Signatures

```
all(list, expression)
```

#### Examples
##### 1. (StructrScript) Check if all of a user's groups have read permissions on the `current` object.
```
${all(user.groups, is_allowed(data, current, 'read'))}
```
##### 2. (StructrScript) Check if all elements of a list are greater than a given number
```
${all(merge(6, 7, 8, 12, 15), gt(data, 10))}
```

### any()
Evaluates a StructrScript expression for every element of a collection and returns `true` if the expression evaluates to `true` for **any** of the elements.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|expression|expression to evaluate for each element|no|


Inside the expression function, the keyword `data` refers to the current element. See also: `all()` and `none()`.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.reduce()`).

#### Signatures

```
any(list, expression)
```

#### Examples
##### 1. (StructrScript) Check if any of a user's groups have read permissions on the `current` object.
```
${any(user.groups, is_allowed(data, current, 'read'))}
```
##### 2. (StructrScript) Check if any element of a list is greater than a given number
```
${any(merge(6, 7, 8, 12, 15), gt(data, 10))}
```

### complement()
Removes objects from a list.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|sourceList|list of objects|no|
|objects..|objects or lists of objects **that are removed from the source list**|no|


This function removes all objects from the source list that are contained in the other parameters.
#### Notes
- If an object in the list of `removeObject`s is a list, all elements of that list are removed from the `sourceList`.
- If an object occurs multiple times in the `sourceList` and is not removed, it will remain multiple times in the returned list.

#### Signatures

```
complement(sourceList, objects...)
```

#### Examples
##### 1. (JavaScript) Removes 5, 1 and 3 from the given list
```
${{ let list = $.complement([3, 4, 2, 1, 5, 6], 5, 1, 3); }}
```

### doubleSum()
Returns the sum of all the values in the given collection as a floating-point value.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of values to sum|no|


This function will most likely be used in combination with the `extract()` or `merge()` functions.
#### Signatures

```
doubleSum(list)
```

#### Examples
##### 1. (StructrScript) Return the sum of all `itemPrice` values of all `Product` entities
```
${doubleSum(extract(find('Product'), 'itemPrice'))}
```

### each()
Evaluates a StructrScript expression for every element of a collection.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|expression|expression to evaluate for each element|no|


This function returns no value. Inside the expression function, the keyword `data` refers to the current element. See also: `all()`, `any()`, and `none()` if the expression returns a value.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.forEach()`).
- The collection can also be a list of strings or numbers (see example 2).

#### Signatures

```
each(list, expression)
```

#### Examples
##### 1. (StructrScript) Log the names of all users
```
${each(find('User'), log(data.name))}
```
##### 2. (StructrScript) Log the session IDs of a given user
```
${each(find('User', '0b514b0bd5ef4f2e8ad7230cb2e6c9d1').sessionIds), log(data))}
```

### extract()
Extracts property values from all elements of a collection and returns them as a collection.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|source collection|no|
|propertyName|name of property value to extract|no|


This function iterates over the given collection and extracts the value for the given property key of each element. The return value of this function is a collection of extracted property values. It is often used in combination with `find()` and `join()` to create comma-separated lists of entity values.
#### Notes
- This function is the StructrScript equivalent of JavaScript's `map()` function with a lambda expression of `l -> l.propertyName`.

#### Signatures

```
extract(list, propertyName)
```

#### Examples
##### Example 1 (StructrScript)
```
${extract(find('User'), 'name')} => [admin, user1, user2, user3]
```

### filter()
Filters a list using a StructrScript expression.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|filterExpression|expression to evaluate for each element|no|


The function will remove any object from the list for which the filter expression returns **false**, and return the filtered list. The filter expression can be any valid expression that returns a boolean value. Inside the expression function, the keyword `data` refers to the current element.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.filter()`).

#### Signatures

```
filter(list, filterExpression)
```

#### Examples
##### 1. (StructrScript) Remove admin users from a list of users
```
${filter(find('User'), not(data.isAdmin))}
```

### first()
Returns the first element of the given collection.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection to return first element of|no|


This function is often used in conjunction with `find()` to return the first result of a query. See also `last()` and `nth()`.
#### Signatures

```
first(collection)
```

#### Examples
##### 1. (StructrScript) Return the first of the existing users
```
${first(find('User'))}
```

### intSum()
Returns the sum of the given arguments as an integer.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection of values to sum|no|


This function will most likely be used in combination with the `extract()` or `merge()` functions.
#### Signatures

```
intSum(list)
```

#### Examples
##### 1. (StructrScript) Return the sum of a list of values
```
${intSum(merge(1, 2, 3, 4))}
```

### isCollection()
Returns true if the given argument is a collection.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value|value to check|no|

#### Signatures

```
isCollection(value)
```


### last()
Returns the last element of the given collection.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection to return last element of|no|


This function is often used in conjunction with `find()`. See also `first()` and `nth()`.
#### Signatures

```
last(collection)
```


### map()
Transforms a list using a transformation expression.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|transformationExpression|transformation expression that is applied to each element|no|


This function evaluates the transformationExpression for each element of the list and returns the list of transformed values. Inside the expression function, the keyword `data` refers to the current element. See also: `each()` and `filter()`.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.map()`).
- The collection can also be a list of strings or numbers (see example 2).

#### Signatures

```
map(list, transformationExpression)
```

#### Examples
##### 1. (StructrScript) Return only the names of all users
```
${map(find('User'), data.name)}
```

### map()
Returns a single result from all elements of a list by applying a reduction function.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|initialValue|expression that creates the initial value, e.g. 0|no|
|reductionExpression|reduce expression that gets `accumulator` and `data` to reduce the two to a single value|no|


This function evaluates the reductionExpression for each element of the list and returns a single value. Inside the reduction expression, the keyword `accumulator` refers to the result of the previous reduction, and `data` refers to the current element. See also: `map()`, `each()` and `filter()`.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.reduce()`).
- The collection can also be a list of strings or numbers (see example 2).

#### Signatures

```
map(list, initialValue, reductionExpression)
```

#### Examples
##### 1. (StructrScript) Add
```
${reduce(merge(1, 2, 3, 4), 0, add(accumulator, data))}
```

### merge()
Merges collections and objects into a single collection.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|objects...|collections or objects to merge into a single collection|no|


You can use this function to create collections of objects, add objects to a collection, or to merge multiple collections into a single one. All objects that are passed to this function will be added to the resulting collection. If an argument is a collection, all objects in that collection are added to the resulting collection as well.
#### Notes
- This function will not remove duplicate entries. Use `mergeUnique()` for that.

#### Signatures

```
merge(objects...)
```


### mergeUnique()
Merges collections and objects into a single collection, removing duplicates.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|objects...|collections or objects to merge into a single collection|no|


You can use this function to create collections of objects, add objects to a collection, or to merge multiple collections into a single one. All objects that are passed to this function will be added to the resulting collection. If an argument is a collection, all objects in that collection are added to the resulting collection as well.

This function is very similar to `merge()` except that the resulting collection will **not** contain duplicate entries.

#### Notes
- This function will remove duplicate entries. If you don't want that, use `merge()`.

#### Signatures

```
mergeUnique(list1, list2, list3...)
```


### none()
Evaluates a StructrScript expression for every element of a collection and returns `true` if the expression evaluates to `true` for **none** of the elements.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|list|list of elements to loop over|no|
|expression|expression to evaluate for each element|no|


Inside the expression function, the keyword `data` refers to the current element. See also: `all()` and `any()`.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.reduce()`).

#### Signatures

```
none(list, expression)
```

#### Examples
##### 1. (StructrScript) Check if none of a user's groups have read permissions on the `current` object.
```
${none(user.groups, is_allowed(data, current, 'read'))}
```
##### 2. (StructrScript) Check if no element of a list is greater than a given number
```
${none(merge(6, 7, 8, 12, 15), gt(data, 10))}
```

### nth()
Returns the element with the given index of the given collection.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection to return element of|no|
|index|index of the object to return (0-based)|no|


`first()` and `last()`.
#### Signatures

```
nth(collection, index)
```

#### Examples
##### 1. (StructrScript) Return the second of the existing users
```
${nth(find('User'), 1)}
```

### size()
Returns the size of the given collection.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection to count|no|

#### Signatures

```
size(collection)
```

#### Examples
##### Example 1 (StructrScript)
```
${size(page.children)}
```
##### Example 2 (StructrScript)
```
${size(merge('a', 'b', 'c'))}
```
##### Example 3 (JavaScript)
```
${{ return $.size([1, 2, 3, 5, 8]); }}
```

### sort()
Sorts the given collection or array according to the given property key. Default sort key is 'name'.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection to be sorted|no|
|propertyKey|name of the property|no|
|sortDescending|sort descending, if true. Default: false)|yes|


Sorts the given collection according to the given property key and returns the result in a new collection.
The optional parameter `sortDescending` is a **boolean flag** that indicates whether the sort order is ascending (default) or descending.
This function is often used in conjunction with `find()`.
The `sort()` and `find()` functions are often used in repeater elements in a function query, see Repeater Elements.

#### Notes
- Do not use JavaScript build-in function `sort` on node collections! This can damage relationships of sorted nodes

#### Signatures

```
sort(list [, propertyName [, descending=false] ])
```

#### Examples
##### Example 1 (StructrScript)
```
${extract(sort(find('User'), 'name'), 'name')}
```
##### Example 2 (StructrScript)
```
${extract(sort(find('User'), 'name', true), 'name')}
```
##### Example 3 (JavaScript)
```
${{ $.sort($.find('User'), 'name') }}
```

### unwind()
Converts a list of lists into a flat list.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|collections|collection(s) to unwind|no|


Combines the given nested collections into to a single, "flat" collection.
This method is the reverse of `extract()` and can be used to flatten collections of related nodes that were
created with nested `extract()` calls etc. It is often used in conjunction with the `find()` method like in the example below.

#### Notes
- `unwind()` is quite similar to `merge()`. The big difference is that `unwind()` filters out empty collections.

#### Signatures

```
unwind(list1, list2, ...)
```

#### Examples
##### Example 1 (StructrScript)
```
${unwind(this.children)}
```
##### Example 2 (JavaScript)
```
${{ $.unwind([[1,2,3],4,5,[6,7,8]])}}
> [1, 2, 3, 4, 5, 6, 7, 8]

```

## Conversion Functions

### bson()
Creates BSON document from a map / object.
#### Signatures

```
bson(data)
```


### coalesce()
Returns the first non-null value in the list of expressions passed to it. In case all arguments are null, null will be returned.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|strings..|list of strings to coalesce|no|

#### Signatures

```
coalesce(value1, value2, value3, ...)
```

#### Examples
##### 1. (StructrScript) Returns either the name, the title or the UUID of a node, depending on which one is non-null
```
coalesce(node.name, node.title, node.id)
```
##### 2. (JavaScript) Returns either the name, the title or the UUID of a node, depending on which one is non-null
```
$.coalesce(node.name, node.title, node.id)
```

### dateFormat()
Formats the given date object according to the given pattern, using the current locale (language/country settings).
#### Parameters

|Name|Description|Optional|
|---|---|---|
|date|date to format|no|
|pattern|format pattern|no|


This function uses the Java SimpleDateFormat class which provides the following pattern chars:

| Letter | Date or Time Component |
| --- | --- |
| G | Era designator |
| y | Year |
| Y | Week year |
| M | Month in year |
| w | Week in year |
| W | Week in month |
| D | Day in year |
| d | Day in month |
| F | Day of week in month |
| E | Day name in week |
| u | Day number of week (1 = Monday, ..., 7 = Sunday) |
| a | AM/PM marker |
| H | Hour in day (0-23) |
| k | Hour in day (1-24) |
| K | Hour in AM/PM (0-11) |
| h | Hour in AM/PM (1-12) |
| m | Minute in hour |
| s | Second in minute |
| S | Millisecond |
| z | General time zone |
| Z | RFC 822 time zone |
| X | ISO 8601 time zone |

Each character can be repeated multiple times to control the output format.

| Pattern | Description |
| --- | --- |
| d | prints one or two numbers (e.g. "1", "5" or "20") |
| dd | prints two numbers (e.g. "01", "05" or "20") |
| EEE | prints the shortened name of the weekday (e.g. "Mon") |
| EEEE | prints the long name of the weekday (e.g. "Monday") |

#### Notes
- Some format options are locale-specific. See the examples or the `locale` keyword for information about locales.

#### Signatures

```
dateFormat(value, pattern)
```

#### Examples
##### 1. (StructrScript) 2020-03-29
```
${dateFormat(toDate(1585504800000), 'yyyy-MM-dd')}
```
##### 2. (StructrScript) Sunday
```
${dateFormat(toDate(1585504800000), 'EEEE')}
```
##### 3. (StructrScript) Sonntag
```
${(setLocale('de'), dateFormat(toDate(1585504800000), 'EEEE'))}
```

### escapeHtml()
Replaces HTML characters with their corresponding HTML entities.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|text|text to escape|no|


Supports all known HTML 4.0 entities, including accents and special characters.
#### Notes
- Note that the commonly used apostrophe escape character (') is not a legal entity and so is not supported.

#### Signatures

```
escapeHtml(string)
```

#### Examples
##### Example 1 (StructrScript)
```
${escapeHtml('Test & Test"')} => Test &amp; Test&quot;
```

### escapeJavascript()
Escapes the given string for use with Javascript.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|text|text to escape|no|

#### Notes
- Escapes the characters in a string using EcmaScript String rules.
- Escapes any values it finds into their EcmaScript String form.
- Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.).

#### Signatures

```
escapeJavascript(string)
```

#### Examples
##### Example 1 (StructrScript)
```
${escapeJavascript('This is a "test"')} => This is a \"test\" 
```

### escapeJson()
Escapes the given string for use within JSON.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|text|text to escape|no|

#### Notes
- Escapes the characters in a string using Json String rules.
- Escapes any values it finds into their Json String form.
- Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.)

#### Signatures

```
escapeJson(string)
```

#### Examples
##### Example 1 (StructrScript)
```
${escapeJson('This is a "test"')} => This is a \"test\"
```

### escapeXml()
Replaces XML characters with their corresponding XML entities.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|text|text to escape|no|

#### Notes
- Supports only the five basic XML entities (gt, lt, quot, amp, apos).
- Does not support DTDs or external entities.
- Unicode characters greater than 0x7f are currently escaped to their numerical \u equivalent. This may change in future releases.

#### Signatures

```
escapeXml(string)
```

#### Examples
##### Example 1 (StructrScript)
```
${escapeXml('This is a "test" & another "test"')} => This is a &quot;test&quot; &amp; another &quot;test&quot;
```

### formurlencode()
Encodes the given object to an application/x-www-form-urlencoded string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|object|object to encode|no|


This function encodes the given object for use in an URL, replacing invalid characters with their valid URL equivalent and joining the key/value pairs with an ampersand.
#### Notes
- This function is best used in a JavaScript context.

#### Signatures

```
formurlencode(object)
```

#### Examples
##### Example 1 (StructrScript)
```
$.formurlencode({name:'admin', p1: 12, apiKey: 'abc123', text:'Text with umläut'}) => name=admin&p1=12&apiKey=abc123&text=Text+with+uml%C3%A4ut
```

### hash()
Returns the hash (as a hexadecimal string) of a given string, using the given algorithm (if available via the underlying JVM).
#### Parameters

|Name|Description|Optional|
|---|---|---|
|algorithm|Hash algorithm that will be used to convert the string|no|
|value|String that will be converted to hash string|no|


Returns the hash (as a hexadecimal string) of a given string, using the given algorithm (if available via the underlying JVM).
Currently, the SUN provider makes the following hashes/digests available: MD2, MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512, SHA-512/224, SHA-512/256, SHA3-224, SHA3-256, SHA3-384, SHA3-512
If an algorithm does not exist, an error message with all available algorithms will be logged and a null value will be returned.

#### Signatures

```
hash(algorithm, value)
```

#### Examples
##### Example 1 (StructrScript)
```
${hash('SHA-512', 'Hello World!')}
```
##### Example 2 (JavaScript)
```
${{ $.hash('SHA-512', 'Hello World!') }}
```

### int()
Tries to convert the given object into an integer value.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|input|input value to convert to an integer, can be string or floating-point number|no|

#### Signatures

```
int(value)
```

#### Examples
##### 1. (StructrScript) Convert a floating-point value into an integer
```
${int(5.8)}
```
##### 2. (StructrScript) Convert a string into an integer
```
${int('35.8')}
```

### long()
Tries to convert the given object into a long integer value.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|object|input object to convert to a long integer, can be string, date or floating-point number|no|


This function is especially helpful when trying to set a date value in the database via the `cypher()` function.

Date values are also supported and are converted to the number of milliseconds since January 1, 1970, 00:00:00 GMT.

Other date strings are also supported in the following formats:
- "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
- "yyyy-MM-dd'T'HH:mm:ssXXX"
- "yyyy-MM-dd'T'HH:mm:ssZ"
- "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

#### Notes
- See also `num()`.

#### Signatures

```
long(input)
```

#### Examples
##### 1. (StructrScript) Return the milliseconds since epoch of the current date
```
${long(now)}
```
##### 2. (StructrScript) Convert a floating-point value into a long
```
${long(5.8)}
```
##### 3. (StructrScript) Convert a string into a long
```
${long('35.8')}
```

### md5()
Returns the MD5 hash of a given object.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|input|input object to hash|no|


This function converts its argument into a string, creates the 32-character alphanumeric MD5 hash of that string and returns the resulting hash string.
#### Signatures

```
md5(str)
```


### num()
Tries the convert given object into a floating-point number with double precision.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|object|input object to convert to a long integer, can be string, date or floating-point number|no|


Date values are also supported and are converted to the number of milliseconds since January 1, 1970, 00:00:00 GMT.

Other date strings are also supported in the following formats:
- "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
- "yyyy-MM-dd'T'HH:mm:ssXXX"
- "yyyy-MM-dd'T'HH:mm:ssZ"
- "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

#### Notes
- See also `long()`.

#### Signatures

```
num(object)
```

#### Examples
##### 1. (StructrScript) Return the milliseconds since epoch of the current date
```
${num(now)}
```
##### 2. (StructrScript) Convert a string into a floating-point number
```
${num('35.8')}
```

### numberFormat()
Formats the given value using the given locale and format string.

This function uses the Java NumberFormat class which supports the ISO two-letter language codes, e.g. "en", "de" etc.

The following four pattern chars are supported:

| Letter | Description |
| --- | --- |
| 0 | Any number, or "0" |
| # | Any number, or nothing |
| . | The decimal separator |
| , | The thousands-separator |

#### Notes
- In general, if you want a formatted number to be visible all the time, use "0" in the pattern, otherwise use "#".

#### Signatures

```
numberFormat(value, locale, format)
```


### parseDate()
Parses the given date string using the given format string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|date string|no|
|pattern|date pattern|no|


Parses the given string according to the given pattern and returns a date object. This method is the inverse of <a href='#date_format'>date_format()</a>.
#### Signatures

```
parseDate(str, pattern)
```

#### Examples
##### Example 1 (StructrScript)
```
${parseDate('2015-12-12', 'yyyy-MM-dd')}
```
##### Example 2 (JavaScript)
```
${{ $.parseDate('2015-12-12', 'yyyy-MM-dd') }}
```

### parseNumber()
Parses the given string using the given (optional) locale.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|String that will be parsed into numerical value|no|
|locale|Locale string for specific number formatting|yes|


Parses the given string into a numerical value. With the second (optional) parameter you can pass a locale string to take a country-/language specific number formatting into account.
#### Notes
- If no locale parameter is given, the default locale for the context is used. See the `locale` keyword.

#### Signatures

```
parseNumber(number [, locale ])
```

#### Examples
##### Example 1 (StructrScript)
```
${parseNumber('123,456,789.123', 'en')}
```
##### Example 2 (StructrScript)
```
${parseNumber('123.456.789,123', 'de')}
```
##### Example 3 (JavaScript)
```
${{ $.parseNumber('123,456,789.123', 'en') }}
```
##### Example 4 (JavaScript)
```
${{ $.parseNumber('123.456.789,123', 'de') }}
```

### toDate()
Converts the given number to a date.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|number|unix timestamp|no|


The number is interpreted as UNIX timestamp (milliseconds from Jan. 1, 1970).
#### Signatures

```
toDate(number)
```

#### Examples
##### Example 1 (StructrScript)
```
${toDate(1585504800000)}
```
##### Example 2 (JavaScript)
```
${{ $.toDate(1585504800000) }}
```

### toGraphObject()
Converts the given entity to GraphObjectMap.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|source|object or collection|no|
|view|view (default: `public`)|yes|
|depth|conversion depth (default: 3)|yes|


Tries to convert given object or collection containing graph objects into a graph object.
If an element in the source can not be converted to a graph object, it is ignored.
Graph objects can be used in repeaters for example and thus it can be useful to create custom graph
objects for iteration in such contexts. The optional `view` parameter can be used to select the view
representation of the entity. If no view is given, the `public` view is used. The optional `depth`
parameter defines at which depth the conversion stops. If no depth is given, the default value of 3 is used.
#### Notes
- Since strings can not be converted to graph objects but it can be desirable to use collections of strings in repeaters (e.g. the return value of the `inheriting_types()` function), collections of strings are treated specially and converted to graph objects with `value` => `<string>` as its result. (see example 2)

#### Signatures

```
toGraphObject(obj)
```

#### Examples
##### Example 1 (JavaScript)
```
${{
	let coll = $.toGraphObject([
		{id:"o1",name:"objectA"},
		{id:"o2",name:"objectB"}
	]);
	$.print(coll.join(', '));
}}
> {id=o1, name=objectA}, {id=o2, name=objectB}

```
##### Example 2 (StructrScript)
```
${toGraphObject(inheritingTypes('Principal'))}
> [{value=Principal},{value=Group},{value=LDAPGroup},{value=LDAPUser},{value=User}]

```

### unescapeHtml()
Reverses the effect of `escape_html()`.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|escaped string|no|


Relaces escaped HTML entities with the actual characters, e.g. &lt; with <.
#### Signatures

```
unescapeHtml(text)
```

#### Examples
##### Example 1 (StructrScript)
```
${unescapeHtml('test &amp; test')}
```
##### Example 2 (JavaScript)
```
${{Structr.unescapeHtml('test &amp; test')}}
```

### urlencode()
URL-encodes the given string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|str|given string|no|

#### Signatures

```
urlencode(str)
```

#### Examples
##### Example 1 (StructrScript)
```
${urlencode(this.email)}
```
##### Example 2 (JavaScript)
```
${{ $.urlencode($.this.email) }}
```

## Database Functions

### addLabels()
Adds the given set of labels to the given node.
#### Signatures

```
addLabels(node, labels)
```


### create()
Creates a new node with the given type and key-value pairs in the database.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of node to create|no|
|additionalValues|key-value pairs or a map thereof|yes|

#### Notes
- In a StructrScript environment, parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function takes a map as the second parameter.

#### Signatures

```
create(type [, parameterMap ])
```

#### Examples
##### Example 1 (StructrScript)
```
${create('User', 'name', 'tester', 'passwword', 'changeMeNow!')}
```
##### 2. (JavaScript) Create a new User with the given name and password
```
${{
	let user = $.create('User', { name: 'tester', password: 'changeMeNow!' });
}}

```

### createOrUpdate()
Creates an object with the given properties or updates an existing object if it can be identified by a unique property.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type to create or update|no|
|properties|properties|no|


This function is a shortcut for a code sequence with a query that looks up an existing object and a set() operation it if an object was found, or creates a new object with the given properties, if no object was found.
#### Notes
- In a StructrScript environment, parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
createOrUpdate(type, propertyMap)
```

#### Examples
##### 1. (StructrScript) If no object with the given e-mail address exists, a new object is created, because "eMail" is unique. Otherwise, the existing object is updated with the new name.
```
${createOrUpdate('User', 'eMail', 'tester@test.com', 'name', 'New Name')}
```

### createRelationship()
Creates and returns relationship of the given type between two entities.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|fromNode|start node of the new relationship|no|
|toNode|end node of the new relationship|no|
|relationshipType|relationship type to create|no|

#### Notes
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the fourth parameter.
- The relationshipType is the literal name of the relationship that you see between two nodes in the schema editor, e.g. "FOLLOWS" or "HAS".

#### Signatures

```
createRelationship(from, to, relType [, parameterMap ])
```


### cypher()
Executes the given Cypher query directly on the database and returns the results as Structr entities.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|query|query to execute|no|
|parameters|map to supply parameters for the variables in the query|yes|
|runInNewTransaction|whether the Cypher query should be run in a new transaction - see notes about the implications of that flag|yes|


Structr will automatically convert all query results into the corresponding Structr objects, i.e. Neo4j nodes will be instantiated to Structr node entities, Neo4j relationships will be instantiated to Structr relationship entities, and maps will converted into Structr maps that can be accessed using the dot notation (`map.entry.subentry`).
#### Notes
- If the `runInNewTransaction` parameter is set to `true`, the query runs in a new transaction, **which means that you cannot access use objects created in the current context due to transaction isolation**.
- The `cypher()` function always returns a collection of objects, even if `LIMIT 1` is specified!
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
cypher(query [, parameterMap, runInNewTransaction])
```

#### Examples
##### 1. (StructrScript) Run a simple query
```
${cypher('MATCH (n:User) RETURN n')}
```
##### 2. (JavaScript) Run a query with variables in it
```
${{
	let query = "MATCH (user:User) WHERE user.name = $userName RETURN user";
	let users = $.cypher(query, {userName: 'admin'});
}}

```

### delete()
Deletes the one or more nodes or relationships from the database.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|objectOrList|object(s) to delete, can also be a list|no|

#### Signatures

```
delete(objectOrList)
```

#### Examples
##### 1. (StructrScript) Delete the first project
```
${delete(first(find('Project')))}
```
##### 2. (StructrScript) Delete all projects
```
${delete(find('Project'))}
```

### find()
Returns a collection of entities of the given type from the database.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type to return (includes inherited types)|no|
|predicates|list of predicates|yes|
|uuid|uuid, makes the function return **a single object**|yes|


This function is one of the most important and frequently used built-in functions. It returns a collection of entities, which can be empty if none of the existing nodes or relationships matches the given search parameters. `find()` accepts several different predicates (key, value pairs) and other query options like sort order or pagination controls. See the examples below for an overview of the possible parameter combinations for an advanced find() query.

**Predicates**

The following predicates can be specified. Predicates can be combined and mixed to build complex queries. Some  predicates and property keys need to be combined in a different way than others, please refer to the examples below for an overview.

Please note that in JavaScript, the predicates need to be prefixed with `$.predicate` to avoid confusing them with built-in functions of the same name.

|Predicate              |Description|
|-----------------------|-----------|
| `and(...)`            | Logical AND |
| `or(...)`             | Logical OR |
| `not(...)`            | Logical NOT |
| `equals(key, value)`  | Returns only those nodes that match the given (key, value) pair |
| `contains(key, text)` | Returns only those nodes whose value contains the given text |
| `empty(key)`        | Returns only those nodes that don't have a value set for the given key |
| `lt(upperBound)`    | Returns nodes where a key is less than `upperBound` [Only available in Structr version > 6.0] |
| `lte(upperBound)`    | Returns nodes where a key is less than or equal to `upperBound`[Only available in Structr version > 6.0] |
| `gte(lowerBound)`    | Returns nodes where a key is greater than or equal to `lowerBound` [Only available in Structr version > 6.0]|
| `gt(lowerBound)`    | Returns nodes where a key is greater than `lowerBound` [Only available in Structr version > 6.0]|
| `range(start, end [, withStart = true [, withEnd = true ]] )` | Returns only those nodes where the given propertyKey is in the range between start and end |
| `range(null, end)`    | Unbounded `range()` to emulate "lower than or equal" |
| `range(start, null)`  | Unbounded `range()` to emulate "greater than or equal" |
| `startsWith(key, prefix)` | Returns only those nodes whose value for the given key starts with the given prefix |
| `endsWith(key, suffix)` | Returns only those nodes whose value for the given key ends with the given suffix |
| `withinDistance(latitude, longitude, distance)`  | Returns only those nodes that are within `distance` meters around the given coordinates. The type that is being searched for needs to extend the built-in type `Location` |

**Options**

|Option   |Description|
|---------|-----------|
|`sort(key)`|Sorts the result according to the given property key (ascending)|
|`sort(key, true)`|Sorts the result according to the given property key (descending)|
|`page(page, pageSize)`|Limits the result size to `pageSize`, returning the given `page`|


#### Notes
- In a StructrScript environment parameters are passed as pairs of 'key1', 'value1'.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.
- The `find()` method will always use **exact** search, if you are interested in inexact / case-insensitive search, use `search()`.
- Calling `find()` with only a single parameter will return all the nodes of the given type (which might be problematic if there are so many of them in the database so that they do not fit in the available memory).

#### Signatures

```
find(type, map)
find(type, key, value)
find(type, uuid)
find(type, uuid)
```

#### Examples
##### 1. (StructrScript) Return the User entity with the UUID `b3175257898440ff99e78ca8fedfd832`
```
${find('User', 'b3175257898440ff99e78ca8fedfd832')}
```
##### 2. (JavaScript) Return the User entity with the UUID `b3175257898440ff99e78ca8fedfd832`
```
${{ $.find('User', 'b3175257898440ff99e78ca8fedfd832'); }}
```
##### 3. (StructrScript) Return all User entities in the database, sorted by name
```
${find('User', sort('name'))}
```
##### 4. (StructrScript) Return the first 10 User entities in the database, sorted by name
```
${find('User', sort('name'), page(1, 10))}
```
##### 5. (StructrScript) Return all user entities whose name property contains the letter 'e' (showcases the different ways to use the contains predicate)
```
${find('User', contains('name', 'e'))}
```
##### 6. (StructrScript) Return all user entities whose age property is greater than or equal to 18
```
${find('User', 'age', gte(18))}
```
##### 7. (StructrScript) Return all user entities whose age property is between 0 and 18 (inclusive!)
```
${find('User', 'age', range(0, 18))}
```
##### 8. (StructrScript) Return all user entities whose age property is between 0 and 18 (exclusive!)
```
${find('User', 'age', range(0, 18, false, false))}
```
##### 9. (StructrScript) Return all user entities whose name is 'Tester' and whose age is between 0 and 18 (inclusive). Showcases the implicit and explicit logical AND conjunction of root clauses
```
${find('User', equals('name', 'Tester'), equals('age', range(0, 18)))}
```
##### 10. (StructrScript) Return all user entities whose name is 'Tester' and whose age is between 0 and 18 (inclusive). Showcases the implicit and explicit logical AND conjunction of root clauses
```
${find('User', and(equals('name', 'Tester'), equals('age', range(0, 18))))}
```
##### 11. (StructrScript) Return all bakeries (type BakeryLocation extends Location) within 1000 meters around the Eiffel Tower
```
${find('BakeryLocation', and(withinDistance(48.858211, 2.294507, 1000)))}
```
##### 12. (JavaScript) Return the first 10 User entities in the database, sorted by name
```
${{
	$.find('User', $.predicate.sort('name'), $.predicate.page(1, 10))
}}

```
##### 13. (JavaScript) Returns all user entities whose `age` property is greater than or equal to 21.
```
${{
	// Note that the syntax is identical to StructrScript
	$.find('User', 'age', $.predicate.gte(21))
}}

```
##### 14. (JavaScript) Return all user entities whose `age` property is greater than or equal to 21.
```
${{
	// In JavaScript, we can use a map as second parameter
	// to have a more concise and programming-friendly way
	// of query building

	$.find('User', {
		'age': $.predicate.gte(21)
	})
}}

```
##### 15. (JavaScript) Returns all user entities whose `age` property is greater than or equal to 21.
```
${{
	// Instead of a map, we can use a predicate-only approach.
	$.find('User', $.predicate.equals('age', $.predicate.gte(21))
}}

```
##### 16. (JavaScript) Return the first 10 projects (sorted descending by name) where `name` equals "structr" and `age` is between 30 and 50 (inclusive)
```
${{
	let projects = $.find(
		'Project',
		{
			$and: {
				'name': 'structr',
				'age': $.predicate.range(30, 50)
			}
		},
		$.predicate.sort('name', true),
		$.predicate.page(1, 10)
	);

	return users;
}}

```
##### 17. (JavaScript) Only the user "joe" will be returned because the map key `name` is used twice (which overrides the first entry)
```
${{
	// Showcasing the *limitation* of the MAP SYNTAX: OR on the same property is not possible.
	let users = $.find(
		'User',
		{
			$or: {
				'name': 'jeff',
				'name': 'joe'
			}
		}
	);

	// Note: this returns the WRONG result!
	return users;
}}

```
##### 18. (JavaScript) Return all users named "joe" or "jeff"
```
${{
	// Showcasing the *advantage* of the PREDICATE SYNTAX: OR on the same property is possible.
	let users = $.find(
		'User',
		$.predicate.or(
			$.predicate.equals('name', 'jeff'),
			$.predicate.equals('name', 'joe')
		)
	);

	return users;
}}

```
##### 19. (JavaScript) Return all users named "joe" or "jeff"
```
${{
	// More elegant example where predicates are created before of the query
	let nameConditions = [
		$.predicate.equals('name', 'jeff'),
		$.predicate.equals('name', 'joe')
	];

	// This showcases how to create condition predicates before adding them to the query
	let users = $.find('User', $.predicate.or(nameConditions));

	return users;
}}

```
##### 20. (JavaScript) Return all users with the `isAdmin` flag set to true and named "joe" or "jeff".
```
${{
	// More advanced example where predicates are created before of the query
	let nameConditions = [
		$.predicate.equals('name', 'jeff'),
		$.predicate.equals('name', 'joe')
	];

	let rootPredicate = $.predicate.and(
		$.predicate.equal('isAdmin', true),
		$.predicate.or(nameConditions)
	);

	// This showcases how to create the complete predicate before the query
	let users = $.find('User', rootPredicate);

	return users;
}}

```

### find.and()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.and(predicates)
```


### find.contains()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.contains(key, value)
```


### find.empty()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.empty(key)
```


### find.endsWith()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.endsWith(key, value)
```


### find.equals()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.equals(value)
```


### find.gt()
Returns a gt predicate that can be used in find() function calls.
#### Signatures

```
find.gt(value)
```


### find.gte()
Returns a gte predicate that can be used in find() function calls.
#### Signatures

```
find.gte(value)
```


### find.lt()
Returns an lt predicate that can be used in find() function calls.
#### Signatures

```
find.lt(value)
```


### find.lte()
Returns an lte predicate that can be used in find() function calls.
#### Signatures

```
find.lte(value)
```


### find.not()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.not(predicate)
```


### find.or()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.or(predicates)
```


### find.page()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.page(page, pageSize)
```


### find.range()
Returns a range predicate that can be used in find() function calls.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|start|start of range interval|no|
|end|end of range interval|no|
|includeStart|true to include startpoint of given interval - default: false|yes|
|includeEnd|true to include endpoint of given interval - default: false|yes|


Returns a search predicate to specify value ranges, greater and less-than searches in [find()](53)  and [search()](109) functions.
The first two parameters represent the first and the last element of the desired query range. Both start and end of the range can be
set to `null` to allow the use of `range()` for `<`, `<=`. `>` and `>=` queries.
There are two optional boolean parameters, `includeStart` and `includeEnd` that control whether the search range
should **include** or **exclude** the endpoints of the interval.

#### Signatures

```
find.range(key, value)
```

#### Examples
##### Example 1 (StructrScript)
```
${find('Project', 'taskCount', range(0, 10))}
```
##### Example 2 (StructrScript)
```
${find('Project', 'taskCount', range(0, 10, true, false))}
```
##### Example 3 (StructrScript)
```
${find('Project', 'taskCount', range(0, 10, false, false))}
```
##### Example 4 (JavaScript)
```
${{ $.find('Project').forEach(p => $.print(p.index + ", ")) }}
> 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,

```
##### Example 5 (JavaScript)
```
${{ $.find('Project', { index: $.predicate.range(2, 5) }).forEach(p => $.print(p.index + ", ")); }}
> 2, 3, 4, 5,

```
##### Example 6 (JavaScript)
```
${{ $.find('Project', { index: $.predicate.range(2, 5, false, false) }).forEach(p => $.print(p.index + ", ")); }}
> 3, 4,

```
##### 7. (JavaScript) Range on datetime attribute
```
${{ $.find('Article', 'createdDate', $.predicate.range(
			$.parseDate('01.12.2024 00:00', 'dd.MM.yyyy hh:mm'),
			$.parseDate('03.12.2024 23:59', 'dd.MM.yyyy hh:mm')
	)
); }}

```

### find.sort()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.sort(key, value)
```


### find.startsWith()
Returns a query predicate that can be used with find() or search().
#### Signatures

```
find.startsWith(key, value)
```


### find.withinDistance()
Returns a query predicate that can be used with find() or search().
#### Parameters

|Name|Description|Optional|
|---|---|---|
|latitude|latitude of the center point|no|
|longitude|longitude of the center point|no|
|distance|circumference of the circle around the center point|no|

#### Signatures

```
find.withinDistance(latitude, longitude, distance)
```


### findPrivileged()
Executes a `find()` operation with elevated privileges.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type to return (includes inherited types|no|
|predicates|list of predicates|yes|
|uuid|uuid, makes the function return **a single object**|yes|


You can use this function to query data from an anonymous context or when a users privileges need to be escalated. See documentation of `find()` for more details.
#### Notes
- It is recommended to use `find()` instead of `findPrivileged()` whenever possible, as improper use of `findPrivileged()` can result in the exposure of sensitive data.
- In a StructrScript environment parameters are passed as pairs of 'key1', 'value1'.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
findPrivileged(type, options...)
```


### findRelationship()
Returns a collection of relationship entities of the given type from the database, takes optional key/value pairs.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type to return (includes inherited types|no|
|predicates|list of predicates|yes|
|uuid|uuid, makes the function return **a single object**|yes|

#### Notes
- The relationship type for custom schema relationships is auto-generated as `<source type name><relationship type><target type name>`
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
findRelationship(type [, parameterMap ])
```


### get()
Returns the value with the given name of the given entity, or an empty string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|node or object|no|
|propertyKey|requested property name|no|


Returns the value for the given property key from the given entity.
This function will print an error message if the first parameter is null / not accessible.
See `getOrNull()` for a more tolerant get method.

#### Notes
- The result value of the get() method can differ from the result value of property access using the dot notation (`get(this, 'name')` vs `this.name`) for certain property types (e.g. date properties), because get() converts the property value to its output representation.
- That means that a Date object will be formatted into a string when fetched via `get(this, 'date')`, whereas `this.date` will return an actual date object.

#### Signatures

```
get(entity, propertyName)
```

#### Examples
##### Example 1 (JavaScript)
```
${get(page, 'name')}
```
##### Example 2 (StructrScript)
```
${{ $.get(page, 'name') }}
```

### getIncomingRelationships()
Returns all incoming relationships between the given nodes, with an optional qualifying relationship type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|from|source node|no|
|to|target node|no|
|relType|relationship type|yes|

#### Signatures

```
getIncomingRelationships(source, target [, relType ])
```

#### Examples
##### Example 1 (StructrScript)
```
${getIncomingRelationships(page, me)}
```
##### Example 2 (JavaScript)
```
${{ $.getIncomingRelationships($.page, $.me) }}
```

### getOrCreate()
Returns an entity with the given properties, creating one if it doesn't exist.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of node|no|
|map|values map (only for javascript)|yes|
|key1|key for key-value-pair 1 (only for structrScript)|yes|
|value1|value for key-value-pair 1 (only for structrScript)|yes|
|key2|key for key-value-pair 2 (only for structrScript)|yes|
|value2|value for key-value-pair 2 (only for structrScript)|yes|
|keyN|key for key-value-pair N (only for structrScript)|yes|
|valueN|value for key-value-pair N (only for structrScript)|yes|


`getOrCreate()` finds and returns a single object with the given properties
(key/value pairs or a map of properties) and **creates** that object if it does not exist yet.
The function accepts three different parameter combinations, where the first parameter is always the
name of the type to retrieve from the database. The second parameter can either
be a map (e.g. a result from nested function calls) or a list of (key, value) pairs.

#### Notes
- The `getOrCreate()` method will always use **exact** search, if you are interested in inexact / case-insensitive search, use `search()`.
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
getOrCreate(type, propertyMap)
```

#### Examples
##### 1. (StructrScript) The example shows that repeated calls to `getOrCreate()` with the same parameters will always return the same object.
```
${getOrCreate('User', 'name', 'admin')}
> 7379af469cd645aebe1a3f8d52b105bd
${getOrCreate('User', 'name', 'admin')}
> 7379af469cd645aebe1a3f8d52b105bd
${getOrCreate('User', 'name', 'admin')}
> 7379af469cd645aebe1a3f8d52b105bd

```
##### 2. (JavaScript) The example shows that repeated calls to `getOrCreate()` with the same parameters will always return the same object.
```
${{ $.getOrCreate('User', {name: 'admin'}) }}
> 7379af469cd645aebe1a3f8d52b105bd
${{ $.getOrCreate('User', {name: 'admin'}) }}
> 7379af469cd645aebe1a3f8d52b105bd
${{ $.getOrCreate('User', {name: 'admin'}) }}
> 7379af469cd645aebe1a3f8d52b105bd

```

### getOrNull()
Returns the value with the given name of the given entity, or null.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|node or object|no|
|propertyKey|requested property name|no|


Returns the value for the given property key from the given entity, but doesn't print an error message when the given entity is not accessible.
See `get()` for the equivalent method that prints an error if the first argument is null.
#### Signatures

```
getOrNull(entity, propertyName)
```

#### Examples
##### Example 1 (StructrScript)
```
${getOrNull(page, 'name')}
```
##### Example 2 (JavaScript)
```
${{ $.getOrNull(page, 'name') }}
```

### getOutgoingRelationships()
Returns the outgoing relationships of the given entity with an optional relationship type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|from|source node|no|
|to|target node|no|
|relType|relationship type|yes|

#### Signatures

```
getOutgoingRelationships(source, target [, relType ])
```

#### Examples
##### Example 1 (StructrScript)
```
${getOutgoingRelationships(page, me)}
```
##### Example 2 (JavaScript)
```
${{ $.getOutgoingRelationships($.page, $.me) }}
```

### getRelationships()
Returns the relationships of the given entity with an optional relationship type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|from|source node|no|
|to|target node|no|
|relType|relationship type|yes|

#### Signatures

```
getRelationships(source, target [, relType ])
```

#### Examples
##### Example 1 (StructrScript)
```
${getRelationships(me, page)}
```
##### Example 2 (JavaScript)
```
${{ $.getRelationships($.me, $.page) }}
```

### hasIncomingRelationship()
Returns true if the given entity has incoming relationships of the given type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|from|entity the relationship goes from|no|
|to|entity the relationship goes to|no|
|relType|type of relationship|yes|


Returns a boolean value indicating whether **at least one** incoming relationship exists between the given entities, with an optional qualifying relationship type. See also `incoming()`, `outgoing()`, `has_relationship()` and `has_outgoing_relationship()`.
#### Signatures

```
hasIncomingRelationship(source, target [, relType ])
```

#### Examples
##### Example 1 (StructrScript)
```
${hasIncomingRelationship(me, page, 'OWNS')}
```
##### Example 2 (JavaScript)
```
${{ $.hasIncomingRelationship($.me, $.page, 'OWNS') }}
```

### hasOutgoingRelationship()
Returns true if the given entity has outgoing relationships of the given type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|from|entity the relationship goes from|no|
|to|entity the relationship goes to|no|
|relType|type of relationship|yes|


returns a boolean value indicating whether **at least one** outgoing relationship exists between the given entities, with an optional qualifying relationship type. See also `incoming()`, `outgoing()`, `has_relationship()` and `has_incoming_relationship()`.
#### Signatures

```
hasOutgoingRelationship(source, target [, relType ])
```

#### Examples
##### Example 1 (StructrScript)
```
${hasOutgoingRelationship(me, page, 'OWNS')}
```
##### Example 2 (JavaScript)
```
${{ $.hasOutgoingRelationship($.me, $.page, 'OWNS') }}
```

### hasRelationship()
Returns true if the given entity has relationships of the given type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity1|entity that has a relationship|no|
|entity2|entity that has a relationship|no|
|relType|type of relationship|yes|


Returns a boolean value indicating whether **at least one** relationship exists between the given entities, with an optional qualifying relationship type. See also `incoming()` and `outgoing()`.
#### Signatures

```
hasRelationship(source, target [, relType ])
```

#### Examples
##### Example 1 (StructrScript)
```
${hasRelationship(me, page, 'OWNS')}
```
##### Example 2 (JavaScript)
```
${{ $.hasRelationship($.me, $.page, 'OWNS') }}
```

### incoming()
Returns all incoming relationships of a node, with an optional qualifying relationship type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|entity to fetch relationships for|no|
|relType|relationship type|yes|


You can use this function in the Function Query section of a Repeater Element to access the relationships of a node. See also `outgoing()` and `hasRelationship()`.
#### Signatures

```
incoming(entity [, relType ])
```


### instantiate()
Converts the given raw Neo4j entity to a Structr entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|entity to instantiate|no|


You can use this function to convert raw Neo4j entities from a `cypher()` result into Structr entities.
#### Signatures

```
instantiate(node)
```


### isEntity()
Returns true if the given argument is a Structr entity (node or relationship).

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value|value to check|no|

#### Signatures

```
isEntity(value)
```


### jdbc()
Fetches data from a JDBC source.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|JDBC url to connect to the server|no|
|query|query to execute|no|
|username|username to use to connect|yes|
|password|password to used to connect|yes|


Make sure the driver specific to your SQL server is available in a JAR file in Structr's lib directory (`/usr/lib/structr/lib` in Debian installations).

Other JAR sources are available for Oracle (https://www.oracle.com/technetwork/database/application-development/jdbc/downloads/index.html) or MSSQL (https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server).

For other SQL Servers, please consult the documentation of that server.

#### Notes
- Username and password can also be included in the JDBC connection string.

#### Signatures

```
jdbc(jdbcUrl, sqlQuery[, username, password])
```

#### Examples
##### 1. (JavaScript) Fetch data from an Oracle database
```
{
    let rows = $.jdbc('jdbc:oracle:thin:test/test@localhost:1521:orcl', 'SELECT * FROM test.emails');

    rows.forEach(function(row) {

	$.log('Fetched row from Oracle database: ', row);

	let fromPerson = $.getOrCreate('Person', 'name', row.fromAddress);
	let toPerson   = $.getOrCreate('Person', 'name', row.toAddress);

	let message = $.getOrCreate('EMailMessage',
	    'content',   row.emailBody,
	    'sender',    fromPerson,
	    'recipient', toPerson,
	    'sentDate',  $.parseDate(row.emailDate, 'yyyy-MM-dd')
	);

	$.log('Found existing or created new EMailMessage node: ', message);
    });
}

```

### mongodb()
Opens a connection to a MongoDB source and returns a MongoCollection which can be used to further query the Mongo database.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|connection URL to MongoDB|no|
|database|name of the database to connect to|no|
|collection|name of the collection to fetch|no|

#### Notes
- The returned MongoCollection object has the functions exposed as described in https://mongodb.github.io/mongo-java-driver/4.2/apidocs/mongodb-driver-sync/com/mongodb/client/MongoCollection.html
- Native MongoDB operators (https://docs.mongodb.com/manual/reference/operator/) can be used.
- Every function without parameters or with Bson parameter can be used.
- Creating a Bson object is done with the `$.bson()` function which simple converts a JSON object to Bson.
- The result of a `collection.find()` is not a native JS array, so functions like `.filter()` or `.map()` are not available - the `for of` syntax applies.
- The records in a result are also not native JS objects, so the dot notation (i.e. `record.name`) does not work - the `record.get('name')` syntax applies.
- All examples assume a MongoDB instance has been locally started via Docker with the following command: docker run -ti -p 27017:27017 mongo

#### Signatures

```
mongodb(url, database, collection)
```

#### Examples
##### 1. (JavaScript) Open connection, insert object and retrieve objects with identical name
```
{
	// Open the connection to mongo and return the testCollection
	let collection = $.mongodb('mongodb://localhost', 'testDatabase', 'testCollection');

	// Insert a record
	collection.insertOne($.bson({
		name: 'Test4'
	}));

	// Query all records with a give property set
	return collection.find($.bson({ name: 'Test4' }));
}

```
##### 2. (JavaScript) Open connection and find objects with regex name
```
{
	// Open the connection to mongo and return the testCollection
	let collection = $.mongodb('mongodb://localhost', 'testDatabase', 'testCollection');

	// Query all records with a give property set
	return collection.find($.bson({ name: { $regex: 'Test[0-9]' } }));
}

```
##### 3. (JavaScript) Open connection, insert object with date and query all objects with dates greater than equal (gte) that date
```
{
	// Open the connection to mongo and return the testCollection
	let collection = $.mongodb('mongodb://localhost', 'testDatabase', 'testCollection');

	 // Insert a record
	collection.insertOne($.bson({
		name: 'Test9',
		date: new Date(2018, 1, 1)
	}));

	return collection.find($.bson({ date: { $gte: new Date(2018, 1, 1) } }));
}

```

### outgoing()
Returns all outgoing relationships of a node, with an optional qualifying relationship type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|entity to fetch relationships for|no|
|relType|relationship type|yes|


You can use this function in the Function Query section of a Repeater Element to access the relationships of a node. See also `outgoing()` and `hasRelationship()`.
#### Signatures

```
outgoing(entity [, relType ])
```


### prefetch()
Prefetches a subgraph.
#### Signatures

```
prefetch(query, listOfKeys)
```


### remoteCypher()
Returns the result of the given Cypher query against a remote instance.
#### Signatures

```
remoteCypher(url, username, password, query [, parameterMap ])
```


### removeLabels()
Removes the given set of labels from the given node.
#### Signatures

```
removeLabels(node, labels)
```


### rollbackTransaction()
Marks the current transaction as failed and prevents all objects from being persisted in the database.
#### Signatures

```
rollbackTransaction()
```


### search()
Returns a collection of entities of the given type from the database, takes optional key/value pairs. Searches case-insensitive / inexact.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type to return (includes inherited types|no|
|predicates|list of predicates|yes|
|uuid|uuid, makes the function return **a single object**|yes|


The `search()` method is very similar to `find()`, except that it is case-insensitive / inexact. It returns a collection of entities,
which can be empty if none of the existing nodes or relationships matches the given search parameters.
`search()` accepts several different parameter combinations, whereas the first parameter is always the name of
the type to retrieve from the database. The second parameter can either be a map (e.g. a result from nested function calls)
or a list of (key, value) pairs. Calling `search()` with only a single parameter will return all the nodes of the
given type (which might be dangerous if there are many of them in the database).

For more examples see `find()`.

#### Signatures

```
search(type, options...)
```

#### Examples
##### Example 1 (StructrScript)
```
// For this example, we assume that there are three users in the database: [admin, tester1, tester2]
${search('User')}
> [7379af469cd645aebe1a3f8d52b105bd, a05c044697d648aefe3ae4589af305bd, 505d0d469cd645aebe1a3f8d52b105bd]
${search('User', 'name', 'test')}
> [a05c044697d648aefe3ae4589af305bd, 505d0d469cd645aebe1a3f8d52b105bd]

```
##### Example 2 (JavaScript)
```
${{ $.search('User', 'name', 'abc')} }
```

### searchFulltext()
Returns a map of entities and search scores matching the given search string from the given fulltext index.

Searches are **case-insensitive**. A query term matches only if the **complete token** exists in the fulltext index; partial substrings inside a longer token won't match unless that exact token is indexed.

For type attributes with the fulltext index flag enabled, the fulltext index name is auto-generated as `<type name>_<attribute name>_fulltext` (for example, `Employee_firstName_fulltext`).

Typically, the following features are available. Refer to the documentation for the database you're connecting to.

Wildcards:

- `?` matches exactly one character
- `*` matches zero or more characters

For performance reasons, avoid placing `*` at the beginning of a term (for example `*test`), because leading wildcards can make queries much more expensive/slower. Prefer anchored patterns like `test*` where possible.

Query modifiers:

- `~` enables fuzzy matching for the preceding term, allowing similar words rather than exact equality.

#### Signatures

```
searchFulltext(indexName, searchString)
```

#### Examples
##### 1. (JavaScript) Wildcard fulltext search for all employees with a senior role
```
${{
	$.searchFulltext("Employee_title_fulltext", "Senior*");
}}

```
##### 2. (JavaScript) Fuzzy fulltext search for all employees with names similar to 'alex'
```
${{
	$.searchFulltext("Employee_firstName_fulltext", "alex~");
}}

```

### searchRelationshipsFulltext()
Returns a map of entities and search scores matching the given search string from the given fulltext index. Searches case-insensitve / inexact.
#### Signatures

```
searchRelationshipsFulltext(indexName, searchString)
```


### set()
Sets a value or multiple values on an entity. The values can be provided as a map or as a list of alternating keys and values.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|node to be updated|no|
|map|parameterMap (only JavaScript)|no|
|key1|key1 (only StructrScript)|no|
|value1|value for key1 (only StructrScript)|no|
|key2|key2 (only JavaScript)|no|
|value2|value for key1 (only StructrScript)|no|
|keyN|keyN (only JavaScript)|no|
|valueN|value for keyN (only StructrScript)|no|


Sets the passed values for the given property keys on the specified entity, using the security context of the current user.
`set()` accepts several different parameter combinations, where the first parameter is always a graph object.
The second parameter can either be a list of (key, value) pairs, a JSON-coded string (to accept return values of the
`geocode()` function) or a map (e.g. a result from nested function calls or a simple map built in serverside JavaScript).

#### Notes
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.
- When using the `set()` method on an entity that is not writable by the current user, a 403 Forbidden HTTP error will be thrown. In this case, use `set_privileged()` which will execute the `set()` operation with elevated privileges.

#### Signatures

```
set(entity, parameterMap)
```

#### Examples
##### Example 1 (StructrScript)
```
${set(user, 'name', 'new-user-name', 'eMail', 'new@email.com')}
```
##### Example 2 (StructrScript)
```
${set(page, 'name', 'my-page-name')}
```
##### Example 3 (JavaScript)
```
${{
    let me = $.me;
    $.set(me, {name: 'my-new-name', eMail: 'new@email.com'});
}}

```

### setPrivileged()
Sets the given key/value pair(s) on the given entity with super-user privileges.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|URL to connect to|no|
|map|parameterMap (only JavaScript)|no|
|key1|key1 (only StructrScript)|no|
|value1|value for key1 (only StructrScript)|no|
|key2|key2 (only JavaScript)|no|
|value2|value for key1 (only StructrScript)|no|
|keyN|keyN (only JavaScript)|no|
|valueN|value for keyN (only StructrScript)|no|

#### Notes
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
setPrivileged(entity, parameterMap)
```

#### Examples
##### Example 1 (StructrScript)
```
${ setPrivileged(page, 'accessCount', '2')}
```
##### Example 2 (JavaScript)
```
${{ $.setPrivileged($.page, 'accessCount', '2')} }}
```

## EMail Functions

### sendHtmlMail()
Sends an HTML email.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|fromAddress|sender address|no|
|fromName|sender name|no|
|toAddress|recipient address|no|
|toName|recipient name|no|
|subject|subject|no|
|htmlContent|HTML content|no|
|textContent|text content|no|
|files|collection of file nodes to send as attachments|yes|

#### Notes
- Attachments must be provided as a list, even when only a single file is included.
- `htmlContent` and `textContent` are typically generated using the `template()` function.
- Emails are sent based on the SMTP configuration defined in structr.conf.
- For advanced scenarios, refer to the extended mail functions prefixed with `mail_`, beginning with `mailBegin()`.

#### Signatures

```
sendHtmlMail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files])
```

#### Examples
##### Example 1 (StructrScript)
```
${sendPlaintextMail('info@structr.com', 'Structr', 'user@domain.com', 'Test User', 'Welcome to Structr', 'Hi User, welcome to <b>Structr</b>!', 'Hi User, welcome to Structr!', find('File', 'name', 'welcome-to-structr.pdf')))}
```

### sendPlaintextMail()
Sends a plaintext email.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|fromAddress|sender address|no|
|fromName|sender name|no|
|toAddress|recipient address|no|
|toName|recipient name|no|
|subject|subject|no|
|textContent|text content|no|

#### Notes
- `textContent` is typically generated using the `template()` function.
- Emails are sent based on the SMTP configuration defined in structr.conf.
- For advanced scenarios, refer to the extended mail functions prefixed with `mail_`, beginning with `mailBegin()`.

#### Signatures

```
sendPlaintextMail(fromAddress, fromName, toAddress, toName, subject, content)
```

#### Examples
##### Example 1 (StructrScript)
```
${sendPlaintextMail('info@structr.com', 'Structr', 'user@domain.com', 'Test User', 'Welcome to Structr', 'Hi User, welcome to Structr!')}
```

## Geocoding Functions

### geocode()
Returns the geolocation (latitude, longitude) for the given street address using the configured geocoding provider.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|street|street of place to geocode|no|
|city|city of place to geocode|no|
|country|country of place to geocode|no|


Returns the geocoding result for the given parameters.
See Geocoding Configuration for more information.
This function returns a nested object with latitude / longitude that can directly be used in the `set()` method.

#### Notes
- An API Key (`geocoding.apikey`) has to be configured in structr.conf.
- Also this key is configurable through **Config -> Advanced Settings**.

#### Signatures

```
geocode(street, city, country)
```

#### Examples
##### Example 1 (StructrScript)
```
${set(this, geocode(this.street, this.city, this.country))}
```
##### Example 2 (JavaScript)
```
${{ $.set(this, $.geocode(this.street, this.city, this.country)) }}
```

## Http Functions

### DELETE()
Sends an HTTP DELETE request with an optional content type to the given URL and returns the response headers and body.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|contentType|content type|yes|


This function can be used in a script to make an HTTP DELETE request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `DELETE()` function will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

#### Notes
- The `DELETE()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

#### Signatures

```
DELETE(url [, contentType])
```

#### Examples
##### Example 1 (StructrScript)
```
${DELETE('http://localhost:8082/structr/rest/User/6aa10d68569d45beb384b42a1fc78c50')}
```

### GET()
Sends an HTTP GET request to the given URL and returns the response headers and body.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|contentType|expected content type (see notes)|yes|
|username|username for the connection|yes|
|password|password for the connection|yes|


This function can be used in a script to make an HTTP GET request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `GET()` function will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

#### Notes
- From version 3.5 onwards, GET() supports binary content by setting the `contentType` parameter to `application/octet-stream`. (This is helpful when creating files - see example.)
- v4.0+: `contentType` can be used like the `Content-Type` header - to set the **expected** response mime type and to set the `charset` with which the response will be interpreted (**unless** the server sends provides a charset, then this charset will be used).
- Prior to v4.0: `contentType` is the **expected** response content type (it does not influence the charset of the response - the charset from the **sending server** will be used).
- The parameters `username` and `password` are intended for HTTP Basic Auth. For header authentication use `addHeader()`.
- The `GET()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

#### Signatures

```
GET(url [, contentType [, username, password]])
GET(url [, contentType [, username, password]])
GET(url, 'text/html', selector)
GET(url, 'application/octet-stream' [, username, password]])
```

#### Examples
##### 1. (StructrScript) Return an 'Access denied' error message with code 401 from the local Structr instance (depending on the configuration of that instance), because you cannot access the User collection from the outside without authentication.
```
${GET('http://localhost:8082/structr/rest/User')}
```
##### 2. (StructrScript) Return the list of users from the local Structr instance (depending on the configuration of that instance).
```
${
	(
	  addHeader('X-User', 'admin'),
	  addHeader('X-Password', 'admin'),
	  GET('http://localhost:8082/structr/rest/User')
	)
}

```
##### 3. (StructrScript) Return the HTML source code of the front page of google.com.
```
${GET('http://www.google.com', 'text/html')}
```
##### 4. (StructrScript) Return the HTML source code of the front page of google.com (since the server sends a charset in the response, the given charset parameter is overridden).
```
${GET('http://www.google.com', 'text/html; charset=UTF-8')}
```
##### 5. (StructrScript) Return the HTML source code of the front page of google.com (since the server sends a charset in the response, the given charset parameter is overridden).
```
${GET('http://www.google.com', 'text/html; charset=ISO-8859-1')}
```
##### 6. (StructrScript) Return the HTML content of the element with the ID 'footer' from google.com.
```
${GET('http://www.google.com', 'text/html', '#footer')}
```
##### 7. (StructrScript) Create a new file with the google logo in the local Structr instance.
```
${setContent(create('File', 'name', 'googleLogo.png'), GET('https://www.google.com/images/branding/googlelogo/1x/googlelogoLightColor_272x92dp.png', 'application/octet-stream'))}
```

### HEAD()
Sends an HTTP HEAD request with optional username and password to the given URL and returns the response headers.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|username|username for the connection|yes|
|password|password for the connection|yes|


This function can be used in a script to make an HTTP HEAD request **from within the Structr Server**, triggered by a frontend control like a button etc. The optional username and password parameters can be used to authenticate the request.

The `HEAD()` function will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |

#### Notes
- The `HEAD()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

#### Signatures

```
HEAD(url [, username, password]])
```


### PATCH()
Sends an HTTP PATCH request to the given URL and returns the response headers and body.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|body|request body (JSON data)|yes|
|contentType|content type of the request body|yes|
|charset|charset of the request body|yes|


This function can be used in a script to make an HTTP PATCH request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `PATCH()` function will return a response object containing the response headers, body and status code. The object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

#### Notes
- The `PATCH()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

#### Signatures

```
PATCH(url, body [, contentType, charset ])
```


### POST()
Sends an HTTP POST request to the given URL and returns the response body.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|body|request body (JSON data)|yes|
|contentType|content type of the request body|yes|
|charset|charset of the request body|yes|
|username|username for the connection|yes|
|password|password for the connection|yes|
|configMap|JSON object for request configuration, supports `timeout` in seconds, `redirects` with true or false to follow redirects|yes|


This function can be used in a script to make an HTTP POST request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `POST()` function will return a response object containing the response headers, body and status code. The object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. config = { timeout: 60, redirects: true } ). By default there is not timeout and redirects are not followed.

#### Notes
- The `POST()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.
- `contentType` is the expected response content type. If you need to define the request content type, use `addHeader('Content-Type', 'your-content-type-here')`
- If the `contentType` is `application/json`, the response body is automatically parsed and the `body` key of the returned object is a map

#### Signatures

```
POST(url, body [, contentType, charset, username, password, configMap ])
```


### POST_multiPart()
Sends a multi-part HTTP POST request to the given URL and returns the response body.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|partsMap|map with multipart parts (type, content)|yes|
|responseContentType|expected content type of the response body|yes|


This function can be used in a script to make a multi-part HTTP POST request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `POST()` function will return a response object containing the response headers, body and status code. The object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. config = { timeout: 60, redirects: true } ). By default there is not timeout and redirects are not followed.

#### Notes
- The `POST()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

#### Signatures

```
POST_multiPart(url, partsMap [, responseContentType])
```


### PUT()
Sends an HTTP PUT request with an optional content type to the given URL and returns the response headers and body.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|URL to connect to|no|
|body|request body (JSON data)|yes|
|contentType|content type of the request body|yes|
|charset|charset of the request body|yes|
|username|username for the connection|yes|
|password|password for the connection|yes|
|configMap|JSON object for request configuration, supports `timeout` in seconds, `redirects` with true or false to follow redirects|yes|


This function can be used in a script to make an HTTP PUT request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `PUT()` function will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

#### Notes
- The `PUT()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.
- `contentType` is the expected response content type. If you need to define the request content type, use `addHeader('Content-Type', 'your-content-type-here')`
- If the MIME type of the response is `application/json`, the `body` field will contain the mapped response as a Structr object that can be accessed using the dot notation (e.g. `result.body.resultCount`). Otherwise, the body field will contain the response as a string.  (see the related articles for more information)

#### Signatures

```
PUT(url, body [, contentType, charset ])
```


### addHeader()
Temporarily adds the given (key, value) tuple to the local list of request headers.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|name of the header field|no|
|value|value of the header field|no|


All headers added with this function will be sent with any subsequent `GET()`, `HEAD()`, `POST()`, `PUT()` or `DELETE()` call in the same request (meaning the request from the client to Structr).
#### Notes
- Prior to 3.5.0 it was not possible to remove headers. In 3.5.0 this function was changed to remove a header if `value = null` was provided as an argument.

#### Signatures

```
addHeader(name, value)
```

#### Examples
##### 1. (StructrScript) Authenticate an HTTP GET request with addHeader (StructrScript version)
```
${
    (
	addHeader('X-User', 'tester1'),
	addHeader('X-Password', 'test'),
	GET('http://localhost:8082/structr/rest/User')
    )
}

```
##### 2. (JavaScript) Authenticate an HTTP GET request with addHeader (JavaScript version)
```
	${{
	    $.addHeader('X-User', 'tester1');
	    $.addHeader('X-Password', 'test');
	    let result = $.GET('http://localhost:8082/structr/rest/User');
	}}

```

### clearHeaders()
Clears headers for the next HTTP request.

Removes all headers previously set with `addHeader()` in the same request. This function is a helper for the HTTP request functions that make HTTP requests **from within the Structr Server**, triggered by a frontend control like a button etc.

#### Notes
- This is important if multiple calls to the family of HTTP functions is made in the same request to clear the headers in between usages to prevent sending the wrong headers in subsequent requests.

#### Signatures

```
clearHeaders()
```


### getCookie()
Returns the requested cookie if it exists.
#### Signatures

```
getCookie(name)
```


### getRequestHeader()
Returns the value of the given request header field.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|name of request header field|no|


This function can be used both in Entity Callback Functions and in the Page Rendering process to obtain the value of a given HTTP header, allowing the user to use HTTP headers from their web application clients to control features of the application.
#### Signatures

```
getRequestHeader(name)
```

#### Examples
##### Example 1 (StructrScript)
```
${getRequestHeader('User-Agent')}
```
##### Example 2 (JavaScript)
```
${{ $.getRequestHeader('User-Agent') }}
```

### removeResponseHeader()
Removes the given header field from the server response.
#### Signatures

```
removeResponseHeader(field)
```


### setCookie()
Sets the given cookie.
#### Signatures

```
setCookie(name, value[, secure[, httpOnly[, maxAge[, domain[, path]]]]])
```


### setResponseCode()
Sets the response code of the current rendering run.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|code|HTTP response code|no|


Very useful in conjunction with `setResponseHeader()` for redirects.
#### Signatures

```
setResponseCode(code)
```

#### Examples
##### Example 1 (StructrScript)
```
${setResponseCode(302)}
```
##### Example 2 (JavaScript)
```
${{ $.setResponseCode(302) }}
```

### setResponseHeader()
Adds the given header field and value to the response of the current rendering run.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|HTTP header name|no|
|value|HTTP header value|no|
|override|override previous header|yes|


Sets the value of the HTTP response header with the given name to the given value.
This function can be used to set and/or override HTTP response headers in the Structr server implementation to
control certain aspects of browser / client behaviour.

#### Notes
- The following example will cause the browser to display a 'Save as...' dialog when visiting the page, because the response content type is set to `text/csv`.

#### Signatures

```
setResponseHeader(name, value [, override = false ])
```

#### Examples
##### Example 1 (StructrScript)
```
${setResponseHeader('Content-Type', 'text/csv')}
```
##### Example 2 (JavaScript)
```
${{ $.setResponseHeader('Content-Type', 'text/csv') }}
```

### validateCertificates()
Disables or enables strict certificate checking when performing a request in a scripting context. The setting remains for the whole request.

Disables or enables certificate validation for outgoing requests. All subsequent `GET()`, `HEAD()`, `POST()`, `PUT()` or `DELETE()` calls in the same request (meaning the request from the client to Structr) will use the setting configured here.
#### Notes
- By default, certificate validation is always enabled - only in rare cases would/should it be necessary to change this behaviour

#### Signatures

```
validateCertificates(boolean)
```

#### Examples
##### Example 1 (JavaScript)
```
	${{
	    $.validateCertificates(false);
	    let result = $.GET('https://www.domain-with-invalid-certificate.com/resource.json');
	}}

```

## Input Output Functions

### append()
Appends text to a file in the `exchange/` folder.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|filename|name of the file to append to|no|
|text|text to append|yes|


This function appends the given text to the file with the given file name in the exchange/ folder. If the file does not exist yet, it will be created.

To prevent data leaks, Structr allows very limited access to the underlying file system. The only way to read or write files on the harddisk is to use files in the exchange/ folder of the Structr runtime directory. All calls to `read()`, `write()` and `append()` will check that before reading from or writing to the disk.";

#### Notes
- The `exchange/` folder itself may be a symbolic link.
- The canonical path of a file has to be identical to the provided filepath in order to prevent directory traversal attacks. This means that symbolic links inside the `exchange/` folder are forbidden
- Absolute paths and relative paths that traverse out of the exchange/ folder are forbidden.
- Allowed 'sub/dir/file.txt'
- Forbidden '../../../../etc/passwd'
- Forbidden '/etc/passwd'

#### Signatures

```
append(filename, text)
```

#### Examples
##### 1. (StructrScript) Append the text 'hello world' to a file in the exchange/ folder.
```
${append('test.txt', 'hello world')}
```
##### 2. (JavaScript) Append the text 'hello world' to a file in the exchange/ folder.
```
${{ $.append('test.txt', 'hello world'); }}
```

### appendContent()
Appends content to a Structr File.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|file|Structr File entity to append the content to|no|
|content|content to append|no|
|encoding|encoding to use, e.g. 'UTF-8'|yes|


This function appends text or binary data to the content of the given Structr File entity.
#### Notes
- The `encoding` parameter is used when writing the data to the file. By default the input is not encoded, but when given an encoding such as `UTF-8` the content is transformed before being written to the file.

#### Signatures

```
appendContent(file, content [, encoding ])
```

#### Examples
##### 1. (StructrScript) Append the string '\nAdditional Content' to the file with the name 'test.txt'.
```
appendContent(first(find('File', 'name', 'test.txt')), '\nAdditional Content')
```

### barcode()
Creates a barcode image of given type with the given data.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of barcode to create|no|
|data|data to encode|no|
|width|width of the resulting image|yes|
|height|height of the resulting image|yes|
|hints|map of hints to control barcode details|yes|


The following barcode types are supported.

| Barcode Type | Result |
|---|---|
| AZTEC | Aztec 2D barcode format (beta) |
| CODABAR | CODABAR 1D format |
| CODE_39 | Code 39 1D format |
| CODE_93 | Code 93 1D format |
| CODE_128 | Code 128 1D format |
| DATA_MATRIX | Data Matrix 2D barcode format |
| EAN_8 | EAN-8 1D format |
| EAN_13 | EAN-13 1D format |
| ITF | ITF (Interleaved Two of Five) 1D format |
| PDF_417 | PDF417 format (beta) |
| QR_CODE | QR Code 2D barcode format |
| UPC_A | UPC-A 1D format |
| UPC_E | UPC-E 1D format |

Most barcode types support different hints which are explained [here](https://zxing.github.io/zxing/apidocs/index.html?com/google/zxing/EncodeHintType.html). Probably the most interesting hints are `MARGIN` and `ERROR_CORRECTION`. Following is an excerpt from the source code. More information on the error correction level can be found [here](https://zxing.github.io/zxing/apidocs/index.html?com/google/zxing/qrcode/decoder/ErrorCorrectionLevel.html)

| Hint | Description |
| --- | --- |
| MARGIN | Specifies margin, in pixels, to use when generating the barcode. The meaning can vary by format; for example it controls margin before and after the barcode horizontally for most 1D formats. (Type Integer, or String representation of the integer value).|
| ERROR_CORRECTION | Specifies what degree of error correction to use, for example in QR Codes. Type depends on the encoder. For example for QR codes it's type ErrorCorrectionLevel. For Aztec it is of type Integer, representing the minimal percentage of error correction words. For PDF417 it is of type  Integer, valid values being 0 to 8. In all cases, it can also be a String representation of the desired value as well. Note: an Aztec symbol should have a minimum of 25% EC words. |

#### Notes
- In StructrScript, you can provide alternating key and value entries , i.e. key1, value1, key2, value2, ...

#### Signatures

```
barcode(type, data [, width, height, hintKey, hintValue ])
```

#### Examples
##### 1. (StructrScript) Example usage in a dynamic file
```
File content: ${barcode('QR_CODE', 'My testcode', 200, 200, "MARGIN", 0, "ERROR_CORRECTION", "Q")}
File contentType: image/png; charset=iso-8859-1

```
##### 2. (StructrScript) Usage in an HTML IMG element
```
<img src="data:image/png;base64, ${base64encode(barcode('QR_CODE', 'My testcode', 200, 200, 'MARGIN', 0, 'ERROR_CORRECTION', 'Q'), 'basic', 'ISO-8859-1')}" />
```

### broadcastEvent()
Triggers the sending of a sever-sent event to all authenticated and/or anonymous users with an open connection.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|eventType|type of event to send, usually `message`|no|
|message|message to send|no|
|authenticatedUsers|whether to send messages to authenticated users, defaults to `true`|yes|
|anonymousUser|whether to send messages to anonymous users, defaults to `false`|yes|


The `broadcastEvent()` function implements the server-side part of server-sent events based on the EventSource servlet. Server-sent events allow you to send messages from the server to the client asynchronously, e.g. you can update data or trigger a reload based on events that happen on ther server.

See https://developer.mozilla.org/en-US/docs/Web/API/EventSource for more information about server-sent events.

In order to use server-sent events, you need to enable the EventSource servlet in structr.conf. After that, you can use the below code in your HTML frontend to start receiving events.

**Example setup in HTML**
```
// this needs to be done in your frontend HTML, not on the server
var source = new EventSource("/structr/EventSource", { withCredentials: true });
source.onmessage = function(event) {
	console.log(event);
};
```

#### Notes
- In order to use server-sent events, you need to enable the EventSource servlet in structr.conf.
- If you want to use the generic `onmessage` event handler in your frontend, the messageType **must** be set to `message`. For message types other than `message`, you need to add a dedicated event listener to the EventSource instance using `addEventListener()` in your frontend.

#### Signatures

```
broadcastEvent(eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]])
```

#### Examples
##### 1. (StructrScript) Send a generic message to the frontend
```
${ broadcastEvent('message', 'Hello world!', true, false) }
```
##### 2. (JavaScript) Send a JSON message to the frontend
```
${{ $.broadcastEvent('message', JSON.stringify({id: 'APP_MAINTENANCE_SOON', message: 'Application going down for maintenance soon!', date: new Date().getTime()}), true, false); }}
```

### config()
Returns the configuration value associated with the given key from structr.conf.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key to read from structr.conf|no|
|defaultValue|default value to use if the configuration key is not present|yes|


This function can be used to read values from the configuration file and use it to configure frontend behaviour, default values etc. The optional second parameter is the default value to be returned if the configuration key is not present.
#### Notes
- For security reasons the superuser password can not be read with this function.

#### Signatures

```
config(configKey [, defaultValue ])
```


### copyFileContents()
Copies the content of sourceFile to targetFile and updates the meta-data accordingly.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|sourceFile|source file to copy content from|no|
|targetFile|target file to copy content to|no|

#### Signatures

```
copyFileContents(sourceFile, destinationFile)
```


### createArchive()
Creates and returns a ZIP archive with the given files (and folders).
#### Parameters

|Name|Description|Optional|
|---|---|---|
|archiveFileName|name of the resulting archive (without the .zip suffix)|no|
|filesOrFolders|file, folder or list thereof to add to the archive|no|
|customFileType|custom archive type other than `File` (must be a subtype of `File`)|yes|


This function creates a ZIP archive with the given files and folder and stores it as a File with the given name in Structr's filesystem. The second parameter can be either a single file, a single folder or a list of files and folders, but all of the objects must be Structr entities. The third parameter can be used to set the node type of the resulting archive to something other than `File`, although the given type must be a subtype of `File`.
#### Notes
- The resulting file will be named `archiveFileName` + `.zip` and will be put in the root folder of the structr filesystem.
- The second parameter can be a single file, a collection of files, a folder, a collection of folders or a mixture.
- If you use the result of a `find()` call to collect the files and folder for the archive, please note that there can be multiple folders with the same name that might end up in the archive.
- You can set the destination folder of the created archive by setting the `parent` property of the returned entity.

#### Signatures

```
createArchive(fileName, files [, customFileTypeName ])
```

#### Examples
##### 1. (StructrScript) Create an archive named `logs.zip` with the contents of all Structr Folders named "logs"
```
${createArchive('logs', find('Folder', 'name', 'logs'))}
```
##### 2. (JavaScript) Create an archive named `logs.zip` with the contents of exactly one Structr Folder
```
${{
	// find a single folder with an absolute path
	let folders = $.find('Folder', { path: '/data/logs' }));
	if (folders.length > 0) {

		// use the first folder here
		let archive = $.createArchive('logs', folders[0]);
	}
}}

```
##### 3. (JavaScript) Create an archive named `logs.zip` with the contents of all Structr Folders named \"logs\"
```
${{
	// find all the folders with the name "logs"
	let folders = $.find('Folder', { name: 'logs' }));
	let archive = $.createArchive('logs', folders);
}}

```
##### 4. (JavaScript) Create an archive and put it in a specific parent folder
```
${{
	let parentFolder = $.getOrCreate('Folder', { name: 'archives' });
	let files        = $.methodParameters.files;
	let name         = $.methodParameters.name;

	let archive = $.createArchive(name, files);

	archive.parent = parentFolder;
}}

```

### createFolderPath()
Creates a new folder in the virtual file system including all parent folders if they don't exist already.
#### Signatures

```
createFolderPath(type [, parameterMap ])
```


### createZip()
Creates and returns a ZIP archive with the given files (and folders).
#### Parameters

|Name|Description|Optional|
|---|---|---|
|archiveFileName|name of the resulting archive (without the .zip suffix)|no|
|filesOrFolders|file, folder or list thereof to add to the archive|no|
|password|password to encrypt the resulting ZIP file|yes|
|encryptionType|encryptionType to encrypt the resulting ZIP file, e.g. 'aes'|yes|


This function creates a ZIP archive with the given files and folder and stores it as a File with the given name in Structr's filesystem. The second parameter can be either a single file, a single folder or a list of files and folders, but all of the objects must be Structr entities. If the third parameter is set, the resulting archive will be encrypted with the given password.
#### Notes
- Creates and returns a ZIP archive with the given name (first parameter), containing the given files/folders (second parameter).
- By setting a password as the optional third parameter, the ZIP file will be encrypted.
- If the optional fourth parameter is `aes` or `AES`, the ZIP file will be encrypted with the AES256 method.

#### Signatures

```
createZip(archiveFileName, files [, password [, encryptionMethod ] ])
```

#### Examples
##### 1. (StructrScript) Create an archive named `logs.zip` with the contents of all Structr Folders named "logs"
```
${createZip('logs', find('Folder', 'name', 'logs'))}
```
##### 2. (JavaScript) Create an archive named `logs.zip` with the contents of exactly one Structr Folder
```
${{
	// find a single folder with an absolute path
	let folders = $.find('Folder', { path: '/data/logs' }));
	if (folders.length > 0) {

		// use the first folder here
		let archive = $.createZip('logs', folders[0]);
	}
}}

```
##### 3. (JavaScript) Create an archive named `logs.zip` with the contents of all Structr Folders named \"logs\"
```
${{
	// find all the folders with the name "logs"
	let folders = $.find('Folder', { name: 'logs' }));
	let archive = $.createZip('logs', folders);
}}

```
##### 4. (JavaScript) Create an archive and put it in a specific parent folder
```
${{
	let parentFolder = $.getOrCreate('Folder', { name: 'archives' });
	let files        = $.methodParameters.files;
	let name         = $.methodParameters.name;

	let archive = $.createZip(name, files);

	archive.parent = parentFolder;
}}

```

### decrypt()
Decrypts a base 64 encoded AES ciphertext and returns the decrypted result.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|encryptedText|base64-encoded ciphertext to decrypt|no|
|secret|secret key|yes|


This function either uses the internal global encryption key from the 'application.encryption.secret' setting in structr.conf, or the optional second parameter.
#### Signatures

```
decrypt(value [, secret ])
```

#### Examples
##### 1. (StructrScript) Decrypt a string with the global encryption key from structr.conf
```
${print(decrypt(this.encryptedString))}
```
##### 2. (StructrScript) Decrypt a string with the key 'secret key'
```
${print(decrypt(this.encryptedString'), 'secret key')}
```

### encrypt()
Encrypts the given string using AES and returns the ciphertext encoded in base 64.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|text|text to encrypt|no|
|secret|secret key|yes|


This function either uses the internal global encryption key from the 'application.encryption.secret' setting in structr.conf, or the optional second parameter.
#### Signatures

```
encrypt(value [, key])
```

#### Examples
##### 1. (StructrScript) Encrypt a string with the global encryption key from structr.conf
```
${set(this, 'encryptedString', encrypt('example string'))}
```
##### 2. (StructrScript) Encrypt a string with the key 'secret key'
```
${set(this, 'encryptedString', encrypt('example string', 'secret key'))}
```

### exec()
Executes a script returning the standard output of the script.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|scriptConfigKey|configuration key used to resolve the script's filename|no|
|parameters|collection of script parameters, each either a raw string or an object containing a `value` field and a `mask` flag|yes|
|logBehaviour|Specifies the function's call-logging behavior:<p>`0`: skip logging the command line<br>`1`: log only the script's full path<br>`2`: log the script path and all parameters, applying masking as configured</p>The default for this can be set via `log.scriptprocess.commandline`|yes|


In order to prevent execution of arbitrary code, the script must be registered in structr.conf file using the following syntax.
`key.for.my.script = my-script.sh`

Upon successful execution, the complete output of the script (not the return value) is returned.

`logBehaviour` controls how and if the command line is logged upon execution. If no value is given, the global setting `log.scriptprocess.commandline` will be used.

#### Notes
- Scripts are executed using `/bin/sh` - thus this function is only supported in environments where this exists.
- All script files are looked up inside the `scripts` folder in the main folder of the installation (not in the files area).
- Symlinks are not allowed, director traversal is not allowed.
- The key of the script must be all-lowercase.
- The script must be executable (`chmod +x`)
- This function does not preserve binary content, it can *not* be used to stream binary data through Structr. Use `execBinary()` for that.
- Caution: Supplying unvalidated user input to this command may introduce security vulnerabilities.
- All parameter values are automatically put in double-quotes
- All parameters can be passed as a string or as an object containing a `value` field and a `mask` flag.
- Double-quotes in parameter values are automatically escaped as `\"`

#### Signatures

```
exec(scriptConfigKey [, parameters [, logBehaviour ] ])
```

#### Examples
##### 1. (StructrScript) Execute a script with 2 parameters and no log output, using merge() to create the parameter list
```
${exec('key.for.my.script', merge('param1', 'param2'), 0)}
```
##### 2. (JavaScript) Execute a script with 2 parameters, where one is being masked in the log output
```
${{
	$.exec('key.for.my.script', ['param1', { value: 'CLIENT_SECRET', mask: true }], 2);
}}

```

### execBinary()
Executes a script returning the returning the raw output directly into the output stream.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|outputStream|output stream to write the output to|no|
|scriptConfigKey|configuration key used to resolve the script's filename|no|
|parameters|collection of script parameters, each either a raw string or an object containing a `value` field and a `mask` flag|yes|
|logBehaviour|Specifies the function's call-logging behavior:<p>`0`: skip logging the command line<br>`1`: log only the script's full path<br>`2`: log the script path and all parameters, applying masking as configured</p>The default for this can be set via `log.scriptprocess.commandline`|yes|


This function is very similar to `exec()`, but instead of returning the (text) result of the execution, it will copy its input stream to the given output buffer **without modifying the binary data**.

This is important to allow streaming of binary data from a script to the client.

If a page is used to serve binary data, it must have the correct content-type and have the `pageCreatesRawData` flag enabled.

#### Notes
- Scripts are executed using `/bin/sh` - thus this function is only supported in environments where this exists.
- All script files are looked up inside the `scripts` folder in the main folder of the installation (not in the files area).
- Symlinks are not allowed, director traversal is not allowed.
- The key of the script must be all-lowercase.
- The script must be executable (`chmod +x`)
- The first parameter is usually the builtin keyword `response` and this function is usually used in a page context.
- A page using this should have the correct content-type and have the `pageCreatesRawData` flag enabled
- Caution: Supplying unvalidated user input to this command may introduce security vulnerabilities.
- All parameters are automatically put in double-quotes
- All parameters can be passed as a string or as an object containing a `value` field and a `mask` flag.
- Double-quotes in parameter values are automatically escaped as `\"`

#### Signatures

```
execBinary(outputStream, scriptConfigKey [, parameters [, logBehaviour ] ])
```

#### Examples
##### 1. (StructrScript) Streaming binary content of the `my.create.pdf` script to the client.
```
${execBinary(response, 'my.create.pdf')}
```

### fromJson()
Parses the given JSON string and returns an object.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|src|JSON source to parse|no|


This function is the inverse of the `toJson()` function.
#### Notes
- In a JavaScript scripting context the `JSON.parse()` function is available.

#### Signatures

```
fromJson(source)
```

#### Examples
##### Example 1 (StructrScript)
```
${fromJson('{name: Test, value: 123}')}
```

### fromXml()
Parses the given XML and returns a JSON string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|source|XML source to parse|no|


This function parses the given XML and returns a JSON representation of the XML which can be further processed using `fromJson()` or `JSON.parse()`.
#### Signatures

```
fromXml(source)
```


### getContent()
Returns the content of the given file.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|file|source file to extract content|no|
|encoding|encoding of source data, see notes and description|yes|


Retrieves the content of the given file from the Structr filesystem. This function can be used to access the binary content of a file stored in Structr.

The `encoding` argument controls the type of the returned data. Without an encoding, the raw data is returned as an array of bytes.

To get the content as a string, you must provide an encoding, e.g. 'UTF-8'.

#### Notes
- The parameter `encoding` is available from version 2.3.9+
- If you want to access the raw binary content of the file, omit the `encoding` argument.
- If you don't provide the `encoding` argument, this function returns a byte array.

#### Signatures

```
getContent(file [, encoding ])
```

#### Examples
##### Example 1 (StructrScript)
```
${getContent(first(find('File', 'name', 'test.txt')))}
```
##### Example 2 (JavaScript)
```
${{ let bytes = $.getContent($.first($.find('File', 'name', 'test.txt'))) }}
```
##### Example 3 (JavaScript)
```
${{ let content = $.getContent($.first($.find('File', 'name', 'test.txt')), 'UTF-8') }}
```

### importCss()
Imports CSS classes, media queries etc. from given CSS file.
#### Signatures

```
importCss(file)
```


### importHtml()
Imports HTML source code into an element.
#### Signatures

```
importHtml(parent, html)
```


### log()
Logs the given objects to the logfile.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|objects...|object or list of objects to log|no|


This function takes one or more arguments and logs the string representation of all of them to the Structr logfile. Please note that the individual objects are logged in a single line, one after another, without a separator.
#### Notes
- Single nodes are printed as `NodeType(name, uuid)`, unless they are in a collection that is being logged.
- If you want a JSON representation in the log file, you can use `toJson(node, view)`
- If you use `JSON.stringify()`, the default view `public` will be used

#### Signatures

```
log(objects...)
```

#### Examples
##### 1. (StructrScript) Logs a string with the current user ID
```
${log('user is ', $.me)}
```

### read()
Reads text from a file in the `exchange/` folder.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|filename|name of the file to read from|no|


This function reads text from the file with the given file name in the exchange/ folder. If the file does not exist, nothing will be returned, but no error will be thrown.

To prevent data leaks, Structr allows very limited access to the underlying file system. The only way to read or write files on the harddisk is to use files in the exchange/ folder of the Structr runtime directory. All calls to `read()`, `write()` and `append()` will check that before reading from or writing to the disk.";

#### Notes
- The `exchange/` folder itself may be a symbolic link.
- The canonical path of a file has to be identical to the provided filepath in order to prevent directory traversal attacks. This means that symbolic links inside the `exchange/` folder are forbidden
- Absolute paths and relative paths that traverse out of the exchange/ folder are forbidden.
- Allowed 'sub/dir/file.txt'
- Forbidden '../../../../etc/passwd'
- Forbidden '/etc/passwd'

#### Signatures

```
read(filename)
```

#### Examples
##### 1. (StructrScript) Read from a file named 'test.txt' in the exchange/ folder.
```
${read('test.txt')}
```
##### 2. (JavaScript) Read from a file named 'test.txt' in the exchange/ folder.
```
${{ $.read('test.txt'); }}
```

### sendEvent()
Triggers the sending of a sever-sent event to a given list of recipients. The message will only be sent if they have an open connection.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|eventType|event type name|no|
|message|message content|no|
|recipients|single user, list of users, list of group and users|no|

#### Notes
- Recipients can either be a single user, a single group or a mixed list of both.

#### Signatures

```
sendEvent(eventType, message, recipient(s))
```

#### Examples
##### Example 1 (StructrScript)
```
${sendEvent('message', 'Welcome!', find('User', 'name', 'Bob'))}
```
##### Example 2 (JavaScript)
```
${{ $.sendEvent('message', 'Welcome!', $.find('User', 'name', 'Bob')) }}
```

### setContent()
Sets the content of the given file. Content can either be of type String or byte[].
#### Parameters

|Name|Description|Optional|
|---|---|---|
|file|file node|no|
|content|content to set|no|
|encoding|encoding, default: UTF-8|yes|

#### Notes
- The `encoding` parameter is used when writing the data to the file. The default (`UTF-8`) rarely needs to be changed but can be very useful when working with binary strings. For example when using the `toExcel()` function.

#### Signatures

```
setContent(file, content[, encoding ])
```

#### Examples
##### 1. (StructrScript) Simply overwrite file with static content
```
${setContent(first(find('File', 'name', 'test.txt')), 'New Content Of File test.txt')}
```
##### 2. (StructrScript) Create new file with Excel content
```
${setContent(create('File', 'name', 'new_document.xlsx'), toExcel(find('User'), 'public'), 'ISO-8859-1')}
```
##### 3. (StructrScript) Create a new file and retrieve content from URL
```
${setContent(create('File', 'name', 'web-data.json'), GET('https://api.example.com/data.json'))}
```
##### 4. (StructrScript) Download binary data (an image) and store it in a local file
```
${setContent(create('File', 'name', 'logo.png'), GET('https://example.com/img/logo.png', 'application/octet-stream'))}
```
##### 5. (JavaScript) Create new file with Excel content (JS version)
```
${{ $.setContent($.create('File', 'name', 'new_document.xlsx'), $.toExcel($.find('User'), 'public'), 'ISO-8859-1') }}
```

### toJson()
Serializes the given object to JSON.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|source|object or collection|no|
|view|view (default: `public`)|yes|
|depth|conversion depth (default: 3)|yes|
|serializeNulls|nulled keep properties (default: true)|yes|


Returns a JSON string representation of the given object very similar to `JSON.stringify()` in JavaScript.
The output of this method will be very similar to the output of the REST server except for the response
headers and the result container. The optional `view` parameter can be used to select the view representation
of the entity. If no view is given, the `public` view is used. The optional `depth` parameter defines
at which depth the JSON serialization stops. If no depth is given, the default value of 3 is used.

#### Notes
- For database objects this method is preferrable to `JSON.stringify()` because a view can be chosen. `JSON.stringify()` will only return the `id` and `type` property for nodes.

#### Signatures

```
toJson(obj [, view, depth = 3, serializeNulls = true ])
```

#### Examples
##### Example 1 (StructrScript)
```
${ toJson(find('MyData'), 'public', 4) }
```
##### Example 2 (JavaScript)
```
${{$.toJson($.this, 'public', 4)}}
```

### unarchive()
Unarchives given file to an optional parent folder.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|archiveFile|file node|no|
|parentFolder|parent folder node|yes|


The `unarchive()` function takes two parameter.
The first parameter is a file object that is linked to an archive file, the second (optional)
parameter points to an existing parent folder. If no parent folder is given, a new subfolder with the
same name as the archive (without extension) is created.

#### Notes
- The supported file types are ar, arj, cpio, dump, jar, tar, zip and 7z.

#### Signatures

```
unarchive(file, [, parentFolder ])
```

#### Examples
##### Example 1 (StructrScript)
```
${unarchive(first(find('File', 'name', 'archive.zip')), first(find('Folder', 'name', 'parent')) )}
```
##### Example 2 (JavaScript)
```
${{ $.unarchive($.first($.find('File', 'name', 'archive.zip')), $.first($.find('Folder', 'name', 'parent')) )}}
```

### write()
Writes text to a new file in the `exchange/` folder.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|filename|name of the file to write to|no|
|text|text to write|yes|


This function writes the given text to the file with the given file name in the exchange/ folder. If the file already exist, an error will be thrown.

To prevent data leaks, Structr allows very limited access to the underlying file system. The only way to read or write files on the harddisk is to use files in the exchange/ folder of the Structr runtime directory. All calls to `read()`, `write()` and `append()` will check that before reading from or writing to the disk.";

#### Notes
- The `exchange/` folder itself may be a symbolic link.
- The canonical path of a file has to be identical to the provided filepath in order to prevent directory traversal attacks. This means that symbolic links inside the `exchange/` folder are forbidden
- Absolute paths and relative paths that traverse out of the exchange/ folder are forbidden.
- Allowed 'sub/dir/file.txt'
- Forbidden '../../../../etc/passwd'
- Forbidden '/etc/passwd'

#### Signatures

```
write(filename, text)
```

#### Examples
##### 1. (StructrScript) Write 'hello world' to a new file named 'test.txt' in the exchange/ folder.
```
${write('test.txt', 'hello world')}
```
##### 2. (JavaScript) Write 'hello world' to a new file named 'test.txt' in the exchange/ folder.
```
${{ $.write('test.txt', 'hello world'); }}
```

### xml()
Tries to parse the contents of the given string into an XML document, returning the document on success.

This function can be used in conjunction with `xpath()` to extract data from an XML document.

By default, the following features of DocumentBuilderFactory are active to protect against malicious input.
This is controlled via the configuration setting `application.xml.parser.security`:

```
factory.setNamespaceAware(true);
factory.setXIncludeAware(false);
factory.setExpandEntityReferences(false);
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
```

#### Notes
- Disabling `application.xml.parser.security` reduces protection against malicious input. Do this only when the data source is fully trusted and you accept the associated risks.

#### Signatures

```
xml(xmlString)
```

#### Examples
##### 1. (StructrScript) Read file test.xml from exchange/ directory and return parsed document
```
${xml(read('test.xml'))}
```
##### 2. (StructrScript) Read first file named test.xml from virtual filesystem and return parsed document
```
${xml(getContent(first(find('File', 'name', 'test.xml'))))}
```

### xpath()
Returns the value of the given XPath expression from the given XML document.

The optional third parameter defines the return type, possible values are: NUMBER, STRING, BOOLEAN, NODESET, NODE, default is STRING. This function can be used in conjunction with `xml()` to extract data from an XML document.
#### Signatures

```
xpath(document, xpath [, returnType ])
```

#### Examples
##### 1. (JavaScript) Read project information from XML and convert it to JSON.
```
{
	let xmlString = `
	<xml>
		<projects>
			<project name="Project 1">
				<budget>100</budget>
			</project>
			<project name="Project 2">
				<budget>500</budget>
			</project>
			<project name="Project 3">
				<budget>750</budget>
			</project>
		</projects>
	</xml>`;

	let xmlDocument  = $.xml(xmlString);
	let projectCount = $.xpath(xmlDocument, "count(/xml/projects/project)", "NUMBER");

	const projectData = [...Array(projectCount).keys()].map(i => {
		return {
			name:   $.xpath(xmlDocument, `/xml/projects/project[${i+1}]/@name`, "STRING"),
			budget: $.xpath(xmlDocument, `/xml/projects/project[${i+1}]/budget`, "NUMBER"),
		}
	});

	return projectData;
}

```

## Logic Functions

### and()
Returns the logical AND result of the given boolean expressions.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|expressions...|list of expressions to evaluate|no|


This function takes two or more arguments and ANDs them together, returning `true` if all the expressions evaluate to true, and `false` otherwise.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (the && operator).

#### Signatures

```
and(bool1, bool2, ...)
```

#### Examples
##### 1. (StructrScript) true && false = false
```
${and(true, false)}
```

### empty()
Returns a boolean value that indicates whether the given object is null or empty.

This function works for all sorts of objects: strings, collections, variables, etc., with different semantics depending on the input object.

| Input Type | Behaviour |
| --- | --- |
| string | Returns `true` if the string is non-null and not empty. A string with length > 0 is non-empty, even if it contains only whitespace. |
| collection | Returns `true` if the collection is non-null and contains at least one object (even if the object itself might be null). |
| variable | Returns `true` if the variable is neither null nor undefined nor the empty string. |

This function is the go-to replacement for more complex checks in both JavaScript and StructrScript for null references, undefined variables, empty strings etc., since you can simply use `!$.empty(..)` on all objects.

#### Signatures

```
empty(value)
```

#### Examples
##### 1. (StructrScript) Returns `true`
```
${empty('')}
```
##### 2. (StructrScript) Returns `false`
```
${empty('test')}
```
##### 3. (StructrScript) Returns `false` if there are Project entites in the database
```
${empty(find('Project'))}
```
##### 4. (StructrScript) WARNING: the call in this example returns `false`  because the error message returned by the `find()` call is non-empty.
```
${empty(find('NonExistentType'))}
```

### equal()
Returns a boolean value that indicates whether the values are equal.

This function is very lenient; you can use it to compare dates and strings, strings and numbers, etc., based on the actual values of the converted objects.

If the two values are of different types, Structr tries to determine the desired comparison type and convert the values before comparing.

#### Notes
- This function can also be called using just `eq()` as an alias.

#### Signatures

```
equal(value1, value2)
```


### equal()
Returns a boolean value that indicates whether the values are equal.

This function is very lenient; you can use it to compare dates and strings, strings and numbers, etc., based on the actual values of the converted objects.

If the two values are of different types, Structr tries to determine the desired comparison type and convert the values before comparing.

#### Notes
- This function can also be called using just `eq()` as an alias.

#### Signatures

```
equal(value1, value2)
```


### gt()
Returns true if the first argument is greater than the second argument.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value|no|
|value2|second value|no|


This function tries to convert its arguments into numerical values, i.e. you can compare strings numerically. It is often used in conjunction with `size()` to determine if a collection is empty or not.

#### Signatures

```
gt(value1, value2)
```

#### Examples
##### 1. (StructrScript) This will return `false`
```
 ${gt(1, 2)} 
```
##### 2. (StructrScript) This will return `true`
```
 ${gt(2, 1)} 
```

### gte()
Returns true if the first argument is greater than or equal to the second argument.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value|no|
|value2|second value|no|


This function tries to convert its arguments into numerical values, i.e. you can compare strings numerically.
#### Signatures

```
gte(value1, value2)
```

#### Examples
##### 1. (StructrScript) This will return `false`
```
 ${gte(1, 2)} 
```
##### 2. (StructrScript) This will return `true`
```
 ${gte(2, 1)} 
```
##### 3. (StructrScript) This will return `true`
```
 ${gte(2, 2)} 
```

### if()
Evaluates a condition and executes different expressions depending on the result.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|condition|condition to evaluate|no|
|trueExpression|expression to evaluate if condition is `true`|no|
|falseExpression|expression to evaluate if condition is `false`|no|

#### Notes
- This function is only available in StructrScript.
- This function is often used in HTML attributes, for example to conditionally output CSS classes etc.
- The `is()` function is a shortcut for `if(condition, trueExpression, null)`.

#### Signatures

```
if(condition, trueExpression, falseExpression)
```

#### Examples
##### 1. (StructrScript) Make the background color of an element red if the current user is an admin user
```
${if(me.isAdmin, 'background-color-red', 'background-color-white')}
```
##### 2. (StructrScript) Display different strings depending on the status of a user
```
${if(me.isAdmin, 'You have admin rights.', 'You do not have admin rights.')}
```

### is()
Evaluates a condition and executes an expressions if the result is `true`.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|condition|condition to evaluate|no|
|trueExpression|expression to evaluate if condition is `true`|no|

#### Notes
- This function is only available in StructrScript.
- This function is often used in HTML attributes, for example to conditionally output CSS classes or other attributes.
- This function is essentially a shortcut for the 'if()` function that only evaluates the trueExpression and does nothing if the condition evaluates to `false`.

#### Signatures

```
is(condition, trueExpression)
```

#### Examples
##### 1. (StructrScript) Make the background color of an element red if the current user is an admin user
```
${is(me.isAdmin, 'background-color-red')}
```

### lt()
Returns true if the first argument is less than the second argument.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value|no|
|value2|second value|no|


This function tries to convert its parameter objects into numerical values, i.e. you can compare strings numerically.
#### Signatures

```
lt(value1, value2)
```

#### Examples
##### 1. (StructrScript) This will return `true`
```
 ${lt(1, 2)} 
```
##### 2. (StructrScript) This will return `false`
```
 ${lt(2, 1)} 
```

### lte()
Returns true if the first argument is less that or equal to the second argument.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value|no|
|value2|second value|no|

#### Signatures

```
lte(value1, value2)
```


### not()
Returns the logical negation given boolean expression.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|expression|boolean expression to negate|no|


This function takes a single arguments and returns the negation of its boolean value.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (the ! operator).

#### Signatures

```
not(bool)
```

#### Examples
##### 1. (StructrScript) Return false
```
${not(true)}
```

### one()
Checks if a number is equal to 1, returns the oneValue if yes, the otherValue if no.

**StructrScript only**

#### Signatures

```
one(number, oneValue, otherValue)
```


### or()
Returns the logical OR result of the given boolean expressions.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|expressions...|list of expressions to evaluate|no|


This function takes two or more arguments and ORs them together, returning `true` if any of the expressions evaluates to true, and `false` otherwise.
#### Notes
- This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (the || operator).

#### Signatures

```
or(bool1, bool2, ...)
```

#### Examples
##### 1. (StructrScript) true && false = true
```
${or(true, false)}
```

## Mathematical Functions

### add()
Returns the sum of the given arguments.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|values|list of values to add|no|

#### Signatures

```
add(values...)
```


### ceil()
Returns the given value, rounded up to the nearest integer.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|value|input value (can also be a string with a number in it)|no|


This function tries to convert its arguments into numerical values, i.e. you can use strings as arguments.
#### Signatures

```
ceil(value)
```

#### Examples
##### 1. (StructrScript) Returns 6
```
${ ceil(5.8) }
```

### div()
Returns the result of value1 divided by value2.

**StructrScript only**

#### Notes
- This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments.

#### Signatures

```
div(value1, value2)
```


### floor()
Returns the given value, rounded down to the nearest integer.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value|value to round down|no|


This function tries to convert its arguments into numerical values, i.e. you can use strings as arguments.
#### Signatures

```
floor(value)
```


### max()
Returns the greater of the given values.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value to compare|no|
|value2|second value to compare|no|


This function tries to convert its arguments into numerical values, i.e. you can use strings as arguments. See also `min()`.
#### Signatures

```
max(value1, value2)
```


### min()
Returns the smaller of the given values.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value to compare|no|
|value2|second value to compare|no|


This function tries to convert its arguments into numerical values, i.e. you can use strings as arguments. See also `max()`.
#### Signatures

```
min(value1, value2)
```


### mod()
Implements the modulo operation on two integer values.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|first value of the quotient|no|
|value1|second value of the quotient|no|


Returns the remainder of the quotient of val1 and val2. Both values are first converted to a number.
#### Signatures

```
mod(value1, value2)
```


### mult()
Returns the product of all given arguments.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|values...|one or more values to multiply|no|

#### Notes
- This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments.

#### Signatures

```
mult(values...)
```


### quot()
Divides the first argument by the second argument.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value1|Numerical value. Can be also given as string|no|
|value2|Numerical value. Can be also given as string|no|


Returns the quotient of value1 and value2. This method tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments.
#### Signatures

```
quot(value1, value2)
```

#### Examples
##### Example 1 (StructrScript)
```
${quot(10, 2)}
```
##### Example 2 (JavaScript)
```
${{ $.quot(10, 2) }}
```

### rint()
Returns a random integer in the given range.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|bound|end of random number range (exclusive)|no|


Returns a random integer value between 0 (inclusive) and the `bound` parameter (exclusive).
#### Signatures

```
rint(bound)
```

#### Examples
##### Example 1 (StructrScript)
```
${rint(bound)}
```
##### Example 2 (JavaScript)
```
${{ $.rint(bound) }}
```

### round()
Rounds the given argument to the nearest integer.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|value|value to round|no|
|decimalPlaces|target decimal places|yes|


This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments.
If the optional parameter `decimalPlaces` is given, this function rounds to the given number of decimal places.
#### Signatures

```
round(value [, decimalPlaces ])
```

#### Examples
##### Example 1 (StructrScript)
```
 ${round(2.345678, 2)} 
```
##### Example 2 (JavaScript)
```
${{ $.round(2.345678, 2) }}
```

### subt()
Subtracts the second argument from the first argument.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|val1|minuend|no|
|val2|subtrahend|no|


This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments.
#### Signatures

```
subt(value1, value2)
```

#### Examples
##### Example 1 (StructrScript)
```
${subt(5, 2)}
```
##### Example 2 (StructrScript)
```
${subt('5', '2')}
```
##### Example 3 (JavaScript)
```
${{ $.subt(5, 2)  }}
```
##### Example 4 (JavaScript)
```
${{ $.subt('5', '2')  }}
```

## Miscellaneous Functions

### cache()
Stores a value in the global cache.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|cache key|no|
|timeout|timeout in seconds|no|
|valueExpression|expression that generates the stored value|no|


This function can be used to store a value (which is costly to obtain or should not be updated frequently) under the given key in a global cache. The method will execute the valueExpression to obtain the value, and store it for the given time (in seconds). All subsequent calls to the `cache()` method will return the stored value (until the timeout expires) instead of evaluating the valueExpression.
#### Notes
- The valueExpression will be used to create the initial value.
- Since subsequent calls to cache() will return the previous result it can be desirable to delete the previous value in order to be able to store a new value. This can be done via the `delete_cache_value()` function.,
- Usage in JavaScript is almost identical, but a complex `valueExpression` needs to be wrapped in an anonymous function so execution can be skipped if a valid cached value is present. If no anonymous function is used, the code is *always* executed and thus defeats the purpose of using `$.cache()`

#### Signatures

```
cache(key, timeout, valueExpression)
```

#### Examples
##### 1. (StructrScript) Fetch a value from an external API endpoint and cache the result for an hour
```
${cache('externalResult', 3600, GET('http://api.myservice.com/get-external-result'))}
```
##### 2. (JavaScript) Log `initialCacheValue` to the server log because the initialCacheValue holds for one hour
```
${{
    $.cache('myCacheKey', 3600, 'initialCacheValue');
    $.cache('myCacheKey', 3600, 'test test test');
    $.log($.cache('myCacheKey', 3600, 'test 2 test 2 test 2'));
}}

```
##### 3. (JavaScript) Fetch a value from an external API endpoint and cache the result for an hour
```
${{
    $.cache('externalResult', 3600, () => {
        return $.GET('http://api.myservice.com/get-external-result');
    });
}}

```

### dateAdd()
Adds the given values to a date.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|date|date to manipulate|no|
|years|number of years to add|no|
|months|number of months to add|yes|
|days|number of days to add|yes|
|hours|number of hours to add|yes|
|minutes|number of minutes to add|yes|
|seconds|number of seconds to add|yes|


The result is returned as new date object, leaving the original date untouched.
#### Notes
- The `date` parameter accepts actual date objects, numbers (interpreted as ms after 1970) and strings (formatted as `yyyy-MM-dd'T'HH:mm:ssZ`)
- All other parameters must be provided as numbers

#### Signatures

```
dateAdd(date, years[, months[, days[, hours[, minutes[, seconds]]]]])
```

#### Examples
##### 1. (StructrScript) Adds one year to the current date
```
${dateAdd(now, 1)}
```
##### 2. (StructrScript) Adds one week to the current date
```
${dateAdd(now, 0, 0, 7)}
```
##### 3. (StructrScript) Subtracts one week from the current date
```
${dateAdd(now, 0, 0, -7)}
```

### invalidateCacheValue()
Invalidates the cached value for the given key (if present).
#### Signatures

```
invalidateCacheValue(cacheKey)
```


## Rendering Functions

### getSource()
Returns the rendered HTML content for the given element.
#### Signatures

```
getSource(element, editMode)
```


### hasCssClass()
Returns whether the given element has the given CSS class(es).
#### Signatures

```
hasCssClass(element, css)
```


### include()
Loads the element with the given name and renders its HTML representation into the output buffer.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|name of the node (and subtree) to include|no|
|collection|collection to repeat over|yes|
|dataKey|dataKey to use in Repeater|yes|


Nodes can be included via their `name` property. When used with an optional collection and data key argument, the included HTML element will be rendered as a Repeater Element.

Possible nodes **MUST**:
1. have a unique name
2. NOT be in the trash

Possible nodes **CAN**:
1. be somewhere in the pages tree
2. be in the Shared Components

See also `includeChild()` and `render()`.

#### Signatures

```
include(name [, collection, dataKey])
```

#### Examples
##### 1. (StructrScript) Render the contents of the Shared Component "Main Menu" into the output buffer
```
${include('Main Menu')}
```
##### 2. (StructrScript) Render the contents of the Shared Component "Item Template" once for every Item node in the database
```
${include('Item Template', find('Item'), 'item')}
```

### includeChild()
Loads a template's child element with the given name and renders its HTML representation into the output buffer.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|name of the node (and subtree) to include|no|
|collection|collection to repeat over|yes|
|dataKey|dataKey to use in Repeater|yes|


Nodes can be included via their `name` property. When used with an optional collection and data key argument, the included HTML element will be rendered as a Repeater Element.

See also `include()` and `render()`.

#### Notes
- Works only during page rendering in Template nodes.
- Child nodes must be direct children of the template node.
- Underneath the template node, child node names MUST be unique in order for `includeChild()` to work.

#### Signatures

```
includeChild(name [, collection, dataKey])
```

#### Examples
##### 1. (StructrScript) Render the contents of the child node named "Child1" into the output buffer
```
${includeChild('Child1')}
```
##### 2. (StructrScript) Render the contents of the child node named "Item Template" once for every Item node in the database
```
${includeChild('Item Template', find('Item'), 'item')}
```

### insertHtml()
Inserts a new HTML subtree into the DOM.
#### Signatures

```
insertHtml(parent, html)
```


### print()
Prints the given strings or objects to the output buffer.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|objects|Objects that will be printed into the page rendering buffer|no|


Prints the string representation of all of the given objects into the page rendering buffer. This method is often used in conjunction with `each()` to create rendering output for a collection of entities etc. in scripting context.
#### Signatures

```
print(objects...)
```

#### Examples
##### Example 1 (StructrScript)
```
${print('Hello, world!')}
```
##### Example 2 (StructrScript)
```
${print(this.name, 'test')}
```
##### Example 3 (JavaScript)
```
${{ $.print('Hello, world!') }}
```
##### Example 4 (JavaScript)
```
${{ $.print($.get('this').name, 'test') }}
```

### removeDomChild()
Removes a node from the DOM.
#### Signatures

```
removeDomChild(parent, child)
```


### render()
Renders the children of the current node.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|node|node or list of nodes to be rendered|no|


Renders the HTML representation of the given node(s) into the output buffer. This function is exactly equivalent to the rendering process that Structr uses internally to create the HTML output of pages etc. It can be used to render dynamic content in pages with placeholders etc. Together with `include()`, `render()` is one of the the most important method when dealing with HTML web templates, since it allows the user to fill static HTML pages with dynamic content from the underlying node structure.

See the documentation article about Page Rendering for more information on this topic.

#### Signatures

```
render(list)
```

#### Examples
##### Example 1 (StructrScript)
```
${render(children)}
```
##### Example 2 (JavaScript)
```
${{ $.render($.children) }}
```

### replaceDomChild()
Replaces a node from the DOM with new HTML.
#### Signatures

```
replaceDomChild(parent, child, html)
```


### setDetailsObject()
Allows overriding the `current` keyword with a given entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|entity to be linked to `current`|no|

#### Signatures

```
setDetailsObject(obj)
```

#### Examples
##### Example 1 (StructrScript)
```
${setDetailsObject(first(find('User')))}
```
##### Example 2 (JavaScript)
```
${{ $.setDetailsObject($.first($.find('User')))}
```

### setLocale()
Sets the locale for the current request.

This function gives granular control of the current locale and directly influences the result of date parsing and formatting functions as well as the results of calls to localize().

For page rendering and REST requests, the builtin request parameter `_locale` can be used to set the locale for the whole request.

#### Signatures

```
setLocale(locale)
```

#### Examples
##### 1. (StructrScript) Get name of current weekday in german.
```
${ (setLocale('de_DE'), dateFormat(now, 'E')) }
```

### template()
Returns a MailTemplate object with the given name, replaces the placeholders with values from the given entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|Mail-Template name|no|
|locale|Mail-Template locale|no|
|source|source entity for given expressions|no|


Loads a node of type `MailTemplate` with the given name and locale values and uses the given source entity to resolve template expressions in the content field of the loaded node, returning the resulting text.
#### Notes
- Short example for mail-template: `Welcome, ${this.name}!`
- This function is quite similar to the `replace()` function which serves a similar purpose but works on any string rather than on a mail template.
- The third parameter 'source' expects a node or relationship object fetched from the database. If the third parameter is an arbitrary design JavaScript object, it has to be wrapped with the `toGraphObject()` function, before being passed as the parameter.

#### Signatures

```
template(name, locale, entity)
```

#### Examples
##### 1. (StructrScript) Passing the Structr me object, representing the current user
```
${template('TEXT_TEMPLATE_1', 'en', this)}
```
##### Example 2 (JavaScript)
```
${{ return $.template('TEXT_TEMPLATE_1', 'en', $.this)}}
```
##### 3. (JavaScript) passing an arbitrary JavaScript object
```
${{
    return $.template('MAIL_SUBJECT', 'de', $.toGraphObject({name: "Mr. Foo"}))
}}

```

## Schema Functions

### ancestorTypes()
Returns the list of parent types of the given type **including** the type itself.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type to fetch ancestor types for|no|
|blacklist|blacklist to remove unwanted types from result|yes|


The blacklist of type names can be extended by passing a list as the second parameter. If omitted, the function uses the following blacklist: [AccessControllable, GraphObject, NodeInterface, PropertyContainer].
#### Signatures

```
ancestorTypes(type [, blacklist ])
```

#### Examples
##### 1. (StructrScript) Return all ancestor types of MyType
```
${ancestorTypes('MyType')}
```
##### 2. (StructrScript) Remove MyOtherType from the returned result
```
${ancestorTypes('MyType', merge('MyOtherType))}
```

### enumInfo()
Returns the possible values of an enum property.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type on which the property is defined|no|
|propertyName|name of the property|no|
|raw|whether to return a raw list of enum values or a list of objects|yes|


The default behaviour of this function is to return a list of objects with a single `value` entry that contains the enum value, so it can be used in a repeater to configure HTML select dropdowns etc:

```
[ { value: 'ExampleEnum1' }, { value: 'ExampleEnum2' }, { value: 'ExampleEnum3' } ]
```

If the `raw` parameter is set to `true`, a simple list will be returned:
```
[ 'ExampleEnum1', 'ExampleEnum2', 'ExampleEnum3' } ]
```

#### Signatures

```
enumInfo(type, propertyName [, raw])
```

#### Examples
##### 1. (Html) Configure an HTML select element with the enum options of a property
```
<select>
	<option data-structr-meta-data-key="activityType" data-structr-meta-function-query="enumInfo('Activity', 'activityType')">${activityType.value}</option>
</select>

```

### functionInfo()
Returns information about the currently running Structr method, or about the method defined in the given type and name.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type name|yes|
|name|function name|yes|


The function returns an object with the following structure.

| Key                   | Type    | Description                                                                                                                   |
|-----------------------|---------|-------------------------------------------------------------------------------------------------------------------------------|
| name                  | String  | name of the method                                                                                                            |
| declaringTrait        | String  | name of the type the method is declared on (`null` if if `isUserDefinedFunction === true`)                                    |
| isUserDefinedFunction | boolean | `true` if the method is not a type- or service class method, `false` otherwise                                                |
| isStatic              | boolean | `true` if the method can be called statically, `false` if it can only be called in an object context                          |
| isPrivate             | boolean | `true` if the method can only be called via scripting, `false` if it can be called via REST as well                           |
| httpVerb              | String  | The HTTP verb this function can be called with (only present if `isPrivate === false`)                                        |
| summary               | String  | summary as defined in OpenAPI (only present if summary is defined)                                                            |
| description           | String  | description as defined in OpenAPI (only present if description is defined)                                                    |
| parameters            | object  | key-value map of parameters as defined in OpenAPI (key = name, value = type) (only present if OpenAPI parameters are defined) |

#### Signatures

```
functionInfo([type, name])
```

#### Examples
##### 1. (JavaScript) Add function information to log output
```
{
	let info = $.functionInfo();

	$.log(`[${info.declaringClass}][${info.name}] task started...`);

	// ...

	$.log(`[${info.declaringClass}][${info.name}] task finished...`);
}

```

### getRelationshipTypes()
Returns the list of available relationship types form and/or to this node. Either potentially available (schema) or actually available (database).
#### Parameters

|Name|Description|Optional|
|---|---|---|
|node|node for which possible relationship types should be checked|no|
|lookupType|`existing` or `schema` - default: `existing`|yes|
|direction|`incoming`, `outgoing` or `both` - default: `both`|yes|

#### Signatures

```
getRelationshipTypes(node, lookupType [, direction ])
```

#### Examples
##### Example 1 (StructrScript)
```
${getRelationshipTypes(me, 'schema')}
${getRelationshipTypes(me, 'schema', 'incoming')}
${getRelationshipTypes(me, 'schema', 'outgoing')}
${getRelationshipTypes(me, 'existing')}
${getRelationshipTypes(me, 'existing', 'incoming')}
${getRelationshipTypes(me, 'existing', 'outgoing')}

```
##### Example 2 (JavaScript)
```
${{ $.getRelationshipTypes($.me, 'schema') }}
${{ $.getRelationshipTypes($.me, 'schema', 'incoming') }}
${{ $.getRelationshipTypes($.me, 'schema', 'outgoing') }}
${{ $.getRelationshipTypes($.me, 'existing') }}
${{ $.getRelationshipTypes($.me, 'existing', 'incoming') }}
${{ $.getRelationshipTypes($.me, 'existing', 'outgoing') }}

```

### inheritingTypes()
Returns the list of subtypes of the given type **including** the type itself.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type name to fetch subtypes for|no|
|blacklist|list of unwanted type names that are removed from the result|yes|


You can remove unwanted types from the resulting list by providing a list of unwanted type names as a second parameter.
#### Signatures

```
inheritingTypes(type [, blacklist ])
```

#### Examples
##### 1. (StructrScript) Returns a list of subtypes of type "MyType"
```
${inheritingTypes('MyType', merge('UndesiredSubtype'))}
```

### propertyInfo()
Returns the schema information for the given property.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of the object|no|
|name|name of the object|no|


Returns a property info object for the property of the given type with the given name. A property info object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
| dbName | Database (Neo4j) name - can be used in Cypher etc. | String |
| jsonName | JSON name (as it appears in JSON REST output) | String |
| className | Class name of the property type | String |
| declaringClass | Name of the declaring class | String |
| defaultValue | Default value or null | String |
| contentType | Content type or null (String only) | String |
| format | Format or null | String |
| readOnly | Read-only flag | Boolean |
| system | System flag | Boolean |
| indexed | Indexed flag | Boolean |
| indexedWhenEmpty | Indexed-when-empty flag | Boolean |
| unique | Unique flag | Boolean |
| notNull | Not-null flag | Boolean |
| dynamic | Dynamic flag | Boolean |
| relatedType | Related type (for relationship properties) | String |
| type | Property type from definition | String |
| uiType | Extended property type for Edit Mode (e.g. String, String[] etc.) | String |
| isCollection | Collection or entity (optional) | String |
| databaseConverter | Database converter type (internal) | String |
| inputConverter | Input converter type (internal) | String |
| relationshipType | Relationship type (for relationship properties) | String |

#### Signatures

```
propertyInfo(type, propertyName)
```

#### Examples
##### Example 1 (StructrScript)
```
${propertyInfo('User', 'name').uiType}
```
##### Example 2 (JavaScript)
```
${{ $.propertyInfo('User', 'name').uiType }}
```

### typeInfo()
Returns the type information for the specified type.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|type|schema type|no|
|view|view (default: `public`)|yes|


If called with a view, all properties of that view are returned as a list. The items of the list are in the same
format as `property_info()` returns. This is identical to the result one would get from `/structr/rest/_schema/<type>/<view>`.
If called without a view, the complete type information is returned as an object.
This is identical to the result one would get from `/structr/rest/_schema/<type>`.

#### Signatures

```
typeInfo(type [, view])
```

#### Examples
##### Example 1 (StructrScript)
```
${typeInfo('User', 'public')}
```
##### Example 2 (JavaScript)
```
${{ $.typeInfo('User', 'public') }}
```

## Scripting Functions

### applicationStoreDelete()
Removes a stored value from the application level store.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key whose value should be removed from the store|no|


The application store can be used to store data in-memory as long as the instance is running. You can use it to store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
#### Signatures

```
applicationStoreDelete(key)
```


### applicationStoreGet()
Returns a stored value from the application level store.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key under which the desired value is stored|no|


The application store can be used to store data in-memory as long as the instance is running. You can use it to store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
#### Signatures

```
applicationStoreGet(key)
```


### applicationStoreGetKeys()
Lists all keys stored in the application level store.

**StructrScript only**


The application store can be used to store data in-memory as long as the instance is running. You can use it to store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
#### Signatures

```
applicationStoreGetKeys()
```


### applicationStoreHas()
Checks if a key is present in the application level store.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key under which the desired value is stored|no|


The application store can be used to store data in-memory as long as the instance is running. You can use it to store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
#### Signatures

```
applicationStoreHas(key)
```


### applicationStorePut()
Stores a value in the application level store.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key under which the desired value should be store|no|
|value|value to store|no|


The application store can be used to store data in-memory as long as the instance is running. You can use it to store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
#### Signatures

```
applicationStorePut(key, value)
```


### call()
Calls the given user-defined function in the current users context.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|functionName|name of the user-defined function to call|no|
|parameterMap|map of parameters|no|

#### Notes
- The method can only be executed if it is visible in the user context it's call()'ed in. This is analogous to calling user-defined functions via REST.
- Useful in situations where different types have the same or similar functionality but no common base class so the method can not be attached there
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
call(functionName [, parameterMap ])
```

#### Examples
##### 1. (StructrScript) Call the user-defined function `updateUsers` with two key-value pairs as parameters
```
${call('updateUsers', 'param1', 'value1', 'param2', 'value2')}
```
##### 2. (JavaScript) Call the user-defined function `updateUsers` with a map of parameters
```
${{
	$.call('updateUsers', {
		param1: 'value1',
		param2: 'value2'
	})
        }}

```

### callPrivileged()
Calls the given user-defined function **in a superuser context**.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|functionName|name of the user-defined function to call|no|
|parameterMap|map of parameters|no|

#### Notes
- Useful in situations where different types have the same or similar functionality but no common base class so the method can not be attached there
- In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
callPrivileged(functionName [, parameterMap ])
```

#### Examples
##### 1. (StructrScript) Call the user-defined function `updateUsers` with two key-value pairs as parameters
```
${call('updateUsers', 'param1', 'value1', 'param2', 'value2')}
```
##### 2. (JavaScript) Call the user-defined function `updateUsers` with a map of parameters
```
${{
	$.call('updateUsers', {
		param1: 'value1',
		param2: 'value2'
	})
        }}

```

### coalesceObjects()
Returns the first non-null value in the list of expressions passed to it. In case all arguments are null, null will be returned.
#### Signatures

```
coalesceObjects(obj1, obj2, obj3, ...)
```


### mergeProperties()
Copies the values for the given list of property keys from the source entity to the target entity.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|source|source node to copy properties from|no|
|target|target node to copy properties to|no|
|key1|first key to copy|no|
|key2|second key to copy|yes|
|key3|third key to copy|yes|
|additionalKeys...|more keys|yes|

#### Signatures

```
mergeProperties(source, target, keys)
```


### requestStoreDelete()
Removes a stored value from the request level store.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key to remove|no|

#### Signatures

```
requestStoreDelete(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${requestStoreDelete('do_no_track')}
```
##### Example 2 (JavaScript)
```
${{ $.requestStoreDelete('do_not_track'); }}
```

### requestStoreGet()
Retrieves a stored value from the request level store.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|stored key|no|

#### Signatures

```
requestStoreGet(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${requestStoreGet('do_no_track')}
```
##### Example 2 (JavaScript)
```
${{ $.requestStoreGet('do_not_track'); }}
```

### requestStoreGetKeys()
Lists all keys stored in the request level store.
#### Signatures

```
requestStoreGetKeys()
```

#### Examples
##### Example 1 (StructrScript)
```
${requestStoreGetKeys()}
```
##### Example 2 (JavaScript)
```
${{ $.requestStoreGetKeys(); }}
```

### requestStoreHas()
Checks if a key is present in the request level store.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key to check|no|

#### Signatures

```
requestStoreHas(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${requestStoreHas('do_no_track')}
```
##### Example 2 (JavaScript)
```
 ${{ $.requestStoreHas('do_not_track'); }}
```

### requestStorePut()
Stores a value in the request level store.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|given key|no|
|value|value for given key|yes|

#### Signatures

```
requestStorePut(key, value)
```

#### Examples
##### Example 1 (StructrScript)
```
${requestStorePut('do_no_track', true)}
```
##### Example 2 (JavaScript)
```
${{ $.requestStorePut('do_not_track', true); }}
```

### retrieve()
Returns the value associated with the given key from the temporary store.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|url|key to retrieve|no|


Retrieves the value previously stored under the given key in the current request context.
This function can be used to obtain the results of a previous computation step etc. and is often used to provide
some sort of "variables" in the scripting context. See `store()` for the inverse operation.
Additionally, the `retrieve()` function is used to retrieve the parameters supplied to the execution of a custom method.

#### Signatures

```
retrieve(key)
```

#### Examples
##### Example 1 (StructrScript)
```
 ${retrieve('tmpUser')}
```
##### Example 2 (JavaScript)
```
${{ $.retrieve('tmpUser') }}
```

### schedule()
Schedules a script or a function to be executed in a separate thread.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|expression|function to run later|no|
|title|title of schedule|yes|
|onFinish|function to be called when main expression finished|yes|


Allows the user to insert a script snippet into the import queue for later execution.
Useful in situations where a script should run after a long-running import job, or if the script should run in
a separate transaction that is independent of the calling transaction.
The `title` parameter is optional and is displayed in the Structr admin UI in the Importer section and in the
notification messages when a script is started or finished.
The `onFinish` parameter is a script snippet which will be called when the process finishes (successfully or with an exception).
A parameter `jobInfo` is injected in the context of the `onFinish` function (see `job_info()` for more information on this object).
The schedule function returns the job id under which it is registered.

#### Signatures

```
schedule(script [, title ])
```

#### Examples
##### Example 1 (StructrScript)
```
${schedule('call("myCleanupScript")', 'Cleans unconnected nodes from the graph')}
```
##### Example 2 (JavaScript)
```
${{
    $.schedule(function() {
        // execute global method
        $.call('myCleanupScript');
    }, 'Cleans unconnected nodes from the graph');
}}

```
##### Example 3 (JavaScript)
```
${{
    $.schedule(function() {
        // execute global method
        Structr.call('myCleanupScript');
    }, 'Cleans unconnected nodes from the graph', function() {
        $.log('scheduled function finished!');
        $.log('Job Info: ', $.get('jobInfo'));
    });
}}

```

### weekDays()
Calculates the number of week days (working days) between given dates.
#### Signatures

```
weekDays(from, to)
```


## Security Functions

### confirmationKey()
Creates a confirmation key to use as a one-time token. Used for user confirmation or password reset.
#### Signatures

```
confirmationKey()
```


### createAccessAndRefreshToken()
Creates both JWT access token and refresh token for the given User entity that can be used for request authentication and authorization.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|user|user entity to create tokens for|no|
|accessTokenTimeout|access token timeout in **minutes**, defaults to 1 hour (60 minutes)|yes|
|refreshTokenTimeout|refresh token timeout in **minutes**, defaults to 1 day (1440 minutes)|yes|


The return value of this function is a map with the following structure:

```
{
    "accessToken":"eyJhbGciOiJIUzUxMiJ9.eyJ[...]VkIn0.fbwKEQ4dELHuXXmPiNtn8XNWh6ShesdlTZsXf-CojTmxQOWUxkbHcroj7gVz02twox82ChTuyxkyHeIoiidU4g",
    "refreshToken":"eyJhbGciOiJIUzUxMiJ9.eyJ[...]lbiJ9.GANUkPH09pBimd5EkJmrEbsYQhDw6hXULZGSldHSZYqq1FNjM_g6wfxt1217TlGZcjKyXEL_lktcPzjOeEU3A",
    "expirationDate":"1616692902820"
}
```

#### Notes
- In order to use JWT in your application, you must configure `security.jwt.secrettype` and the corresponding settings your structr.conf.
- You can configure the timeouts for access and refresh tokens in your structr.conf by setting `security.jwt.expirationtime` and `security.jwt.refreshtoken.expirationtime` respectively.
- The refresh token is stored in the `refreshTokens` property in the given User entity.

#### Signatures

```
createAccessAndRefreshToken(user, accessTokenTimeout, refreshTokenTimeout)
```

#### Examples
##### 1. (JavaScript) Create a new tokens with non-default validity periods
```
{
	// create an access token that is valid for 30 minutes
	// and a refresh token that is valid for 2 hours
	let tokens       = $.createAccessAndRefreshToken($.me, 30, 120);
	let accessToken  = tokens.accessToken;
	let refreshToken = tokens.refreshToken;

	// ... use the tokens
}

```
##### 2. (JavaScript) Authenticate a request to the Structr backend with an existing access token
```
fetch("http://localhost:8082/structr/rest/User", {
	method: "GET",
	headers: {
		"authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ[...]VkIn0.fbwKEQ4dELHuXXmPiNtn8XNWh6ShesdlTZsXf-CojTmxQOWUxkbHcroj7gVz02twox82ChTuyxkyHeIoiidU4g"
	}
});

```

### createAccessToken()
Creates a JWT access token for the given user entity that can be used for request authentication and authorization.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|user|user entity to create a token for|no|
|accessTokenTimeout|access token timeout in **minutes**, defaults to 1 hour (60 minutes)|yes|


The return value of this function is a single string with the generated access token. This token can then be used in the `Authorization` HTTP header to authenticate requests against to Structr.
#### Notes
- In order to use JWT in your application, you must configure `security.jwt.secrettype` and the corresponding settings your structr.conf.
- You can configure the timeouts for access and refresh tokens in your structr.conf by setting `security.jwt.expirationtime` and `security.jwt.refreshtoken.expirationtime` respectively.
- The refresh token is stored in the `refreshTokens` property in the given User entity.

#### Signatures

```
createAccessToken(user, accessTokenTimeout)
```

#### Examples
##### 1. (JavaScript) Create a new access token with a validity of 30 minutes
```
{
	// create an access token that is valid for 30 minutes
	let accessToken = $.createAccessToken($.me, 30);

	// ... use the token
}

```
##### 2. (JavaScript) Authenticate a request to the Structr backend with an existing access token
```
fetch("http://localhost:8082/structr/rest/User", {
	method: "GET",
	headers: {
		"authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ[...]VkIn0.fbwKEQ4dELHuXXmPiNtn8XNWh6ShesdlTZsXf-CojTmxQOWUxkbHcroj7gVz02twox82ChTuyxkyHeIoiidU4g"
	}
});

```

### hmac()
Returns a keyed-hash message authentication code generated out of the given payload, secret and hash algorithm.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|value|Payload that will be converted to hash string|no|
|secret|Secret value|no|
|hashAlgorithm|Hash algorithm that will be used to convert the payload|yes|


Returns a keyed-hash message authentication code generated out of the given payload, secret and hash algorithm.
#### Notes
- Default value for parameter hashAlgorithm is SHA256.

#### Signatures

```
hmac(value, secret [, hashAlgorithm ])
```

#### Examples
##### Example 1 (StructrScript)
```
${hmac(to_json(me)), 'aVeryGoodSecret')}
```
##### Example 2 (JavaScript)
```
${{ $.hmac(JSON.stringify({key1: 'test'}), 'aVeryGoodSecret') }}
```

### login()
Logs the given user in if the given password is correct. Returns true on successful login.
#### Signatures

```
login(user, password)
```


## String Functions

### abbr()
Abbreviates the given string at the last space character before the maximum length is reached.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|string to abbreviate|no|
|maxLength|maximum length of the returned string (including the ellipsis at the end)|yes|
|abbr|last character(s) of the returned string after abbreviation|yes|


The remaining characters are replaced with the ellipsis character (…) or the given `abbr` parameter.
#### Signatures

```
abbr(string, maxLength[, abbr = '…'])
```


### base64decode()
Decodes the given base64 text using the supplied scheme.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|base64Text|base64-encoded text to decode|no|
|scheme|decoding scheme, `url`, `mime` or `basic`, defaults to `basic`|yes|
|charset|charset to use, defaults to UTF-8|yes|


Valid values for `scheme` are `basic` (default), `url` and `mime`. The following explanation of the encoding schemes is taken directly from https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html

**Basic**
Uses "The Base64 Alphabet" as specified in Table 1 of RFC 4648 and RFC 2045 for encoding and decoding operation. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.

**URL and Filename safe**
Uses the "URL and Filename safe Base64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.

**MIME**
Uses the "The Base64 Alphabet" as specified in Table 1 of RFC 2045 for encoding and decoding operation. The encoded output must be represented in lines of no more than 76 characters each and uses a carriage return '\r' followed immediately by a linefeed '\n' as the line separator. No line separator is added to the end of the encoded output. All line separators or other characters not found in the base64 alphabet table are ignored in decoding operation.

#### Signatures

```
base64decode(text [, scheme, charset ])
```

#### Examples
##### 1. (StructrScript) Decode a base64-encoded string
```
${base64decode("Q2hlY2sgb3V0IGh0dHBzOi8vc3RydWN0ci5jb20=")}
```

### base64encode()
Encodes the given string and returns a base64-encoded string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|base64Text|text to encode|no|
|scheme|encoding scheme, `url`, `mime` or `basic`, defaults to `basic`|yes|
|charset|charset to use, defaults to UTF-8|yes|


Valid values for `scheme` are `basic` (default), `url` and `mime`. The following explanation of the encoding schemes is taken directly from https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html

**Basic**
Uses "The Base64 Alphabet" as specified in Table 1 of RFC 4648 and RFC 2045 for encoding and decoding operation. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.

**URL and Filename safe**
Uses the "URL and Filename safe Base64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.

**MIME**
Uses the "The Base64 Alphabet" as specified in Table 1 of RFC 2045 for encoding and decoding operation. The encoded output must be represented in lines of no more than 76 characters each and uses a carriage return '\r' followed immediately by a linefeed '\n' as the line separator. No line separator is added to the end of the encoded output. All line separators or other characters not found in the base64 alphabet table are ignored in decoding operation.

#### Signatures

```
base64encode(text [, scheme, charset ])
```

#### Examples
##### 1. (StructrScript) Encode a string
```
${base64encode("Check out https://structr.com")}
```

### capitalize()
Capitalizes the given string.

No other characters are changed. If the first character has no explicit titlecase mapping and is not itself a titlecase char according to UnicodeData, then the uppercase mapping is returned as an equivalent titlecase mapping.
#### Signatures

```
capitalize(string)
```

#### Examples
##### 1. (StructrScript) Results in "Cat dog bird"
```
${capitalize('cat dog bird')}
```
##### 2. (StructrScript) Results in "CAT DOG BIRD"
```
${capitalize('cAT DOG BIRD')}
```
##### 3. (StructrScript) Only the first character is capitalized, so quoted strings are not changed
```
${capitalize('"cat dog bird"')}
```

### clean()
Cleans the given string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|stringOrList|string or list of strings to clean|no|


This function can be used to convert complex strings or collections of strings (e.g. user names, article titles, etc.) into simple strings that can be used in URLs etc.

| Characters | Action |
| --- | --- | --- |
| Whitespace | Replace with `-` (consecutive whitespaces are replaced with a single `-`) |
|  `–'+/|\` | Replace with `-` |
| Uppercase letters | Replace with corresponding lowercase letter |
| `<>.?(){}[]!,` | Remove |

#### Notes
- Strings are normalized in the NFD form (see. http://www.unicode.org/reports/tr15/tr15-23.html) before the replacements are applied.

#### Signatures

```
clean(string)
```

#### Examples
##### 1. (StructrScript) Results in "this-is-an-example"
```
${clean('This   is Än   example')}
```
##### 2. (StructrScript) Clean a list of strings
```
${clean(merge('This   is Än   example', 'This   is   Änother   example'))}
=> ['this-is-an-example', 'this-is-another-example']

```
##### 3. (JavaScript) Clean a list of strings
```
${{ $.clean(['This   is Än   example', 'This   is   Änother   example'])}}
=> ['this-is-an-example', 'this-is-another-example']

```

### concat()
Concatenates the given list of objects into a single string without a separator between them.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|objects...|one or more objects to concatenate|no|


The objects can be of any type: string, number, entity, collection. If a collection is encountered, all elements of that collection are concatenated.
#### Notes
- If nodes and relationships are among the parameters, their UUIDs will be written into the result.
- `null` values are filtered and not concatenated.

#### Signatures

```
concat(values...)
```

#### Examples
##### 1. (StructrScript) Results in "test1.04c8a42581fb74ea092552539d0b594f0 a string"
```
${concat('test', 1, me, ' a string')}
```

### contains()
Returns true if the given string or collection contains a given element.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|stringOrCollection|string or collection to check|no|
|wordOrObject|word or object to check|no|


Returns a boolean value that indicates whether the given string contains the given word or the given collection contains the given element.
#### Notes
- In JavaScript, this function is **not** the `contains` predicate to be used in `$.find()`, please use `$.predicate.contains()` for that.

#### Signatures

```
contains(stringOrList, wordOrObject)
```

#### Examples
##### 1. (StructrScript) Check if a given string contains the word "test"
```
${contains(request.inputString, 'test')}
```
##### 2. (StructrScript) Check if the given collection contains a node
```
${contains(project.members, me)}
```
##### 3. (JavaScript) Check if the given collection contains a node
```
${{ $.contains(project.members, $.me); }}
```

### endsWith()
Returns true if the given string ends with the given suffix.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|string to check|no|
|suffix|suffix to check|no|

#### Signatures

```
endsWith(str, suffix)
```


### indexOf()
Returns the position of the first occurrence of the given word in the given string, or -1 if the string doesn't contain the word.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|input string|no|
|word|word to search|no|

#### Signatures

```
indexOf(string, word)
```


### join()
Joins the given collection of strings into a single string, separated by the given separator.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|collection|collection of values to join|no|
|separator|separator string|no|


This function is often used in conjunction with `find()` and `extract()` to create comma-separated lists of property values.
#### Signatures

```
join(list, separator)
```

#### Examples
##### 1. (StructrScript) Create a comma-separated list of all the user names in the database
```
${join(extract(find('User'), 'name'), ', ')}
```

### length()
Returns the length of the given string.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|input string to measure|no|


This function **only** works for strings, do not use it on collections. See `size()` for that.
#### Notes
- **Do not** use this function on collections, it will return a result, but not the result you expect, because the collection will be converted to a string and the length of that string will be returned.

#### Signatures

```
length(string)
```


### localize()
Returns a (cached) Localization result for the given key and optional domain.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|keyOrKeys|string or list of keys to localize|no|
|domain|localization domain to use for lookup|yes|


The `localize()` function can be used to localize a key or a list of keys. It uses the current `locale` (see keyword locale) to search for nodes of type `Localization` in the database. This lookup works in multiple steps. If a Localization object is found, the process is stopped and the result returned. If no localization is found, the search key itself is returned.

1. find localization with exact match on key, given domain and full locale
2. find localization with exact match on key, no domain and full locale
3. find localization with exact match on key, given domain and language part of locale only
4. find localization with exact match on key, no domain and language part of locale  only
5. If defined and active via structr.conf, restart steps 1-4 with the fallback locale:
	- `application.localization.usefallbacklocale` = enable/disable use of the fallback locale
	- `application.localization.fallbacklocale` = the fallback locale

If after step 4 no localization is found, the input parameters are logged if the configuration entry `application.localization.logmissing` in structr.conf is enabled.

If the first parameter is a single key, the return value is a string. If it is a collection of keys, the return value is a list of objects with keys "name" and "localizedName".

#### Signatures

```
localize(keyOrKeys [, domain ])
```

#### Examples
##### 1. (StructrScript) Salutation for the current locale in the 'Formal' domain
```
${localize('Hello', 'Formal')}
```
##### 2. (Html) HTML input field with a localized placeholder
```
<input type="text" name="username" placeholder="${localize('username')}...">

// if the current locale is en_US
<input type="text" name="username" placeholder="Username...">

// if the current locale is de_DE
<input type="text" name="username" placeholder="Benutzername...">

```
##### 3. (JavaScript) Localization of multiple keys at once
```
${{
	$.localize(['Hello', 'Goodbye']);

	/*
		// if the current locale is en_US
		[{name: "Hello", localizedName: "Hi"}, {name: "Goodbye", localizedName: "Bye"}]

		// if the current locale is de_DE
		[{name: "Hello", localizedName: "Hallo"}, {name: "Goodbye", localizedName: "Auf Wiedersehen"}]
	*/
}}

```

### lower()
Returns the lowercase version of the given string.

**StructrScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|string to lowercase|no|

#### Signatures

```
lower(string)
```


### replace()
Replaces script expressions in the given template with values from the given entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|template|template for replacement|no|
|entity|target object|yes|


This function can be used to evaluate template expressions in database objects, for example to create customized e-mails etc.
#### Notes
- Allowing user input to be evaluated in a template expression poses a security risk. You have no control over what the user can do!

#### Signatures

```
replace(template, entity)
```

#### Examples
##### Example 1 (StructrScript)
```
${replace('Welcome, ${this.name}!', me)}
```
##### Example 2 (JavaScript)
```
${{ $.replace('Welcome, ${this.name}!', $.me) }}
> Welcome, admin!

```

### split()
Splits the given string by the whole separator string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|string to split|no|
|separator|separator string|yes|


Uses the given separator to split the given string into a collection of strings. This is the opposite of `join()`.
The default separator is a regular expression which splits the string at ANY of the following characters: `,;(whitespace)`
The optional second parameter is used as literal separator, it is NOT used as a regex. To use a regular expression to split
a string, see `split_regex()`.

#### Notes
- Adjacent separators are treated as one separator

#### Signatures

```
split(str [, separator ])
```

#### Examples
##### Example 1 (StructrScript)
```
${split('one,two,three,four')}
```
##### Example 2 (StructrScript)
```
${split('one;two;three;four')}
```
##### Example 3 (StructrScript)
```
${split('one two three four')}
```
##### Example 4 (StructrScript)
```
${split('one::two::three::four', ':')}
```
##### Example 5 (StructrScript)
```
${split('one.two.three.four', '.')}
```
##### Example 6 (StructrScript)
```
${split('one,two;three four')}
```
##### Example 7 (JavaScript)
```
${{ $.split('one-two-three-four', '-') }}
```

### splitRegex()
Splits the given string by given regex.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|string to split|no|
|separator|separator regex|yes|


Uses the given separator to split the given string into a collection of strings. This is the opposite of `join()`.
The default separator is a regular expression which splits the string at any of the following characters: `,;(whitespace)`
The optional second parameter is used as regex. To use a literal string as separator, see `split()`.

#### Signatures

```
splitRegex(str [, regex ])
```

#### Examples
##### Example 1 (StructrScript)
```
${splitRegex('one,two,three,four')}
```
##### Example 2 (StructrScript)
```
${splitRegex('one;two;three;four')}
```
##### Example 3 (StructrScript)
```
${splitRegex('one two three four')}
```
##### Example 4 (StructrScript)
```
${splitRegex('one::two::three::four', ':+')}
```
##### Example 5 (StructrScript)
```
${splitRegex('one.two.three.four', '\\.')}
```
##### Example 6 (StructrScript)
```
${splitRegex('one:two&three%four', ':|&|%')}
```
##### Example 7 (JavaScript)
```
${{ $.splitRegex('one:two&three%four', ':|&|%') }}
```

### startsWith()
Returns true if the given string starts with the given prefix.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|string to check|no|
|prefix|given start prefix|no|

#### Signatures

```
startsWith(str, prefix)
```

#### Examples
##### Example 1 (StructrScript)
```
${startsWith('Hello World!', 'Hello')}
> true
${startsWith('Hello World!', 'Hola')}
> false

```
##### Example 2 (JavaScript)
```
${{ $.startsWith('Hello World!', 'Hello') }}
```

### strReplace()
Replaces **each** substring of the subject that matches the given regular expression with the given replacement.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|subject|subject string|no|
|search|search string|no|
|replacement|replacement string|no|

#### Signatures

```
strReplace(str, substring, replacement)
```

#### Examples
##### Example 1 (StructrScript)
```
${strReplace('Hello Wrlod!', 'Wrlod', 'World')}
```
##### Example 2 (JavaScript)
```
${{ $.strReplace('Hello Wrlod!', 'Wrlod', 'World') }}
```

### stripHtml()
Removes all HTML tags from the given source string and returns only the content.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|source|HTML string for content extraction|no|

#### Notes
- Similar results can be produced by `strReplace(source, "\\<[a-zA-Z].*?>", "")`

#### Signatures

```
stripHtml(html)
```

#### Examples
##### Example 1 (StructrScript)
```
${stripHtml('<h3><p>Clean me!</p></h3>')}
```
##### Example 2 (JavaScript)
```
${{ $.stripHtml('<h3><p>Clean me!</p></h3>') }}
```

### substring()
Returns the substring of the given string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|URL to connect to|no|
|start|URL to connect to|no|
|length|length of string from start|yes|


Returns a portion with the given length of the given string, starting from the given start index.
If no length parameter is given or the length would exceed the string length (calculated from the start index), the rest of the string is returned.

#### Signatures

```
substring(str, start [, length ])
```

#### Examples
##### Example 1 (StructrScript)
```
${substring('This is my test', 2)}
> is is my test
${substring('This is my test', 8, 2)}
> my
${substring('This is my test', 8, 100)}
> my test

```
##### Example 2 (JavaScript)
```
${{ $.substring('This is my test', 2) }}
```

### titleize()
Titleizes the given string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|URL to connect to|no|
|separatorChars|string separator (default: ` `)|yes|

#### Signatures

```
titleize(str)
```

#### Examples
##### Example 1 (StructrScript)
```
${titleize('structr has a lot of built-in functions')}
> 'Structr Has A Lot Of Built-in Functions'

```
##### 2. (JavaScript) Different separator
```
${{ titleize('structr has a lot of built-in functions', '- ') }}
> 'Structr Has A Lot Of Built In Functions'

```

### trim()
Removes whitespace at the edges of the given string.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|object|object to trim|no|


Removes any leading or trailing whitespace from the given object. If the object is a string, a trimmed version
will be returned. If it is a collection, a collection of trimmed strings will be returned.
#### Notes
- A space is defined as any character whose codepoint is less than or equal to `U+0020` (the space character).

#### Signatures

```
trim(str)
```

#### Examples
##### Example 1 (StructrScript)
```
${trim('         A text with lots of whitespace        ')}
> 'A text with lots of whitespace'
```
##### Example 2 (JavaScript)
```
${{ $.trim(
	$.merge('     A text with lots of whitespace    ', '     Another text with lots of whitespace     ')
	)
}}
>['A text with lots of whitespace', 'Another text with lots of whitespace']
```

### upper()
Returns the uppercase value of its parameter.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|str|given string|no|

#### Signatures

```
upper(str)
```

#### Examples
##### Example 1 (StructrScript)
```
${upper(this.nickName)}
```
##### Example 2 (JavaScript)
```
${{ $.upper($.this.nickName) }}
```

## System Functions

### changelog()
Returns the changelog for a given entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entityOrUUID|entity to fetch changelog for|no|
|resolve|whether remote entities are resolved and returned|yes|
|filterKey|filter key, see above table|yes|
|filterValue|filter value, see above table|yes|


The `resolve` parameter controls if remote entities are resolved. Every changelog entry which has a `target` will be resolved as `targetObj` (if the remote entity still exists in the database).

**Filtering**
All filter options are chained using the boolean AND operator. Only changelog entries matching all of the specified filters will be returned.
For filter keys which can occurr more than once, the filter values are combined using the boolean OR operator (see examples 1 and 2)

| Filter Key | Applicable Changelog verbs (\*) | Changelog Entry will be returned if | max. occurrences |
|---|---|---|---|
| timeFrom (\*\*) | create, delete, link, unlink, change | `timeFrom` <= `time` of the entry | 1 (\*\*\*) |
| timeTo (\*\*) | create, delete, link, unlink, change | `timeTo` >= `time` of the entry | 1 (\*\*\*) |
| verb | create, delete, link, unlink, change | `verb` of the entry matches at least one of the verbs | n (\*\*\*\*) |
| userId | create, delete, link, unlink, change | `userId` of the entry matches at least one of the userIds     | n (\*\*\*\*) |
| userName | create, delete, link, unlink, change | `userName` of the entry matches at least one of the userNames | n (\*\*\*\*) |
| relType | link, unlink | `rel` of the entry matches at least one of the relTypes | n (\*\*\*\*) |
| relDir | link, unlink | `relDir` of the entry matches the given relDir | 1 (\*\*\*) |
| target | create, delete, link, unlink | `target` of the entry matches at least one of the targets     | n (\*\*\*\*) |
| key | change | `key` of the entry matches at least one of the keys | n (\*\*\*\*) |

(\*) If a filter parameter is supplied, only changelog entries can be returned to which it is applicable. (e.g. combining `key` and `relType` can never yield a result as they are mutually exclusive)
(\*\*) timeFrom/timeTo can be specified as a Long (time in ms since epoch), as a JavaScript Date object, or as a String with the format `yyyy-MM-dd'T'HH:mm:ssZ`
(\*\*\*) The last supplied parameter takes precedence over the others
(\*\*\*\*) The way we supply multiple occurrences of a keyword can differ from StructrScript to JavaScript

#### Notes
- The Changelog has to be enabled for this function to work properly. This can be done via the `application.changelog.enabled` key in configuration file structr.conf
- The `prev` and `val` keys in the `change` event contain JSON encoded elements since they can be strings or arrays.
- In a StructrScript environment parameters are passed as pairs of `'filterKey1', 'filterValue1'`.
- In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.

#### Signatures

```
changelog(entity [, resolve=false [, filterKey, filterValue ]... ])
changelog(uuid [, resolve=false [, filterKey, filterValue ]... ])
changelog(entity [, resolve=false [, filterKey, filterValue ]... ])
changelog(uuid [, resolve=false [, filterKey, filterValue ]... ])
```

#### Examples
##### 1. (StructrScript) Return all changelog entries with verb=link
```
${changelog(node, false, 'verb', 'link')}
```
##### 2. (JavaScript) Return all changelog entries with verb=(link OR unlink)
```
${{ $.changelog(node, false, {verb: ['link', 'unlink']}); }}
```
##### 3. (JavaScript) Return all changelog entries with (rel=OWNS) AND (verb=(link OR unlink))
```
${{ $.changelog(node, false, {verb: ['link', 'unlink'], 'relType': 'OWNS'}); }}
```
##### 4. (JavaScript) Return all changelog entries with (target=<NODEID>) AND (verb=(link OR unlink))
```
${{ $.changelog(node, false, {verb: ['link', 'unlink'], 'target': '<NODEID>'}); }}
```

### disableCascadingDelete()
Disables cascading delete in the Structr Backend for the current transaction.
#### Signatures

```
disableCascadingDelete()
```


### disableNotifications()
Disables the Websocket broadcast notifications in the Structr Backend UI for the current transaction.

This function can be used to temporarily disable the broadcasting of large modification operations, which greatly reduces the processing time. If you experience very slow (i.e. more than 10 seconds) object creation, modification or deletion, try to disable notifications before executing the operation. See also `enableNotifications()`.
#### Signatures

```
disableNotifications()
```


### disablePreventDuplicateRelationships()
Disables the check that prevents the creation of duplicate relationships in the Structr Backend for the current transaction.
#### Notes
- USE AT YOUR OWN RISK!

#### Signatures

```
disablePreventDuplicateRelationships()
```


### disableUuidValidation()
Disables the validation of user-supplied UUIDs when creating objects.
#### Notes
- This is a performance optimization for large imports, use at your own risk!

#### Signatures

```
disableUuidValidation()
```


### doAs()
Runs the given function in the context of the given user.

**JavaScript only**

#### Notes
- **Important**: Any node resource that was loaded outside of the function scope must be looked up again **inside** the function scope to prevent access problems.

#### Signatures

```
doAs(user, function)
```

#### Examples
##### Example 1 (JavaScript)
```
${{
    let user = $.find('User', { name: 'user_to_impersonate' })[0];

    $.doAs(user, () => {

        // code to be run as the given user
    });
}}

```

### doInNewTransaction()
Runs the given function in a new transaction context.

**JavaScript only**

#### Parameters

|Name|Description|Optional|
|---|---|---|
|function|lambda function to execute|no|
|errorHandler|error handler that receives the error / exception as an argument|yes|


This function allows you to detach long-running functions from the current transaction context (which is bound to the request), or execute large database operations in batches. Useful in situations where large numbers of nodes are created, modified or deleted.

This function is only available in JavaScript and takes a worker function as its first parameter and an optional error handler function as its second parameter.

**If the worker function returns `true`, it is run again.** If it returns anything else it is not run again.

If an exception occurs in the worker function, the error handler function (if present) is called. If the error handler returns `true`, the worker function is called again. If the error handler function returns anything else (or an exception occurs) the worker function is not run again.

When the `errorHandler` function is called, it receives the error / exception that was raised in the worker function. Depending on the error type, different methods are available on that object. Syntax errors will yield a `PolyglotException` (see https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/PolyglotException.html), other error types will yield different exception object.

#### Notes
- WARNING: This function is a prime candidate for an endless loop - be careful what your return condition is!
- WARNING: You have to be aware of the transaction context. Anything from the outermost transaction is not yet committed to the graph and thus can not be used to connect to in an inner transaction. The outer transaction context is only committed after the method is finished without a rollback. See example (2) for code which will result in an error and example (3) for a solution
- See also `schedule()`.

#### Signatures

```
doInNewTransaction(workerFunction [, errorHandlerFunction])
```

#### Examples
##### 1. (JavaScript) Regular working example
```
${{
	// Iterate over all users in packets of 10 and do stuff
	let pageSize = 10;
	let pageNo	= 1;

	$.doInNewTransaction(function() {

		// in Structr versions lower than 4.0 replace "$.predicate.page" with "$.page"
		let nodes = $.find('User', $.predicate.page(pageNo, pageSize));

		// do compute-heavy stuff here..

		pageNo++;

		return (nodes.length > 0);

	}, function() {
		$.log('Error occurred in batch function. Stopping.');
		return false;
	});
}}

```
##### 2. (JavaScript) Example where the inner transaction tries to create a relationship to a node which has not yet been committed and thus the whole code fails
```
${{
	// create a new group
	let group = $.getOrCreate('Group', 'name', 'ExampleGroup');

	$.doInNewTransaction(() => {
		let page = $.create('Page', 'name', 'ExamplePage');

		// this is where the error occurs - the Group node is not yet committed to the graph and when this context is closed a relationship between the group and the page is created - which can not work because only the page is committed to the graph
		$.grant(group, page, 'read');
	});
}}

```
##### 3. (JavaScript) Example to fix the problems of example (2)
```
${{
	// create a new group
	let groupId;

	$.doInNewTransaction(() => {
		let group = $.getOrCreate('Group', 'name', 'ExampleGroup');

		// save the group id to be able to fetch it later
		groupId = group.id;

		// after this context, the group is committed to the graph and relationships to it can later be created
	});

	$.doInNewTransaction(() => {
		let page = $.create('Page', 'name', 'ExamplePage' });

		/* fetch previously created group */
		let group = $.find('Group', groupId);

		// this works now
		$.grant(group, page, 'read');
	});
}}

```
##### 4. (JavaScript) Example where an error occurs and is handled (v4.0+)
```
${{
	$.doInNewTransaction(() => {
		let myString = 'the following variable is undefined ' + M;
	}, (ex) => {
		// depending on the exception type different methods will be available
		$.log(ex.getClass().getSimpleName());
		$.log(ex.getMessage());
	});
}

```

### doPrivileged()
Runs the given function in a privileged (superuser) context.

**JavaScript only**


This can be useful in scenarios where no security checks should run (e.g. bulk import, bulk deletion).

**Important**: Any node resource, which was loaded outside of the function scope, must be looked up again inside the function scope to prevent access problems.

#### Signatures

```
doPrivileged(function)
```

#### Examples
##### Example 1 (JavaScript)
```
${{
	let userToDelete = $.find('User', { name: 'user_to_delete' })[0];

	$.doPrivileged(() => {

		// look up user again to set correct access rights
		let user = $.find('User', userToDelete.id);

		// delete all projects owned by user
		$.delete($.find('Project', { projectOwner: user }));

		// delete user
		$.delete(user);
	});
}}

```

### enableCascadingDelete()
Enables cascading delete in the Structr Backend for the current transaction.
#### Signatures

```
enableCascadingDelete()
```


### enableNotifications()
Enables the Websocket broadcast notifications in the Structr Backend Ui for the current transaction.

This function can be used to re-enable the Websocket broadcast notifications disabled by the `disableNotifications()` function.
#### Signatures

```
enableNotifications()
```


### evaluateScript()
Evaluates a serverside script string in the context of the given entity.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|entity|`this` entity in the script|no|
|script|script source|no|


You can use this function to evaluate a dynamic script in the context of a Structr application. Please note that there are many different way to exploit this function to gain privileged access to your application and the underlying server. It is almost never a good idea to use this function.
#### Notes
- This function poses a **very severe** security risk if you are using it with user-provided content!
- The function runs in an auto-script context, i.e. you don't need to put ${ ... } around the script.
- If you want to run a JavaScript snippet, put curly braces around the script: { ... }.

#### Signatures

```
evaluateScript(entity, source)
```

#### Examples
##### 1. (StructrScript) Print the name of the current user
```
${evaluateScript(me, 'print(this.name)')}
```

### getAvailableServerlogs()
Returns a collection of available server logs files.

The collection of available server logs files is identical to the list of available server log files in the dashboard area.
#### Signatures

```
getAvailableServerlogs()
```


### getCacheValue()
Retrieves the cached value for the given key.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|cache key|no|


Returns null if there is no stored value for the given key or if the stored value is expired.
#### Signatures

```
getCacheValue(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${getCacheValue('externalResult')}
```
##### Example 2 (JavaScript)
```
${{ $.getCacheValue('externalResult') }}
```

### getSessionAttribute()
Retrieve a value for the given key from the user session.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|stored key of user session|no|

#### Signatures

```
getSessionAttribute(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${getSessionAttribute('doNotTrack')}
```
##### Example 2 (JavaScript)
```
${{ $.getSessionAttribute('doNotTrack') }}
```

### getenv()
Returns the value of the specified environment variable. If no value is specified, all environment variables are returned as a map. An environment variable is a system-dependent external named value.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|variable|name of enviroment variable|yes|

#### Notes
- This function was added in v4.0

#### Signatures

```
getenv([variable])
```

#### Examples
##### Example 1 (StructrScript)
```
${getenv('JAVA_HOME')}
```
##### Example 2 (JavaScript)
```
${{ return $.getenv().PATH; }}
${{ return $.getenv('PATH'); }}

```

### hasCacheValue()
Checks if a cached value exists for the given key.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|cache key|no|


Checks if a cached value exists for the given key. Returns false if there is no stored value for the given key or if the stored value is expired.
This function is especially useful if the result of a JavaScript function should be cached (see Example 2).

#### Signatures

```
hasCacheValue(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${hasCacheValue('externalResult')}
```
##### Example 2 (JavaScript)
```
${{
	let myComplexFunction = function() {
		// computation... for brevity just return a date string
		return new Date().toString();
	};
	let cacheKey = 'myKey';
	if ($.hasCacheValue(cacheKey)) {
		// retrieve cached value
		let cacheValue = $.getCacheValue(cacheKey);
		// ...
		// ...
	} else {
		// cache the result of a complex function
		let cacheResult = $.cache(cacheKey, 30, myComplexFunction());
		// ...
		// ...
	}
}}

```

### isLocale()
Returns true if the current user locale is equal to the given argument.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|locales|list of strings that represent different locales to check|no|

#### Notes
- See the `locale` keyword to learn how the locale of the current context is determined.

#### Signatures

```
isLocale(locales...)
```

#### Examples
##### 1. (StructrScript) Check whether the current locale is an English variant
```
${isLocale('en_GB', 'en_US')}
```

### jobInfo()
Returns information about the job with the given job ID.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|jobId|ID of the job to query|no|


If the job does not exist (anymore) the function returns `false`.

For **script jobs** the returned information is:

| Key | Value |
| --- | --- |
| `jobId` | The job ID |
| `jobtype` | The job type |
| `username` | The username of the user who started the job |
| `status` | The current status of the job |
| `jobName` | The name of the script job |
| `exception` | <p>**If an exception was caught** during the execution, an exception object containing:</p><p></p><p>`message` : The message of the exception</p><p>`cause` : The cause of the exception</p><p>`stacktrace` : The stacktrace of the exception |

For **file import** the returned information is:

| Key | Value |
| --- | --- |
| `jobId` | The job ID |
| `jobtype` | The job type |
| `username` | The username of the user who started the job |
| `status` | The current status of the job |
| `fileUuid` | The UUID of the imported file |
| `filepath` | The path of the imported file |
| `filesize` | The size of the imported file |
| `processedChunks` | The number of chunks already processed |
| `processedObjects` | The number of objects already processed |
| `exception` | <p>**If an exception was caught** during the execution, an exception object containing:</p><p></p><p>`message` : The message of the exception</p><p>`cause` : The cause of the exception</p><p>`stacktrace` : The stacktrace of the exception |

#### Signatures

```
jobInfo(jobId)
```

#### Examples
##### 1. (StructrScript) Return information about the job with ID 1
```
${jobInfo(1)}
```

### jobList()
Returns a list of running jobs.
#### Signatures

```
jobList()
```


### logEvent()
Logs an event to the Structr log.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|action|action to log (the verb)|no|
|message|message to log|no|
|subject|subject of the event (who did it?)|yes|
|object|object of the event (on which object was the action done?)|yes|


This function creates an entity of type `LogEvent` with the current timestamp and the given values. All four parameters (`action`, `message`, `subject` and `object`) can be arbitrary strings.

In JavaScript, the function can be called with a single map as parameter.

#### Signatures

```
logEvent(action, message [, subject [, object ]])
```

#### Examples
##### 1. (StructrScript) Log a simple "VIEW" event
```
${logEvent('VIEW', me.id)}
```
##### 2. (JavaScript) Log a simple "VIEW" event
```
${{
    $.logEvent({
	action: "VIEW",
	message: Structr.me.id
    });
}}

```

### maintenance()
Allows an admin user to execute a maintenance command from within a scripting context.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|command|name of the command to execute|no|
|arguments...|map (or key-value pairs) of arguments (depends on the command)|yes|


The following maintenance commands exist:

|Name|Description|
|---|---|
|changeNodePropertyKey|Migrates property values from one property key to another.|
|clearDatabase|Clears the database, i.e. removes all nodes and relationships from the database.|
|copyRelationshipProperties|Copies relationship properties from one key to another.|
|createLabels|Updates the type labels of a node in the database so they match the type hierarchy of the Structr type.|
|deleteSpatialIndex|Removes a (broken) spatial index from the database.|
|deployData|Creates a Data Deployment Export or Import of the application data.|
|deploy|Creates a Deployment Export or Import of the Structr application.|
|directFileImport|Imports files from a local directory into the Structr filesystem.|
|fixNodeProperties|Tries to fix properties in the database that have been stored with the wrong type.|
|flushCashes|Clears all internal caches.|
|letsencrypt|Triggers creation or update of an SSL certificate using Let’s Encrypt.|
|maintenanceMode|Enables or disables the maintenance mode.|
|rebuildIndex|Rebuilds the internal indexes, either for nodes, or for relationships, or for both.|
|setNodeProperties|Sets a given set of property values on all nodes of a certain type.|
|setRelationshipProperties|Sets a given set of property values on all relationships of a certain type.|
|setUuid|Adds UUIDs to all nodes and relationships that don’t have a value in their `id` property.|
#### Notes
- In a StructrScript environment arguments are passed as pairs of 'key1', 'value1'.
- In a JavaScript environment, this function takes a map as the second argument.

#### Signatures

```
maintenance(command [, arguments...])
```

#### Examples
##### 1. (JavaScript) Rebuild the index for the type "Article"
```
${{
    $.maintenance('rebuildIndex', { type: 'Article' });
}}

```

### random()
Returns a random alphanumeric string of the given length.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|length|length of random string|no|


This function can for example be used to create default passwords etc.
#### Signatures

```
random(length)
```

#### Examples
##### Example 1 (StructrScript)
```
${random(8)}
```
##### Example 2 (JavaScript)
```
${{ $.random(8) }}
```

### randomUuid()
Returns a random UUID.

New in v3.6: Returns a new random UUID, a string with 32 characters [0-9a-f].
#### Signatures

```
randomUuid()
```

#### Examples
##### Example 1 (StructrScript)
```
${random_uuid()}
```
##### Example 2 (JavaScript)
```
${{
    const newId = $.randomUuid();
    return newId;
}}

```

### removeSessionAttribute()
Remove key/value pair from the user session.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|key to remove from session|no|

#### Signatures

```
removeSessionAttribute(key)
```

#### Examples
##### Example 1 (StructrScript)
```
${removeSessionAttribute('do_no_track')}
```
##### Example 2 (JavaScript)
```
${{ $.removeSessionAttribute('do_not_track') }}
```

### serverlog()
Returns the last n lines from the server log file.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|lines|number of lines to return|yes|
|truncateLinesAfter|number of characters after which each log line is truncated with "[...]"|yes|
|logFile|log file to read from|yes|

#### Notes
- The `getAvailableServerlogs()` function can be used for the `logFile` parameter

#### Signatures

```
serverlog([ lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' (default different based on configuration) ] ] ])
```


### setEncryptionKey()
Sets the secret key for encryt()/decrypt(), overriding the value from structr.conf.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|secret|new secret key|no|

#### Notes
- Please note that this function overwrites the encryption key that is stored in structr.conf.
- The overwritten key can be restored by using `null` as a parameter to this function, as shown in the example below.

#### Signatures

```
setEncryptionKey(secretKey)
```

#### Examples
##### Example 1 (StructrScript)
```
${setEncryptionKey('MyNewSecret')}
```
##### Example 2 (JavaScript)
```
${{ $.setEncryptionKey('MyNewSecret') }}
```

### setLogLevel()
Sets the application log level to the given level, if supported.

Supported values are: ALL, TRACE, DEBUG, INFO, WARN, ERROR. The log level can also be set via the configuration setting "log.level". Using this function overrides the base configuration.

Change takes effect immediately until another call is made or the application is restarted. On system start, the configuration value is used.

#### Signatures

```
setLogLevel(string)
```


### setSessionAttribute()
Store a value under the given key in the users session.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|given key|no|
|value|given value for key|no|

#### Signatures

```
setSessionAttribute(key, value)
```

#### Examples
##### Example 1 (StructrScript)
```
${setSessionAttribute('do_no_track', true)}
```
##### Example 2 (JavaScript)
```
${{ $.setSessionAttribute('do_not_track', true) }}
```

### sleep()
Pauses the execution of the current thread for the given number of milliseconds.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|milliseconds|milliseconds to sleep|no|

#### Signatures

```
sleep(milliseconds)
```

#### Examples
##### Example 1 (StructrScript)
```
${sleep(1000)}
```
##### Example 2 (JavaScript)
```
${{$.sleep(1000)}}
```

### stackDump()
Logs the current execution stack.
#### Signatures

```
stackDump()
```


### store()
Stores the given value in the current request context under the given key.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|given key|no|
|value|value to store|no|


This function can be used to temporarily save the results of a computation step etc. and is often used to provide
some sort of "variables" in the scripting context. See `retrieve()` for the inverse operation.

#### Signatures

```
store(key, value)
```

#### Examples
##### Example 1 (StructrScript)
```
${store('users', find('User'))}
```
##### Example 2 (JavaScript)
```
${{ $.store('users', $.find('User')) }}
```

### structrEnv()
Returns Structr runtime env information.
#### Signatures

```
structrEnv()
```


### systemInfo()
Returns information about the system.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|key|info key|yes|


When called without parameters all info will be returned, otherwise specify a key to request specific info.
| Key | Value |
| --- | --- |
| now | Current time in ms |
| uptime | Time in ms since the application started |
| runtime | Version string of the java runtime |
| counts | number of nodes and relationships in the database (if connected) |
| caches | Size and max size of the node/relationship/localizations caches |
| memory | Memory information gathered from the runtime and from management beans (in bytes) |

#### Signatures

```
systemInfo()
```

#### Examples
##### Example 1 (StructrScript)
```
${ systemInfo()}
```
##### Example 2 (JavaScript)
```
${{ $.systemInfo() }}
```

### timer()
Starts/Stops/Pings a timer.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|name|name of timer|no|
|action|action (`start` or `get`)|no|


This function can be used to measure the performance of sections of code. The `action` parameter can be `start` to create a new timer or `get` to retrieve the elapsed time (in milliseconds) since the start of the timer.
#### Notes
- Using the `get` action before the `start` action returns 0 and starts the timer.
- Using the `start` action on an already existing timer overwrites the timer.

#### Signatures

```
timer(name, action)
```

#### Examples
##### Example 1 (StructrScript)
```
${timer('benchmark1', 'start')}
```
##### Example 2 (JavaScript)
```
${{ $.timer('benchmark1', 'start') }}
```

### unlockReadonlyPropertiesOnce()
Unlocks any read-only property for a single access.
#### Signatures

```
unlockReadonlyPropertiesOnce()
```


### unlockSystemPropertiesOnce()
Unlocks any system property for a single access.
#### Signatures

```
unlockSystemPropertiesOnce()
```


### userChangelog()
Returns the changelog for the changes a specific user made.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|user|given user|no|
|resolve|resolve user (default: `false`)|yes|
|filterKey1|key1 (only StructrScript)|yes|
|filterValue1|value1 (only StructrScript)|yes|
|filterKey1|keyN (only StructrScript)|yes|
|filterValue1|valueN (only StructrScript)|yes|
|map|data map (only JavaScript)|yes|

#### Notes
- Functionally identical to the changelog() function - only the data source is different.
- The User changelog has to be enabled for this function to work properly. This can be done via the application.changelog.user_centric.enabled key in configuration file structr.conf

#### Signatures

```
userChangelog(user [, resolve=false [, filterKey, filterValue ]... ])
```

#### Examples
##### Example 1 (StructrScript)
```
${userChangelog(current, false, 'verb', 'change', 'timeTo', now)}
```
##### Example 2 (JavaScript)
```
${{ $.userChangelog($.me, false, {verb:"change", timeTo: new Date()})) }}
```

## Validation Functions

### assert()
Aborts the current request if the given condition evaluates to false.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|condition|condition to evaluate|no|
|statusCode|statusCode to send **if the condition evaluates to `false`**|no|
|message|error message to send **if the condition evaluates to `false`**|no|


This function allows you to check a precondition and abort the execution flow immediately if the condition is not satisfied, sending a customized error code and error message to the caller, along with all the error tokens that have accumulated in the error buffer.

If you want to collect errors (e.g in a validation function), use `error()` which allows you to store error tokens in the context without aborting the execution flow.

#### Notes
- See also `getErrors()`, `clearError()`, `clearErrors()` and `error()`.
- Only works in schema methods, not in page rendering.

#### Signatures

```
assert(condition, statusCode, message)
```

#### Examples
##### 1. (JavaScript) Make sure only admin users can continue here
```
$.assert($.me.name == 'admin', 422, 'Only admin users are allowed to access this resource.')
```

### clearError()
Clears the given error token from the current context.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|errorToken|error token as returned by `getErrors()`|no|


This function only supports error tokens returned by the `getErrors()` function as arguments.
#### Notes
- See also `getErrors()`, `clearErrors()`, `error()` and `assert()`.

#### Signatures

```
clearError(errorToken)
```


### clearErrors()
Clears all error tokens present in the current context.
#### Notes
- See also `getErrors()`, `clearError()`, `error()` and `assert()`.

#### Signatures

```
clearErrors()
```


### error()
Stores error tokens in the current context causing the transaction to fail at the end of the request.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|propertyName|name of the property that caused the error, will end up in the `property` field of the response|no|
|errorToken|arbitrary string that represents the error, will end up in the `token` field of the response|no|
|errorMessage|more detailed description of the error for humans to read, will end up in the `detail` field of the response|yes|


This function allows you to store error tokens in the current context without aborting the execution flow. Errors that have accumulated in the error buffer can be fetched with `getErrors()` and cleared with either `clearError()` or `clearErrors()`.

If there are still error tokens in the error buffer at the end of the transaction, the transaction is rolled back. If the calling context was an HTTP request, the HTTP status code 422 Unprocessable Entity will be sent to the client together with the error tokens.

This function is mainly used in entity callback functions like `onCreate()` or `onSave()` to allow the user to implement custom validation logic.

If you want to abort the execution flow immediately, use `assert()`.

#### Notes
- See also `getErrors()`, `clearError()`, `clearErrors()` and `assert()`.

#### Signatures

```
error(propertyName, errorToken [, errorMessage])
```


### getErrors()
Returns all error tokens present in the current context.
#### Signatures

```
getErrors()
```


### hasError()
Allows checking if an error has been raised in the scripting context.
#### Signatures

```
hasError()
```


### isValidEmail()
Checks if the given address is a valid email address.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|address|address to validate|no|


The validation uses the email validation regular expression configured in `application.email.validation.regex`
#### Signatures

```
isValidEmail(address)
```

#### Examples
##### 1. (StructrScript) Valid email
```
${validateEmail('john@example.com')}
```
##### 2. (StructrScript) Invalid email
```
${validateEmail('John Doe <john@example.com>')}
```
##### 3. (StructrScript) Invalid email
```
${validateEmail('john@example')}
```
##### 4. (JavaScript) Script that checks if request parameter 'email' is a valid email address.
```
${{
	let potentialEmail = $.request.email;

	if ($.isValidEmail(potentialEmail)) {

		return 'Given email is valid.';

	} else {

		return 'Please supply a valid email address.';
	}
}}

```

### isValidUuid()
Tests if a given string is a valid UUID.
#### Parameters

|Name|Description|Optional|
|---|---|---|
|string|Input string to be evaluated as a valid UUID|no|


Returns true if the provided string is a valid UUID according to the configuration (see `application.uuid.allowedformats`). Returns false otherwise, including when the argument is not a string.
#### Signatures

```
isValidUuid(string)
```

#### Examples
##### 1. (JavaScript) Validate user input to prevent errors
```
${{
	let uuid = $.request.nodeId;

	if ($.isValidUuid(uuid)) {

		let node = $.find('MyNodeType', uuid);

		if ($.empty(node)) {

			// process further

		} else {

			return 'Invalid parameter!';
		}

	} else {

		return 'Invalid parameter!';
	}
}}

```
