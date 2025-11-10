## Built-in Functions
### assert

Aborts the current request if the given condition evaluates to false.

Usage: `${assert(condition, statusCode, message)}.
${{Structr.assert(condition, statusCode, message);}}.`
Example: `${assert(empty(str), 422, 'str must be empty!')}
${{Structr.assert(empty(str), 422, 'str must be empty!');}}`

### error

Signals an error to the caller

Usage: `${error(property, token[, detail])}.`
Example: `${error(\"name\", \"already_taken\"[, \"Another node with that name already exists\"])}`

### has_error

Allows checking if an error occurred in the scripting context

Usage: `	public static final String ERROR_MESSAGE_HAS_ERROR    = "Usage: ${has_error()}";
	public static final String ERROR_MESSAGE_HAS_ERROR_JS = "Usage: ${{ Structr.has_error() }}";`
Example: ``

### config

Returns the structr.conf value for the given key

Usage: `${config(keyFromStructrConf[, \"default\"])}.
${{Structr.config(keyFromStructrConf[, \"default\"])}}.`
Example: `${config(\"base.path\")}
${{Structr.config(\"base.path\")}}`

### getenv

Returns the value of the specified environment variable. If no value is specified, all environment variables are returned as a map. An environment variable is a system-dependent external named value.

Usage: `${getenv()}.
${{ $.getenv() }.`
Example: `${getenv('JAVA_HOME')}
${{ $.getenv('JAVA_HOME'); }}`

### changelog

Returns the changelog object

Usage: `${changelog(entity[, resolve=false[, filterKey, filterValue...]])}.
${{Structr.changelog(entity[, resolve=false[, filterObject]])}}.`
Example: `${changelog(current, false, 'verb', 'change', 'timeTo', now)}
${{Structr.changelog(Structr.get('current'), false, {verb:\"change\", timeTo: new Date()}))}}`

### user_changelog



Usage: `${user_changelog(user[, resolve=false[, filterKey, filterValue...]])}.
${{Structr.userChangelog(user[, resolve=false[, filterObject]])}}.`
Example: `${user_changelog(current, false, 'verb', 'change', 'timeTo', now)}
${{Structr.userChangelog(Structr.get('me'), false, {verb:\"change\", timeTo: new Date()}))}}`

### serverlog

Returns the last n lines from the server log file

Usage: `${serverlog([lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' ]]])}.
${{ $.serverlog([lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' ]]]); }}.`
Example: `${serverlog(200, -1, '/var/log/structr.log')}
${{ $.serverlog(200, -1, '/var/log/structr.log'); }}`

### get_available_serverlogs

Returns the last n lines from the server log file

Usage: `${get_available_serverlogs()}.
${{ $.get_available_serverlogs(); }}.`
Example: `${get_available_serverlogs()}
${{ $.get_available_serverlogs(); }}`

### grant

Grants the given permissions on the given entity to a user

Usage: `${grant(principal, node, permissions)}.
${{Structr.grant(principal, node, permissions)}}.`
Example: `${grant(me, this, 'read, write, delete'))}
${{Structr.grant(Structr.get('me'), Structr.this, 'read, write, delete'))}}`

### revoke

Revokes the given permissions on the given entity from a user

Usage: `${revoke(principal, node, permissions)}.
${{Structr.revoke(principal, node, permissions)}}.`
Example: `${revoke(me, this, 'write, delete'))}
${{Structr.revoke(Structr.('me'), Structr.this, 'write, delete'))}}`

### is_allowed

Returns whether the principal has all of the permission(s) on the given node.

Usage: `${is_allowed(principal, node, permissions)}.
${{Structr.is_allowed(principal, node, permissions)}}.`
Example: `${is_allowed(me, this, 'write, delete'))}
${{Structr.is_allowed(Structr.('me'), Structr.this, 'write, delete'))}}`

### add_to_group

Adds a user to a group.

Usage: `	public static final String ERROR_MESSAGE    = "Usage: ${add_to_group(group, user)}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.addToGroup(group, user);}}";`
Example: ``

### remove_from_group

Removes the given user from the given group

