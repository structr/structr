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
package org.structr.test;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.bpmn.BpmnPageSkeletonGenerator;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.VisibilityMappingTraitDefinition;
import org.structr.web.entity.Widget;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.VisibilityMapping;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;
import org.structr.web.traits.definitions.WidgetTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * End-to-end behaviour of the page-skeleton generator behind the editor's "Create page
 * skeleton" button.
 *
 * <p>Fixture: {@code insurance-claim.bpmn} -- start event, two userTasks, a message
 * intermediate catch event, plus a service task, a script task, gateways, a boundary timer
 * and end events, so both selection and exclusion are exercised.</p>
 */
public class BpmnPageSkeletonTest extends AbstractProcessEngineTest {

	/** The human-facing steps of the fixture, in flow order: generated html id per step. */
	private static final List<String> EXPECTED_IDS = List.of(
		"start-claim-submitted",
		"task-initial-review",
		"task-manual-assessment",
		"event-wait-for-customer-response"
	);

	/** The same steps' bpmnIds, in the same order -- what each div's mapping binds to. */
	private static final List<String> EXPECTED_BPMN_IDS = List.of(
		"Start_1",
		"Task_InitialReview",
		"Task_ManualAssessment",
		"Event_WaitForResponse"
	);

	@Test
	public void testSkeletonStructureAndVisibilityMappings() {

		final String pageId;
		final String procUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			assertNotNull("fixture should have a process", procNode);

			final BpmnPageSkeletonGenerator.Result result = BpmnPageSkeletonGenerator.createSkeleton(
				app, securityContext, procNode.as(BpmnProcess.class), null);

			assertEquals("page name should be derived from the process name", "insurance-claim-handling", result.pageName());
			assertEquals("one div per human-facing step", EXPECTED_IDS.size(), result.stepCount());
			assertTrue("an unbound process should get the new page bound as its instance page", result.boundAsInstancePage());

			pageId   = result.pageId();
			procUuid = procNode.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			assertNotNull("page should exist", page);

			// page > html > body > wrapper
			final List<DOMNode> pageChildren = children(page);
			assertEquals("page should have a single html element", 1, pageChildren.size());

			final List<DOMNode> htmlChildren = children(pageChildren.get(0));
			assertEquals("html should have head and body", 2, htmlChildren.size());

			final List<DOMNode> bodyChildren = children(htmlChildren.get(1));
			assertEquals("body should hold a single process wrapper", 1, bodyChildren.size());

			final DOMNode wrapper = bodyChildren.get(0);
			assertEquals("bpmn-process sw-group", htmlClass(wrapper));
			assertEquals("insurance-claim-handling", htmlId(wrapper));

			// the running-instance metadata header is the wrapper's first child (visible only on an
			// instance page); the human-facing step divs follow it
			final DOMNode detailsBlock = children(wrapper).get(0);
			assertEquals("process-details", htmlId(detailsBlock));
			assertEquals("bpmn-process-details sw-card sw-card-content", htmlClass(detailsBlock));

			// one card per human-facing step, in flow order
			final List<DOMNode> stepDivs = humanStepDivs(wrapper);
			final List<String> actualIds = new ArrayList<>();

			assertEquals("one div per human-facing step", EXPECTED_IDS.size(), stepDivs.size());

			for (int i = 0; i < stepDivs.size(); i++) {

				final DOMNode div = stepDivs.get(i);

				actualIds.add(htmlId(div));

				// every step div leads with a visible heading, so a rendered-but-empty step is
				// distinguishable from one its mapping vetoed
				final DOMNode heading = children(div).get(0);
				assertEquals("h2", heading.getProperty(heading.getTraits().key(DOMElementTraitDefinition.TAG_PROPERTY)));
				assertEquals("bpmn-step-title sw-card-heading", htmlClass(heading));
				assertEquals("bpmn-step sw-card sw-card-content", htmlClass(div));

				// the heading reads the step's CURRENT name via localize(), so renaming the step
				// in the editor updates the page instead of leaving a stale literal behind
				final DOMNode headingText = children(heading).get(0);
				final String script       = headingText.getProperty(headingText.getTraits().key(ContentTraitDefinition.CONTENT_PROPERTY));
				// compact StructrScript, reading the binding off the visibilityMapping keyword and
				// falling back to the literal name so the heading never renders empty
				assertEquals("${localize(coalesce(visibilityMapping.boundStep.bpmnName, '" + div.getName() + "'))}", script);
				assertFalse("heading must not query for the step, was: " + script, script.contains("find("));
				assertFalse("heading must not walk the parent chain itself, was: " + script, script.contains(".parent"));

				// no form leaked: this run creates no form widget, so no step div may carry a
				// form (a ComponentConfiguration). Help-text paragraphs from imported
				// <documentation> / instructions are legitimate content, so we assert the
				// absence of a form rather than an exact child count.
				assertNull("no step may carry a form when no form widget was chosen: " + htmlId(div), formChildOf(div));
			}

			assertEquals("machine-side steps must be skipped and order must follow the flow", EXPECTED_IDS, actualIds);

			// each div is bound to its own step, with the states for that step's kind
			assertMappings(stepDivs.get(0), "Start_1", Set.of(VisibilityMappingTraitDefinition.STATE_NO_INSTANCE));
			assertMappings(stepDivs.get(1), "Task_InitialReview", Set.of(
				VisibilityMappingTraitDefinition.STATE_TASK_AVAILABLE,
				VisibilityMappingTraitDefinition.STATE_TASK_RESERVED_BY_ME));
			assertMappings(stepDivs.get(2), "Task_ManualAssessment", Set.of(
				VisibilityMappingTraitDefinition.STATE_TASK_AVAILABLE,
				VisibilityMappingTraitDefinition.STATE_TASK_RESERVED_BY_ME));
			assertMappings(stepDivs.get(3), "Event_WaitForResponse", Set.of(VisibilityMappingTraitDefinition.STATE_TOKEN_WAITING_HERE));

			// the descriptive name is the BPMN name, so the Pages tree reads like the model
			assertEquals("Initial review", stepDivs.get(1).getName());

			// binding landed on the process
			final NodeInterface procNode = app.getNodeById(procUuid);
			final NodeInterface bound    = procNode.getProperty(procNode.getTraits().key(BpmnProcessTraitDefinition.INSTANCE_PAGE_PROPERTY));
			assertNotNull("instance page should be bound", bound);
			assertEquals(pageId, bound.getUuid());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * Generating twice must not collide on the page name (page names route requests) and
	 * must not steal an instance-page binding the user already has.
	 */
	@Test
	public void testSecondRunGetsItsOwnPageAndKeepsTheExistingBinding() {

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process    = firstProcess(defNode).as(BpmnProcess.class);

			final BpmnPageSkeletonGenerator.Result first  = BpmnPageSkeletonGenerator.createSkeleton(app, securityContext, process, null);
			final BpmnPageSkeletonGenerator.Result second = BpmnPageSkeletonGenerator.createSkeleton(app, securityContext, process, null);

			assertEquals("insurance-claim-handling",   first.pageName());
			assertEquals("insurance-claim-handling-2", second.pageName());
			assertTrue("the first run binds the instance page", first.boundAsInstancePage());
			assertFalse("the second run must not re-bind", second.boundAsInstancePage());

			final NodeInterface bound = process.getProperty(process.getTraits().key(BpmnProcessTraitDefinition.INSTANCE_PAGE_PROPERTY));
			assertEquals("the existing binding must survive", first.pageId(), bound.getUuid());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
		}
	}

