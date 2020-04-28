/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.web.entity;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.structr.api.graph.Cardinality;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;

/**
 * Base class for minifiable files in Structr.
 */
public interface AbstractMinifiedFile extends File {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("AbstractMinifiedFile");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/AbstractMinifiedFile"));
		type.setExtends(URI.create("#/definitions/File"));
		type.setIsAbstract();

		type.overrideMethod("getMaxPosition", false, "return " + AbstractMinifiedFile.class.getName() + ".getMaxPosition(this);");
		type.overrideMethod("onModification", true,  AbstractMinifiedFile.class.getName() + ".onModification(this, arg0, arg1, arg2);");

		// relationships
		final JsonObjectType file   = (JsonObjectType)schema.getType("File");
		final JsonReferenceType rel = type.relate(file, "MINIFICATION", Cardinality.ManyToMany, "minificationTargets", "minificationSources");

		// add method
		rel.overrideMethod("onRelationshipCreation", true, "try { setProperty(positionProperty, getSourceNode().getMaxPosition()); } catch (FrameworkException fex) { /* ignore? */ }");

		rel.addIntegerProperty("position", PropertyView.Public);
		rel.addPropertyGetter("position", Integer.TYPE);

		// view configuration
		type.addViewProperty(PropertyView.Public, "minificationSources");
		type.addViewProperty(PropertyView.Ui,     "minificationSources");

		type.addMethod("moveMinificationSource")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("from", "int")
			.addParameter("to", "int")
			.setSource(AbstractMinifiedFile.class.getName() + ".moveMinificationSource(this, from, to, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);
	}}

	int getMaxPosition();
	void minify(final SecurityContext securityContext) throws FrameworkException, IOException;
	boolean shouldModificationTriggerMinifcation(final ModificationEvent modState);

	public static int getMaxPosition (final AbstractMinifiedFile thisFile) {

		final Class<Relation> relType  = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
		final PropertyKey<Integer> key = StructrApp.key(relType, "position");
		int max                        = 0;

		for (final Relation neighbor : AbstractMinifiedFile.getMinificationRelationships(thisFile, false)) {
			if (neighbor.getProperty(key) != null) {
				max = Math.max(max, neighbor.getProperty(key));
			}
		}

		return max;
	}

	static void onModification(final AbstractMinifiedFile thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		boolean shouldMinify = false;
		final String myUUID = thisFile.getUuid();

		for (ModificationEvent modState : modificationQueue.getModificationEvents()) {

			// only take changes on this exact file into account
			if (myUUID.equals(modState.getUuid())) {

				shouldMinify = shouldMinify || thisFile.shouldModificationTriggerMinifcation(modState);
			}
		}

		if (shouldMinify) {

			try {

				thisFile.minify(securityContext);

			} catch (IOException ex) {
				logger.warn("Could not automatically minify file", ex);
			}
		}
	}

	public static String getConcatenatedSource(final AbstractMinifiedFile thisFile) throws FrameworkException, IOException {

		final Class<Relation> relType          = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
		final PropertyKey<Integer> key         = StructrApp.key(relType, "position");
		final StringBuilder concatenatedSource = new StringBuilder();
		int cnt = 0;

		for (Relation rel : AbstractMinifiedFile.getSortedMinificationRelationships(thisFile)) {

			final File src = (File)rel.getTargetNode();

			concatenatedSource.append(FileUtils.readFileToString(src.getFileOnDisk(), Charset.forName("utf-8")));

			// compact the relationships (if necessary)
			if (rel.getProperty(key) != cnt) {

				rel.setProperty(key, cnt);
			}

			cnt++;
		}

		return concatenatedSource.toString();
	}

	public static List<Relation> getMinificationRelationships(final AbstractMinifiedFile thisFile, final boolean sort) {

		final Class<Relation> relType  = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
		final PropertyKey<Integer> key = StructrApp.key(relType, "position");
		final List<Relation> rels      = new ArrayList<>();

		rels.addAll(Iterables.toList(thisFile.getOutgoingRelationships(relType)));

		if (Boolean.TRUE.equals(sort)) {

			Collections.sort(rels, (arg0, arg1) -> {
				return arg0.getProperty(key).compareTo(arg1.getProperty(key));
			});
		}

		return rels;
	}

	public static List<Relation> getSortedMinificationRelationships(final AbstractMinifiedFile thisFile) {

		return AbstractMinifiedFile.getMinificationRelationships(thisFile, true);
	}

	/**
	 * Move a minification source to a new position.
	 * All minification sources between those positions have to be adjusted as well.
	 *
	 * @param from The position from where the minification source is moved
	 * @param to The position where to move the minification source
	 * @throws FrameworkException
	 */
	public static void moveMinificationSource(final AbstractMinifiedFile thisFile, final int from, final int to, final SecurityContext securityContext) throws FrameworkException {

		final Class<Relation> relType  = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
		final PropertyKey<Integer> key = StructrApp.key(relType, "position");

		for (Relation rel : AbstractMinifiedFile.getMinificationRelationships(thisFile, false)) {

			int currentPosition = rel.getProperty(key);

			int change = 0;
			if (from < to) {
				change = -1;
			} else if (from > to) {
				change = 1;
			}

			if (currentPosition > from && currentPosition <= to) {

				rel.setProperties(securityContext, new PropertyMap(key, currentPosition + change));

			} else if (currentPosition >= to && currentPosition < from) {

				rel.setProperties(securityContext, new PropertyMap(key, currentPosition + change));

			} else if (currentPosition == from) {

				rel.setProperties(securityContext, new PropertyMap(key, to));
			}
		}
	}
}
