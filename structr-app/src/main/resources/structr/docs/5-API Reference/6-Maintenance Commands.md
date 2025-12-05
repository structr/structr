# Maintenance Commands
## changeNodePropertyKey
Migrates property values from one property key to another.
### Parameters

|Name|Description|Optional|
|---|---|---|
|oldKey|source key|no|
|newKey|target key|no|


This command can for example be used to move all name values to a description property.

## clearDatabase
Clears the database, i.e. removes all nodes and relationships from the database.
### Notes
- Warning: this action cannot be reversed, it will delete your application and everything else in the database, even non-Structr nodes and relationships.


## copyRelationshipProperties
Copies relationship properties from one key to another.
### Parameters

|Name|Description|Optional|
|---|---|---|
|sourceKey|source key|no|
|destKey|destination key|no|


## createLabels
Updates the type labels of a node in the database so they match the type hierarchy of the Structr type.
### Parameters

|Name|Description|Optional|
|---|---|---|
|type|if set, labels are only updated on nodes with the given type|yes|
|removeUnused|if set to `false`, unused labels are left on the node (default is `true`)|yes|


This command looks at the value in the `type` property of an object and tries to identify a corresponding schema type. If the schema type exists, it creates a label on the object for each type in the inheritance hierarchy and removes labels that don’t have a corresponding type.
### Notes
- This command will only work for objects that have a value in their `type` property.


## deleteSpatialIndex
Removes a (broken) spatial index from the database.

This command deletes all Structr nodes with the properties `bbox` and `gtype`.
### Notes
- This is a legacy command which you will probably never need.


## deploy
Creates a Deployment Export or Import of the Structr application.
### Parameters

|Name|Description|Optional|
|---|---|---|
|mode|deployment mode, `import` or `export`|no|
|source|source folder for `import` mode|yes|
|target|target folder for `export` mode|yes|
|extendExistingApp|`import` only: if set to `true`, the import will be incremental, i.e. the existing Structr app will not be removed before importing the new application|yes|


This command reads or writes a text-based export of the application (without its data!) that can be stored in a version control system. The maintenance command is used internally in the Dashboard section.

## deployData
Creates a Data Deployment Export or Import of the application data.
### Parameters

|Name|Description|Optional|
|---|---|---|
|mode|deployment mode, `import` or `export`|no|
|source|source folder for `import` mode|yes|
|target|target folder for `export` mode|yes|
|types|comma-separated list of data types to export|yes|


This command reads or writes a text-based export of the application data (not the application itself) that can be stored in a version control system.

## directFileImport
Imports files from a local directory into the Structr filesystem.
### Parameters

|Name|Description|Optional|
|---|---|---|
|source|source directory to import files from|no|
|mode|import mode (`copy` or `move`)|no|
|existing|how to handle existing files in the destination (`skip`, `overwrite` or `rename`, default is `skip`)|yes|
|index|whether to index the copied files (`true` or `false`, default is `true`)|yes|


The files can either be copied or moved (i.e. deleted after copying into Structr), depending on the mode parameter. The existing parameter determines how Structr handles existing files in the Structr Filesystem. The index parameter allows you to enable or disable indexing for the imported files.
### Notes
- When using Docker, you first have to copy the files to the Docker container, or use a files volume.


## fixNodeProperties
Tries to fix properties in the database that have been stored with the wrong type.
### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of nodes to fix|no|
|name|name of property to fix (defaults to "all properties" if omitted)|yes|


This command can be used to convert property values whose property type was changed, e.g. from String to Integer.

## flushCashes
Clears all internal caches.

This command can be used to reduce the amount of memory consumed by Structr, or to fix possible cache invalidation errors.

## letsencrypt
Triggers creation or update of an SSL certificate using Let’s Encrypt.
### Parameters

|Name|Description|Optional|
|---|---|---|
|server|`staging` or `production`, `staging` mode is meant for testing and will generate invalid dummy certificates only, while `production` creates real, valid certificates but is throttled.|no|
|challenge|overwrite the default challenge method as set in structr.conf. This is convenient to test an alternative challenge type without the need to restart the Structr instance.|yes|
|wait|let the client wait for the given number of seconds in order to have enough time to prepare the DNS TXT record in case of the dns challenge type, or the HTTP response in case of the http challenge.|yes|
|reload|`true` or `false`, reload the HTTPS certificate after updating it. Allows using the new certificate without restarting, defaults to `false`.|yes|

### Notes
- Please note that the configuration setting `letsencrypt.domains` must contain the full domain name of the server you want to create the certificate for.


## maintenanceMode
Enables or disables the maintenance mode.
### Parameters

|Name|Description|Optional|
|---|---|---|
|action|`enable` or `disable`|no|


When the maintenance mode is started, the following services are shut down:

- FtpService
- HttpService
- SSHService
- AgentService
- CronService
- DirectoryWatchService
- LDAPService
- MailService

After a short delay, the following services are restarted on different ports:
- FtpService
- HttpService
- SSHService

### Notes
- Active processes will keep running until they are finished. If for example a cron job is running, it will not be halted. Only the services are stopped so no NEW processes are started.


## rebuildIndex
Rebuilds the internal indexes, either for nodes, or for relationships, or for both.
### Parameters

|Name|Description|Optional|
|---|---|---|
|type|limit the execution to the given node type|yes|
|relType|limit the execution to the given relationship|yes|
|mode|`nodesOnly` or `relsOnly` to rebuild the index only for nodes or relationships|yes|


Rebuilding the index means that all objects are first removed from the index and then added to the index again with all properties that have the `indexed` flag set.

## setNodeProperties
Sets a given set of property values on all nodes of a certain type.
### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of nodes to set properties on|no|
|newType|can be used to update the `type` property of nodes (because type is already taken)|yes|


This command takes all arguments other than `type` and `newType` for input properties and sets the given values on all nodes of the given type.
### Notes
- Warning: if this command is used to change the `type` property of nodes, the "Create Labels" command has to be called afterwards to update the labels of the changed nodes - otherwise they will not be accessible.


## setRelationshipProperties
Sets a given set of property values on all relationships of a certain type.
### Parameters

|Name|Description|Optional|
|---|---|---|
|type|type of relationships to set properties on|no|


This command takes all arguments other than `type` for input properties and sets the given values on all nodes of the given type.

Please note that you can not set the `type` property of a relationship with this command. Relationship types can only be changed by removing and re-creating the relationship.


## setUuid
Adds UUIDs to all nodes and relationships that don’t have a value in their `id` property.
### Parameters

|Name|Description|Optional|
|---|---|---|
|type|if set, this command will only be applied to nodes with the given type|yes|
|relType|if set, this command will only be applied to relationships with the given type|yes|
|allNodes|if set to `true`, this command will only be applied to nodes|yes|
|allRels|if set to `true`, this command will only be applied to relationships|yes|