	/**
	 * With a page-template widget chosen, the page is built from the template's source and
	 * the step divs land in the node the template names "Main Content" -- not in a shell of
	 * our own, and not merely appended after the template's layout.
	 */
	@Test
	public void testSkeletonIsBuiltFromAPageTemplateWidget() {

		final String pageId;

		try (final Tx tx = app.tx()) {

			// a minimal page-template widget: full page shell with a content area
			final NodeInterface widget = app.create(StructrTraits.WIDGET, "Test Page Template");
			widget.setProperty(widget.getTraits().key(WidgetTraitDefinition.IS_PAGE_TEMPLATE_PROPERTY), true);
			// A widget's source names a node with the deployment instruction
			// @structr:name(...) (DeploymentCommentHandler), which is how page templates
			// mark their content area as "Main Content".
			widget.setProperty(widget.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<html><head><title>Template</title></head><body>"
				+ "<header id=\"branding\"></header>"
				+ "<!-- @structr:name(Main Content) --><main id=\"content\"></main>"
				+ "</body></html>");

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));

			pageId = BpmnPageSkeletonGenerator.createSkeleton(
				app, securityContext, firstProcess(defNode).as(BpmnProcess.class),
				BpmnPageSkeletonGenerator.humanFacingSteps(firstProcess(defNode).as(BpmnProcess.class)),
				null, widget.as(Widget.class)).pageId();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final List<DOMNode> html = children(page);
			assertEquals("the template should provide the page's single root", 1, html.size());

