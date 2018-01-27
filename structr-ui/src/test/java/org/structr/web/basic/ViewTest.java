/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.basic;

import com.jayway.restassured.RestAssured;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.entity.User;


public class ViewTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ViewTest.class.getName());

	private static final Map<String, List<Map<String, List<String>>>> viewMap = new LinkedHashMap<>();

	static {

		final String[] categoryBase = {};
		final String[] _html_Base   = {};
		final String[] schemaBase   = {};
		final String[] exportBase   = {};
		final String[] favBase      = {};
		final String[] publicBase   = { "id", "type" };
		final String[] uiBase       = { "id", "name", "owner", "createdBy", "deleted", "hidden", "createdDate", "lastModifiedDate", "visibleToPublicUsers", "visibleToAuthenticatedUsers" };

		viewMap.put("A", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "linkable", "linkableId", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Abbr", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("AbstractFile", Arrays.asList(
			toMap("public",    publicBase, "isExternal", "isMounted", "lastSeenMounted", "path"),
			toMap("ui",        uiBase, "isExternal", "isMounted", "lastSeenMounted", "path")
		));

		viewMap.put("AbstractMinifiedFile", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "fileModificationDate", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "name", "owner", "path", "size", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "extractedContent", "hasParent", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "linkingElements", "md5", "parent", "path", "size", "url", "useAsJavascriptLibrary", "version")
		));

		viewMap.put("AbstractNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("AbstractSchemaLocalization", Arrays.asList(
			toMap("public",    publicBase, "locale", "name"),
			toMap("ui",        uiBase, "locale")
		));

		viewMap.put("AbstractSchemaNode", Arrays.asList(
			toMap("export",    exportBase),
			toMap("public",    publicBase, "icon", "name"),
			toMap("schema",    schemaBase),
			toMap("ui",        uiBase, "description", "icon", "schemaMethods", "schemaProperties", "schemaViews")
		));

		viewMap.put("AbstractUser", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("Address", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Area", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Article", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Aside", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Audio", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("B", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Base", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Bdi", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Bdo", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Blockquote", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Body", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Br", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Button", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Canvas", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Caption", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Cdata", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery"),
			toMap("ui",        uiBase, "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery")
		));

		viewMap.put("Cite", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Code", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Col", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Colgroup", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Command", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Comment", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery"),
			toMap("ui",        uiBase, "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery")
		));

		viewMap.put("Component", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "kind", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "kind", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("ConceptBTConcept", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ConceptGroup", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("ConceptPreferredTerm", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ConceptRTConcept", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ConceptTerm", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ContainerContentContainer", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ContainerContentItems", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Content", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery"),
			toMap("ui",        uiBase, "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery")
		));

		viewMap.put("ContentContainer", Arrays.asList(
			toMap("public",    publicBase, "childContainers", "isContentContainer", "items", "name", "owner", "parent", "path"),
			toMap("ui",        uiBase, "childContainers", "isContentContainer", "items", "parent", "path")
		));

		viewMap.put("ContentItem", Arrays.asList(
			toMap("public",    publicBase, "containers", "isContentItem", "name", "owner"),
			toMap("ui",        uiBase, "containers", "isContentItem")
		));

		viewMap.put("CrawlerTreeNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("CsvFile", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "fileModificationDate", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "name", "owner", "path", "size", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "extractedContent", "hasParent", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "linkingElements", "md5", "parent", "path", "size", "url", "useAsJavascriptLibrary", "version")
		));

		viewMap.put("CustomTermAttribute", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("DataFeed", Arrays.asList(
			toMap("public",    publicBase, "description", "feedType", "items", "url"),
			toMap("ui",        uiBase, "description", "feedType", "items", "lastUpdated", "maxAge", "maxItems", "updateInterval", "url")
		));

		viewMap.put("Datalist", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Dd", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Del", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Details", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Dfn", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Div", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Dl", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("DocumentFragment", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("DocumentResult", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("DocumentTemplate", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("DOMChildren", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("DOMElement", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("DOMNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("DOMSiblings", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Dt", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("DynamicResourceAccess", Arrays.asList(
			toMap("public",    publicBase, "flags", "isResourceAccess", "signature"),
			toMap("ui",        uiBase, "flags", "isResourceAccess", "position", "signature")
		));

		viewMap.put("Em", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Embed", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("FeedItem", Arrays.asList(
			toMap("public",    publicBase, "author", "comments", "contentType", "contents", "description", "enclosures", "name", "owner", "pubDate", "url"),
			toMap("ui",        uiBase, "author", "cacheForSeconds", "checksum", "comments", "contentType", "contents", "description", "enclosures", "extractedContent", "feed", "indexedWords", "pubDate", "url", "version")
		));

		viewMap.put("FeedItemContent", Arrays.asList(
			toMap("public",    publicBase, "contentType", "itemType", "mode", "owner", "value"),
			toMap("ui",        uiBase, "contentType", "extractedContent", "indexedWords", "item", "itemType", "mode", "value")
		));

		viewMap.put("FeedItemContents", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("FeedItemEnclosure", Arrays.asList(
			toMap("public",    publicBase, "contentType", "enclosureLength", "enclosureType", "item", "owner", "url"),
			toMap("ui",        uiBase, "contentType", "enclosureLength", "enclosureType", "extractedContent", "indexedWords", "item", "url")
		));

		viewMap.put("FeedItemEnclosures", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("FeedItems", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Fieldset", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Figcaption", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Figure", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("File", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "fileModificationDate", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "name", "owner", "path", "size", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "extractedContent", "hasParent", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "linkingElements", "md5", "parent", "path", "size", "url", "useAsJavascriptLibrary", "version")
		));

		viewMap.put("FileBase", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "fileModificationDate", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "name", "owner", "path", "size", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "extractedContent", "hasParent", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "linkingElements", "md5", "parent", "path", "size", "url", "useAsJavascriptLibrary", "version")
		));

		viewMap.put("FileChildren", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("Files", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("FileSiblings", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Folder", Arrays.asList(
			toMap("public",    publicBase, "enabledChecksums", "files", "folders", "isExternal", "isFolder", "isMounted", "lastSeenMounted", "mountDoFulltextIndexing", "mountLastScanned", "mountScanInterval", "mountTarget", "name", "owner", "parentId", "path", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "enabledChecksums", "files", "folders", "images", "includeInFrontendExport", "isExternal", "isFolder", "isMounted", "lastSeenMounted", "mountDoFulltextIndexing", "mountLastScanned", "mountScanInterval", "mountTarget", "parent", "path")
		));

		viewMap.put("FolderChildren", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("Folders", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("Footer", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Form", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("G", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("GenericNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("GenericRelation", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("GenericRelationship", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "endNodeId", "relType", "sourceId", "startNodeId", "targetId")
		));

		viewMap.put("Group", Arrays.asList(
			toMap("public",    publicBase, "blocked", "isGroup", "members", "name"),
			toMap("ui",        uiBase, "blocked", "customPermissionQueryAccessControl", "customPermissionQueryDelete", "customPermissionQueryRead", "customPermissionQueryWrite", "isGroup", "members")
		));

		viewMap.put("Groups", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("H1", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("H2", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("H3", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("H4", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("H5", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("H6", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Head", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Header", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Hgroup", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Hr", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Html", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "_html_manifest", "children", "childrenIds", "customOpeningTag", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("I", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Iframe", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Image", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "exifIFD0Data", "exifSubIFDData", "fileModificationDate", "gpsData", "height", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isImage", "isMounted", "isTemplate", "isThumbnail", "lastSeenMounted", "name", "orientation", "owner", "parent", "path", "size", "tnMid", "tnSmall", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers", "width"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "exifIFD0Data", "exifSubIFDData", "extractedContent", "gpsData", "hasParent", "height", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isImage", "isMounted", "isTemplate", "isThumbnail", "lastSeenMounted", "linkingElements", "md5", "orientation", "parent", "path", "size", "tnMid", "tnSmall", "url", "useAsJavascriptLibrary", "version", "width")
		));

		viewMap.put("Images", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("ImageWidget", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Img", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Input", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Ins", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Kbd", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Keygen", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Label", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Language", Arrays.asList(
			toMap("public",    publicBase, "concept", "customAttributes", "lang", "name", "normalizedWords"),
			toMap("ui",        uiBase, "concept", "customAttributes", "lang", "normalizedWords")
		));

		viewMap.put("LDAPAttributeImpl", Arrays.asList(
			toMap("public",    publicBase, "name", "oid", "values"),
			toMap("ui",        uiBase, "oid", "values")
		));

		viewMap.put("LDAPAttributes", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("LDAPChildren", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("LDAPNodeImpl", Arrays.asList(
			toMap("public",    publicBase, "attributes", "children", "name", "rdn"),
			toMap("ui",        uiBase, "attributes", "children", "rdn")
		));

		viewMap.put("LDAPUser", Arrays.asList(
			toMap("public",    publicBase, "commonName", "description", "distinguishedName", "entryUuid", "isUser", "name"),
			toMap("ui",        uiBase, "backendUser", "blocked", "commonName", "confirmationKey", "description", "distinguishedName", "eMail", "entryUuid", "favorites", "frontendUser", "groups", "homeDirectory", "img", "isAdmin", "isUser", "locale", "password", "proxyPassword", "proxyUrl", "proxyUsername", "publicKey", "sessionIds", "skipSecurityRelationships", "workingDirectory")
		));

		viewMap.put("LDAPValueImpl", Arrays.asList(
			toMap("public",    publicBase, "value"),
			toMap("ui",        uiBase, "value")
		));

		viewMap.put("LDAPValues", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Legend", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Li", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Link", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "linkable", "linkableId", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("LinkedListNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("LinkedTreeNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("LinkSource", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Localization", Arrays.asList(
			toMap("public",    publicBase, "description", "domain", "imported", "locale", "localizedName", "name"),
			toMap("ui",        uiBase, "description", "domain", "imported", "locale", "localizedName")
		));

		viewMap.put("Location", Arrays.asList(
			toMap("public",    publicBase, "altitude", "latitude", "longitude"),
			toMap("ui",        uiBase, "altitude", "latitude", "longitude")
		));

		viewMap.put("LogEvent", Arrays.asList(
			toMap("public",    publicBase, "action", "message", "object", "subject", "timestamp"),
			toMap("ui",        uiBase)
		));

		viewMap.put("LogObject", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("LogSubject", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("MailTemplate", Arrays.asList(
			toMap("public",    publicBase, "locale", "name", "text"),
			toMap("ui",        uiBase, "locale", "text")
		));

		viewMap.put("Map", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Mark", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Menu", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Meta", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Meter", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("MinificationSource", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "position", "relType", "sourceId", "targetId")
		));

		viewMap.put("MinifiedCssFile", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "fileModificationDate", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "lineBreak", "minificationSources", "name", "owner", "path", "size", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "extractedContent", "hasParent", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "lineBreak", "linkingElements", "md5", "minificationSources", "parent", "path", "size", "url", "useAsJavascriptLibrary", "version")
		));

		viewMap.put("MinifiedJavaScriptFile", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "contentType", "errors", "fileModificationDate", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "minificationSources", "name", "optimizationLevel", "owner", "path", "size", "url", "visibleToAuthenticatedUsers", "visibleToPublicUsers", "warnings"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "enableBasicAuth", "errors", "extractedContent", "hasParent", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "lastSeenMounted", "linkingElements", "md5", "minificationSources", "optimizationLevel", "parent", "path", "size", "url", "useAsJavascriptLibrary", "version", "warnings")
		));

		viewMap.put("MQTTClient", Arrays.asList(
			toMap("public",    publicBase, "isConnected", "isEnabled", "port", "protocol", "qos", "subscribers", "url"),
			toMap("ui",        uiBase, "isConnected", "isEnabled", "port", "protocol", "qos", "subscribers", "url")
		));

		viewMap.put("MQTTClientHAS_SUBSCRIBERMQTTSubscriber", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("MQTTSubscriber", Arrays.asList(
			toMap("public",    publicBase, "client", "source", "topic"),
			toMap("ui",        uiBase, "client", "source", "topic")
		));

		viewMap.put("Nav", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("NodeHasLocation", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Noscript", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Object", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("ObjectEventRelationship", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ODFExporter", Arrays.asList(
			toMap("public",    publicBase, "documentTemplate", "resultDocument", "transformationProvider"),
			toMap("ui",        uiBase, "documentTemplate", "resultDocument", "transformationProvider")
		));

		viewMap.put("ODSExporter", Arrays.asList(
			toMap("public",    publicBase, "documentTemplate", "resultDocument", "transformationProvider"),
			toMap("ui",        uiBase, "documentTemplate", "resultDocument", "transformationProvider")
		));

		viewMap.put("ODTExporter", Arrays.asList(
			toMap("public",    publicBase, "documentTemplate", "resultDocument", "transformationProvider"),
			toMap("ui",        uiBase, "documentTemplate", "resultDocument", "transformationProvider")
		));

		viewMap.put("Ol", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Optgroup", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Option", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Output", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("P", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Page", Arrays.asList(
			toMap("category",  categoryBase),
			toMap("public",    publicBase, "basicAuthRealm", "cacheForSeconds", "category", "children", "contentType", "dontCache", "enableBasicAuth", "isPage", "linkingElements", "name", "owner", "pageCreatesRawData", "path", "position", "showOnErrorCodes", "site", "version"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "category", "children", "contentType", "dontCache", "enableBasicAuth", "isPage", "linkingElements", "pageCreatesRawData", "path", "position", "showOnErrorCodes", "site", "version")
		));

		viewMap.put("PageLink", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "linkType", "relType", "sourceId", "targetId")
		));

		viewMap.put("Pages", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Param", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("PaymentItemNode", Arrays.asList(
			toMap("public",    publicBase, "amount", "description", "name", "number", "quantity", "url"),
			toMap("ui",        uiBase, "amount", "description", "number", "quantity", "url")
		));

		viewMap.put("PaymentItems", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("PaymentNode", Arrays.asList(
			toMap("public",    publicBase, "billingAddressCity", "billingAddressCountry", "billingAddressName", "billingAddressStreet1", "billingAddressStreet2", "billingAddressZip", "billingAgreementId", "currency", "description", "invoiceId", "items", "note", "payer", "payerAddressCity", "payerAddressCountry", "payerAddressName", "payerAddressStreet1", "payerAddressStreet2", "payerAddressZip", "payerBusiness", "state", "token"),
			toMap("ui",        uiBase, "billingAddressCity", "billingAddressCountry", "billingAddressName", "billingAddressStreet1", "billingAddressStreet2", "billingAddressZip", "billingAgreementId", "currency", "description", "invoiceId", "items", "note", "payer", "payerAddressCity", "payerAddressCountry", "payerAddressName", "payerAddressStreet1", "payerAddressStreet2", "payerAddressZip", "payerBusiness", "state", "token")
		));

		viewMap.put("Person", Arrays.asList(
			toMap("public",    publicBase, "city", "country", "eMail", "firstName", "lastName", "middleNameOrInitial", "name", "salutation", "state", "street", "twitterName", "zipCode"),
			toMap("ui",        uiBase, "birthday", "city", "country", "eMail", "eMail2", "faxNumber1", "faxNumber2", "firstName", "gender", "lastName", "middleNameOrInitial", "newsletter", "phoneNumber1", "phoneNumber2", "salutation", "state", "street", "twitterName", "zipCode")
		));

		viewMap.put("Pre", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("PreferredTerm", Arrays.asList(
			toMap("public",    publicBase, "concept", "customAttributes", "lang", "name", "normalizedWords"),
			toMap("ui",        uiBase, "concept", "customAttributes", "lang", "normalizedWords")
		));

		viewMap.put("PrincipalOwnsNode", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Progress", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Q", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("RemoteDocument", Arrays.asList(
			toMap("public",    publicBase, "contentType", "name", "owner", "url"),
			toMap("ui",        uiBase, "cacheForSeconds", "checksum", "contentType", "extractedContent", "indexedWords", "url", "version")
		));

		viewMap.put("RenderNode", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ResourceAccess", Arrays.asList(
			toMap("public",    publicBase, "flags", "isResourceAccess", "signature"),
			toMap("ui",        uiBase, "flags", "isResourceAccess", "position", "signature")
		));

		viewMap.put("ResourceLink", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Rp", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Rt", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Ruby", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("S", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Samp", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("SchemaMethod", Arrays.asList(
			toMap("export",    exportBase),
			toMap("fav",       favBase),
			toMap("public",    publicBase, "comment", "isFavoritable", "name", "schemaNode", "source"),
			toMap("ui",        uiBase, "comment", "isFavoritable", "schemaNode", "source")
		));

		viewMap.put("SchemaNode", Arrays.asList(
			toMap("export",    exportBase),
			toMap("public",    publicBase, "defaultSortKey", "defaultSortOrder", "extendsClass", "hierarchyLevel", "icon", "isBuiltinType", "name", "relCount", "relatedFrom", "relatedTo"),
			toMap("schema",    schemaBase),
			toMap("ui",        uiBase, "defaultSortKey", "defaultSortOrder", "description", "extendsClass", "hierarchyLevel", "icon", "isBuiltinType", "relCount", "relatedFrom", "relatedTo", "schemaMethods", "schemaProperties", "schemaViews")
		));

		viewMap.put("SchemaNodeMethod", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SchemaNodeProperty", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SchemaNodeView", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SchemaProperty", Arrays.asList(
			toMap("export",    exportBase),
			toMap("public",    publicBase, "compound", "contentType", "dbName", "declaringClass", "defaultValue", "format", "indexed", "isBuiltinProperty", "isDynamic", "name", "notNull", "propertyType", "readFunction", "schemaNode", "schemaViews", "unique", "writeFunction"),
			toMap("schema",    schemaBase),
			toMap("ui",        uiBase, "compound", "contentType", "dbName", "declaringClass", "defaultValue", "format", "indexed", "isBuiltinProperty", "isDynamic", "notNull", "propertyType", "readFunction", "schemaNode", "schemaViews", "unique", "writeFunction")
		));

		viewMap.put("SchemaRelationship", Arrays.asList(
			toMap("public",    publicBase, "autocreationFlag", "cascadingDeleteFlag", "extendsClass", "name", "relType", "relationshipType", "sourceId", "sourceJsonName", "sourceMultiplicity", "sourceNotion", "targetId", "targetJsonName", "targetMultiplicity", "targetNotion"),
			toMap("ui",        uiBase, "autocreationFlag", "cascadingDeleteFlag", "extendsClass", "relType", "relationshipType", "sourceId", "sourceJsonName", "sourceMultiplicity", "sourceNotion", "targetId", "targetJsonName", "targetMultiplicity", "targetNotion")
		));

		viewMap.put("SchemaRelationshipNode", Arrays.asList(
			toMap("export",    exportBase),
			toMap("public",    publicBase, "accessControlPropagation", "autocreationFlag", "cascadingDeleteFlag", "deletePropagation", "extendsClass", "icon", "name", "oldSourceJsonName", "oldTargetJsonName", "permissionPropagation", "propertyMask", "readPropagation", "relationshipType", "sourceId", "sourceJsonName", "sourceMultiplicity", "sourceNotion", "targetId", "targetJsonName", "targetMultiplicity", "targetNotion", "writePropagation"),
			toMap("schema",    schemaBase),
			toMap("ui",        uiBase, "accessControlPropagation", "autocreationFlag", "cascadingDeleteFlag", "deletePropagation", "description", "extendsClass", "icon", "oldSourceJsonName", "oldTargetJsonName", "permissionPropagation", "propertyMask", "readPropagation", "relationshipType", "schemaMethods", "schemaProperties", "schemaViews", "sourceId", "sourceJsonName", "sourceMultiplicity", "sourceNotion", "targetId", "targetJsonName", "targetMultiplicity", "targetNotion", "writePropagation")
		));

		viewMap.put("SchemaRelationshipSourceNode", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SchemaRelationshipTargetNode", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SchemaReloadingNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("SchemaView", Arrays.asList(
			toMap("export",    exportBase),
			toMap("public",    publicBase, "name", "nonGraphProperties", "schemaNode", "schemaProperties"),
			toMap("schema",    schemaBase),
			toMap("ui",        uiBase, "isBuiltinView", "nonGraphProperties", "schemaNode", "schemaProperties", "sortOrder")
		));

		viewMap.put("SchemaViewProperty", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Script", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "linkable", "linkableId", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Section", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Security", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "allowed", "relType", "sourceId", "targetId")
		));

		viewMap.put("Select", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("ShadowDocument", Arrays.asList(
			toMap("category",  categoryBase),
			toMap("public",    publicBase, "basicAuthRealm", "cacheForSeconds", "category", "children", "contentType", "dontCache", "enableBasicAuth", "isPage", "linkingElements", "name", "owner", "pageCreatesRawData", "path", "position", "showOnErrorCodes", "site", "version"),
			toMap("ui",        uiBase, "basicAuthRealm", "cacheForSeconds", "category", "children", "contentType", "dontCache", "enableBasicAuth", "isPage", "linkingElements", "pageCreatesRawData", "path", "position", "showOnErrorCodes", "site", "version")
		));

		viewMap.put("Site", Arrays.asList(
			toMap("public",    publicBase, "hostname", "name", "pages", "port"),
			toMap("ui",        uiBase, "hostname", "pages", "port")
		));

		viewMap.put("Small", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Source", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("SourcePage", Arrays.asList(
			toMap("public",    publicBase, "name"),
			toMap("ui",        uiBase, "isLoginPage", "parentPage", "patterns", "site", "subPageOf", "subPages", "url")
		));

		viewMap.put("SourcePageSUBSourcePage", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SourcePageUSESourcePattern", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SourcePattern", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase, "from", "inputValue", "mappedAttribute", "mappedAttributeFunction", "mappedType", "parentPattern", "selector", "sourcePage", "subPage", "subPatterns", "to")
		));

		viewMap.put("SourcePatternSUBPAGESourcePage", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SourcePatternSUBSourcePattern", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("SourceSite", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase, "authPassword", "authUsername", "cookie", "pages", "proxyPassword", "proxyUrl", "proxyUsername")
		));

		viewMap.put("SourceSiteCONTAINSSourcePage", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Span", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Strong", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Style", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Sub", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("SubjectEventRelationship", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Summary", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Sup", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Sync", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Table", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Tbody", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Td", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Template", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "children", "childrenIds", "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery"),
			toMap("ui",        uiBase, "children", "childrenIds", "content", "contentType", "cypherQuery", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isContent", "isDOMNode", "isFavoritable", "pageId", "parent", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "xpathQuery")
		));

		viewMap.put("TemplateText", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("TermHasCustomAttributes", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("TermHasLabel", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Textarea", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Tfoot", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Th", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Thead", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Thesaurus", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("ThesaurusConcept", Arrays.asList(
			toMap("public",    publicBase, "broaderTerms", "name", "narrowerTerms", "preferredTerms", "relatedTerms", "relatedTermsOf", "terms", "thesaurus"),
			toMap("ui",        uiBase, "broaderTerms", "narrowerTerms", "preferredTerms", "relatedTerms", "relatedTermsOf", "terms", "thesaurus")
		));

		viewMap.put("ThesaurusContainsConcepts", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ThesaurusTerm", Arrays.asList(
			toMap("public",    publicBase, "concept", "customAttributes", "lang", "name", "normalizedWords"),
			toMap("ui",        uiBase, "concept", "customAttributes", "lang", "normalizedWords")
		));

		viewMap.put("Thumbnails", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Time", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Title", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Tr", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Track", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("TransformationRules", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Trash", Arrays.asList(
			toMap("public",    publicBase, "enabledChecksums", "files", "folders", "isExternal", "isFolder", "isMounted", "lastSeenMounted", "mountDoFulltextIndexing", "mountLastScanned", "mountScanInterval", "mountTarget", "name", "owner", "parentId", "path", "visibleToAuthenticatedUsers", "visibleToPublicUsers"),
			toMap("ui",        uiBase, "enabledChecksums", "files", "folders", "images", "includeInFrontendExport", "isExternal", "isFolder", "isMounted", "lastSeenMounted", "mountDoFulltextIndexing", "mountLastScanned", "mountScanInterval", "mountTarget", "parent", "path")
		));

		viewMap.put("U", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Ul", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("User", Arrays.asList(
			toMap("public",    publicBase, "isUser", "name"),
			toMap("ui",        uiBase, "backendUser", "blocked", "confirmationKey", "eMail", "favorites", "frontendUser", "groups", "homeDirectory", "img", "isAdmin", "isUser", "locale", "password", "proxyPassword", "proxyUrl", "proxyUsername", "publicKey", "sessionIds", "skipSecurityRelationships", "workingDirectory")
		));

		viewMap.put("UserFavoriteFavoritable", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("UserFavoriteFile", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("UserHomeDir", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("UserImage", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("UserWorkDir", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("ValidatedNode", Arrays.asList(
			toMap("public",    publicBase),
			toMap("ui",        uiBase)
		));

		viewMap.put("Var", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Video", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("VideoFile", Arrays.asList(
			toMap("fav",       favBase),
			toMap("public",    publicBase, "audioChannels", "audioCodec", "audioCodecName", "contentType", "convertedVideos", "duration", "fileModificationDate", "height", "includeInFrontendExport", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "isVideo", "lastSeenMounted", "name", "owner", "parent", "path", "pixelFormat", "posterImage", "sampleRate", "size", "url", "videoCodec", "videoCodecName", "visibleToAuthenticatedUsers", "visibleToPublicUsers", "width"),
			toMap("ui",        uiBase, "audioChannels", "audioCodec", "audioCodecName", "basicAuthRealm", "cacheForSeconds", "checksum", "contentType", "convertedVideos", "duration", "enableBasicAuth", "extractedContent", "hasParent", "height", "includeInFrontendExport", "indexedWords", "isExternal", "isFavoritable", "isFile", "isMounted", "isTemplate", "isVideo", "lastSeenMounted", "linkingElements", "md5", "originalVideo", "parent", "path", "pixelFormat", "posterImage", "sampleRate", "size", "url", "useAsJavascriptLibrary", "version", "videoCodec", "videoCodecName", "width")
		));

		viewMap.put("VideoFileHasConvertedVideoFile", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("VideoFileHasPosterImage", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("VirtualProperty", Arrays.asList(
			toMap("public",    publicBase, "inputFunction", "outputFunction", "position", "sourceName", "targetName", "virtualType"),
			toMap("ui",        uiBase, "inputFunction", "outputFunction", "position", "sourceName", "targetName", "virtualType")
		));

		viewMap.put("VirtualType", Arrays.asList(
			toMap("public",    publicBase, "filterExpression", "name", "position", "properties", "sourceType"),
			toMap("ui",        uiBase, "filterExpression", "position", "properties", "sourceType")
		));

		viewMap.put("VirtualTypeProperty", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("Wbr", Arrays.asList(
			toMap("_html_",    _html_Base),
			toMap("public",    publicBase, "children", "cypherQuery", "dataKey", "functionQuery", "isDOMNode", "name", "pageId", "parent", "partialUpdateKey", "path", "restQuery", "sharedComponent", "syncedNodes", "tag", "xpathQuery"),
			toMap("ui",        uiBase, "_html_class", "_html_id", "children", "childrenIds", "cypherQuery", "data-structr-action", "data-structr-append-id", "data-structr-attr", "data-structr-attributes", "data-structr-confirm", "data-structr-custom-options-query", "data-structr-edit-class", "data-structr-hide", "data-structr-id", "data-structr-name", "data-structr-options-key", "data-structr-placeholder", "data-structr-raw-value", "data-structr-reload", "data-structr-return", "data-structr-type", "dataKey", "functionQuery", "hideConditions", "hideForLocales", "hideOnDetail", "hideOnIndex", "isDOMNode", "pageId", "parent", "partialUpdateKey", "path", "renderDetails", "restQuery", "sharedComponent", "sharedComponentConfiguration", "showConditions", "showForLocales", "syncedNodes", "tag", "xpathQuery")
		));

		viewMap.put("Widget", Arrays.asList(
			toMap("public",    publicBase, "configuration", "description", "isWidget", "name", "pictures", "source", "treePath"),
			toMap("ui",        uiBase, "configuration", "description", "isWidget", "pictures", "source", "treePath")
		));

		viewMap.put("XMPPClient", Arrays.asList(
			toMap("public",    publicBase, "isConnected", "isEnabled", "pendingRequests", "presenceMode", "xmppHandle", "xmppHost", "xmppPassword", "xmppPort", "xmppService", "xmppUsername"),
			toMap("ui",        uiBase, "isConnected", "isEnabled", "pendingRequests", "presenceMode", "xmppHandle", "xmppHost", "xmppPassword", "xmppPort", "xmppService", "xmppUsername")
		));

		viewMap.put("XMPPClientRequest", Arrays.asList(
			toMap("public",    publicBase, "relType", "sourceId", "targetId"),
			toMap("ui",        uiBase, "relType", "sourceId", "targetId")
		));

		viewMap.put("XMPPRequest", Arrays.asList(
			toMap("public",    publicBase, "client", "content", "requestType", "sender"),
			toMap("ui",        uiBase, "client", "content", "requestType", "sender")
		));

	}

	@Test
	public void testViews() {

		// create test user
		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// create an instance of each of the internal types and check the views
		for (final Entry<String, List<Map<String, List<String>>>> entry : viewMap.entrySet()) {

			final String typeName                      = entry.getKey();
			final List<Map<String, List<String>>> list = entry.getValue();
			final Class type                           = StructrApp.getConfiguration().getNodeEntityClass(typeName);
			int i                                      = 0;

			System.out.println("####################################### Testing " + typeName + "..");

			assertNotNull("Type " + type + " should exist", type);

			for (final Map<String, List<String>> listEntry : list) {

				for (final Entry<String, List<String>> view : listEntry.entrySet()) {

					final String viewName    = view.getKey();
					final List<String> keys  = view.getValue();

					// create entity
					final String uuid = StringUtils.substringAfterLast(RestAssured
						.given()
						.header("X-User", "admin")
						.header("X-Password", "admin")
						.body("{ name: 'test" + i++ + "' }")
						.expect()
						.statusCode(201)
						.when()
						.post("/" + typeName)
						.header("Location"), "/");

					// check entity
					final Map<String, Object> result = RestAssured
						.given()
						.header("X-User", "admin")
						.header("X-Password", "admin")
						.expect()
						.statusCode(200)
						.when()
						.get("/" + typeName + "/" + uuid + "/" + viewName)
						.andReturn()
						.body()
						.as(Map.class);

					final Map<String, Object> item = (Map<String, Object>)result.get("result");
					final Set<String> expectedKeys = new TreeSet<>(keys);
					final Set<String> itemKeySet   = item.keySet();

					expectedKeys.removeAll(itemKeySet);

					assertTrue("\"" + viewName + "\" view of type \"" + type.getSimpleName() + "\" is missing the following keys: " + expectedKeys, expectedKeys.isEmpty());

					expectedKeys.clear();
					expectedKeys.addAll(keys);

					itemKeySet.removeAll(expectedKeys);

					assertTrue("\"" + viewName + "\" view of type \"" + type.getSimpleName() + "\" contains keys that are not listed in the specification: " + itemKeySet, itemKeySet.isEmpty());
				}

			}
		}

	}

	// ----- private methods -----
	private static Map<String, List<String>> toMap(final String key, final String[] base, final String... elements) {


		final Map<String, List<String>> map = new LinkedHashMap<>();
		final List<String> list             = new LinkedList<>();

		list.addAll(Arrays.asList(base));
		list.addAll(Arrays.asList(elements));

		map.put(key, list);

		return map;
	}
}
