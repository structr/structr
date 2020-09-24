/*
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
package org.structr.web.function;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.internal.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Parser;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ImportCssFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_IMPORT_CSS    = "Usage: ${import_css(file)}. Example: ${import_css(cssFile)}";
	public static final String ERROR_MESSAGE_IMPORT_CSS_JS = "Usage: ${{Structr.importCss(file)}}. Example: ${{Structr.importCss(cssFile)}}";

	Class cssSelectorClass;
	Class cssRuleClass;
	Class cssSemanticClassClass;
	Class cssDeclarationClass;
	App   app;

	@Override
	public String getName() {
		return "import_css";
	}

	@Override
	public String getSignature() {
		return "file";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

		if (sources[0] instanceof File) {

			final File file = (File) sources[0];

			if (file.getSize() == 0) {
				return "";
			}

			try {

				cssSelectorClass      = StructrApp.getConfiguration().getNodeEntityClass("CssSelector");
				cssRuleClass          = StructrApp.getConfiguration().getNodeEntityClass("CssRule");
				cssSemanticClassClass = StructrApp.getConfiguration().getNodeEntityClass("CssSemanticClass");
				cssDeclarationClass   = StructrApp.getConfiguration().getNodeEntityClass("CssDeclaration");

				app = StructrApp.getInstance();

				final CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
				final CSSStyleSheet styleSheet = parser.parseStyleSheet(new InputSource(new FileReader(file.getFileOnDisk())), null, null);

				final CSSRuleList rules = styleSheet.getCssRules();

				for (int i=0; i<rules.getLength(); i++) {

					final CSSRule rule = rules.item(i);

					importCSSRule(rule);

				}

			} catch (final Exception e) {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_IMPORT_CSS_JS : ERROR_MESSAGE_IMPORT_CSS);
	}

	@Override
	public String shortDescription() {
		return "Imports CSS classes, media queries etc. from given CSS file.";
	}

	private NodeInterface importCSSRule(final CSSRule rule) throws FrameworkException {

		// Check if rule already exists and skip if yes
		final String cssText         = rule.getCssText();
		final String selectorsString = StringUtils.trim(StringUtils.substringBefore(cssText, "{"));

		final NodeInterface existingRuleNode = (NodeInterface) app.nodeQuery(cssRuleClass).andName(selectorsString).getFirst();
		if (existingRuleNode != null) {
			return existingRuleNode;
		}

		// Create node for CSS rule
		final NodeInterface cssRuleNode = app.create(cssRuleClass, selectorsString);

		cssRuleNode.setProperty(StructrApp.key(cssRuleClass,"cssText"), cssText);
		cssRuleNode.setProperty(StructrApp.key(cssRuleClass,"ruleType"), Short.toUnsignedInt(rule.getType()));

		// Extract and link selectors
		final List<NodeInterface> cssSelectors = new LinkedList<>();
		final String[] selectors = StringUtils.split(selectorsString, ",");

		for (final String selector : selectors) {

			final NodeInterface cssSelectorNode = app.create(cssSelectorClass, StringUtils.trim(selector));
			cssSelectors.add(cssSelectorNode);
		}

		cssRuleNode.setProperty(StructrApp.key(cssRuleClass,"selectors"), cssSelectors);

		// Extract and link declarations
		final List<NodeInterface> cssDeclarations = new LinkedList<>();
		final String declarationsString           = StringUtils.stripEnd(StringUtils.substringAfter(cssText, "{"), "}");
		final String[] declarations               = StringUtils.split(declarationsString, ";");

		for (final String declaration : declarations) {

			if (StringUtils.isNotBlank(declaration)) {

				final NodeInterface cssDeclarationNode = app.create(cssDeclarationClass, StringUtils.trim(declaration));
				cssDeclarations.add(cssDeclarationNode);
			}
		}

		cssRuleNode.setProperty(StructrApp.key(cssRuleClass,"declarations"), cssDeclarations);

		// Import and link parent rule
		CSSRule parentRule = rule.getParentRule();
		if (parentRule != null) {

			final NodeInterface parentRuleNode = importCSSRule(parentRule);
			cssRuleNode.setProperty(StructrApp.key(cssRuleClass,"parentRule"), parentRuleNode);
		}

		return cssRuleNode;
	}
}
