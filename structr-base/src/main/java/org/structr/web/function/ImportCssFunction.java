/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.CssRuleTraitDefinition;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class ImportCssFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_IMPORT_CSS    = "Usage: ${import_css(file)}. Example: ${import_css(cssFile)}";
	public static final String ERROR_MESSAGE_IMPORT_CSS_JS = "Usage: ${{Structr.importCss(file)}}. Example: ${{Structr.importCss(cssFile)}}";

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

		if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

			final File file = n.as(File.class);

			if (StorageProviderFactory.getStorageProvider(file).size() == 0) {
				return "";
			}

			try (final InputStreamReader reader = new InputStreamReader(StorageProviderFactory.getStorageProvider(file).getInputStream())) {

				logger.info("Parsing CSS from {}..", file.getName());

				final InputSource source       = new InputSource(reader);
				final CSSOMParser parser       = new CSSOMParser(new SACParserCSS3());
				final CSSStyleSheet styleSheet = parser.parseStyleSheet(source, null, null);
				final CSSRuleList rules        = styleSheet.getCssRules();
				int count                      = 0;

				logger.info("{} rules", styleSheet.getCssRules().getLength());

				for (int i=0; i<rules.getLength(); i++) {

					final CSSRule rule = rules.item(i);

					importCSSRule(rule);

					count++;
				}

				logger.info("{} rules imported", count);

				return true;

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
		final Traits traits          = Traits.of(StructrTraits.CSS_RULE);
		final App app                = StructrApp.getInstance();

		final NodeInterface existingRuleNode = app.nodeQuery(StructrTraits.CSS_RULE).name(selectorsString).getFirst();
		if (existingRuleNode != null) {
			return existingRuleNode;
		}

		// Create node for CSS rule
		final NodeInterface cssRuleNode = app.create(StructrTraits.CSS_RULE, selectorsString);

		cssRuleNode.setProperty(traits.key(CssRuleTraitDefinition.CSS_TEXT_PROPERTY), cssText);
		cssRuleNode.setProperty(traits.key(CssRuleTraitDefinition.RULE_TYPE_PROPERTY), Short.toUnsignedInt(rule.getType()));

		// Extract and link selectors
		final List<NodeInterface> cssSelectors = new LinkedList<>();
		final String[] selectors = StringUtils.split(selectorsString, ",");

		for (final String selector : selectors) {

			final NodeInterface cssSelectorNode = app.create(StructrTraits.CSS_SELECTOR, StringUtils.trim(selector));
			cssSelectors.add(cssSelectorNode);
		}

		cssRuleNode.setProperty(traits.key(CssRuleTraitDefinition.SELECTORS_PROPERTY), cssSelectors);

		// Extract and link declarations
		final List<NodeInterface> cssDeclarations = new LinkedList<>();
		final String declarationsString           = StringUtils.stripEnd(StringUtils.substringAfter(cssText, "{"), "}");
		final String[] declarations               = StringUtils.split(declarationsString, ";");

		for (final String declaration : declarations) {

			if (StringUtils.isNotBlank(declaration)) {

				final NodeInterface cssDeclarationNode = app.create(StructrTraits.CSS_DECLARATION, StringUtils.trim(declaration));
				cssDeclarations.add(cssDeclarationNode);
			}
		}

		// FIXME: where is "declarations" defined? (even in main)
		cssRuleNode.setProperty(traits.key("declarations"), cssDeclarations);

		// Import and link parent rule
		final CSSRule parentRule = rule.getParentRule();
		if (parentRule != null) {

			final NodeInterface parentRuleNode = importCSSRule(parentRule);
			cssRuleNode.setProperty(traits.key(CssRuleTraitDefinition.PARENT_RULE_PROPERTY), parentRuleNode);
		}

		return cssRuleNode;
	}
}
