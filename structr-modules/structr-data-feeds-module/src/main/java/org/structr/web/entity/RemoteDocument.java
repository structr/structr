/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.io.InputStream;
import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 *
 *
 */
public interface RemoteDocument extends NodeInterface, Indexable {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("RemoteDocument");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/RemoteDocument"));
		type.setImplements(URI.create("#/definitions/Indexable"));

		type.addStringProperty("url",              PropertyView.Public);
		type.addLongProperty("checksum",           PropertyView.Public).setReadOnly(true);
		type.addIntegerProperty("cacheForSeconds", PropertyView.Public);
		type.addIntegerProperty("version",         PropertyView.Public).setIndexed(true).setReadOnly(true);

		type.addPropertyGetter("url", String.class);

		type.overrideMethod("afterCreation",    true,  "update();");
		type.overrideMethod("getSearchContext", false, "return " + RemoteDocument.class.getName() + ".getSearchContext(this, arg0, arg1);");
		type.overrideMethod("getInputStream",   false, "return " + RemoteDocument.class.getName() + ".getInputStream(this);");

		type.addMethod("update").setSource(RemoteDocument.class.getName() + ".update(this);").setDoExport(true);

	}}

	void update();
	String getUrl();

	/*

	private static final Logger logger = LoggerFactory.getLogger(RemoteDocument.class.getName());

	public static final Property<String> url                     = new StringProperty("url");
	public static final Property<Long> checksum                  = new LongProperty("checksum").indexed().unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                = new IntProperty("version").indexed().readOnly();

	public static final View publicView = new View(RemoteDocument.class, PropertyView.Public, type, name, contentType, url, owner);
	public static final View uiView = new View(RemoteDocument.class, PropertyView.Ui, type, contentType, url, checksum, version, cacheForSeconds, owner, extractedContent, indexedWords);

	@Override
	public void afterCreation(SecurityContext securityContext) {

		update();
	}
	*/

	static void update(final RemoteDocument thisDocument) {

		final SecurityContext securityContext = thisDocument.getSecurityContext();

		try {

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			indexer.addToFulltextIndex(thisDocument);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + thisDocument, fex);
		}
	}

	static GraphObject getSearchContext(final RemoteDocument thisDocument, final String searchTerm, final int contextLength) {

		final SecurityContext securityContext = thisDocument.getSecurityContext();
		final String text                     = thisDocument.getExtractedContent();

		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	/*
	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(RemoteDocument.version);

		unlockSystemPropertiesOnce();
		if (_version == null) {

			setProperty(RemoteDocument.version, 1);

		} else {

			setProperty(RemoteDocument.version, _version + 1);
		}
	}
	*/

	static InputStream getInputStream(final RemoteDocument thisDocument) {

		final String remoteUrl = thisDocument.getUrl();
		if (StringUtils.isNotBlank(remoteUrl)) {

			return HttpHelper.getAsStream(remoteUrl);
		}

		return null;
	}
}
