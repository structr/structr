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
package org.structr.schema.parser;

import org.structr.schema.CodeSource;
import org.structr.schema.SchemaHelper.Type;

public interface PropertyDefinition extends CodeSource {

	String getClassName();
	String getPropertyName();
	Type getPropertyType();
	String getSource();
	String getDbName();
	String getFormat();
	String getTypeHint();
	String getHint();
	String getCategory();
	String getFqcn();
	boolean isNotNull();
	boolean isCompound();
	boolean isUnique();
	boolean isIndexed();
	boolean isReadOnly();
	boolean isPartOfBuiltInSchema();
	boolean isCachingEnabled();
	String getDefaultValue();
	String getContentType();
	String getReadFunction();
	String getWriteFunction();
	String getOpenAPIReturnType();
	String[] getTransformators();
	String[] getValidators();
}
