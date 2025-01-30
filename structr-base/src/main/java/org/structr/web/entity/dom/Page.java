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
package org.structr.web.entity.dom;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.web.entity.Site;
import org.structr.web.entity.path.PagePath;

import java.util.List;

public interface Page extends DOMNode {

	void setVersion(int version) throws FrameworkException;
	void increaseVersion() throws FrameworkException;
	int getVersion();

	Integer getCacheForSeconds();
	Integer getPosition();

	String getPath();
	String getCategory();
	String getShowOnErrorCodes();

	boolean pageCreatesRawData();

	Iterable<DOMNode> getElements();
	Iterable<PagePath> getPaths();
	Iterable<Site> getSites();

	DOMElement getElementById(final String id) throws FrameworkException;
	DOMElement createElement(final String tag) throws FrameworkException;
	DOMElement createElement(final String tag, final boolean suppressException) throws FrameworkException;
	DocumentFragment createDocumentFragment();
	Content createTextNode(final String text);
	Comment createComment(final String comment);

	void adoptNode(final DOMNode newHtmlNode);
	DOMNode importNode(final DOMNode node, final boolean deep) throws FrameworkException;

	List<DOMNode> getElementsByTagName(final String head) throws FrameworkException;

	/**
	 * Creates a new Page entity with the given name in the database.
	 *
	 * @param securityContext the security context to use
	 * @param name the name of the new ownerDocument, defaults to
	 * "ownerDocument" if not set
	 *
	 * @return the new ownerDocument
	 * @throws FrameworkException
	 */
	static Page createNewPage(final SecurityContext securityContext, final String name) throws FrameworkException {
		return createNewPage(securityContext, null, name);
	}

	/**
	 * Creates a new Page entity with the given name in the database.
	 *
	 * @param securityContext the security context to use
	 * @param uuid the UUID of the new page or null
	 * @param name the name of the new page, defaults to "page"
	 * "ownerDocument" if not set
	 *
	 * @return the new ownerDocument
	 * @throws FrameworkException
	 */
	static Page createNewPage(final SecurityContext securityContext, final String uuid, final String name) throws FrameworkException {

		final Traits traits                           = Traits.of("Page");
		final PropertyKey<String> nameKey             = traits.key("name");
		final PropertyKey<String> typeKey             = traits.key("type");
		final PropertyKey<String> contentTypeKey      = traits.key("contentType");
		final PropertyKey<Boolean> enableBasicAuthKey = traits.key("enableBasicAuth");
		final App app                                 = StructrApp.getInstance(securityContext);
		final PropertyMap properties                  = new PropertyMap();

		// set default values for properties on creation to avoid them
		// being set separately when indexing later
		properties.put(nameKey,            name != null ? name : "page");
		properties.put(typeKey,            Page.class.getSimpleName());
		properties.put(contentTypeKey,     "text/html");
		properties.put(enableBasicAuthKey, false);

		if (uuid != null) {
			properties.put(Traits.idProperty(), uuid);
		}

		return app.create("Page", properties).as(Page.class);
	}

	/**
	 * Creates a default simple page for the Structr backend "add page" button.
	 *
	 * @param securityContext
	 * @param name
	 * @return
	 * @throws FrameworkException
	 */
	static Page createSimplePage(final SecurityContext securityContext, final String name) throws FrameworkException {

		final Page page = Page.createNewPage(securityContext, name);
		if (page != null) {

			DOMElement html  = page.createElement("html");
			DOMElement head  = page.createElement("head");
			DOMElement body  = page.createElement("body");
			DOMElement title = page.createElement("title");
			DOMElement h1    = page.createElement("h1");
			DOMElement div   = page.createElement("div");

			// add HTML element to page
			page.appendChild(html);

			// add HEAD and BODY elements to HTML
			html.appendChild(head);
			html.appendChild(body);

			// add TITLE element to HEAD
			head.appendChild(title);

			// add H1 element to BODY
			body.appendChild(h1);

			// add DIV element to BODY
			body.appendChild(div);

			// add text nodes
			title.appendChild(page.createTextNode("${capitalize(page.name)}"));
			h1.appendChild(page.createTextNode("${capitalize(page.name)}"));
			div.appendChild(page.createTextNode("Initial body text"));
		}

		return page;
	}

}
