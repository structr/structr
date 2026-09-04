/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.pdf;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.common.HttpHelper;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Supplies a document's images, stylesheets and fonts from the Structr filesystem, under the rendering user's permissions. */
public class StructrResourceFactory implements FSStreamFactory {

	private static final Logger logger = LoggerFactory.getLogger(StructrResourceFactory.class.getName());

	private final Set<String> unresolved = new LinkedHashSet<>();
	private final SecurityContext securityContext;

	public StructrResourceFactory(final SecurityContext securityContext) {

		this.securityContext = securityContext;
	}

	/** References nothing answered; reported rather than thrown, since one missing decoration should not cost the document. */
	public Set<String> getUnresolvedReferences() {

		return unresolved;
	}

	@Override
	public FSStream getUrl(final String url) {

		final File file = fileFor(url);

		if (file != null) {

			return streamOf(() -> StorageProviderFactory.getStorageProvider(file).getInputStream());
		}

		if (isExternal(url)) {

			if (PDFModule.AllowExternalResources.getValue()) {

				return externalStream(url);
			}

			logger.warn("Not loading external resource {} while rendering a PDF: set {} to allow it.", url, PDFModule.AllowExternalResources.getKey());
		}

		unresolved.add(url);

		return streamOf(() -> new ByteArrayInputStream(new byte[0]));
	}

	private File fileFor(final String url) {

		final String path = pathOf(url);

		if (path == null) {

			return null;
		}

		final NodeInterface node = FileHelper.getFileByAbsolutePath(securityContext, path);

		if (node != null && node.is(StructrTraits.FILE)) {

			return node.as(File.class);
		}

		return null;
	}

	private FSStream externalStream(final String url) {

		try {

			final Map<String, Object> response = HttpHelper.getAsStream(url);

			if (response.get(HttpHelper.FIELD_BODY) instanceof InputStream body) {

				return streamOf(() -> body);
			}

		} catch (final Throwable t) {

			logger.warn("Could not load external resource {} while rendering a PDF: {}", url, t.getMessage());
		}

		unresolved.add(url);

		return streamOf(() -> new ByteArrayInputStream(new byte[0]));
	}

	// deliberately NOT the java.nio "structr" provider: this resolves under an explicit SecurityContext, the document's permission boundary
	private static String pathOf(final String url) {

		try {

			final String path = URI.create(url).getPath();

			return (path == null || path.isEmpty()) ? null : path;

		} catch (final IllegalArgumentException iae) {

			return null;
		}
	}

	private static boolean isExternal(final String url) {

		return url != null && (url.startsWith("http://") || url.startsWith("https://"));
	}

	private interface StreamSource { InputStream open() throws Exception; }

	private static FSStream streamOf(final StreamSource source) {

		return new FSStream() {

			@Override
			public InputStream getStream() {

				try {

					return source.open();

				} catch (final Throwable t) {

					logger.warn("Could not open a resource while rendering a PDF: {}", t.getMessage());

					return new ByteArrayInputStream(new byte[0]);
				}
			}

			@Override
			public Reader getReader() {

				return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
			}
		};
	}
}