			// the template's own markup survived untouched
			final DOMNode body               = children(html.get(0)).get(1);
			final List<DOMNode> bodyChildren = children(body);
			assertEquals("body should still hold exactly the template's own children", 2, bodyChildren.size());
			assertEquals("branding", htmlId(bodyChildren.get(0)));

			// the wrapper went into the named content slot, not into the body
			final DOMNode mainContent = bodyChildren.get(1);
			assertEquals("content", htmlId(mainContent));
			assertEquals("Main Content", mainContent.getName());

			final List<DOMNode> slotChildren = children(mainContent);
			assertEquals("the slot should hold the process wrapper", 1, slotChildren.size());

			final DOMNode wrapper = slotChildren.get(0);
			assertEquals("bpmn-process sw-group", htmlClass(wrapper));
			assertEquals(EXPECTED_IDS.size(), humanStepDivs(wrapper).size());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * The skeleton carries a way to launch an instance: a named div with a button wired to the
	 * start-process action mapping, nested in the start event's div (which is already the
	 * no-instance partial) rather than duplicating that visibility rule.
	 */
	@Test
	public void testStartProcessPartialIsWiredToTheProcess() {

		final String pageId;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));

			pageId = BpmnPageSkeletonGenerator.createSkeleton(
				app, securityContext, firstProcess(defNode).as(BpmnProcess.class), null).pageId();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface page  = app.getNodeById(pageId);
			final DOMNode wrapper     = children(children(children(page).get(0)).get(1)).get(0);
			final DOMNode startDiv    = humanStepDivs(wrapper).get(0);

			assertEquals("the launch partial belongs to the start event's div", "start-claim-submitted", htmlId(startDiv));

			final List<DOMNode> startChildren = children(startDiv);
			assertEquals("start event div should hold its heading plus the launch partial", 2, startChildren.size());

			final DOMNode startPartial = startChildren.get(1);
			assertEquals("Start Process", startPartial.getName());
			assertEquals("start-process", htmlId(startPartial));

			// it inherits the start event div's no-instance rule instead of duplicating it
			final PropertyKey<Iterable<NodeInterface>> vmKey = startPartial.getTraits().key(DOMNodeTraitDefinition.VISIBILITY_MAPPINGS_PROPERTY);
			assertTrue("nested launch partial should have no visibility mappings of its own",
				collect(startPartial.getProperty(vmKey)).isEmpty());

			final DOMNode button = children(startPartial).get(0);
			assertEquals("button", button.getProperty(button.getTraits().key(DOMElementTraitDefinition.TAG_PROPERTY)));
			final DOMNode label = children(button).get(0);
			assertEquals("${localize('Start Process Insurance Claim Handling')}",
				label.getProperty(label.getTraits().key(ContentTraitDefinition.CONTENT_PROPERTY)));
			assertEquals("the button should carry the theme's button classes",
				"bpmn-start-process-button sw-button sw-button-primary", htmlClass(button));

			// the action mapping starts THIS process and navigates to the new instance
			final PropertyKey<Iterable<NodeInterface>> actionsKey = button.getTraits().key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY);
			final List<NodeInterface> actions                    = collect(button.getProperty(actionsKey));
			assertEquals("button should trigger exactly one action", 1, actions.size());

			final NodeInterface action = actions.get(0);
			final Traits amTraits      = action.getTraits();

			assertEquals("click",            action.getProperty(amTraits.key(ActionMappingTraitDefinition.EVENT_PROPERTY)));
			assertEquals("control-process",  action.getProperty(amTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY)));
			assertEquals("start",            action.getProperty(amTraits.key(ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY)));
			assertEquals("navigate-to-url",  action.getProperty(amTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY)));
			assertEquals("{result.url}",     action.getProperty(amTraits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY)));
			assertEquals("Process_ClaimHandling", action.getProperty(amTraits.key(ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_PROPERTY)));

			final NodeInterface controlled = action.getProperty(amTraits.key(ActionMappingTraitDefinition.CONTROLS_PROCESS_PROPERTY));
			assertNotNull("action mapping should point at the process", controlled);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * The launch partial has to actually render on the bare page, for anyone. Its
	 * {@code no-instance} mapping is therefore written WITHOUT a boundProcessId: with one, the
	 * predicate becomes "the current user has no active instance of this process", which is
	 * false for an anonymous visitor and false for a user who already started one -- in both
	 * cases the start button silently disappears.
	 */
	@Test
	public void testLaunchPartialRendersWithNoInstanceInContext() {

		final String pageId;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));

			pageId = BpmnPageSkeletonGenerator.createSkeleton(
				app, securityContext, firstProcess(defNode).as(BpmnProcess.class), null).pageId();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);
			final DOMNode startDiv   = humanStepDivs(wrapper).get(0);

			final PropertyKey<Iterable<NodeInterface>> vmKey = startDiv.getTraits().key(DOMNodeTraitDefinition.VISIBILITY_MAPPINGS_PROPERTY);
			final List<NodeInterface> mappings               = collect(startDiv.getProperty(vmKey));
			assertEquals("the start event div should carry exactly its no-instance mapping", 1, mappings.size());

			final NodeInterface mapping = mappings.get(0);
			final Traits vmTraits       = mapping.getTraits();

			assertEquals(VisibilityMappingTraitDefinition.STATE_NO_INSTANCE,
				mapping.getProperty(vmTraits.key(VisibilityMappingTraitDefinition.VISIBLE_WHEN_PROPERTY)));
			// evaluate() derives the id from the relationship, so BOTH have to be absent for the
			// context semantics to apply -- clearing only the string is not enough
			assertNull("a no-instance mapping must not carry boundProcessId, or it turns into a per-user query",
				mapping.getProperty(vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_ID_PROPERTY)));
			assertNull("a no-instance mapping must not bind a process, or evaluate() derives the id from it",
				mapping.getProperty(vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_PROPERTY)));

			// the STEP is bound though: no predicate reads it for no-instance, and the heading
			// script gets the step's live name from it
			assertNotNull("a no-instance mapping should still record its step",
				mapping.getProperty(vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY)));

			// the predicate itself: no instance in render context -> render
			assertTrue("the launch partial must be visible when the page has no instance in context",
				mapping.as(VisibilityMapping.class).evaluate(securityContext, null));

			// and a step-scoped mapping still carries the id, where it is needed to check that
			// a context instance belongs to this process
			final DOMNode taskDiv                       = humanStepDivs(wrapper).get(1);
			final List<NodeInterface> taskMappings      = collect(taskDiv.getProperty(taskDiv.getTraits().key(DOMNodeTraitDefinition.VISIBILITY_MAPPINGS_PROPERTY)));
			assertEquals("Process_ClaimHandling",
				taskMappings.get(0).getProperty(vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_ID_PROPERTY)));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * A form widget inserted for a user task must be bound to the STEP, not to a fixed type: at
	 * render time (ComponentConfigurationTraitWrapper) the form then binds to the `current`
	 * channel (the process instance's single subject) with the process's subjectType as its
	 * expected data type, and the field list is seeded from the step's own subjectFormView.
	 * Because the subject type is process-level, every user task gets a form; a non-user-task
	 * human step does not.
	 */
	@Test
	public void testSubjectFormIsBoundToTheStep() {

		final String procId;

		try (final Tx tx = app.tx()) {

			// a subject type with a form view, and a minimal form widget carrying a configuration
			final NodeInterface subject = app.create(StructrTraits.SCHEMA_NODE, "Claim");
			final NodeInterface view    = app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "form"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), subject),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "name")
			);
			assertNotNull("the form view should exist", view);

			final NodeInterface formWidget = app.create(StructrTraits.WIDGET, BpmnPageSkeletonGenerator.PROCESS_SUBJECT_FORM_WIDGET);
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<div data-structr-meta-name=\"Test Form\" config=\"{ displayMode: 'input' }\"></div>");
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.DIMENSIONS_PROPERTY), 1);

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process    = firstProcess(defNode).as(BpmnProcess.class);

			// the subject type is process-level (one subject per instance)
			process.setProperty(process.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY), "Claim");
			// this step narrows which fields it exposes
			final NodeInterface step = elementByBpmnId(firstProcess(defNode), "Task_InitialReview");
			step.setProperty(step.getTraits().key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY), "form");

			procId = process.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		final String pageId = generateSkeletonFor(procId);

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);

			// the step that declares a subject gets a form (alongside its heading and the
			// imported <documentation> help paragraph -- so the form is found by its
			// ComponentConfiguration, not a fixed child index)
			final DOMNode reviewDiv = humanStepDivs(wrapper).get(1);
			assertEquals("task-initial-review", htmlId(reviewDiv));

			final DOMNode form = formChildOf(reviewDiv);
			assertNotNull("the subject-bearing step should hold a form", form);
			final ComponentConfiguration config = form.getComponentConfiguration();
			assertNotNull("the inserted form should carry a component configuration", config);

			final Traits configTraits = config.getTraits();

			assertEquals("the form must be process-bound, or its data source stays a fixed type",
				ComponentConfigurationTraitDefinition.BINDING_MODE_PROCESS_BOUND,
				config.getProperty(configTraits.key(ComponentConfigurationTraitDefinition.BINDING_MODE_PROPERTY)));

			final NodeInterface boundTask = config.getProperty(configTraits.key(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY));
			assertNotNull("the form should be bound to the user task", boundTask);
			assertEquals("Task_InitialReview", boundTask.getProperty(boundTask.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));

			// the form binds to the current process instance (a single subject), not a
			// type-wide collection; the subject TYPE is the expected data type, both derived
			// at render time and not written into the page as literals
			assertEquals("channel:current", config.getDataSourceName());
			assertEquals("Claim", config.getExpectedDataType());

			// field list seeded from the step's form view
			assertEquals("name", config.getFieldSet());

			// the subject type is process-level, so EVERY user task gets a form -- not just the
			// one that set a form view. The second user task binds to the same process type.
			final DOMNode assessmentDiv = humanStepDivs(wrapper).get(2);
			assertEquals("task-manual-assessment", htmlId(assessmentDiv));

			final DOMNode assessmentForm = formChildOf(assessmentDiv);
			assertNotNull("every user task gets a form when the process declares a subject type", assessmentForm);
			assertEquals("all user-task forms bind to the current process instance",
				"channel:current", assessmentForm.getComponentConfiguration().getDataSourceName());
			assertEquals("all user-task forms of one process share the process-level subject type",
				"Claim", assessmentForm.getComponentConfiguration().getExpectedDataType());

			// a non-user-task human step (the message catch event) still gets no subject form
			final DOMNode waitDiv = humanStepDivs(wrapper).get(3);
			assertNull("a non-user-task human step must not get a subject form", formChildOf(waitDiv));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * When no bespoke "Process Subject Form" widget is installed, the generator falls back to the
	 * standard "Edit Form" widget (which ships with every widget set), so form generation works
	 * after a plain widget-set import instead of hard-failing on a missing bespoke widget.
	 */
	@Test
	public void testFallsBackToEditFormWidgetWhenBespokeIsAbsent() {

		final String procId;

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, "Claim");

			// ONLY the fallback widget exists -- no "Process Subject Form".
			final NodeInterface fallback = app.create(StructrTraits.WIDGET, BpmnPageSkeletonGenerator.FALLBACK_SUBJECT_FORM_WIDGET);
			fallback.setProperty(fallback.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<div data-structr-meta-name=\"Test Form\" config=\"{ displayMode: 'input' }\"></div>");
			fallback.setProperty(fallback.getTraits().key(WidgetTraitDefinition.DIMENSIONS_PROPERTY), 1);

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process   = firstProcess(defNode).as(BpmnProcess.class);
			process.setProperty(process.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY), "Claim");

			procId = process.getUuid();
			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		final String pageId = generateSkeletonFor(procId);

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);

			final DOMNode form = formChildOf(humanStepDivs(wrapper).get(1));
			assertNotNull("a subject form is inserted via the Edit Form fallback", form);
			assertEquals("fallback form binds to the current instance", "channel:current",
				form.getComponentConfiguration().getDataSourceName());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * The whole point of a subject form on a process page: submitting it must advance the
	 * process, not merely save the subject. The form widget ships a {@code create} submit
	 * (persists the subject and stops); the generator must rewrite it to the engine's
	 * {@code completeWithSubject} task operation, targeting {@code ${task.id}}.
	 *
	 * <p>Uses a widget whose source carries a real {@code <form>} with a
	 * {@code data-structr-meta-triggered-actions} attribute, so expansion produces an actual
	 * ActionMapping node -- which is what the retarget walks for. A form that never advances
	 * the process is the exact failure this pins.</p>
	 */
	@Test
	public void testSubjectFormSubmitAdvancesTheProcess() {

		final String procId;

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = app.create(StructrTraits.SCHEMA_NODE, "Claim");

			// a create form: the <form>'s submit persists the subject via action=create, exactly
			// like the real Create Form widget
			final NodeInterface formWidget = app.create(StructrTraits.WIDGET, BpmnPageSkeletonGenerator.PROCESS_SUBJECT_FORM_WIDGET);
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<div data-structr-meta-name=\"Test Create Form\" config=\"{ displayMode: 'input' }\">"
				+ "<form data-structr-meta-triggered-actions=\"{ type:'ActionMapping', event: 'submit', action: 'create', dataType: '${dataSource.dataType}', successBehaviour: 'component-based' }\"></form>"
				+ "</div>");
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.DIMENSIONS_PROPERTY), 1);

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process   = firstProcess(defNode).as(BpmnProcess.class);

			// subject type is process-level
			process.setProperty(process.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY), "Claim");

			procId = process.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		final String pageId = generateSkeletonFor(procId);

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);
			final DOMNode reviewDiv  = humanStepDivs(wrapper).get(1);

			// find the submit ActionMapping anywhere in the inserted form's subtree
			final NodeInterface mapping = findSubmitActionMapping(reviewDiv);
			assertNotNull("the form must carry a submit action mapping (else expansion did not create one)", mapping);

			final Traits amTraits = mapping.getTraits();

			assertEquals("the submit action must become a process control action",
				"control-process", mapping.getProperty(amTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY)));
			assertEquals("a create form completes the task by creating its subject",
				"completeWithSubject", mapping.getProperty(amTraits.key(ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY)));
			// idExpression is variable-replaced against the ActionMapping node, where $.task
			// cannot resolve; ${current.id} (the ProcessInstance) plus the step is what the
			// runtime uses to find the active task.
			assertEquals("the completion targets the ProcessInstance in render context",
				"${current.id}", mapping.getProperty(amTraits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY)));

			final NodeInterface targetStep = mapping.getProperty(amTraits.key(ActionMappingTraitDefinition.TARGETS_ELEMENT_PROPERTY));
			assertNotNull("the action must target the step so the runtime can resolve the task", targetStep);
			assertEquals("Task_InitialReview", targetStep.getProperty(targetStep.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));

			assertEquals("completeWithSubject needs a concrete subject type, not the widget's expression",
				"Claim", mapping.getProperty(amTraits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY)));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * A process that declares a subjectType while a step sets no subjectFormView must still get a
	 * form with the subject's own fields, taken from the {@code custom} view (every dynamically
	 * registered property lands there). The ComponentConfiguration's default field set is {@code name}, so
	 * without that fallback the generated form showed exactly one field.
	 */
	@Test
	public void testSubjectFormFallsBackToTheCustomViewWhenNoFormViewIsDeclared() {

		final String procId;

		try (final Tx tx = app.tx()) {

			// a subject type with two custom properties and NO explicit form view
			final NodeInterface subject = app.create(StructrTraits.SCHEMA_NODE, "Request");

			for (final String propertyName : List.of("amount", "reason")) {

				app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), propertyName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), subject)
				);
			}

			final NodeInterface formWidget = app.create(StructrTraits.WIDGET, BpmnPageSkeletonGenerator.PROCESS_SUBJECT_FORM_WIDGET);
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<div data-structr-meta-name=\"Fallback Form\" config=\"{ displayMode: 'input' }\"></div>");
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.DIMENSIONS_PROPERTY), 1);

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process   = firstProcess(defNode).as(BpmnProcess.class);

			// subject type is process-level; deliberately no per-step subjectFormView
			process.setProperty(process.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY), "Request");

			procId = process.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		final String pageId = generateSkeletonFor(procId);

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);
			final DOMNode form       = formChildOf(humanStepDivs(wrapper).get(1));

			assertNotNull("the inserted form should exist", form);
			final ComponentConfiguration config = form.getComponentConfiguration();
			assertNotNull("the inserted form should carry a component configuration", config);

			final String fieldSet = config.getFieldSet();
			assertNotNull("a form for a declared subject must get a field set", fieldSet);

			final List<String> fields = List.of(fieldSet.split(","));

			assertTrue("the subject's own properties should be seeded from the custom view, was: " + fieldSet,
				fields.contains("amount") && fields.contains("reason"));
			assertFalse("framework properties have no place in a generated form, was: " + fieldSet,
				fields.contains("type") || fields.contains("owner") || fields.contains("visibleToPublicUsers"));
			assertFalse("the default single-field set means the fallback did not run, was: " + fieldSet, fields.size() == 1 && fields.contains("name"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * The subject type is process-level, so a process that declares none must produce no forms at
	 * all -- not even on its user tasks. This is the negative half of the process-level contract:
	 * with per-element subjectType, a user task without one silently got no form; now the absence
	 * is a property of the whole process.
	 */
	@Test
	public void testNoProcessSubjectTypeMeansNoForms() {

		try (final Tx tx = app.tx()) {

			final NodeInterface formWidget = app.create(StructrTraits.WIDGET, BpmnPageSkeletonGenerator.PROCESS_SUBJECT_FORM_WIDGET);
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<div data-structr-meta-name=\"Test Form\" config=\"{ displayMode: 'input' }\"></div>");
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.DIMENSIONS_PROPERTY), 1);

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process   = firstProcess(defNode).as(BpmnProcess.class);
			// deliberately no subjectType on the process

			final BpmnPageSkeletonGenerator.Result result = BpmnPageSkeletonGenerator.createSkeleton(
				app, securityContext, process, BpmnPageSkeletonGenerator.humanFacingSteps(process), null, null);

			assertEquals("a process with no subject type must produce no forms", 0, result.formCount());

			final NodeInterface page = app.getNodeById(result.pageId());
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);
			final DOMNode reviewDiv  = humanStepDivs(wrapper).get(1);

			assertEquals("task-initial-review", htmlId(reviewDiv));
			assertNull("a user task gets no form when the process declares no subject", formChildOf(reviewDiv));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
		}
	}

	/**
	 * Hoisting the *type* to the process must NOT flatten the per-step field selection: two user
	 * tasks of one process, each with its own {@code subjectFormView}, must still render different
	 * field sets -- while both bind to the single process-level subject type. This is the whole
	 * reason the views stay on the element.
	 */
	@Test
	public void testPerStepFormViewsStillDiverge() {

		final String procId;

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = app.create(StructrTraits.SCHEMA_NODE, "Claim");

			for (final String propertyName : List.of("amount", "reason")) {

				app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), propertyName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), subject)
				);
			}

			// two views, one field each
			app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "reviewForm"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), subject),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "amount")
			);
			app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "assessForm"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), subject),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "reason")
			);

			final NodeInterface formWidget = app.create(StructrTraits.WIDGET, BpmnPageSkeletonGenerator.PROCESS_SUBJECT_FORM_WIDGET);
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.SOURCE_PROPERTY),
				"<div data-structr-meta-name=\"Test Form\" config=\"{ displayMode: 'input' }\"></div>");
			formWidget.setProperty(formWidget.getTraits().key(WidgetTraitDefinition.DIMENSIONS_PROPERTY), 1);

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/insurance-claim.bpmn"));
			final BpmnProcess process   = firstProcess(defNode).as(BpmnProcess.class);

			// one process-level type, two per-step field views
			process.setProperty(process.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY), "Claim");

			final NodeInterface review = elementByBpmnId(firstProcess(defNode), "Task_InitialReview");
			review.setProperty(review.getTraits().key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY), "reviewForm");

			final NodeInterface assess = elementByBpmnId(firstProcess(defNode), "Task_ManualAssessment");
			assess.setProperty(assess.getTraits().key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY), "assessForm");

			procId = process.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		final String pageId = generateSkeletonFor(procId);

		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.getNodeById(pageId);
			final DOMNode wrapper    = children(children(children(page).get(0)).get(1)).get(0);

			final DOMNode reviewForm = formChildOf(humanStepDivs(wrapper).get(1));
			final DOMNode assessForm = formChildOf(humanStepDivs(wrapper).get(2));
			assertNotNull("the review step should have a form",     reviewForm);
			assertNotNull("the assessment step should have a form", assessForm);

			final ComponentConfiguration reviewConfig = reviewForm.getComponentConfiguration();
			final ComponentConfiguration assessConfig = assessForm.getComponentConfiguration();
			assertNotNull("the review form should carry a configuration",     reviewConfig);
			assertNotNull("the assessment form should carry a configuration", assessConfig);

			assertEquals("the review step shows its own form view",     "amount", reviewConfig.getFieldSet());
			assertEquals("the assessment step shows its own form view", "reason", assessConfig.getFieldSet());

			// ...but both bind to the current process instance and expect the one
			// process-level subject type
			assertEquals("channel:current", reviewConfig.getDataSourceName());
			assertEquals("channel:current", assessConfig.getDataSourceName());
			assertEquals("Claim", reviewConfig.getExpectedDataType());
			assertEquals("Claim", assessConfig.getExpectedDataType());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	// ----- helpers -----

	/**
	 * Run the skeleton generator for the already-committed process {@code procId} in its own
	 * transaction, returning the generated page id.
	 *
	 * <p>Splitting generation from setup is essential: {@code findProcessSubjectForm} does an
	 * indexed {@code name()} lookup for the "Process Subject Form" widget, and an indexed query
	 * does not reliably return a node created in the same, still-open transaction. Committing the
	 * widget (and the subject schema types the field-set seeding needs) first makes the lookup
	 * deterministic -- which also matches production, where the widget already exists.</p>
	 */
	private String generateSkeletonFor(final String procId) {

		try (final Tx tx = app.tx()) {

			final BpmnProcess process = app.getNodeById(procId).as(BpmnProcess.class);
			final String pageId       = BpmnPageSkeletonGenerator.createSkeleton(
				app, securityContext, process, BpmnPageSkeletonGenerator.humanFacingSteps(process),
				null, null).pageId();

			tx.success();
			return pageId;

		} catch (final FrameworkException fex) {

			fail("Unexpected generation failure: " + fex.getMessage());
			return null;
		}
	}

	/**
	 * The inserted form inside {@code div}: its direct child carrying a ComponentConfiguration, or
	 * null when the div holds no form. Located by ComponentConfiguration rather than child index
	 * because a step div legitimately also carries help-text paragraphs (imported
	 * {@code <documentation>} / instructions), so the form is not at a fixed position.
	 */
	private DOMNode formChildOf(final DOMNode div) throws FrameworkException {

		for (final DOMNode child : children(div)) {

			if (child.getComponentConfiguration() != null) {

				return child;
			}
		}
		return null;
	}

	/**
	 * The wrapper's step divs: its children minus the leading process-details header block
	 * (created before the step loop, so it is deterministically the first child). Lets the
	 * step-structure assertions stay 0-based instead of accounting for the header everywhere.
	 */
	private List<DOMNode> humanStepDivs(final DOMNode wrapper) {

		final List<DOMNode> all = children(wrapper);
		assertFalse("wrapper should hold the details header plus step divs", all.isEmpty());
		return all.subList(1, all.size());
	}

	/** The first triggered ActionMapping found anywhere in {@code root}'s subtree, or null. */
	private NodeInterface findSubmitActionMapping(final DOMNode root) throws FrameworkException {

		final List<DOMNode> nodes = new ArrayList<>();
		nodes.add(root);
		for (final NodeInterface descendant : root.getAllChildNodes()) {
			if (descendant.is(StructrTraits.DOM_ELEMENT)) {
				nodes.add(descendant.as(DOMNode.class));
			}
		}

		for (final DOMNode node : nodes) {

			if (!node.getTraits().hasKey(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY)) {
				continue;
			}

			for (final NodeInterface mapping : collect(node.getProperty(node.getTraits().key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY)))) {
				return mapping;
			}
		}

		return null;
	}

	/** Assert the VisibilityMappings of {@code div}: bound step bpmnId and the set of states. */
	private void assertMappings(final DOMNode div, final String expectedStepBpmnId, final Set<String> expectedStates) throws FrameworkException {

		final Traits vmTraits                         = Traits.of(ProcessTraits.VISIBILITY_MAPPING);
		final PropertyKey<String> visibleWhenKey      = vmTraits.key(VisibilityMappingTraitDefinition.VISIBLE_WHEN_PROPERTY);
		final PropertyKey<NodeInterface> boundStepKey = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY);
		final PropertyKey<String> stepBpmnIdKey       = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_BPMN_ID_PROPERTY);
		final Set<String> states                      = new LinkedHashSet<>();

		final Iterable<NodeInterface> mappings = div.getProperty(div.getTraits().key(DOMNodeTraitDefinition.VISIBILITY_MAPPINGS_PROPERTY));
		assertNotNull("div " + htmlId(div) + " should have visibility mappings", mappings);

		for (final NodeInterface mapping : mappings) {

			states.add(mapping.getProperty(visibleWhenKey));

			final NodeInterface step = mapping.getProperty(boundStepKey);

			assertNotNull("mapping should be bound to a step", step);
			assertEquals("mapping bound to the wrong step",
				expectedStepBpmnId, step.getProperty(step.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));

			// the denormalized cache the importer's rewire pass reads
			assertEquals("boundStepBpmnId cache should match the bound step", expectedStepBpmnId, mapping.getProperty(stepBpmnIdKey));
		}

		assertEquals("states for " + htmlId(div), expectedStates, states);
	}

	private List<DOMNode> children(final NodeInterface node) {
		return Iterables.toList(node.as(DOMNode.class).getChildren());
	}

	private String htmlId(final NodeInterface element) {
		return element.getProperty(element.getTraits().key(DOMElementTraitDefinition._HTML_ID_PROPERTY));
	}

	private String htmlClass(final NodeInterface element) {
		return element.getProperty(element.getTraits().key(DOMElementTraitDefinition._HTML_CLASS_PROPERTY));
	}
}
