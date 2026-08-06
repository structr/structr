/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.hamcrest.Matchers;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * Tests all visibility conditions evaluated by {@link DOMNode#shouldBeRendered(RenderContext)}.
 *
 * <p>The method checks four gates in order, short-circuiting on the first failure:</p>
 * <ol>
 *   <li>Admin edit-mode bypass (DEPLOYMENT / RAW / WIDGET → always render)</li>
 *   <li>{@code hidden} flag on the node</li>
 *   <li>Locale filtering via {@code showForLocales} / {@code hideForLocales}</li>
 *   <li>Script conditions via {@code showConditions} / {@code hideConditions}</li>
 *   <li>VisibilityMapping relationships (opt-in, only active when process module loaded)</li>
 * </ol>
 */
public class DOMNodeVisibilityTest extends StructrUiTest {

	// =========================================================================
	// Group 1: Default — no visibility flags set
	// =========================================================================

	@Test
	public void testDefaultRenderingWithNoFlagsSet() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("A clean element with no visibility flags should render by default", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 2: hidden flag
	// =========================================================================

	@Test
	public void testHiddenFlagPreventsRendering() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("hidden=true should prevent rendering", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHiddenFalseRendersNormally() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(false);

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("hidden=false should render normally", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 3: EditMode bypass (DEPLOYMENT / RAW / WIDGET)
	// =========================================================================

	@Test
	public void testDeploymentModeRendersHiddenNode() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);

			// DEPLOYMENT mode must render everything, including hidden nodes
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.DEPLOYMENT);

			assertTrue("DEPLOYMENT mode must bypass the hidden flag", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRawModeRendersHiddenNode() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.RAW);

			assertTrue("RAW mode must bypass the hidden flag", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testWidgetModeRendersHiddenNode() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.WIDGET);

			assertTrue("WIDGET mode must bypass the hidden flag", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testPreviewModeRespectsHiddenFlag() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);

			// PREVIEW is not in the bypass list — visibility rules still apply
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.PREVIEW);

			assertFalse("PREVIEW mode must respect the hidden flag", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testContentModeRespectsHiddenFlag() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);

			// CONTENT is not in the bypass list — visibility rules still apply
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.CONTENT);

			assertFalse("CONTENT mode must respect the hidden flag", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testDeploymentModeRendersNodeWithFailingLocale() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "de");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.DEPLOYMENT);
			ctx.setLocale(Locale.ENGLISH);   // locale "en" does not match showForLocales "de"

			assertTrue("DEPLOYMENT mode must bypass locale restrictions", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRawModeRendersNodeWithFailingCondition() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "false");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.RAW);

			assertTrue("RAW mode must bypass script conditions", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 4: Locale filtering (showForLocales / hideForLocales)
	// =========================================================================

	@Test
	public void testHideForLocalesMatchingCurrentLocale() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY), "en");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);  // "en" is in the hide list

			assertFalse("Node should not render when current locale matches hideForLocales", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideForLocalesNotMatchingCurrentLocale() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY), "de");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);  // "en" is NOT in the hide list "de"

			assertTrue("Node should render when current locale does not match hideForLocales", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowForLocalesMatchingCurrentLocale() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);  // "en" is in the show list

			assertTrue("Node should render when current locale matches showForLocales", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowForLocalesNotMatchingCurrentLocale() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.GERMAN);  // "de" is NOT in the show list "en"

			assertFalse("Node should not render when current locale does not match showForLocales", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowForLocalesWithNullLocale() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(null);  // no locale present

			assertFalse("Node should not render when showForLocales is set but no locale is available", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideForLocalesTakesPrecedenceOverShowForLocales() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			// Locale "en" appears in both lists — hide must win
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY), "en");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);

			assertFalse("hideForLocales must take precedence over showForLocales", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowForLocalesWithMultipleLocalesMatchingOne() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en, de, fr");

			final RenderContext ctxEn = makeRenderContext(RenderContext.EditMode.NONE);
			ctxEn.setLocale(Locale.ENGLISH);

			final RenderContext ctxDe = makeRenderContext(RenderContext.EditMode.NONE);
			ctxDe.setLocale(Locale.GERMAN);

			final RenderContext ctxJa = makeRenderContext(RenderContext.EditMode.NONE);
			ctxJa.setLocale(Locale.JAPANESE);

			assertTrue("Node should render when locale 'en' is in showForLocales list", div.shouldBeRendered(ctxEn));
			assertTrue("Node should render when locale 'de' is in showForLocales list", div.shouldBeRendered(ctxDe));
			assertFalse("Node should not render when locale 'ja' is not in showForLocales list", div.shouldBeRendered(ctxJa));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideForLocalesNonMatchShowRenderWithMatchingShow() {

		// hideForLocales does NOT match, showForLocales DOES match → should render
		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY), "de");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);  // "en": not hidden, in show list

			assertTrue("Node should render when locale is not in hide list and is in show list", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 5: Script conditions (showConditions / hideConditions)
	// Note: condition strings are wrapped in ${...} before evaluation, so
	// "true" becomes ${true}, "eq(1, 1)" becomes ${eq(1, 1)}, etc.
	// =========================================================================

	@Test
	public void testHideConditionTrue() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("Node should not render when hideConditions evaluates to true", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideConditionFalse() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "false");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("Node should render when hideConditions evaluates to false", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowConditionTrue() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("Node should render when showConditions evaluates to true", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowConditionFalse() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "false");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("Node should not render when showConditions evaluates to false", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideConditionTrueOverridesShowConditionTrue() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			// hide is checked first: true → return false immediately, show is never evaluated
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "true");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("hideConditions=true must take precedence over showConditions=true", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowConditionAppliedWhenHideConditionFalse() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "false");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("Node should render when hideConditions is false and showConditions is true", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowConditionFalsePreventsRenderingEvenWhenHideConditionFalse() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "false");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "false");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("showConditions=false must prevent rendering even when hideConditions is false", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideConditionWithScriptedTrueExpression() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(1, 1)");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("Scripted hideCondition that evaluates to true (eq(1,1)) should prevent rendering", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowConditionWithScriptedFalseExpression() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(1, 2)");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertFalse("Scripted showCondition that evaluates to false (eq(1,2)) should prevent rendering", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideConditionWithScriptedFalseExpression() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(1, 2)");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("Scripted hideCondition that evaluates to false (eq(1,2)) should allow rendering", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 6: VisibilityMappings
	// In the base test environment the process module is not loaded, so
	// StructrTraits.VISIBILITY_MAPPING is not registered and the gate always
	// returns true (opt-in semantics: no mappings → render).
	// =========================================================================

	@Test
	public void testNoVisibilityMappingsRendersNode() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			assertTrue("Node with no visibility mappings should render (opt-in default)", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 7: Combined scenarios
	// =========================================================================

	@Test
	public void testHiddenFlagTakesPriorityOverLocaleAndConditions() {

		// hidden=true must prevent rendering even when all other conditions would allow it
		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);

			assertFalse("hidden=true must prevent rendering regardless of locale and conditions", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAllVisibilityConditionsPassingRenders() {

		// Every gate configured to allow rendering → node renders
		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(false);
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "en");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "false");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);

			assertTrue("Node should render when every visibility condition is satisfied", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testLocaleFailurePreventsRenderingEvenWithPassingConditions() {

		// Locale gate fails → conditions are short-circuited
		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "de");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "true");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);
			ctx.setLocale(Locale.ENGLISH);  // "en" does not match showForLocales "de"

			assertFalse("Locale failure should prevent rendering even when showConditions is true", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testDeploymentModeBypassesAllConditionsSimultaneously() {

		// Verify the bypass works even when EVERY other gate would block rendering
		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setHidden(true);
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY), "de");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "true");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "false");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.DEPLOYMENT);
			ctx.setLocale(Locale.ENGLISH);

			assertTrue("DEPLOYMENT mode must bypass hidden flag, locale gate, and all conditions simultaneously", div.shouldBeRendered(ctx));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 8: Repeater context — shouldBeRendered with data object injected
	//
	// During rendering a repeater element calls ctx.putDataObject(dataKey, item)
	// for each iteration before evaluating the element's visibility conditions.
	// These tests replicate that setup manually so shouldBeRendered can be tested
	// in isolation without running the full rendering loop.
	// =========================================================================

	@Test
	public void testShowConditionMatchingRepeaterItemRenders() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.name, 'Alice')");

			final NodeInterface alice = app.create("TestOne", "Alice");
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", alice);

			assertTrue("Element should render when repeater item matches showConditions", div.shouldBeRendered(ctx));

			// no tx.success() → transaction rolls back, leaving no persisted data

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testShowConditionNonMatchingRepeaterItemDoesNotRender() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.name, 'Alice')");

			final NodeInterface bob = app.create("TestOne", "Bob");
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", bob);

			assertFalse("Element should not render when repeater item does not match showConditions", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideConditionMatchingRepeaterItemDoesNotRender() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(item.name, 'hidden')");

			final NodeInterface hidden = app.create("TestOne", "hidden");
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", hidden);

			assertFalse("Element should not render when repeater item matches hideConditions", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testHideConditionNonMatchingRepeaterItemRenders() {

		try (final Tx tx = app.tx()) {

			final Page    page = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div  = page.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(item.name, 'hidden')");

			final NodeInterface visible = app.create("TestOne", "visible");
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", visible);

			assertTrue("Element should render when repeater item does not match hideConditions", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRepeaterDataChangeProducesOppositeVisibilityPerIteration() {

		// Core repeater behaviour: the same element renders for some items and not others
		// depending on the condition. This test simulates two consecutive iterations.
		try (final Tx tx = app.tx()) {

			final Page    page    = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final NodeInterface showMe = app.create("TestOne", "show-me");
			final NodeInterface hideMe = app.create("TestOne", "hide-me");

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.name, 'show-me')");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			// iteration 1: item = showMe
			ctx.putDataObject("item", showMe);
			assertTrue("Element should render for the item that matches showConditions", div.shouldBeRendered(ctx));

			// iteration 2: item = hideMe — swapping the context object must flip the result
			ctx.putDataObject("item", hideMe);
			assertFalse("Element should not render for the item that does not match showConditions", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRepeaterHideConditionPerIteration() {

		try (final Tx tx = app.tx()) {

			final Page    page    = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final NodeInterface alice = app.create("TestOne", "Alice");
			final NodeInterface bob   = app.create("TestOne", "Bob");
			final NodeInterface carol = app.create("TestOne", "Carol");

			// hide only "Bob"
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(item.name, 'Bob')");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", alice);
			assertTrue("Alice should render (not in hideConditions)", div.shouldBeRendered(ctx));

			ctx.putDataObject("item", bob);
			assertFalse("Bob should not render (matches hideConditions)", div.shouldBeRendered(ctx));

			ctx.putDataObject("item", carol);
			assertTrue("Carol should render (not in hideConditions)", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRepeaterNumericPropertyInShowCondition() {

		try (final Tx tx = app.tx()) {

			final Page    page   = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final NodeInterface one = app.create("TestOne",
				new org.structr.core.graph.NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "one"),
				new org.structr.core.graph.NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1));
			final NodeInterface two = app.create("TestOne",
				new org.structr.core.graph.NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "two"),
				new org.structr.core.graph.NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2));

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.anInt, 1)");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", one);
			assertTrue("Element with anInt=1 should render", div.shouldBeRendered(ctx));

			ctx.putDataObject("item", two);
			assertFalse("Element with anInt=2 should not render", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testRepeaterItemWithBothShowAndHideConditions() {

		// Both conditions set; hide is checked first.
		// Item "conflict": hide=true → should not render regardless of show.
		// Item "Alice": hide=false, show=true → should render.
		// Item "other": hide=false, show=false → should not render.
		try (final Tx tx = app.tx()) {

			final Page    page    = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div     = page.getElementsByTagName("div").get(0);

			// hideConditions: hide "conflict"
			// showConditions: only show "Alice"
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(item.name, 'conflict')");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.name, 'Alice')");

			final NodeInterface alice    = app.create("TestOne", "Alice");
			final NodeInterface conflict = app.create("TestOne", "conflict");
			final NodeInterface other    = app.create("TestOne", "other");
			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.NONE);

			ctx.putDataObject("item", alice);
			assertTrue("Alice: hide=false, show=true → should render", div.shouldBeRendered(ctx));

			ctx.putDataObject("item", conflict);
			assertFalse("conflict: hide=true → should not render (hide takes priority)", div.shouldBeRendered(ctx));

			ctx.putDataObject("item", other);
			assertFalse("other: hide=false, show=false → should not render", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Group 9: Full rendering integration — repeater with conditions via HTTP
	//
	// These tests verify the complete pipeline: the rendering loop binds each
	// data object to the context before calling shouldBeRendered, so the
	// show/hide conditions correctly filter the repeated element in the HTML
	// output served by the HTTP endpoint.
	// =========================================================================

	@Test
	public void testRepeaterShowConditionFiltersHtmlOutput() {

		createAdminUser();

		// sorted alphabetically by the query below: Alice, Bob, Carol
		try (final Tx tx = app.tx()) {

			app.create("TestOne", "Alice");
			app.create("TestOne", "Bob");
			app.create("TestOne", "Carol");

			// Build page: the body div becomes the repeater
			final Page    page    = Page.createSimplePage(securityContext, "test-visibility-show");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final Content content = div.getFirstChild().as(Content.class);

			// Replace placeholder text with the repeater item's name
			content.setProperty(Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_PROPERTY), "${item.name}");

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('TestOne', sort('name'))");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "item");
			// Only Alice should pass
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.name, 'Alice')");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception during setup: " + fex.getMessage());
		}

		RestAssured.basePath = "/";

		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			// Only the "Alice" iteration should appear in the HTML
			.body("html.body.div",        Matchers.equalTo("Alice"))
			.when()
			.get("/html/test-visibility-show");
	}

	@Test
	public void testRepeaterHideConditionFiltersHtmlOutput() {

		createAdminUser();

		try (final Tx tx = app.tx()) {

			app.create("TestOne", "Alice");
			app.create("TestOne", "Bob");
			app.create("TestOne", "Carol");

			final Page    page    = Page.createSimplePage(securityContext, "test-visibility-hide");
			final DOMNode div     = page.getElementsByTagName("div").get(0);
			final Content content = div.getFirstChild().as(Content.class);

			content.setProperty(Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_PROPERTY), "${item.name}");

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('TestOne', sort('name'))");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "item");
			// Hide "Bob", keep Alice and Carol
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "eq(item.name, 'Bob')");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception during setup: " + fex.getMessage());
		}

		RestAssured.basePath = "/";

		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			// Alice and Carol render; Bob is hidden
			.body("html.body.div[0]",     Matchers.equalTo("Alice"))
			.body("html.body.div[1]",     Matchers.equalTo("Carol"))
			.body("html.body.div",        Matchers.not(Matchers.hasItem("Bob")))
			.when()
			.get("/html/test-visibility-hide");
	}

	@Test
	public void testRepeaterWithHiddenFlagIgnoredInDeploymentMode() {

		// Even if the repeater element itself has hidden=true, DEPLOYMENT mode renders it
		// for every iteration (the bypass check fires before the data-dependent conditions).
		try (final Tx tx = app.tx()) {

			final Page    page   = Page.createSimplePage(securityContext, "test-page");
			final DOMNode div    = page.getElementsByTagName("div").get(0);
			final NodeInterface item = app.create("TestOne", "data");

			div.setHidden(true);
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY), "eq(item.name, 'data')");

			final RenderContext ctx = makeRenderContext(RenderContext.EditMode.DEPLOYMENT);
			ctx.putDataObject("item", item);

			assertTrue("DEPLOYMENT mode must bypass hidden flag even on a repeater element", div.shouldBeRendered(ctx));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private RenderContext makeRenderContext(final RenderContext.EditMode editMode) {

		return new RenderContext(securityContext, new RequestMockUp(), new ResponseMockUp(), editMode);
	}

	// =========================================================================
	// Minimal HTTP mock implementations (same pattern as UiScriptingTest)
	// =========================================================================

	public class RequestMockUp implements HttpServletRequest {

		@Override public String getAuthType() { throw new UnsupportedOperationException(); }
		@Override public Cookie[] getCookies() { throw new UnsupportedOperationException(); }
		@Override public long getDateHeader(String name) { throw new UnsupportedOperationException(); }
		@Override public String getHeader(String name) { throw new UnsupportedOperationException(); }
		@Override public Enumeration<String> getHeaders(String name) { throw new UnsupportedOperationException(); }
		@Override public Enumeration<String> getHeaderNames() { throw new UnsupportedOperationException(); }
		@Override public int getIntHeader(String name) { throw new UnsupportedOperationException(); }
		@Override public String getMethod() { throw new UnsupportedOperationException(); }
		@Override public String getPathInfo() { throw new UnsupportedOperationException(); }
		@Override public String getPathTranslated() { throw new UnsupportedOperationException(); }
		@Override public String getContextPath() { throw new UnsupportedOperationException(); }
		@Override public String getQueryString() { throw new UnsupportedOperationException(); }
		@Override public String getRemoteUser() { throw new UnsupportedOperationException(); }
		@Override public boolean isUserInRole(String role) { throw new UnsupportedOperationException(); }
		@Override public java.security.Principal getUserPrincipal() { throw new UnsupportedOperationException(); }
		@Override public String getRequestedSessionId() { throw new UnsupportedOperationException(); }
		@Override public String getRequestURI() { throw new UnsupportedOperationException(); }
		@Override public StringBuffer getRequestURL() { throw new UnsupportedOperationException(); }
		@Override public String getServletPath() { throw new UnsupportedOperationException(); }
		@Override public HttpSession getSession(boolean create) { throw new UnsupportedOperationException(); }
		@Override public HttpSession getSession() { throw new UnsupportedOperationException(); }
		@Override public String changeSessionId() { throw new UnsupportedOperationException(); }
		@Override public boolean isRequestedSessionIdValid() { throw new UnsupportedOperationException(); }
		@Override public boolean isRequestedSessionIdFromCookie() { throw new UnsupportedOperationException(); }
		@Override public boolean isRequestedSessionIdFromURL() { throw new UnsupportedOperationException(); }
		@Override public boolean authenticate(HttpServletResponse response) throws IOException, ServletException { return false; }
		@Override public void login(String username, String password) throws ServletException { throw new UnsupportedOperationException(); }
		@Override public void logout() throws ServletException { throw new UnsupportedOperationException(); }
		@Override public Collection<Part> getParts() throws IOException, ServletException { throw new UnsupportedOperationException(); }
		@Override public Part getPart(String name) throws IOException, ServletException { throw new UnsupportedOperationException(); }
		@Override public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException { throw new UnsupportedOperationException(); }
		@Override public Object getAttribute(String name) { throw new UnsupportedOperationException(); }
		@Override public Enumeration<String> getAttributeNames() { throw new UnsupportedOperationException(); }
		@Override public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
		@Override public void setCharacterEncoding(String s) throws UnsupportedEncodingException {}
		@Override public int getContentLength() { throw new UnsupportedOperationException(); }
		@Override public long getContentLengthLong() { throw new UnsupportedOperationException(); }
		@Override public String getContentType() { throw new UnsupportedOperationException(); }
		@Override public ServletInputStream getInputStream() throws IOException { throw new UnsupportedOperationException(); }
		@Override public String getParameter(String s) { return null; }
		@Override public Enumeration<String> getParameterNames() { throw new UnsupportedOperationException(); }
		@Override public String[] getParameterValues(String name) { throw new UnsupportedOperationException(); }
		@Override public Map<String, String[]> getParameterMap() { return new HashMap<>(); }
		@Override public String getProtocol() { throw new UnsupportedOperationException(); }
		@Override public String getScheme() { throw new UnsupportedOperationException(); }
		@Override public String getServerName() { return "localhost"; }
		@Override public int getServerPort() { return 12345; }
		@Override public BufferedReader getReader() throws IOException { throw new UnsupportedOperationException(); }
		@Override public String getRemoteAddr() { throw new UnsupportedOperationException(); }
		@Override public String getRemoteHost() { throw new UnsupportedOperationException(); }
		@Override public void setAttribute(String name, Object o) { throw new UnsupportedOperationException(); }
		@Override public void removeAttribute(String name) { throw new UnsupportedOperationException(); }
		@Override public Locale getLocale() { throw new UnsupportedOperationException(); }
		@Override public Enumeration<Locale> getLocales() { throw new UnsupportedOperationException(); }
		@Override public boolean isSecure() { throw new UnsupportedOperationException(); }
		@Override public RequestDispatcher getRequestDispatcher(String path) { throw new UnsupportedOperationException(); }
		@Override public int getRemotePort() { throw new UnsupportedOperationException(); }
		@Override public String getLocalName() { throw new UnsupportedOperationException(); }
		@Override public String getLocalAddr() { return "127.0.0.1"; }
		@Override public int getLocalPort() { return 12345; }
		@Override public ServletContext getServletContext() { throw new UnsupportedOperationException(); }
		@Override public AsyncContext startAsync() throws IllegalStateException { throw new UnsupportedOperationException(); }
		@Override public AsyncContext startAsync(ServletRequest rq, ServletResponse rs) throws IllegalStateException { throw new UnsupportedOperationException(); }
		@Override public boolean isAsyncStarted() { throw new UnsupportedOperationException(); }
		@Override public boolean isAsyncSupported() { throw new UnsupportedOperationException(); }
		@Override public AsyncContext getAsyncContext() { throw new UnsupportedOperationException(); }
		@Override public DispatcherType getDispatcherType() { throw new UnsupportedOperationException(); }
		@Override public String getRequestId() { throw new UnsupportedOperationException(); }
		@Override public String getProtocolRequestId() { throw new UnsupportedOperationException(); }
		@Override public ServletConnection getServletConnection() { throw new UnsupportedOperationException(); }
	}

	public class ResponseMockUp implements HttpServletResponse {

		@Override public void addCookie(Cookie cookie) { throw new UnsupportedOperationException(); }
		@Override public boolean containsHeader(String name) { throw new UnsupportedOperationException(); }
		@Override public String encodeURL(String url) { throw new UnsupportedOperationException(); }
		@Override public String encodeRedirectURL(String url) { throw new UnsupportedOperationException(); }
		@Override public void sendError(int sc, String msg) throws IOException { throw new UnsupportedOperationException(); }
		@Override public void sendError(int sc) throws IOException { throw new UnsupportedOperationException(); }
		@Override public void sendRedirect(String location) throws IOException { throw new UnsupportedOperationException(); }
		@Override public void setDateHeader(String name, long date) { throw new UnsupportedOperationException(); }
		@Override public void addDateHeader(String name, long date) { throw new UnsupportedOperationException(); }
		@Override public void setHeader(String name, String value) { throw new UnsupportedOperationException(); }
		@Override public void addHeader(String name, String value) { throw new UnsupportedOperationException(); }
		@Override public void setIntHeader(String name, int value) { throw new UnsupportedOperationException(); }
		@Override public void addIntHeader(String name, int value) { throw new UnsupportedOperationException(); }
		@Override public void setStatus(int sc) { throw new UnsupportedOperationException(); }
		@Override public int getStatus() { throw new UnsupportedOperationException(); }
		@Override public String getHeader(String name) { throw new UnsupportedOperationException(); }
		@Override public Collection<String> getHeaders(String name) { throw new UnsupportedOperationException(); }
		@Override public Collection<String> getHeaderNames() { throw new UnsupportedOperationException(); }
		@Override public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
		@Override public String getContentType() { throw new UnsupportedOperationException(); }
		@Override public ServletOutputStream getOutputStream() throws IOException { throw new UnsupportedOperationException(); }
		@Override public PrintWriter getWriter() throws IOException { throw new UnsupportedOperationException(); }
		@Override public void setCharacterEncoding(String charset) { throw new UnsupportedOperationException(); }
		@Override public void setContentLength(int len) { throw new UnsupportedOperationException(); }
		@Override public void setContentLengthLong(long len) { throw new UnsupportedOperationException(); }
		@Override public void setContentType(String type) { throw new UnsupportedOperationException(); }
		@Override public void setBufferSize(int size) { throw new UnsupportedOperationException(); }
		@Override public int getBufferSize() { throw new UnsupportedOperationException(); }
		@Override public void flushBuffer() throws IOException { throw new UnsupportedOperationException(); }
		@Override public void resetBuffer() { throw new UnsupportedOperationException(); }
		@Override public boolean isCommitted() { throw new UnsupportedOperationException(); }
		@Override public void reset() { throw new UnsupportedOperationException(); }
		@Override public void setLocale(Locale loc) { throw new UnsupportedOperationException(); }
		@Override public Locale getLocale() { throw new UnsupportedOperationException(); }
	}
}