Usage: `	public static final String ERROR_MESSAGE    = "Usage: ${remove_from_group(group, principal)}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.removeFromGroup(group, principal);}}";`
Example: ``

### is_in_group

Returns true if a user is in the given group. If the optional parameter checkHierarchy is set to false, only a direct group membership is checked. Otherwise the group hierarchy is checked.

Usage: `	public static final String ERROR_MESSAGE    = "Usage: ${is_in_group(group, principal [, checkHierarchy = false ])}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.isInGroup(group, principal [, checkHierarchy = false ]);}}";`
Example: ``

### localize

Returns a (cached) Localization result for the given key and optional domain

Usage: `	public static final String ERROR_MESSAGE_LOCALIZE    = "Usage: ${localize(key[, domain])}. Example ${localize('HELLO_WORLD', 'myDomain')}";
	public static final String ERROR_MESSAGE_LOCALIZE_JS = "Usage: ${{Structr.localize(key[, domain])}}. Example ${{Structr.localize('HELLO_WORLD', 'myDomain')}}";`
Example: `	public static final String ERROR_MESSAGE_LOCALIZE    = "Usage: ${localize(key[, domain])}. Example ${localize('HELLO_WORLD', 'myDomain')}
	public static final String ERROR_MESSAGE_LOCALIZE_JS = "Usage: ${{Structr.localize(key[, domain])}}. Example ${{Structr.localize('HELLO_WORLD', 'myDomain')}}`

### call

Calls the given global schema method in the current users context

Usage: `	public static final String ERROR_MESSAGE_CALL    = "Usage: ${call(key [, key, value]}. Example ${call('myEvent', 'key1', 'value1', 'key2', 'value2')}";
	public static final String ERROR_MESSAGE_CALL_JS = "Usage: ${{Structr.call(key [, parameterMap]}}. Example ${{Structr.call('myEvent', {key1: 'value1', key2: 'value2'})}}";`
Example: `	public static final String ERROR_MESSAGE_CALL    = "Usage: ${call(key [, key, value]}. Example ${call('myEvent', 'key1', 'value1', 'key2', 'value2')}
	public static final String ERROR_MESSAGE_CALL_JS = "Usage: ${{Structr.call(key [, parameterMap]}}. Example ${{Structr.call('myEvent', {key1: 'value1', key2: 'value2'})}}`

### call_privileged

Calls the given global schema method with a superuser context

Usage: `	public static final String ERROR_MESSAGE_CALL_PRIVILEGED    = "Usage: ${call_privileged(key [, key, value]}. Example ${call_privileged('myEvent', 'key1', 'value1', 'key2', 'value2')}";
	public static final String ERROR_MESSAGE_CALL_PRIVILEGED_JS = "Usage: ${{Structr.call_privileged(key [, parameterMap]}}. Example ${{Structr.call_privileged('myEvent', {key1: 'value1', key2: 'value2'})}}";`
Example: `	public static final String ERROR_MESSAGE_CALL_PRIVILEGED    = "Usage: ${call_privileged(key [, key, value]}. Example ${call_privileged('myEvent', 'key1', 'value1', 'key2', 'value2')}
	public static final String ERROR_MESSAGE_CALL_PRIVILEGED_JS = "Usage: ${{Structr.call_privileged(key [, parameterMap]}}. Example ${{Structr.call_privileged('myEvent', {key1: 'value1', key2: 'value2'})}}`

### exec
{}(): Deprecation Warning: The call signature for this method has changed. The old signature of providing all arguments to the script is still supported but will be removed in a future version. Please consider upgrading to the new signature: {}", getName(), getSignature());
	--
{}(): If using a collection of parameters as second argument, the third argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", getName(), sources[2]);								}
{}(): IOException encountered: {}", new Object[]{ getName(), ex.getMessage() });
	--
{}(): Key '{}' not found in structr.conf, nothing executed.", getName(), scriptKey);	
{}(): Key '{}' in structr.conf is builtin. This is not allowed, nothing executed.", getName(), scriptKey);
	--
{}(): No file found for script key '{}' = '{}' ({}), nothing executed.", getName(), scriptKey, scriptName, absolutePath);	
{}(): Script key '{}' = '{}' points to script file '{}' which is either not a file (or a symlink) and not allowed, nothing executed.", getName(), scriptKey, scriptName, absolutePath);
	--
{}(): Script key '{}' = '{}' resolves to '{}' which seems to contain a directory traversal attack, nothing executed.", getName(), scriptKey, scriptName, absolutePath);	

