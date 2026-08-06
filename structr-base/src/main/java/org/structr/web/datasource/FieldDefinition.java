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
package org.structr.web.datasource;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.web.common.RenderContext;

import java.util.List;

public interface FieldDefinition {

	String fieldName();
	String renderTemplate();
	String editTemplate();
	String dataType();
	String nodeType();

	boolean hasOptions();
	boolean isRequired();
	boolean isCollection();
	boolean isIndexed();

	List<GraphObject> getOptions(final RenderContext renderContext, final String filter, final String label) throws FrameworkException;

	static FieldDefinition fromMapField() {

		return new FieldDefinition() {
			@Override
			public String fieldName() {

				return null;
			}

			@Override
			public String renderTemplate() {

				return null;
			}

			@Override
			public String editTemplate() {

				return null;
			}

			@Override
			public String dataType() {

				return null;
			}

			@Override
			public String nodeType() {

				return null;
			}

			@Override
			public boolean hasOptions() {

				return false;
			}

			@Override
			public boolean isRequired() {

				return false;
			}

			@Override
			public boolean isCollection() {

				return false;
			}

			@Override
			public boolean isIndexed() {

				return false;
			}

			@Override
			public List<GraphObject> getOptions(final RenderContext renderContext, final String filter, final String label) throws FrameworkException {

				return null;
			}
		};
	}
}
