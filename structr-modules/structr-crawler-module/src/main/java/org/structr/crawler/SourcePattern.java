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
package org.structr.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.structr.core.entity.AbstractNode;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.common.View;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.schema.ConfigurationProvider;

public class SourcePattern extends AbstractNode {

	public static final Property<List<SourcePattern>> subPatternsProperty               = new EndNodes<>("subPatterns", SourcePatternSUBSourcePattern.class);
	public static final Property<SourcePage>          subPageProperty                   = new EndNode<>("subPage", SourcePatternSUBPAGESourcePage.class);
	public static final Property<SourcePage>          sourcePageProperty                = new StartNode<>("sourcePage", SourcePageUSESourcePattern.class);
	public static final Property<SourcePattern>       parentPatternProperty             = new StartNode<>("parentPattern", SourcePatternSUBSourcePattern.class);
          
	public static final Property<Long>                fromProperty                      = new LongProperty("from");
	public static final Property<Long>                toProperty                        = new LongProperty("to");
	public static final Property<String>              selectorProperty                  = new StringProperty("selector").indexed();
	public static final Property<String>              mappedTypeProperty                = new StringProperty("mappedType").indexed();
	public static final Property<String>              mappedAttributeProperty           = new StringProperty("mappedAttribute").indexed();
	public static final Property<String>              mappedAttributeDataFormatProperty = new StringProperty("mappedAttributeFormat");
  
	public static final View uiView = new View(SourcePattern.class, "ui",
		subPatternsProperty, subPageProperty, sourcePageProperty, parentPatternProperty, fromProperty, toProperty, selectorProperty, mappedTypeProperty, mappedAttributeProperty, mappedAttributeDataFormatProperty
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
	
	private String getContent(final String url, final String cookie) throws FrameworkException {
		
		final HttpClientParams params = new HttpClientParams(HttpClientParams.getDefaultParams());

		final HttpClient client = new HttpClient(params);
		final GetMethod get = new GetMethod(url);

		get.addRequestHeader("User-Agent", "curl/7.35.0");
		get.addRequestHeader("Connection", "close");
		get.getParams().setParameter("http.protocol.single-cookie-header", true);
		get.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		get.setFollowRedirects(true);

		if (StringUtils.isNotBlank(cookie)) {

			get.addRequestHeader("Cookie", cookie);
		}

		String content = "";
		try {
			client.executeMethod(get);
			content = get.getResponseBodyAsString().replace("<head>", "<head>\n  <base href=\"" + url + "\">");
			
		} catch (IOException ex) {
			throw new FrameworkException(422, "Unable to extract content from " + url + ".");
		}

		return content;
	}
	
	private void extractAndSetValue(final NodeInterface obj, final Document doc, final String selector, final String mappedType, final String mappedAttribute, final SourcePage subPage)  throws FrameworkException {

		// If the sub pattern has a mapped attribute, set the extracted value
		if (StringUtils.isNotEmpty(mappedAttribute)) {

			// Extract the value for this sub pattern's selector
			final String ex = doc.select(selector).text();

			final ConfigurationProvider config  = StructrApp.getConfiguration();
			final PropertyKey key = config.getPropertyKeyForJSONName(type(mappedType), mappedAttribute);

			if (key != null) {

				final PropertyConverter inputConverter = key.inputConverter(securityContext);
				
				Object convertedValue = ex;
				
				if (inputConverter != null) {
					convertedValue = inputConverter.convert(ex);
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
			final String subContent = getContent(subUrl, null);

			// Parse the content into a document
			final Document subDoc = Jsoup.parse(subContent);

			final List<SourcePattern> subPagePatterns = subPage.getProperty(SourcePage.patterns);

			// Loop through all patterns of the sub page
			for (final SourcePattern subPagePattern : subPagePatterns) {

				final Map<String, Object> params = new HashMap<>();
				params.put("document", subDoc);
				params.put("object", obj);

				subPagePattern.extract(params);

//				final String subPagePatternSelector = subPagePattern.getProperty(SourcePattern.selectorProperty);
//
//				
//				// Extract 
//				final String subEx = subDoc.select(subPagePatternSelector).text();
//				final String subPagePatternType = subPagePattern.getProperty(SourcePattern.mappedTypeProperty);
//
//				if (subPagePatternType != null) {
//
//
//					final Elements subParts = subDoc.select(subPagePatternSelector);
//
//					final Long j = 1L;
//
//					for (final Element subPart : subParts) {
//
//						final NodeInterface subObj = create(subPagePatternType);
//
//						final List<SourcePattern> subPagePatternPatterns = subPagePattern.getProperty(SourcePattern.subPatternsProperty);
//
//						for (final SourcePattern subPageSubPattern : subPagePatternPatterns) {
//
//
//							final String subPagePatternSelector = subPageSubPattern.getProperty(SourcePattern.selectorProperty);
//
//
//
//							final String subPageSubPatternSelector = subPagePatternSelector + ":nth-child(" + j + ") > " + subPagePatternSelector;
//
//							extractAndSetValue(subObj, subDoc, subSelector, mappedType, subPatternMappedAttribute);
//
//
//							final String subSubEx = subDoc.select(subPageSubPatternSelector).text();
//
//							if (subSubEx != null && subSubEx != = '' && subPageSubPattern.mappedAttribute != null) {
//
//							final PropertyKey key = config.getPropertyKeyForJSONName(type(mappedType), subPatternMappedAttribute);
//							if (key != null) {
//
//								subObj.setProperty(key, subSubEx);
//							}
//
//						}
//
//						final String subPagePatternMappedAttribute = subPagePattern.getProperty(SourcePattern.mappedAttributeProperty);
//
//						final PropertyKey key = config.getPropertyKeyForJSONName(type(mappedType), subPagePatternMappedAttribute);
//						if (key != null) {
//
//							obj.setProperty(key, subSubEx);
//						}
//
//					}
//
//				} else {
//
//					if (subEx != null && subEx != = '' && subPagePattern.mappedAttribute != null) {
//						obj[subPagePattern.mappedAttribute] = subEx;
	//					}
			}
		}
	}

	@Export
	public void extract(final Map<String, Object> parameters) throws FrameworkException {
		
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

		final List<SourcePattern> subPatterns = getProperty(subPatternsProperty);

		
		Document doc = null;
		NodeInterface parentObj = null;
		
			
		if (parameters.containsKey("object")) {

			parentObj = (NodeInterface) parameters.get("object");

		}
		
		if (parameters.containsKey("document")) {

			doc = (Document) parameters.get("document");
			
		} else {
			
			final String url      = page.getProperty(SourcePage.url);
			final String cookie   = page.getProperty(SourcePage.cookie);
			if (url == null) {
				throw new FrameworkException(422, "This pattern's source page has no URL, exiting.");
			}

			// Get the content from the URL
			final String content = getContent(url, cookie);

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

					final String subSelector = selector + ":nth-child(" + i + ") > " + subPattern.getProperty(SourcePattern.selectorProperty);

					final String subPatternMappedAttribute = subPattern.getProperty(SourcePattern.mappedAttributeProperty);
					final SourcePage subPatternSubPage     = subPattern.getProperty(SourcePattern.subPageProperty);

					extractAndSetValue(obj, doc, subSelector, mappedType, subPatternMappedAttribute, subPatternSubPage);

				}
			
			} else {
				
				final String mappedAttribute = getProperty(mappedAttributeProperty);
			
				extractAndSetValue(obj, doc, selector, mappedType, mappedAttribute, null);
				
			}
			
		}

	}
}