Executes a script configured in structr.conf with the given configuration key, a collection of parameters and the desired logging behaviour, returning the standard output of the script. The logging behaviour for the command line has three possible values: [0] do not log command line [1] log only full path to script [2] log path to script and each parameter either unmasked or masked. In JavaScript the function is most flexible - each parameter can be given as a simple string or as a configuration map with a 'value' and a 'masked' flag.

Usage: `${exec(scriptConfigKey [, parameterCollection [, logBehaviour ] ])}.
${{ $.exec(scriptConfigKey  [, parameterCollection [, logBehaviour ] ]); }}.`
Example: `${exec('my-script', merge('param1', 'param2'), 1)}
${{ $.exec('my-script', ['param1', { value: 'CLIENT_SECRET', masked: true }], 2); }}`

### exec_binary
{}(): Deprecation Warning: The call signature for this method has changed. The old signature of providing all arguments to the script is still supported but will be removed in a future version. Please consider upgrading to the new signature: {}", getName(), getSignature());
	--
{}(): If using a collection of parameters as third argument, the fourth argument (logBehaviour) must either be 0, 1 or 2 (or omitted, where default 2 will apply). Value given: {}", getName(), sources[2]);								}
{}(): IOException encountered: {}", new Object[]{ getName(), ex.getMessage() });
	

Executes a script configured in structr.conf with the given configuration key, a collection of parameters and the desired logging behaviour, returning the raw output directly into the output stream. The logging behaviour for the command line has three possible values: [0] do not log command line [1] log only full path to script [2] log path to script and each parameter either unmasked or masked. In JavaScript the function is most flexible - each parameter can be given as a simple string or as a configuration map with a 'value' and a 'masked' flag.

Usage: `${exec_binary(outputStream, scriptConfigKey [, parameterCollection [, logBehaviour ] ])}.
${{Structr.exec_binary(outputStream, scriptConfigKey [, parameterCollection [, logBehaviour ] ]}}.`
Example: `${exec(response, 'my-script', merge('param1', 'param2'), 1)}
${{ $.exec($.response, 'my-script', ['param1', { value: 'CLIENT_SECRET', masked: true }], 2); }}`

### unlock_readonly_properties_once

Unlocks any read-only property for a single access

Usage: `	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE    = "Usage: ${unlock_readonly_properties_once(node)}. Example ${unlock_readonly_properties_once(this)}";
	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS = "Usage: ${{Structr.unlock_readonly_properties_once(node)}}. Example ${{Structr.unlock_readonly_properties_once(Structr.get('this'))}}";`
Example: `	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE    = "Usage: ${unlock_readonly_properties_once(node)}. Example ${unlock_readonly_properties_once(this)}
	public static final String ERROR_MESSAGE_UNLOCK_READONLY_PROPERTIES_ONCE_JS = "Usage: ${{Structr.unlock_readonly_properties_once(node)}}. Example ${{Structr.unlock_readonly_properties_once(Structr.get('this'))}}`

### unlock_system_properties_once

Unlocks any system property for a single access

Usage: `	public static final String ERROR_MESSAGE_UNLOCK_SYSTEM_PROPERTIES_ONCE    = "Usage: ${unlock_system_properties_once(node)}. Example ${unlock_system_properties_once(this)}";
	public static final String ERROR_MESSAGE_UNLOCK_SYSTEM_PROPERTIES_ONCE_JS = "Usage: ${{Structr.unlock_system_properties_once(node)}}. Example ${{Structr.unlock_system_properties_once(Structr.get('this'))}}";`
Example: `	public static final String ERROR_MESSAGE_UNLOCK_SYSTEM_PROPERTIES_ONCE    = "Usage: ${unlock_system_properties_once(node)}. Example ${unlock_system_properties_once(this)}
	public static final String ERROR_MESSAGE_UNLOCK_SYSTEM_PROPERTIES_ONCE_JS = "Usage: ${{Structr.unlock_system_properties_once(node)}}. Example ${{Structr.unlock_system_properties_once(Structr.get('this'))}}`

