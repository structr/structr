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
package org.structr.web.entity;

import java.io.InputStream;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.files.text.FulltextIndexingTask;
import org.structr.web.common.DownloadHelper;

/**
 *
 *
 */
public class RemoteDocument extends AbstractNode implements Indexable {

	private static final Logger logger = Logger.getLogger(RemoteDocument.class.getName());

	public static final Property<String> url                     = new StringProperty("url");
	public static final Property<Long> checksum                  = new LongProperty("checksum").indexed().unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                = new IntProperty("version").indexed().readOnly();

	public static final View publicView = new View(RemoteDocument.class, PropertyView.Public, type, name, contentType, url, owner);
	public static final View uiView = new View(RemoteDocument.class, PropertyView.Ui, type, contentType, url, checksum, version, cacheForSeconds, owner, extractedContent, indexedWords);

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			StructrApp.getInstance(securityContext).processTasks(new FulltextIndexingTask(this));
			
		} catch (Throwable t) {

		}

	}

	@Export
	@Override
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			return DownloadHelper.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(RemoteDocument.version);

		unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			setProperty(RemoteDocument.version, 1);

		} else {

			setProperty(RemoteDocument.version, _version + 1);
		}
	}

	@Override
	public InputStream getInputStream() {

		final String remoteUrl = getProperty(url);
		if (StringUtils.isNotBlank(remoteUrl)) {
			
			return DownloadHelper.getInputStream(remoteUrl);
		}
		
		return null;
	}

	// ----- private methods -----


}
