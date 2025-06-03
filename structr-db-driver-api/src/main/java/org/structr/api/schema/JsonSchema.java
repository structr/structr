/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.api.schema;

import java.net.URI;

/**
 *
 *
 */
public interface JsonSchema {

	enum Direction {
		in, out
	}

	enum Cascade {
		sourceToTarget, targetToSource, always, constraintBased
	}

	enum ImportMode {
		replace, extend
	}

	String KEY_REFERENCE                = "$ref";
	String KEY_EXTENDS                  = "$extends";
	String KEY_IMPLEMENTS               = "$implements";
	String KEY_LINK                     = "$link";
	String KEY_LINK_SOURCE              = "$source";
	String KEY_LINK_TARGET              = "$target";

	String KEY_ID                       = "id";
	String KEY_TYPE                     = "type";
	String KEY_NAME                     = "name";
	String KEY_TRAITS                   = "traits";
	String KEY_TITLE                    = "title";
	String KEY_DESCRIPTION              = "description";
	String KEY_ICON                     = "icon";
	String KEY_ENUM                     = "enum";
	String KEY_FQCN                     = "fqcn";
	String KEY_FORMAT                   = "format";
	String KEY_HINT                     = "hint";
	String KEY_CATEGORY                 = "category";
	String KEY_READ_ONLY                = "readOnly";
	String KEY_SERIALIZATION_DISABLED   = "serializationDisabled";
	String KEY_VALIDATORS               = "validators";
	String KEY_TRANSFORMATORS           = "transformators";
	String KEY_DATE_PATTERN             = "datePattern";
	String KEY_ITEMS                    = "items";
	String KEY_IS_ABSTRACT              = "isAbstract";
	String KEY_IS_INTERFACE             = "isInterface";
	String KEY_IS_BUILTIN_TYPE          = "isBuiltinType";
	String KEY_IS_SERVICE_CLASS         = "isServiceClass";
	String KEY_CHANGELOG_DISABLED       = "changelogDisabled";
	String KEY_VISIBLE_TO_PUBLIC        = "visibleToPublicUsers";
	String KEY_VISIBLE_TO_AUTHENTICATED = "visibleToAuthenticatedUsers";
	String KEY_DEFINITIONS              = "definitions";
	String KEY_PROPERTIES               = "properties";
	String KEY_VIEWS                    = "views";
	String KEY_VIEW_ORDER               = "viewOrder";
	String KEY_METHODS                  = "methods";
	String KEY_GRANTS                   = "grants";
	String KEY_REQUIRED                 = "required";
	String KEY_MIN_ITEMS                = "minItems";
	String KEY_MAX_ITEMS                = "maxItems";
	String KEY_SOURCE                   = "source";
	String KEY_CODE_TYPE                = "codeType";
	String KEY_OVERRIDES_EXISTING       = "overridesExisting";
	String KEY_RETURN_TYPE              = "returnType";
	String KEY_OPENAPI_RETURN_TYPE      = "openAPIReturnType";
	String KEY_EXCEPTIONS               = "exceptions";
	String KEY_CALL_SUPER               = "callSuper";
	String KEY_IS_STATIC                = "isStatic";
	String KEY_IS_PRIVATE               = "isPrivate";
	String KEY_RETURN_RAW_RESULT        = "returnRawResult";
	String KEY_HTTP_VERB                = "httpVerb";
	String KEY_DO_EXPORT                = "doExport";
	String KEY_PARAMETERS               = "parameters";
	String KEY_PARAMETER_TYPE           = "parameterType";
	String KEY_PARAMETER_INDEX          = "parameterIndex";
	String KEY_SUMMARY                  = "summary";
	String KEY_CONTENT_TYPE             = "contentType";
	String KEY_RELATIONSHIP             = "rel";
	String KEY_DIRECTION                = "direction";
	String KEY_COMPOUND                 = "compound";
	String KEY_UNIQUE                   = "unique";
	String KEY_INDEXED                  = "indexed";
	String KEY_DEFAULT                  = "default";
	String KEY_CASCADE                  = "cascade";
	String KEY_CREATE                   = "create";
	String KEY_DELETE                   = "delete";
	String KEY_CARDINALITY              = "cardinality";
	String KEY_SOURCE_NAME              = "sourceName";
	String KEY_TARGET_NAME              = "targetName";
	String KEY_READ_FUNCTION            = "readFunction";
	String KEY_WRITE_FUNCTION           = "writeFunction";
	String KEY_TYPE_HINT                = "typeHint";
	String KEY_ACL_RESOLUTION           = "aclResolution";
	String KEY_ACL_READ_MASK            = "aclReadMask";
	String KEY_ACL_WRITE_MASK           = "aclWriteMask";
	String KEY_ACL_DELETE_MASK          = "aclDeleteMask";
	String KEY_ACL_ACCESS_CONTROL_MASK  = "aclAccessControlMask";
	String KEY_ACL_HIDDEN_PROPERTIES    = "aclHiddenProperties";
	String KEY_IS_CACHING_ENABLED       = "cachingEnabled";
	String KEY_EXAMPLE_VALUE            = "exampleValue";
	String KEY_TAGS                     = "tags";
	String KEY_INCLUDE_IN_OPENAPI       = "includeInOpenAPI";

	String KEY_GRANT_READ               = "read";
	String KEY_GRANT_WRITE              = "write";
	String KEY_GRANT_DELETE             = "delete";
	String KEY_GRANT_ACCESS_CONTROL     = "accessControl";


	String KEY_MINIMUM                  = "minimum";
	String KEY_EXCLUSIVE_MINIMUM        = "exclusiveMinimum";
	String KEY_MAXIMUM                  = "maximum";
	String KEY_EXCLUSIVE_MAXIMUM        = "exclusiveMaximum";
	String KEY_MULTIPLE_OF              = "multipleOf";

	String FORMAT_DATE_TIME             = "date-time";

	String EMPTY_SCHEMA                 = "{\"definitions\":{}, \"methods\":[]}";

	URI getId();

	String getTitle();
	void setTitle(final String title);

	String getDescription();
	void setDescription(final String description);

	JsonObjectType addType(final String name);
	JsonType getType(final String name);
	JsonType getType(final String name, final boolean create);
	void removeType(final String name);
	Iterable<JsonType> getTypes();

	void createDatabaseSchema(final ImportMode importMode) throws Exception;

	Object resolveURI(final URI uri);
	String toJsonPointer(final URI uri);

	void diff(final JsonSchema other) throws Exception;

}