### set_privileged

Sets the given key/value pair(s) on the given entity with super-user privileges

Usage: `${set_privileged(entity, propertyKey, value)}.
${{Structr.setPrvileged(entity, propertyKey, value)}}.`
Example: `${set_privileged(this, \"email\", lower(this.email))}
${{Structr.setPrivileged(Structr.this, \"email\", lower(Structr.this.email))}}`

### Predicate function: privileged

Returns a collection of entities of the given type from the database, takes optional key/value pairs. Executed in a super user context.

Usage: `${find_privileged(type, key, value)}.`
Example: `${find_privileged(\"User\", \"email\", \"tester@test.com\"}`

### read

Reads and returns the contents of the given file from the exchange directoy

Usage: `${read(filename)}.`
Example: `${read(\"text.xml\")}`

### write

Writes to the given file in the exchange directoy

Usage: `${write(filename, value)}.`
Example: `${write(\"text.txt\", this.name)}`

### append

Appends to the given file in the exchange directoy

Usage: `${append(filename, value)}.`
Example: `${append(\"test.txt\", this.name)}`

### xml

Parses the given string to an XML DOM

Usage: `${xml(xmlSource)}.`
Example: `${xpath(xml(this.xmlSource), \"/test/testValue\")}`

### xpath

Returns the value of the given XPath expression from the given XML DOM. The optional third parameter defines the return type, possible values are: NUMBER, STRING, BOOLEAN, NODESET, NODE, default is STRING.

Usage: `${xpath(xmlDocument, expression [, returnType ])}.`
Example: `${xpath(xml(this.xmlSource), \"/test/testValue\", \"STRING\")}`

### geocode

Returns the geolocation (latitude, longitude) for the given street address using the configured geocoding provider

Usage: `${geocode(street, city, country)}.`
Example: `${set(this, geocode(this.street, this.city, this.country))}`

### instantiate

Instantiates the given Neo4j node into a Structr node

Usage: `${instantiate(node)}.`
Example: `${instantiate(result.node)}`

### property_info

Returns the schema information for the given property

Usage: `	public static final String ERROR_MESSAGE_PROPERTY_INFO    = "Usage: ${property_info(type, name)}. Example ${property_info('User', 'name')}";
	public static final String ERROR_MESSAGE_PROPERTY_INFO_JS = "Usage: ${Structr.propertyInfo(type, name)}. Example ${Structr.propertyInfo('User', 'name')}";`
Example: `	public static final String ERROR_MESSAGE_PROPERTY_INFO    = "Usage: ${property_info(type, name)}. Example ${property_info('User', 'name')}
	public static final String ERROR_MESSAGE_PROPERTY_INFO_JS = "Usage: ${Structr.propertyInfo(type, name)}. Example ${Structr.propertyInfo('User', 'name')}`

### function_info

Returns information about the currently running Structr method, OR about the method defined in the given type and name.

Usage: `	public static final String ERROR_MESSAGE_FUNCTION_INFO    = "Usage: ${function_info([type, name])}. Example ${function_info()}";
	public static final String ERROR_MESSAGE_FUNCTION_INFO_JS = "Usage: ${{ $.functionInfo([type, name]) }}. Example ${{ $.functionInfo() }}";`
Example: `	public static final String ERROR_MESSAGE_FUNCTION_INFO    = "Usage: ${function_info([type, name])}. Example ${function_info()}
	public static final String ERROR_MESSAGE_FUNCTION_INFO_JS = "Usage: ${{ $.functionInfo([type, name]) }}. Example ${{ $.functionInfo() }}`

### type_info

Returns the type information for the specified type

Usage: `	public static final String ERROR_MESSAGE_TYPE_INFO    = "Usage: ${type_info(type[, view])}. Example ${type_info('User', 'public')}";
	public static final String ERROR_MESSAGE_TYPE_INFO_JS = "Usage: ${$.typeInfo(type[, view])}. Example ${$.typeInfo('User', 'public')}";`
