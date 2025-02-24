/*
 * Copyright (C) 2010-2024 Structr GmbH
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

	public enum Direction {
		in, out
	}

	public enum Cascade {
		sourceToTarget, targetToSource, always, constraintBased
	}

	public enum ImportMode {
		replace, extend
	}

	public static final String KEY_REFERENCE                = "$ref";
	public static final String KEY_LINK                     = "$link";
	public static final String KEY_LINK_SOURCE              = "$source";
	public static final String KEY_LINK_TARGET              = "$target";

	public static final String KEY_ID                       = "id";
	public static final String KEY_TYPE                     = "type";
	public static final String KEY_NAME                     = "name";
	public static final String KEY_TRAITS                   = "traits";
	public static final String KEY_TITLE                    = "title";
	public static final String KEY_DESCRIPTION              = "description";
	public static final String KEY_ICON                     = "icon";
	public static final String KEY_ENUM                     = "enum";
	public static final String KEY_FQCN                     = "fqcn";
	public static final String KEY_FORMAT                   = "format";
	public static final String KEY_HINT                     = "hint";
	public static final String KEY_CATEGORY                 = "category";
	public static final String KEY_READ_ONLY                = "readOnly";
	public static final String KEY_VALIDATORS               = "validators";
	public static final String KEY_TRANSFORMATORS           = "transformators";
	public static final String KEY_DATE_PATTERN             = "datePattern";
	public static final String KEY_ITEMS                    = "items";
	public static final String KEY_IS_ABSTRACT              = "isAbstract";
	public static final String KEY_IS_INTERFACE             = "isInterface";
	public static final String KEY_IS_SERVICE_CLASS         = "isServiceClass";
	public static final String KEY_CHANGELOG_DISABLED       = "changelogDisabled";
	public static final String KEY_VISIBLE_TO_PUBLIC        = "visibleToPublicUsers";
	public static final String KEY_VISIBLE_TO_AUTHENTICATED = "visibleToAuthenticatedUsers";
	public static final String KEY_DEFINITIONS              = "definitions";
	public static final String KEY_PROPERTIES               = "properties";
	public static final String KEY_VIEWS                    = "views";
	public static final String KEY_VIEW_ORDER               = "viewOrder";
	public static final String KEY_METHODS                  = "methods";
	public static final String KEY_GRANTS                   = "grants";
	public static final String KEY_REQUIRED                 = "required";
	public static final String KEY_MIN_ITEMS                = "minItems";
	public static final String KEY_MAX_ITEMS                = "maxItems";
	public static final String KEY_SOURCE                   = "source";
	public static final String KEY_CODE_TYPE                = "codeType";
	public static final String KEY_OVERRIDES_EXISTING       = "overridesExisting";
	public static final String KEY_RETURN_TYPE              = "returnType";
	public static final String KEY_OPENAPI_RETURN_TYPE      = "openAPIReturnType";
	public static final String KEY_EXCEPTIONS               = "exceptions";
	public static final String KEY_CALL_SUPER               = "callSuper";
	public static final String KEY_IS_STATIC                = "isStatic";
	public static final String KEY_IS_PRIVATE               = "isPrivate";
	public static final String KEY_RETURN_RAW_RESULT        = "returnRawResult";
	public static final String KEY_HTTP_VERB                = "httpVerb";
	public static final String KEY_DO_EXPORT                = "doExport";
	public static final String KEY_PARAMETERS               = "parameters";
	public static final String KEY_PARAMETER_TYPE           = "parameterType";
	public static final String KEY_PARAMETER_INDEX          = "parameterIndex";
	public static final String KEY_SUMMARY                  = "summary";
	public static final String KEY_CONTENT_TYPE             = "contentType";
	public static final String KEY_RELATIONSHIP             = "rel";
	public static final String KEY_DIRECTION                = "direction";
	public static final String KEY_COMPOUND                 = "compound";
	public static final String KEY_UNIQUE                   = "unique";
	public static final String KEY_INDEXED                  = "indexed";
	public static final String KEY_DEFAULT                  = "default";
	public static final String KEY_CASCADE                  = "cascade";
	public static final String KEY_CREATE                   = "create";
	public static final String KEY_DELETE                   = "delete";
	public static final String KEY_CARDINALITY              = "cardinality";
	public static final String KEY_SOURCE_NAME              = "sourceName";
	public static final String KEY_TARGET_NAME              = "targetName";
	public static final String KEY_READ_FUNCTION            = "readFunction";
	public static final String KEY_WRITE_FUNCTION           = "writeFunction";
	public static final String KEY_TYPE_HINT                = "typeHint";
	public static final String KEY_ACL_RESOLUTION           = "aclResolution";
	public static final String KEY_ACL_READ_MASK            = "aclReadMask";
	public static final String KEY_ACL_WRITE_MASK           = "aclWriteMask";
	public static final String KEY_ACL_DELETE_MASK          = "aclDeleteMask";
	public static final String KEY_ACL_ACCESS_CONTROL_MASK  = "aclAccessControlMask";
	public static final String KEY_ACL_HIDDEN_PROPERTIES    = "aclHiddenProperties";
	public static final String KEY_IS_CACHING_ENABLED       = "cachingEnabled";
	public static final String KEY_EXAMPLE_VALUE            = "exampleValue";
	public static final String KEY_TAGS                     = "tags";
	public static final String KEY_INCLUDE_IN_OPENAPI       = "includeInOpenAPI";

	public static final String KEY_GRANT_READ               = "read";
	public static final String KEY_GRANT_WRITE              = "write";
	public static final String KEY_GRANT_DELETE             = "delete";
	public static final String KEY_GRANT_ACCESS_CONTROL     = "accessControl";


	public static final String KEY_MINIMUM                  = "minimum";
	public static final String KEY_EXCLUSIVE_MINIMUM        = "exclusiveMinimum";
	public static final String KEY_MAXIMUM                  = "maximum";
	public static final String KEY_EXCLUSIVE_MAXIMUM        = "exclusiveMaximum";
	public static final String KEY_MULTIPLE_OF              = "multipleOf";

	public static final String FORMAT_DATE_TIME             = "date-time";

	public static final String EMPTY_SCHEMA                 = "{\"definitions\":{}, \"methods\":[]}";

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
