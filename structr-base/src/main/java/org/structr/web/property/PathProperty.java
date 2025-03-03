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
package org.structr.web.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.PathHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SourceSearchAttribute;
import org.structr.core.property.AbstractReadOnlyProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Linkable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A property which returns the complete folder path of a {@link Linkable}
 * including name. The path consists of the names of the parent elements,
 * concatenated by "/" as path separator.
 *
 *
 */
public class PathProperty extends AbstractReadOnlyProperty<String> {

	private static final Logger logger = LoggerFactory.getLogger(PathProperty.class.getName());

	public PathProperty(String name) {
		super(name);
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public Class valueType() {
		return String.class;
	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		if (obj.is(StructrTraits.ABSTRACT_FILE)) {

			final AbstractFile file = obj.as(AbstractFile.class);

			return file.getFolderPath();
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public SearchAttribute getSearchAttribute(final SecurityContext securityContext, final Occurrence occur, final String searchValue, final boolean exactMatch, final Query query) {

		final App app                    = StructrApp.getInstance(securityContext);
		final SourceSearchAttribute attr = new SourceSearchAttribute(occur);

		try {

			final ArrayList<String> parts = new ArrayList<>(Arrays.asList(PathHelper.getParts(searchValue)));
			if (!parts.isEmpty()) {
				searchRecursively(app, null, attr, parts);
			}

		} catch (FrameworkException ex) {

			logger.error("", ex);
		}

		return attr;
	}

	private void searchRecursively(final App app, final NodeInterface parent, final SourceSearchAttribute attr, final ArrayList<String> parts) throws FrameworkException {

		final String currentPart      = parts.remove(0);
		final Traits traits           = Traits.of(StructrTraits.FILE);

		final List<NodeInterface> res = app.nodeQuery(StructrTraits.ABSTRACT_FILE).and(Traits.of(StructrTraits.ABSTRACT_FILE).key("parent"), parent).and(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), currentPart).getAsList();

		if (parts.isEmpty()) {

			for (final NodeInterface fileOrFolder : res) {

				attr.addToResult(fileOrFolder);
			}

		} else {

			for (final NodeInterface folder : res) {
				searchRecursively(app, folder, attr, (ArrayList<String>) parts.clone());
			}
		}
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