Example: `	public static final String ERROR_MESSAGE_TYPE_INFO    = "Usage: ${type_info(type[, view])}. Example ${type_info('User', 'public')}
	public static final String ERROR_MESSAGE_TYPE_INFO_JS = "Usage: ${$.typeInfo(type[, view])}. Example ${$.typeInfo('User', 'public')}`

### enum_info

Returns the enum values as an array

Usage: `	public static final String ERROR_MESSAGE_ENUM_INFO    = "Usage: ${enum_info(type, enumProperty[, raw])}. Example ${enum_info('Document', 'documentType')}";
	public static final String ERROR_MESSAGE_ENUM_INFO_JS = "Usage: ${Structr.enum_info(type, enumProperty[, raw])}. Example ${Structr.enum_info('Document', 'documentType')}";`
Example: `	public static final String ERROR_MESSAGE_ENUM_INFO    = "Usage: ${enum_info(type, enumProperty[, raw])}. Example ${enum_info('Document', 'documentType')}
	public static final String ERROR_MESSAGE_ENUM_INFO_JS = "Usage: ${Structr.enum_info(type, enumProperty[, raw])}. Example ${Structr.enum_info('Document', 'documentType')}`

### structr_env

Returns Structr runtime env information.

Usage: `	public static final String ERROR_MESSAGE_STRUCTR_ENV    = "Usage: ${structr_env()}. Example ${structr_env()}";
	public static final String ERROR_MESSAGE_STRUCTR_ENV_JS = "Usage: ${Structr.structr_env()}. Example ${Structr.structr_env()}";`
Example: `	public static final String ERROR_MESSAGE_STRUCTR_ENV    = "Usage: ${structr_env()}. Example ${structr_env()}
	public static final String ERROR_MESSAGE_STRUCTR_ENV_JS = "Usage: ${Structr.structr_env()}. Example ${Structr.structr_env()}`

### disable_cascading_delete

Disables cascading delete in the Structr Backend for the current transaction

Usage: `	public static final String ERROR_MESSAGE_DISABLE_CASCADING_DELETE    = "Usage: ${disable_cascading_delete()}";
	public static final String ERROR_MESSAGE_DISABLE_CASCADING_DELETE_JS = "Usage: ${Structr.disableCascadingDelete()}";`
Example: ``

### enable_cascading_delete

Enables cascading delete in the Structr Backend for the current transaction

Usage: `	public static final String ERROR_MESSAGE_ENABLE_CASCADING_DELETE    = "Usage: ${enable_cascading_delete()}";
	public static final String ERROR_MESSAGE_ENABLE_CASCADING_DELETE_JS = "Usage: ${Structr.enableCascadingDelete()}";`
Example: ``

### disable_notifications

Disables the Websocket notifications in the Structr Ui for the current transaction

Usage: `	public static final String ERROR_MESSAGE_DISABLE_NOTIFICATIONS    = "Usage: ${disable_notifications()}";
	public static final String ERROR_MESSAGE_DISABLE_NOTIFICATIONS_JS = "Usage: ${Structr.disableNotifications()}";`
Example: ``

### disable_prevent_duplicate_relationships

Disables prevention of duplicate relationships in many-to-many rels in the Structr Backend for the current transaction - USE AT YOUR OWN RISK!

Usage: `	public static final String ERROR_MESSAGE_DISABLE_PREVENT_DUPLICATE_RELATIONSHIPS    = "Usage: ${disable_prevent_duplicate_relationships()}";
	public static final String ERROR_MESSAGE_DISABLE_PREVENT_DUPLICATE_RELATIONSHIPS_JS = "Usage: ${Structr.disablePreventDuplicateRelationships()}";`
Example: ``

### disable_uuid_validation

Disables the validation of user-supplied UUIDs when creating objects. (Note: this is a performance optimization for large imports, use at your own risk!)

Usage: `	public static final String ERROR_MESSAGE_DISABLE_UUID_VALIDATION    = "Usage: ${disable_uuid_validation()}";
	public static final String ERROR_MESSAGE_DISABLE_UUID_VALIDATION_JS = "Usage: ${Structr.disableUuidValidation()}";`
Example: ``

### enable_notifications

