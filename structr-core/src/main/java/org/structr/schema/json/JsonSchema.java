/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.schema.json;

import java.net.URI;
import java.net.URISyntaxException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;

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

	public static final String KEY_SCHEMA                  = "$schema";
	public static final String KEY_SIZE_OF                 = "$size";
	public static final String KEY_REFERENCE               = "$ref";
	public static final String KEY_EXTENDS                 = "$extends";
	public static final String KEY_LINK                    = "$link";
	public static final String KEY_LINK_SOURCE             = "$source";
	public static final String KEY_LINK_TARGET             = "$target";

	public static final String KEY_ID                      = "id";
	public static final String KEY_TYPE                    = "type";
	public static final String KEY_TITLE                   = "title";
	public static final String KEY_DESCRIPTION             = "description";
	public static final String KEY_ENUM                    = "enum";
	public static final String KEY_FORMAT                  = "format";
	public static final String KEY_DATE_PATTERN            = "datePattern";
	public static final String KEY_ITEMS                   = "items";
	public static final String KEY_DEFINITIONS             = "definitions";
	public static final String KEY_PROPERTIES              = "properties";
	public static final String KEY_VIEWS                   = "views";
	public static final String KEY_METHODS                 = "methods";
	public static final String KEY_REQUIRED                = "required";
	public static final String KEY_MIN_ITEMS               = "minItems";
	public static final String KEY_MAX_ITEMS               = "maxItems";
	public static final String KEY_SOURCE                  = "source";
	public static final String KEY_CONTENT_TYPE            = "contentType";
	public static final String KEY_RELATIONSHIP            = "rel";
	public static final String KEY_DIRECTION               = "direction";
	public static final String KEY_UNIQUE                  = "unique";
	public static final String KEY_INDEXED                 = "indexed";
	public static final String KEY_DEFAULT                 = "default";
	public static final String KEY_CASCADE                 = "cascade";
	public static final String KEY_CREATE                  = "create";
	public static final String KEY_DELETE                  = "delete";
	public static final String KEY_CARDINALITY             = "cardinality";
	public static final String KEY_SOURCE_NAME             = "sourceName";
	public static final String KEY_TARGET_NAME             = "targetName";
	public static final String KEY_READ_FUNCTION           = "readFunction";
	public static final String KEY_WRITE_FUNCTION          = "writeFunction";
	public static final String KEY_ACL_RESOLUTION          = "aclResolution";
	public static final String KEY_ACL_READ_MASK           = "aclReadMask";
	public static final String KEY_ACL_WRITE_MASK          = "aclWriteMask";
	public static final String KEY_ACL_DELETE_MASK         = "aclDeleteMask";
	public static final String KEY_ACL_ACCESS_CONTROL_MASK = "aclAccessControlMask";
	public static final String KEY_ACL_HIDDEN_PROPERTIES   = "aclHiddenProperties";


	public static final String KEY_MINIMUM                 = "minimum";
	public static final String KEY_EXCLUSIVE_MINIMUM       = "exclusiveMinimum";
	public static final String KEY_MAXIMUM                 = "maximum";
	public static final String KEY_EXCLUSIVE_MAXIMUM       = "exclusiveMaximum";
	public static final String KEY_MULTIPLE_OF             = "multipleOf";

	public static final String FORMAT_DATE_TIME            = "date-time";

	public URI getId();

	public String getTitle();
	public void setTitle(final String title);

	public String getDescription();
	public void setDescription(final String description);

	public JsonObjectType addType(final String name) throws URISyntaxException;
	public JsonType getType(final String name);

	public void createDatabaseSchema(final App app) throws FrameworkException;

	public Object resolveURI(final URI uri);
	public String toJsonPointer(final URI uri);
}
