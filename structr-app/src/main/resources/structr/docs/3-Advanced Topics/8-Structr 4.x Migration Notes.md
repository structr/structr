# Structr 4.x Migration Notes

All versions starting with the 4.0 release include breaking changes which require a migration of applications that have been built with Structr versions prior to 4.0 (1.x, 2.x and 3.x). The following chapters will give an overview of how the migration can be done.

> **Note:** It is always advised to have a full backup before upgrading Structr.

## GraalVM Migration Guide

With version 4.0 of Structr, the required Java Runtime changed from one of the supported default JVMs (e.g. OpenJDK or Oracle JDK) to a specific implementation called [GraalVM](https://graalvm.org). The GraalVM is a modern JVM framework that brings many new features like a polyglot scripting engine and the option to create so-called Native Images, precompiled native executables of a custom application. 

Also with GraalVM, the JavaScript scripting engine underwent a large rewrite. This gives Structr 4.0 many advantages compared to the prior versions like full support of the latest ECMAScript standards or better overall performance of the system. However, these changes make a couple a manual migrations necessary which should be quite simple to make in a deployment export.

### Installing GraalVM

Each Structr version typically supports the stable GraalVM version current at the time of release. However, there may be exceptions for certain GraalVM versions that have breaking changes that are not fully supported by Structr.

Currently, Structr 5.x depends on GraalVM 22.1.0. The following example shows how it can be installed on linux-based distributions:

    wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
    tar xvzf graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
    sudo mv graalvm-ce-java11-22.1.0 /usr/lib/jvm
    sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/graalvm-ce-java11-22.1.0/bin/java 2210
    sudo update-alternatives --auto java

## Migration of Script Expressions

There are a couple of simple steps to execute in order to make script expressions work with Structr 4.0 and later.

### Predicates in find() and seach()

All predicates in `find()` and `search()` scripting expressions need to be prefixed with `$.predicate`.  The easiest way to achieve this is to export the application using the deployment export and searching all files for occurrences of the advanced find predicate list:

    $.and
    $.or
    $.not
    $.equals
    $.contains
    $.empty
    $.range
    $.within_distance
    $.sort
    $.page

#### Examples for predicates that have to migrated

Before:

    $.find('File', 'size', $.range(null, 100), $.page(1, 10));

After:

    $.find('File', 'size', $.predicate.range(null, 100), $.predicate.page(1, 10));

Before:

    $.find('User', $.sort('createdDate'));

After:

    $.find('User', $.predicate.sort('createdDate'));

Some predicates also exist as regular functions (`$.sort()`, `$.empty()`, …) or keywords (`$.page`, …). They don't have to be changed.

#### Example for keywords that donh't have to be migrated

    $.sort($.find('User'), 'createdDate');

Here, the `sort()` function is used to sort the nodes fetched via `find()` and is not used within the `find()` function itself, so nothing has to be changed here.

### Resource Access Permissions

Resource Permissions (formerly called "Resource Access Grants") are used to define the access permissions for resource endpoints of the integrated APIs.

They have been made more flexible in Structr 4.0 and later. Rights management now also applies to permission nodes which hold the information about who has access to what. This requires users to have read access to the permission object itself to be able to “use” it. Assigning visibility flags (`visibleToPublicUsers`, `visibleToAuthenticatedUsers`) or ACL read rights via users/groups is necessary for this. The way how access flags for HTTP verbs are configured remains unchanged.

When upgrading to a version of Structr >= 4.0 (with an existing application), there are two (similar) approaches we can take to configuring the resource access grants: Manual migration and semi-automatic migration using the deployment.

#### Manual Migration

If we simply install Structr 4.0 or higher over an existing installation, automatic migration is not possible. Fortunately, manual migration is quite simple by the following configuration steps:

- Log in as admin in the administration backend.
- Navigate to the “Security” area.
- Activate the “Resource Permissions” tab.
- Activate the checkbox “Show only used grants”.
- Migrate the permissions according to the following rules:
    - If the permission has active flags set for “Public Users”, `visibleToPublicUsers` will be set to `true`.
    - If the permission has active flags set for “Authenticated Users”, `visibleToAuthenticatedUsers` will be set to `true`.
  - If the permission now has both visibility flags activated, split it into two permissions with identical signature.
  
If there are a lot of permissions to configure, it's best practice to active the switch “Show visibility flags in Resource Permissions table” on the “UI Settings” tab on the dashboard.

### Semi-automatic Migration via Deployment

Importing a deployment export set which has been created with a pre 4.0 version of Structr into a 4+ version, Structr will run an automatic migration using a simple heuristic. This works because the file `security/grants.json` which was created by the older version of Structr does not contain the visibility and grantees properties. Some manual changes might be necessary afterwards though.

The heuristic works basically like step 5 from the manual configuration described above:

- If the grant has active flags set for “Public Users” then the visibleToPublicUsers flag will be set to true
- If the grant has active flags set for “Authenticated Users” then the visibleToAuthenticatedUsers flag will be set to true.
- If both flags are set, an additional warning is issued that the grant should be split into two grants with identical signature. This is due to the fact that an object with visibleToPublicUsers = true is also visible to authenticated users.

## Scripting Considerations

### Comparing Dates

When comparing dates in scripting environments, it is advised to use the `getTime()` function to compare their timestamp long values. This prevents some issues with comparing GraalVM ProxyDate entities.

    {
        return $.me.createdDate.getTime() <= $.now.getTime();
    };

### Conditional Chaining

While conditional chaining is generally supported in our JavaScript environments, there are some niche cases that are not fully supported yet by the GraalVM. Specifically trying to apply conditional chaining on a ProxyObject that might have a function member and then trying to call said function leads to errors.

The following example illustrates this issue:

    {
        const obj = {
            method1: () => "works"
        };

	    // Works
	    obj.method1?.();

	    // Works, call doesn't get executed
	    obj.method2?.();

	    const proxyObject = $.retrieve('passedObject');

    	// Does not work and throws unsupported message exception
	    proxyObject.myMethod?.();
    }

### REST Request Keywords

In 4.0, a number of REST request parameter names have been changed to prevent name collisions with property names, such as `order`, `page` etc.. Starting with version 4.0, the following request parameters must be prefixed with an underscore (unless the legacy mode setting is enabled in structr.conf):

    page
    pageSize
    sort
    order
    loose
    locale
    latlon
    location
    state
    house
    country
    postalCode
    city
    street
    distance
    outputNestingDepth
    debugLoggingEnabled
    forceResultCount
    disableSoftLimit
    parallelizeJsonOutput
    batchSize

There is a legacy mode available that can be activated by setting `application.legacy.requestparameters.enabled = true` in structr.conf. This allows using the old syntax without the prefix but is discouraged for new projects.

## Neo4j Upgrade

The recommended Neo4j version for Structr 4.x is Neo4j, although Neo4j 3.5 is still supported, so an upgrade is not strictly necessary. If an upgrade to a more recent Neo4j version is planned, the Neo4j changelog and migration guide should be consulted before upgrading.

Some deprecated features have been removed in Neo4j 4.x which are described in https://neo4j.com/docs/cypher-manual/current/deprecations-additions-removals-compatibility/#cypher-compatibility.

### Cypher Parameters

With Neo4j 4,   the cypher parameter syntax has changed:

The old parameter syntax {param} was deprecated in Neo4j 3.0 and removed entirely in Neo4j 4.0.
Using it will result in a syntax error. However, it is still possible to use it, with warnings,
if you prefix the query with CYPHER 3.5 (see Cypher Compatibility for further information).

If Neo4j is upgraded is from versions prior than 4, the default database may be named `graph.db` instead of the newer `neo4j` in version 4 and up. Since Structr version 4.1, Structr supports selecting a database (if supported by the Neo4j installation it is connecting to).

The default database Structr is trying to connect to is `neo4j`. If the migration happened from an older version, the correct database name needs to be configured manually via structr.conf file using the `xxxx.database.connection.databasename` setting as shown in the following example in line 4.

    YOUR_CONFIGURED_DB_NAME.database.connection.url = bolt://localhost:7687
    YOUR_CONFIGURED_DB_NAME.database.connection.name = YOUR_CONFIGURED_DB_NAME
    YOUR_CONFIGURED_DB_NAME.database.connection.password = your_neo4j_password
    YOUR_CONFIGURED_DB_NAME.database.connection.databasename = graph.db
    YOUR_CONFIGURED_DB_NAME.database.driver = org.structr.bolt.BoltDatabaseService