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
package org.structr.crawler;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourcePattern extends AbstractNode {

	private static final Logger logger = LoggerFactory.getLogger(SourcePattern.class.getName());

	public static final Property<Iterable<SourcePattern>> subPatternsProperty             = new EndNodes<>("subPatterns", SourcePatternSUBSourcePattern.class);
	public static final Property<SourcePage>              subPageProperty                 = new EndNode<>("subPage", SourcePatternSUBPAGESourcePage.class);
	public static final Property<SourcePage>              sourcePageProperty              = new StartNode<>("sourcePage", SourcePageUSESourcePattern.class);
	public static final Property<SourcePattern>           parentPatternProperty           = new StartNode<>("parentPattern", SourcePatternSUBSourcePattern.class);

	public static final Property<Long>                    fromProperty                    = new LongProperty("from");
	public static final Property<Long>                    toProperty                      = new LongProperty("to");
	public static final Property<String>                  selectorProperty                = new StringProperty("selector").indexed();
	public static final Property<String>                  mappedTypeProperty              = new StringProperty("mappedType").indexed();
	public static final Property<String>                  mappedAttributeProperty         = new StringProperty("mappedAttribute").indexed();
	public static final Property<String>                  mappedAttributeFunctionProperty = new StringProperty("mappedAttributeFunction");
	public static final Property<String>                  inputValue                      = new StringProperty("inputValue").indexed();

	public static final View uiView = new View(SourcePattern.class, "ui",
		subPatternsProperty, subPageProperty, sourcePageProperty, parentPatternProperty, fromProperty, toProperty, selectorProperty, mappedTypeProperty, mappedAttributeProperty, mappedAttributeFunctionProperty, inputValue
	);

	private Class type(final String typeString) throws FrameworkException {

		Class type = null;

		final ConfigurationProvider config = StructrApp.getConfiguration();
		if (typeString != null) {

			type = config.getNodeEntityClass(typeString);

		}

		if (type == null) {

			throw new FrameworkException(422, "Unknown type '" + typeString + "'");
		}

		return type;
	}

	private NodeInterface create(final String typeString) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		return app.create(type(typeString));
	}

	private SourceSite getSite() {

		SourcePattern pattern = this;

		SourcePage page = pattern.getProperty(sourcePageProperty);
		while (page == null) {

			pattern = pattern.getProperty(parentPatternProperty);

			if (pattern != null) {
				page = pattern.getProperty(sourcePageProperty);
			}
		}

		return page.getProperty(SourcePage.site);

	}

	private String getContent(final String urlString) throws FrameworkException {

		final SourceSite site = getSite();

		String proxyUrl = site.getProperty(SourceSite.proxyUrl);
		String proxyUsername = site.getProperty(SourceSite.proxyUsername);
		String proxyPassword = site.getProperty(SourceSite.proxyPassword);

		Principal user = securityContext.getCachedUser();

		if (user != null & StringUtils.isBlank(proxyUrl)) {
			proxyUrl      = user.getProperty(StructrApp.key(User.class, "proxyUrl"));
			proxyUsername = user.getProperty(StructrApp.key(User.class, "proxyUsername"));
			proxyPassword = user.getProperty(StructrApp.key(User.class, "proxyPassword"));
		}

		final String cookie = site.getProperty(SourceSite.cookie);

		return HttpHelper.get(urlString, null, proxyUrl, proxyUsername, proxyPassword, cookie, Collections.EMPTY_MAP, true)
				.replace("<head>", "<head>\n  <base href=\"" + urlString + "\">");
	}

	private void extractAndSetValue(final NodeInterface obj, final Document doc, final String selector, final String mappedType, final String mappedAttribute, final String mappedAttributeFunction, final SourcePage subPage)  throws FrameworkException {

		// If the sub pattern has a mapped attribute, set the extracted value
		if (StringUtils.isNotEmpty(mappedAttribute)) {

			// Extract the value for this sub pattern's selector
			final String ex       = doc.select(selector).text();
			final PropertyKey key = StructrApp.key(type(mappedType), mappedAttribute);

			if (StringUtils.isNotBlank(ex) && key != null) {

				Object convertedValue = ex;

				if (StringUtils.isNotBlank(mappedAttributeFunction)) {

					// input transformation requested
					ActionContext ctx = new ActionContext(securityContext);
					ctx.setConstant("input", convertedValue);
					convertedValue = Scripting.evaluate(ctx, null, "${" + mappedAttributeFunction.trim() + "}", " virtual property " + mappedAttribute, null);

				} else {

					// if no custom transformation is given, try input converter
					final PropertyConverter inputConverter = key.inputConverter(securityContext);

					if (inputConverter != null) {
						convertedValue = inputConverter.convert(convertedValue);
					}
				}

				obj.setProperty(key, convertedValue);
			}

		// If the sub pattern has no mapped attribute but a sub page defined, query the patterns of the sub page
		} else if (subPage != null) {

			final String pageUrl = subPage.getProperty(SourcePage.url);
			final URI uri;

			try {
				uri = new URI(pageUrl);
			} catch (URISyntaxException ex) {
				throw new FrameworkException(422, "Unable to parse sub page url: " + pageUrl);
			}

			// This is the URL of the linked page derived from the enclosing selector
			final String subUrl     = uri.getScheme() + "://" + uri.getAuthority() + doc.select(selector).attr("href");

			// Extract the content of the linked page
			final String subContent = getContent(subUrl);

			// Parse the content into a document
			final Document subDoc = Jsoup.parse(subContent);

			// Loop through all patterns of the sub page
			for (final SourcePattern subPagePattern : subPage.getProperty(SourcePage.patterns)) {

				final Map<String, Object> params = new HashMap<>();
				params.put("document", subDoc);
				params.put("object", obj);

				subPagePattern.extract(securityContext, params);
			}
		}
	}

	@Export
	public void extract(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		final SourcePage page = getProperty(sourcePageProperty);

		if (page == null) {
			throw new FrameworkException(422, "Pattern has no source page, exiting.");
		}

		final String selector        = getProperty(selectorProperty);
		if (selector == null) {
			throw new FrameworkException(422, "Pattern has no selector, exiting.");
		}

		final Long   from     = getProperty(fromProperty);
		final Long   to       = getProperty(toProperty);

		final List<SourcePattern> subPatterns = Iterables.toList(getProperty(subPatternsProperty));


		Document doc = null;
		NodeInterface parentObj = null;


		if (parameters.containsKey("object")) {

			parentObj = (NodeInterface) parameters.get("object");

		}

		if (parameters.containsKey("document")) {

			doc = (Document) parameters.get("document");

		} else {

			final String url      = page.getProperty(SourcePage.url);
			if (url == null) {
				throw new FrameworkException(422, "This pattern's source page has no URL, exiting.");
			}

			// Get the content from the URL
			final String content = getContent(url);

			// Parse the document with Jsoup and extract the elements matched by the given selector
			doc = Jsoup.parse(content);

		}

		final String mappedType      = getProperty(mappedTypeProperty);
		if (mappedType == null) {
			throw new FrameworkException(422, "No mapped type given, exiting.");
		}

		final Elements parts = doc.select(selector);

		// Loop through all elements found for this pattern; if a start index is given, start at this element
		for (int i = (from != null ? from.intValue() : 1); i<= (to != null ? to : parts.size()); i++) {

			// If no object was given (from a higher-level pattern), create a new object of the given type
			final NodeInterface obj = (parentObj == null ? create(mappedType) : parentObj);

			if (subPatterns.size() > 0) {

				// Loop through the sub patterns of this pattern
				for (final SourcePattern subPattern : subPatterns) {

					//final String subSelector = selector + ":nth-child(" + i + ") > " + subPattern.getProperty(SourcePattern.selectorProperty);
					final String subSelector = selector + " > " + subPattern.getProperty(SourcePattern.selectorProperty);

					final String subPatternMappedAttribute         = subPattern.getProperty(SourcePattern.mappedAttributeProperty);
					final String subPatternMappedAttributeFunction = subPattern.getProperty(SourcePattern.mappedAttributeFunctionProperty);
					final SourcePage subPatternSubPage             = subPattern.getProperty(SourcePattern.subPageProperty);

					extractAndSetValue(obj, doc, subSelector, mappedType, subPatternMappedAttribute, subPatternMappedAttributeFunction, subPatternSubPage);

				}

			} else {

				final String mappedAttribute         = getProperty(mappedAttributeProperty);
				final String mappedAttributeFunction = getProperty(mappedAttributeFunctionProperty);

				extractAndSetValue(obj, doc, selector, mappedType, mappedAttribute, mappedAttributeFunction, null);

			}

		}

	}
}