Enables the Websocket notifications in the Structr Ui for the current transaction

Usage: `	public static final String ERROR_MESSAGE_ENABLE_NOTIFICATIONS    = "Usage: ${enable_notifications()}";
	public static final String ERROR_MESSAGE_ENABLE_NOTIFICATIONS_JS = "Usage: ${Structr.enableNotifications()}";`
Example: ``

### evaluate_script

Evaluates a serverside script string in the context of the given entity

Usage: `	public static final String ERROR_MESSAGE_EVALUATE_SCRIPT	 = "Usage: ${evaluate_script(entity, script)}";
	public static final String ERROR_MESSAGE_EVALUATE_SCRIPT_JS	 = "Usage: ${Structr.evaluate_script(entity, script)}";`
Example: ``

### ancestor_types
{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}	

Returns the names of the parent types of the given type and filters out all entries of the blacklist collection.

Usage: `	public static final String ERROR_MESSAGE_ANCESTOR_TYPES    = "Usage: ${ancestor_types(type[, blacklist])}. Example ${ancestor_types('User', ['Principal'])}";
	public static final String ERROR_MESSAGE_ANCESTOR_TYPES_JS = "Usage: ${Structr.ancestor_types(type[, blacklist])}. Example ${Structr.ancestor_types('User', ['Principal'])}";`
Example: `	public static final String ERROR_MESSAGE_ANCESTOR_TYPES    = "Usage: ${ancestor_types(type[, blacklist])}. Example ${ancestor_types('User', ['Principal'])}
	public static final String ERROR_MESSAGE_ANCESTOR_TYPES_JS = "Usage: ${Structr.ancestor_types(type[, blacklist])}. Example ${Structr.ancestor_types('User', ['Principal'])}`

### inheriting_types
{}(): Type not found: {}" + (caller != null ? " (source of call: " + caller.toString() + ")" : ""), getName(), sources[0]);
			}	

Returns the names of the child types of the given type and filters out all entries of the blacklist collection.

Usage: `	public static final String ERROR_MESSAGE_INHERITING_TYPES    = "Usage: ${inheriting_types(type[, blacklist])}. Example ${inheriting_types('User')}";
	public static final String ERROR_MESSAGE_INHERITING_TYPES_JS = "Usage: ${Structr.inheriting_types(type[, blacklist])}. Example ${Structr.inheriting_types('User')}";`
Example: `	public static final String ERROR_MESSAGE_INHERITING_TYPES    = "Usage: ${inheriting_types(type[, blacklist])}. Example ${inheriting_types('User')}
	public static final String ERROR_MESSAGE_INHERITING_TYPES_JS = "Usage: ${Structr.inheriting_types(type[, blacklist])}. Example ${Structr.inheriting_types('User')}`

### template

Returns a MailTemplate object with the given name, replaces the placeholders with values from the given entity

Usage: `${template(name, locale, source)}.
${{Structr.template(name, locale, source)}}.`
Example: `${template(\"TEXT_TEMPLATE_1\", \"en_EN\", this)}
${{Structr.template(\"TEXT_TEMPLATE_1\", \"en_EN\", Structr.get('this'))}}`

### jdbc

Fetches data from a JDBC source

Usage: `${jdbc(url, query[, username, password ])}.`
Example: `${jdbc(\"jdbc:mysql://localhost:3306\", \"SELECT * from Test\", \"user\", \"p4ssw0rd\")}`

### mongodb
{}(): Encountered exception '{}' for input: {}", new Object[] { getName(), t.getMessage(), sources });
			}	

Opens and returns a connection to an external MongoDB instance

Usage: `${mongodb(url, database, collection)}.`
Example: `${mongodb(\"mongodb://localhost:27017\", \"database1\", \"collection1\")}`

### bson

Creates BSON document from a map / object

Usage: `${bson(data)}.`
Example: `${bson({ name: 'Test' })}`

### get_relationship_types

Returns the list of available relationship types form and/or to this node. Either potentially available (schema) or actually available (database).

Usage: `${get_relationship_types(node, lookupType [, direction])}.
${{Structr.get_relationship_types(node, lookupType [, direction ])}}.`
Example: `${get_relationship_types(me, 'existing', 'both')}
${{Structr.get_relationship_types(me, 'existing', 'both')}}`

