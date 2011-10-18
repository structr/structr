/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity.web;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.entity.PlainText;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * A MediaWikiWrapper is an interface to the MediaWiki API:
 *
 * See http://www.mediawiki.org/wiki/API and http://en.wikipedia.org/w/api.php
 * for details.
 *
 * @author axel
 */
public class MediaWikiWrapper extends PlainText {

	private static final Logger logger = Logger.getLogger(MediaWikiWrapper.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(MediaWikiWrapper.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	// private static final String CACHED_MEDIAWIKI_CONTENT = "cached_mediawiki_content";
	public enum Key implements PropertyKey{ source }

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/lightbulb.png";
	}

	// ----- private methods ----
	@Override
	public String getContent() {

		String source = getStringProperty(Key.source);

		try {

			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget       = new HttpGet(source);
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity     = response.getEntity();

			if (entity != null) {

				long len = entity.getContentLength();

				if ((len != -1)) {
					return EntityUtils.toString(entity);
				} else {

					// Stream content out
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return null;
	}
}
