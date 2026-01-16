/*
 * Copyright (C) 2010-2026 Structr GmbH
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

/**
 *
 *
 */
public interface JsonFunctionProperty extends JsonDynamicProperty {

	public JsonFunctionProperty setReadFunction(final String readFunction);
	public String getReadFunction();

	public JsonFunctionProperty setWriteFunction(final String writeFunction);
	public String getWriteFunction();

	public JsonFunctionProperty setWriteFunctionWrapJS(final boolean wrap);
	public Boolean getWriteFunctionWrapJS();

	public JsonFunctionProperty setReadFunctionWrapJS(final boolean wrap);
	public Boolean getReadFunctionWrapJS();

	public JsonFunctionProperty setIsCachingEnabled(final boolean enabled);
	public Boolean getIsCachingEnabled();

	public JsonFunctionProperty setOpenAPIReturnType(final String openAPIReturnType);
	public String getOpenAPIReturnType();

	@Override
	public JsonFunctionProperty setContentType(final String contentType);

	@Override
	public String getContentType();

}