### set_encryption_key

Sets the secret key for encryt()/decrypt(), overriding the value from structr.conf

Usage: `	public static final String ERROR_MESSAGE_SET_KEY    = "Usage: ${set_encryption_key(secret)}";
	public static final String ERROR_MESSAGE_SET_KEY_JS = "Usage: ${{Structr.setEncryptionKey(secret)}}";`
Example: ``

### encrypt

Encrypts the given string with a secret key from structr.conf or argument 2

Usage: `	public static final String ERROR_MESSAGE_ENCRYPT    = "Usage: ${encrypt(value[, key])}";
	public static final String ERROR_MESSAGE_ENCRYPT_JS = "Usage: ${{Structr.encrypt(value[, key])}}";`
Example: ``

### decrypt

Decrypts the given string with a secret key from structr.conf or argument 2

Usage: `	public static final String ERROR_MESSAGE_DECRYPT    = "Usage: ${decrypt(value[, secret])}";
	public static final String ERROR_MESSAGE_DECRYPT_JS = "Usage: ${{Structr.decrypt(value[, secret])}}";`
Example: ``

### Predicate function: range

Returns a range predicate that can be used in find() function calls

Usage: `${range(start, end)}.`
Example: `${find(\"Event\", \"date\", range(\"2018-12-31\", \"2019-01-01\"))}`

### Predicate function: within_distance

Returns a query predicate that can be used with find() or search().

Usage: `${within_distance(latitude, longitude, meters).`
Example: `${find('Location', and(within_distance(51, 7, 10)))}`

### Predicate function: empty

Returns a query predicate that can be used with find() or search().

Usage: `${empty(key).`
Example: `${find('Group', empty('name'))}`

### Predicate function: equals

Returns a query predicate that can be used with find() or search().

Usage: `${equals(key, value).`
Example: `${find('Group', and(equals('name', 'Test')))}`

### Predicate function: contains

Returns a query predicate that can be used with find() or search().

Usage: `${contains(key, value).`
Example: `${find('Group', and(contains('name', 'Test')))}`

### Predicate function: and

Returns a query predicate that can be used with find() or search().

Usage: `${and(predicate, ...).`
Example: `${find('Group', and(equals('name', 'Test')))}`

### Predicate function: or

Returns a query predicate that can be used with find() or search().

Usage: `${or(predicate, ...).`
Example: `${find('Group', or(equals('name', 'Test1'), equals('name', 'Test2')))}`

### Predicate function: not

Returns a query predicate that can be used with find() or search().

Usage: `${not(predicate, ...).`
Example: `${find('Group', not(equals('name', 'Test')))}`

### Predicate function: sort

Returns a query predicate that can be used with find() or search().

Usage: `${sort(key [, descending]).`
Example: `${find('Group', sort('name'))}`

### Predicate function: page

Returns a query predicate that can be used with find() or search().

Usage: `${page(page, pageSize).`
Example: `${find('Group', page(1, 10))}`

### Predicate function: starts_with

Returns a query predicate that can be used with find() or search().

Usage: `${startsWith(key, value).`
Example: `${find('Group', and(startsWith('name', 'Test')))}`

### Predicate function: ends_with

Returns a query predicate that can be used with find() or search().

Usage: `${endsWith(key, value).`
Example: `${find('Group', and(endsWith('name', 'Test')))}`

### Predicate function: lt

Returns an lt predicate that can be used in find() function calls

Usage: `${lt(other)}.`
Example: `${find(\"User\", \"age\", lt(\"42\"))}`

### Predicate function: lte

Returns an lte predicate that can be used in find() function calls

Usage: `${lte(other)}.`
Example: `${find(\"User\", \"age\", lte(\"42\"))}`

### Predicate function: gte

Returns a gte predicate that can be used in find() function calls

Usage: `${gte(other)}.`
Example: `${find(\"User\", \"age\", gte(\"42\"))}`

### Predicate function: gt

Returns a gt predicate that can be used in find() function calls

Usage: `${gt(other)}.`
Example: `${find(\"User\", \"age\", gt(\"42\"))}`

