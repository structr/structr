/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.feed.entity;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.fulltext.Indexable;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.HttpHelper;

import java.io.InputStream;

/**
 *
 *
 */
public class RemoteDocument extends AbstractNode implements Indexable {

	public static final Property<String> urlProperty              = new StringProperty("url");
	public static final Property<Long> checksumProperty           = new LongProperty("checksum").readOnly();
	public static final Property<Integer> cacheForSecondsProperty = new IntProperty("cacheForSeconds");
	public static final Property<Integer> versionProperty         = new IntProperty("version").readOnly();

	public static final View defaultView = new View(RemoteDocument.class, PropertyView.Public,
		name, owner, urlProperty
	);

	public static final View uiView = new View(RemoteDocument.class, PropertyView.Ui,
		urlProperty, checksumProperty, cacheForSecondsProperty, versionProperty
	);

	public String getUrl() {
		return getProperty(urlProperty);
	}

	@Override
	public InputStream getInputStream() {

		final String remoteUrl = getUrl();
		if (StringUtils.isNotBlank(remoteUrl)) {

			return HttpHelper.getAsStream(remoteUrl);
		}

		return null;
	}

	@Override
	public boolean indexingEnabled() {
		return Settings.RemoteDocumentIndexingEnabled.getValue();
	}

	@Override
	public Integer maximumIndexedWords() {
		return Settings.RemoteDocumentIndexingLimit.getValue();
	}

	@Override
	public Integer indexedWordMinLength() {
		return Settings.RemoteDocumentIndexingMinLength.getValue();
	}

	@Override
	public Integer indexedWordMaxLength() {
		return Settings.RemoteDocumentIndexingMaxLength.getValue();
	}

	@Override
	public String getExtractedContent() {
		return getProperty(extractedContentProperty);
	}

	@Override
	public String getContentType() {
		return getProperty(contentTypeProperty);
	}
}
