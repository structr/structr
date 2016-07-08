/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.property;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SourceSearchAttribute;
import org.structr.core.property.AbstractReadOnlyProperty;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Linkable;

/**
 * A property which returns the complete folder path of a {@link Linkable}
 * including name. The path consists of the names of the parent elements,
 * concatenated by "/" as path separator.
 *
 *
 */
public class PathProperty extends AbstractReadOnlyProperty<String> {
	
	private static final Logger logger = Logger.getLogger(PathProperty.class.getName());

	public PathProperty(String name) {
		super(name);
	}

	@Override
	public Class relatedType() {
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
		return FileHelper.getFolderPath((AbstractFile) obj);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Integer;
	}
	
	@Override
	public SearchAttribute getSearchAttribute(final SecurityContext securityContext, final Occurrence occur, final String searchValue, final boolean exactMatch, final Query query) {
		
		final String[]             parts = PathHelper.getParts(searchValue);
		final App                    app = StructrApp.getInstance(securityContext);
		final SourceSearchAttribute attr = new SourceSearchAttribute(occur);
		final Set<GraphObject>    result = new LinkedHashSet<>();
		
		Folder parentFolder = null;
		
		for (final String part : parts) {
			
			try {
				
				logger.log(Level.FINE, "PathProperty path part: {0} (parent folder path: {1})", new Object[] { part, parentFolder == null ? "/" : parentFolder.getPath() });

				final Query<AbstractFile> q = app.nodeQuery(AbstractFile.class).and(AbstractFile.name, part);
				
				if (parentFolder != null) {

					q.and(AbstractFile.parent, parentFolder);
				}
				
				for (final AbstractFile fileOrFolder : q.getAsList()) {
					
					final String currentPath = (parentFolder != null ? FileHelper.getFolderPath(parentFolder) : "") + PathHelper.PATH_SEP + part;

					if (fileOrFolder != null && FileHelper.getFolderPath(fileOrFolder).equals(currentPath)) {

						if (fileOrFolder instanceof Folder) {

							parentFolder = (Folder) fileOrFolder;

						} else {

							result.add(fileOrFolder);
						}
					}
				}
				
				
			} catch (FrameworkException ex) {
				
				logger.log(Level.SEVERE, null, ex);
			}
			
		}
		
		if (result.isEmpty() && parentFolder != null && FileHelper.getFolderPath(parentFolder).equals(searchValue)) {
			
			result.add(parentFolder);
		}

		attr.setResult(result);
		
		return attr;
	}
	
}
