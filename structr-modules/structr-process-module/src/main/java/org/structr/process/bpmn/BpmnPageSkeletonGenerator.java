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
package org.structr.process.bpmn;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.app.App;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.VisibilityMappingTraitDefinition;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.websocket.command.WidgetAutoVisibilityMappingHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Generates a page skeleton for a BpmnProcess: one {@code <div>} per step that needs a
 * human, in flow order, each bound to its step by a VisibilityMapping so it only renders when
 * that step is actionable.
 *
 * <p>Peer of {@link BpmnImporter} / {@link BpmnExporter}: the graph work lives here so it
 * is reachable from anywhere (websocket command, scripting, a maintenance command) and
 * testable without a transport. {@code BpmnPageSkeletonCommand} is the thin editor-facing
 * wrapper.</p>
 *
 * <h3>Which steps get a div</h3>
 * <table>
 *   <tr><th>Element</th><th>visibleWhen</th><th>Why</th></tr>
 *   <tr><td>{@code userTask}</td><td>{@code task-available}, {@code task-reserved-by-me}</td>
 *       <td>The only type the engine creates a TaskInstance for, so the only type where the
 *           task-* states can match at all. The two mappings OR-combine to "this step is
 *           mine to act on, claimed or not".</td></tr>
 *   <tr><td>{@code startEvent}</td><td>{@code no-instance}</td>
 *       <td>The launch partial: renders when the page has no instance in its render context.
 *           Bound to its step (the heading reads its name from there) but deliberately to NO
 *           process -- binding a process changes what no-instance means; see
 *           {@code createVisibilityMapping}.</td></tr>
 *   <tr><td>{@code manualTask}</td><td>{@code process-awaiting-action}</td>
 *       <td>Work performed outside the system. The engine passes straight through these and
 *           never creates a task, so a task-* mapping would never match; the instance-level
 *           "running, nothing for you to do" state is the closest honest predicate.</td></tr>
 *   <tr><td>{@code intermediateCatchEvent} with a message definition</td>
 *       <td>{@code process-awaiting-action}</td>
 *       <td>Waiting on an external message. Same reasoning as manualTask; timer and other
 *           catch events are not human-facing and are skipped.</td></tr>
 * </table>
 *
 * <p>Everything else -- service/script tasks, gateways, end events, boundary timers -- is
 * machine-side and gets no div.</p>
 *
 * <p>Each step div holds an {@code <h2>} whose text is a script reading the step's current name
 * through {@code localize()}, reached via the div's own VisibilityMapping rather than a query, so
 * renaming a step in the editor updates the page and the label can be translated. A truly empty
 * div renders as nothing, which makes "this step is visible" indistinguishable from "its mapping
 * vetoed it" when looking at the page; the heading is scaffolding to be replaced by real
 * content.</p>
 *
 * <p>Generated markup carries both {@code bpmn-*} marker classes and the widget theme's
 * classes ({@code sw-card}, {@code sw-card-heading}, {@code sw-button sw-button-primary} --
 * see {@code structr/themes/style.css}), so a page built from a page-template widget is
 * styled on arrival. On a bare shell, which has no stylesheet, the theme classes are inert.</p>
 *
 * <h3>Launching an instance</h3>
 * <p>On top of the step divs the skeleton gets one named "Start Process" div holding a button
 * wired to the start-process Event Action Mapping, with a navigate-to-{@code {result.url}}
 * follow-up so the click lands on the new instance's page. It nests inside the start event's
 * div when the process has one -- that div is already the {@code no-instance} partial -- and
 * otherwise sits at the top of the wrapper with a {@code no-instance} mapping of its own.</p>
 *
 * <p>Note that the Pages area's own Step picker (pages.js) currently offers a narrower set
 * (userTask and any intermediateCatchEvent), so a mapping generated for a startEvent or a
 * manualTask cannot be re-pointed there yet.</p>
 *
 * <h3>Ordering</h3>
 * <p>Flow order: breadth-first from the process's start events along {@code outgoingFlows},
 * descending into sub-processes when one is reached (the importer attaches sub-process
 * children via {@code parentElement}, not {@code process}, so they are unreachable
 * otherwise). Human-facing steps the walk never reaches -- disconnected fragments, or
 * children of a container without a start event -- are appended by {@code bpmnId} rather
 * than dropped.</p>
 *
 * <p>Re-running produces an ADDITIONAL page (names take a numeric suffix) and never touches
 * an existing binding: the skeleton is a starting point the user then edits, and
 * regenerating in place would throw those edits away.</p>
 */
public class BpmnPageSkeletonGenerator {

	private static final Logger logger = LoggerFactory.getLogger(BpmnPageSkeletonGenerator.class);

	/**
	 * The node name page-template widgets use to mark their content area. Matched
	 * case-insensitively on the trimmed name, since it is authored by hand in the widget.
	 */
	private static final String MAIN_CONTENT_SLOT = "Main Content";

	/**
	 * The value {@code BpmnElement.eventDefinitionType} carries for a message event: the
	 * BPMN element's full local name, which is what BpmnImporter stores (its
	 * EVENT_DEFINITION_TYPES are {@code timerEventDefinition}, {@code messageEventDefinition},
	 * ...) and what BpmnExporter compares against -- not the bare word "message".
	 */
	private static final String MESSAGE_EVENT_DEFINITION = "messageEventDefinition";

	/**
	 * Name of the canonical form Widget inserted for a subject-bearing user task. Provided
	 * externally in the widget set; the generator only looks it up and configures it per step.
	 */
	public static final String PROCESS_SUBJECT_FORM_WIDGET = "Process Subject Form";

	/** What a scaffolded step div looks like: its id prefix and the states it renders in. */
	private record StepKind(String idPrefix, Set<String> visibleWhen) {}

	/**
	 * The human-facing element types, and what their div gets. Single source of truth: a
	 * type is human-facing iff it is a key here, so a new entry can't end up with an id
	 * prefix but no states (a div that never renders) or the reverse.
	 */
	private static final Map<BpmnElementType, StepKind> HUMAN_STEPS = Map.of(

		BpmnElementType.USER_TASK, new StepKind("task-", Set.of(
			VisibilityMappingTraitDefinition.STATE_TASK_AVAILABLE,
			VisibilityMappingTraitDefinition.STATE_TASK_RESERVED_BY_ME)),

		BpmnElementType.START_EVENT, new StepKind("start-", Set.of(
			VisibilityMappingTraitDefinition.STATE_NO_INSTANCE)),

		BpmnElementType.MANUAL_TASK, new StepKind("manual-", Set.of(
			VisibilityMappingTraitDefinition.STATE_PROCESS_AWAITING_ACTION)),

		BpmnElementType.INTERMEDIATE_CATCH_EVENT, new StepKind("event-", Set.of(
			VisibilityMappingTraitDefinition.STATE_PROCESS_AWAITING_ACTION))
	);

	/**
	 * What {@link #createSkeleton} produced, for the caller's reply and for tests.
	 *
	 * @param formCount           forms inserted (0 when no form widget was chosen)
	 * @param stepsMissingSubject user-task steps that got no form because they declare no
	 *                            {@code subjectType} -- reported so "nothing happened" is
	 *                            explainable instead of silent
	 */
	public record Result(String pageId, String pageName, int stepCount, boolean boundAsInstancePage,
	                     int formCount, int stepsMissingSubject) {}

	/**
	 * Human-facing steps of {@code process} in flow order. Exposed so a caller can check
	 * for "nothing to scaffold" without the generation walking the graph a second time.
	 */
	public static List<BpmnElement> humanFacingSteps(final BpmnProcess process) {

		final List<BpmnElement> topLevel = Iterables.toList(process.getElements());
		final List<BpmnElement> ordered  = new ArrayList<>();
		final Set<String> visited        = new HashSet<>();
		final Queue<BpmnElement> queue   = new ArrayDeque<>(startEventsOf(topLevel));

		while (!queue.isEmpty()) {

			final BpmnElement current = queue.remove();
			if (!visited.add(current.getUuid())) {

				continue;
			}

			final BpmnElementType type = current.getElementType();

			if (isHumanFacing(current, type)) {

				ordered.add(current);
			}

			if (type.isSubProcessLike()) {

				queue.addAll(startEventsOf(Iterables.toList(current.getChildElements())));
			}

			for (final BpmnSequenceFlow flow : current.getOutgoingFlows()) {

				final BpmnElement target = flow.getTargetElement();
				if (target != null) {

					queue.add(target);
				}
			}
		}

		final List<BpmnElement> unreached = new ArrayList<>();

		for (final BpmnElement element : allElements(topLevel)) {

			if (!visited.contains(element.getUuid()) && isHumanFacing(element, element.getElementType())) {

				unreached.add(element);
			}
		}

		unreached.sort(Comparator.comparing(e -> StringUtils.defaultString(e.getBpmnId())));
		ordered.addAll(unreached);

		return ordered;
	}

	/** Convenience entry point that determines the steps itself. */
	public static Result createSkeleton(final App app, final SecurityContext securityContext, final BpmnProcess process, final String requestedName) throws FrameworkException {

		return createSkeleton(app, securityContext, process, humanFacingSteps(process), requestedName, null);
	}

	/**
	 * Build the skeleton page for {@code process} from the given steps and return what was
	 * created. Runs in the caller's transaction.
	 *
	 * <p>Every user-task step that declares a {@code subjectType} gets the canonical
	 * {@value #PROCESS_SUBJECT_FORM_WIDGET} widget, looked up by name and configured for that
	 * step (bound data source, seeded fields, a submit that completes the task). No widget is
	 * chosen per page: the subject declaration is the opt-in, and the form is always the same
	 * one. If the widget is not present in the database, forms are skipped and a warning is
	 * logged.</p>
	 *
	 * @param requestedName  page name to use, or null to derive one from the process name
	 * @param templateWidget page-template Widget to build the page from, or null for a bare
	 *                       {@code html/head/body} shell
	 */
	public static Result createSkeleton(final App app, final SecurityContext securityContext, final BpmnProcess process,
	                                    final List<BpmnElement> steps, final String requestedName, final Widget templateWidget) throws FrameworkException {

		final Widget formWidget = findProcessSubjectForm(app);

		final String processLabel = processLabel(process);
		final String processSlug  = slug(processLabel, "process");
		final String pageName     = firstFree(StringUtils.isNotEmpty(requestedName) ? requestedName : processSlug, name -> pageExists(app, name));

		final Page page          = Page.createNewPage(securityContext, pageName);
		final DOMNode stepParent = templateWidget != null
			? expandTemplate(app, page, templateWidget)
			: createDefaultShell(page);

		final DOMElement wrapper = page.createElement("div");

		stepParent.appendChild(wrapper);
		describe(wrapper, processLabel, processSlug, "bpmn-process");

		// boundProcessId caches the BPMN processId, which is process-level: read once.
		final String bpmnProcessId  = StringUtils.defaultIfEmpty(process.getProcessId(), process.getUuid());
		final Set<String> usedIds   = new HashSet<>();
		DOMElement startEventDiv    = null;
		int formCount               = 0;
		int stepsMissingSubject     = 0;

		for (final BpmnElement step : steps) {

			final StepKind kind  = HUMAN_STEPS.get(step.getElementType());
			final String bpmnId  = step.getBpmnId();
			final String label   = StringUtils.defaultIfEmpty(step.getBpmnName(), bpmnId);
			final String htmlId  = firstFree(kind.idPrefix() + slug(label, bpmnId), usedIds::contains);
			final DOMElement div = page.createElement("div");

			wrapper.appendChild(div);
			describe(div, label, htmlId, "bpmn-step sw-card");
			usedIds.add(htmlId);

			// A heading rather than an empty div: an empty div renders as nothing, so there is
			// no way to tell "this step is visible" from "the mapping vetoed it" by looking at
			// the page. Delete it once the step has real content.
			final DOMElement heading = page.createElement("h2");

			div.appendChild(heading);
			heading.appendChild(page.createTextNode(stepHeadingScript(label)));
			final String headingId = firstFree(htmlId + "-heading", usedIds::contains);

			describe(heading, label + " (heading)", headingId, "bpmn-step-title sw-card-heading");
			usedIds.add(headingId);

			// what the modeller wrote for the human doing this step: instructions first (declared
			// for exactly this purpose), then the BPMN <documentation> as secondary help. Both
			// are imported and were previously dropped on the floor.
			appendHelpText(page, div, step, htmlId, usedIds);

			// the subject form: every user task that declares a subject gets the canonical form
			if (formWidget != null && step.isType(BpmnElementType.USER_TASK)) {

				if (insertSubjectForm(app, page, div, formWidget, step)) {

					formCount++;

				} else {

					stepsMissingSubject++;
				}
			}

			for (final String state : kind.visibleWhen()) {

				createVisibilityMapping(app, div, process, step, state, bpmnProcessId);
			}

			if (startEventDiv == null && step.isType(BpmnElementType.START_EVENT)) {

				startEventDiv = div;
			}
		}

		createStartProcessPartial(app, page, wrapper, startEventDiv, process, processLabel, bpmnProcessId, usedIds);

		// Bind as instance page only when nothing is bound yet: replacing an existing
		// binding is the user's call, in the editor's Instance page picker.
		final PropertyKey<NodeInterface> instancePageKey = process.getTraits().key(BpmnProcessTraitDefinition.INSTANCE_PAGE_PROPERTY);
		final boolean bindAsInstancePage                 = process.getProperty(instancePageKey) == null;

		if (bindAsInstancePage) {

			process.setProperty(instancePageKey, page);
		}

		logger.info("BPMN page skeleton: created page '{}' with {} step div(s) and {} form(s) for process '{}'{}{}",
			pageName, steps.size(), formCount, processLabel,
			stepsMissingSubject > 0 ? " (" + stepsMissingSubject + " user task(s) declare no subjectType)" : "",
			bindAsInstancePage ? " (bound as instance page)" : "");

		return new Result(page.getUuid(), pageName, steps.size(), bindAsInstancePage, formCount, stepsMissingSubject);
	}

	// ----- step selection -----

	/**
	 * True if this element needs a human. Catch events additionally have to carry a message
	 * definition: a timer catch event waits on the clock, not on a person.
	 */
	private static boolean isHumanFacing(final BpmnElement element, final BpmnElementType type) {

		if (!HUMAN_STEPS.containsKey(type)) {
			return false;
		}

		return type != BpmnElementType.INTERMEDIATE_CATCH_EVENT || MESSAGE_EVENT_DEFINITION.equals(element.getEventDefinitionType());
	}

	/** The states a scaffolded div for this type renders in; empty if the type gets no div. */
	static Set<String> visibleWhenFor(final BpmnElementType type) {

		final StepKind kind = HUMAN_STEPS.get(type);

		return kind != null ? kind.visibleWhen() : Set.of();
	}

	/** The id prefix for this type's div, so generated ids read as what they are. */
	static String idPrefixFor(final BpmnElementType type) {

		final StepKind kind = HUMAN_STEPS.get(type);

		return kind != null ? kind.idPrefix() : "step-";
	}

	/** The start events among {@code elements}, or all of them when there is no start event. */
	private static List<BpmnElement> startEventsOf(final List<BpmnElement> elements) {

		final List<BpmnElement> starts = new ArrayList<>();

		for (final BpmnElement element : elements) {

			if (element.isType(BpmnElementType.START_EVENT)) {

				starts.add(element);
			}
		}

		// Deliberately more permissive than BpmnProcess.getStartEvent(), which demands
		// exactly one: a skeleton for a half-built model is still useful, so every element
		// becomes a walk seed rather than an error.
		return starts.isEmpty() ? elements : starts;
	}

	/**
	 * {@code topLevel} plus all descendants. Only sub-process-like elements can have
	 * children (the importer recurses into those alone), so leaves are not asked for a
	 * childElements traversal -- that read is a database round trip per node.
	 */
	private static List<BpmnElement> allElements(final List<BpmnElement> topLevel) {

		final List<BpmnElement> all = new ArrayList<>(topLevel);

		for (final BpmnElement element : topLevel) {

			if (element.getElementType().isSubProcessLike()) {

				all.addAll(allElements(Iterables.toList(element.getChildElements())));
			}
		}

		return all;
	}

	// ----- page building -----

	/**
	 * A bare {@code html > head > title / body} shell, used when no page-template widget is
	 * chosen. Returns the element the step divs go into.
	 */
	private static DOMNode createDefaultShell(final Page page) throws FrameworkException {

		final DOMElement html  = page.createElement("html");
		final DOMElement head  = page.createElement("head");
		final DOMElement title = page.createElement("title");
		final DOMElement body  = page.createElement("body");

		page.appendChild(html);
		html.appendChild(head);
		html.appendChild(body);
		head.appendChild(title);
		title.appendChild(page.createTextNode("${capitalize(page.name)}"));

		return body;
	}

	/**
	 * Expand {@code widget}'s source into {@code page} and return the node the step divs go
	 * into. Preference order:
	 *
	 * <ol>
	 * <li>the node named {@value #MAIN_CONTENT_SLOT} -- the convention page-template widgets
	 *     use to mark their content area, so the divs land where page content belongs rather
	 *     than after the template's own layout;</li>
	 * <li>the template's {@code body}, for a template that doesn't name a slot;</li>
	 * <li>the page root, so even a partial template still gets its divs.</li>
	 * </ol>
	 *
	 * <p>Expansion goes through a throwaway parent and then moves the roots across, which is
	 * what {@code AppendWidgetCommand} does -- {@code Widget.expandWidget} post-processes
	 * {@code parent.getChildren()}, so it needs a parent it owns. Widget placeholders are
	 * left as-is when no value is supplied (same as the Pages area's "create page from
	 * template", which also passes source only).</p>
	 */
	private static DOMNode expandTemplate(final App app, final Page page, final Widget widget) throws FrameworkException {

		final String source = widget.getSource();
		if (StringUtils.isBlank(source)) {

			throw new FrameworkException(422, "Page template '" + widget.getName() + "' has no source to expand.");
		}

		final DOMNode tmpParent = page.createElement("div");

		Widget.expandWidget(page, tmpParent, null, expansionParameters(widget), false);

		for (final DOMNode root : Iterables.toList(tmpParent.getChildren())) {

			page.appendChild(root);
		}

		app.delete(tmpParent);

		final DOMNode slot = findFirst(page, node -> MAIN_CONTENT_SLOT.equalsIgnoreCase(StringUtils.trimToEmpty(node.getName())));
		if (slot != null) {

			return slot;
		}

		final DOMNode body = findFirst(page, node -> "body".equals(node.getProperty(node.getTraits().key(DOMElementTraitDefinition.TAG_PROPERTY))));

		if (body == null) {

			logger.info("Page template '{}' declares neither a '{}' node nor a body element -- appending steps to the page root", widget.getName(), MAIN_CONTENT_SLOT);
		}

		return body != null ? body : page;
	}

	/**
	 * The canonical {@value #PROCESS_SUBJECT_FORM_WIDGET} widget, or null when it is not
	 * installed. Looked up once per page. A null means no subject forms are inserted -- logged
	 * once, so a missing widget is diagnosable rather than a silent absence of forms.
	 */
	private static Widget findProcessSubjectForm(final App app) throws FrameworkException {

		final NodeInterface widget = app.nodeQuery(StructrTraits.WIDGET).name(PROCESS_SUBJECT_FORM_WIDGET).getFirst();

		if (widget == null) {

			logger.warn("BPMN page skeleton: widget '{}' not found; user-task steps will get no subject form. Import the widget set that provides it.", PROCESS_SUBJECT_FORM_WIDGET);
			return null;
		}

		return widget.as(Widget.class);
	}

	/**
	 * Insert the subject form for a user-task step, if the step declares what it operates on.
	 *
	 * <p>Nothing about the subject is hardcoded into the page: the form's ComponentConfiguration
	 * is switched to {@code processBound} and bound to the step, and
	 * {@code ComponentConfigurationTraitWrapper#getDataSourceName} then derives the data source
	 * from the step's {@code subjectType} AT RENDER TIME. So changing the subject type on the
	 * BPMN side updates every generated form, and a step whose subject is not declared yet
	 * simply renders nothing rather than a broken form.</p>
	 *
	 * <p>The field list is seeded from the step's {@code subjectFormView} by the same helper the
	 * interactive widget insert uses ({@code WidgetAutoVisibilityMappingHelper}), so a form
	 * generated here and one inserted by hand start from the same fields.</p>
	 *
	 * @return true if a form was inserted
	 */
	private static boolean insertSubjectForm(final App app, final Page page, final DOMElement stepDiv,
	                                         final Widget formWidget, final BpmnElement step) throws FrameworkException {

		final Traits stepTraits = step.getTraits();

		if (!stepTraits.hasKey(BpmnElementTraitDefinition.SUBJECT_TYPE_PROPERTY)) {

			return false;
		}

		final String subjectType = step.getProperty(stepTraits.key(BpmnElementTraitDefinition.SUBJECT_TYPE_PROPERTY));
		if (StringUtils.isBlank(subjectType)) {

			// the process designer has not declared what this task operates on yet; a form bound
			// to nothing would be worse than no form, so skip and say so
			logger.info("BPMN page skeleton: step '{}' declares no subjectType, inserting no form", step.getBpmnId());
			return false;
		}

		final String source = formWidget.getSource();
		if (StringUtils.isBlank(source)) {

			logger.warn("BPMN page skeleton: form widget '{}' has no source, skipping form for step '{}'", formWidget.getName(), step.getBpmnId());
			return false;
		}

		// Expand through a throwaway parent and move the roots across, as AppendWidgetCommand
		// does. The dataSource parameter fills the widget's [dataSource] placeholder; it is only
		// the standalone fallback, since the binding below takes precedence at render time.
		final DOMNode tmpParent              = page.createElement("div");
		final Map<String, Object> parameters = expansionParameters(formWidget);

		parameters.put("dataSource", "node:" + subjectType);

		Widget.expandWidget(page, tmpParent, null, parameters, false);

		DOMNode formRoot = null;

		for (final DOMNode root : Iterables.toList(tmpParent.getChildren())) {

			stepDiv.appendChild(root);

			if (formRoot == null) {

				formRoot = root;
			}
		}

		app.delete(tmpParent);

		if (formRoot == null) {

			logger.warn("BPMN page skeleton: form widget '{}' expanded to nothing for step '{}'", formWidget.getName(), step.getBpmnId());
			return false;
		}

		final ComponentConfiguration config = WidgetAutoVisibilityMappingHelper.findComponentConfiguration(formRoot);
		if (config == null) {

			logger.warn("BPMN page skeleton: form widget '{}' has no ComponentConfiguration, so it cannot be bound to step '{}'",
				formWidget.getName(), step.getBpmnId());
			return true;
		}

		final Traits configTraits = config.getTraits();

		config.setProperty(configTraits.key(ComponentConfigurationTraitDefinition.BINDING_MODE_PROPERTY),
			ComponentConfigurationTraitDefinition.BINDING_MODE_PROCESS_BOUND);

		if (configTraits.hasKey(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY)) {

			config.setProperty(configTraits.key(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY), step);
		}

		// field list from the step's subjectFormView; no-op when the step declares none
		WidgetAutoVisibilityMappingHelper.seedFieldSetFromView(formRoot, step);

		retargetSubmitToTask(formRoot, step, subjectType);

		return true;
	}

	/**
	 * Render the step's {@code instructions} and {@code documentation} as paragraphs above the
	 * form, when the modeller supplied them.
	 *
	 * <p>{@code instructions} is declared as "free-text help shown to the human user above the
	 * form"; {@code documentation} is the BPMN {@code <documentation>} element. Both survive the
	 * import and had no consumer, so a process page discarded the only prose written for the
	 * person doing the work. Text is emitted through {@code localize()} so it can be translated
	 * without touching the page.</p>
	 */
	private static void appendHelpText(final Page page, final DOMElement stepDiv, final BpmnElement step,
	                                   final String htmlId, final Set<String> usedIds) throws FrameworkException {

		final Traits traits = step.getTraits();

		final String instructions = traits.hasKey(BpmnElementTraitDefinition.INSTRUCTIONS_PROPERTY)
			? step.getProperty(traits.key(BpmnElementTraitDefinition.INSTRUCTIONS_PROPERTY))
			: null;

		appendParagraph(page, stepDiv, instructions, htmlId + "-instructions", "bpmn-step-instructions", usedIds);
		appendParagraph(page, stepDiv, step.getDocumentation(), htmlId + "-documentation", "bpmn-step-documentation sw-text-muted", usedIds);
	}

	/** One paragraph of localized static text, or nothing when the text is blank. */
	private static void appendParagraph(final Page page, final DOMElement parent, final String text,
	                                    final String idCandidate, final String cssClass, final Set<String> usedIds) throws FrameworkException {

		if (StringUtils.isBlank(text)) {

			return;
		}

		final DOMElement paragraph = page.createElement("p");
		final String htmlId        = firstFree(idCandidate, usedIds::contains);

		parent.appendChild(paragraph);
		paragraph.appendChild(page.createTextNode("${localize('" + escapeForSingleQuotedLiteral(text.trim()) + "')}"));
		describe(paragraph, cssClass.split(" ")[0], htmlId, cssClass);
		usedIds.add(htmlId);
	}

	/**
	 * Wire the form's submit to {@code completeWithSubject} for this step.
	 *
	 * <p>The step declares a subjectType (that is why a form was inserted), so the operation is
	 * always {@code completeWithSubject} -- which is get-or-create: it captures the subject on
	 * the step that first has one and edits the same subject on later steps. There is no
	 * create-vs-edit choice for the generator to make, because that is a runtime fact the
	 * operation resolves itself. Whatever persist verb the widget shipped ({@code create},
	 * {@code update}, or an already process-aware {@code control-process}) is normalised to it.</p>
	 *
	 * <p>Per-step targeting is {@code idExpression = ${current.id}} plus {@code targetsElement =
	 * step} -- NOT {@code ${task.id}}: idExpression is variable-replaced against the ActionMapping
	 * node (DOMElementTraitDefinition), where {@code $.task} (which resolves only for a DOM node)
	 * cannot. {@code ${current.id}} reads the render context's data object, the ProcessInstance on
	 * the instance page; the runtime resolves the active task at the step via
	 * {@code findActiveTaskForCurrentUser}. {@code targetsElementBpmnId} is set too, so the
	 * importer's rewire pass keeps the button on the right step across re-imports. {@code dataType}
	 * is the concrete subject type, replacing the widget's {@code ${dataSource.dataType}}
	 * placeholder so {@code completeWithSubject} can create the subject when none exists.</p>
	 */
	private static int retargetSubmitToTask(final DOMNode formRoot, final BpmnElement step, final String subjectType) throws FrameworkException {

		final Traits amTraits             = Traits.of(StructrTraits.ACTION_MAPPING);
		final PropertyKey<String> actionKey = amTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY);
		final List<DOMNode> candidates    = new ArrayList<>();

		candidates.add(formRoot);

		for (final NodeInterface descendant : formRoot.getAllChildNodes()) {

			if (descendant.is(StructrTraits.DOM_ELEMENT)) {

				candidates.add(descendant.as(DOMNode.class));
			}
		}

		int retargeted                   = 0;
		final List<String> seenActions   = new ArrayList<>();

		for (final DOMNode candidate : candidates) {

			final PropertyKey<Iterable<NodeInterface>> actionsKey = candidate.getTraits().hasKey(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY)
				? candidate.getTraits().key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY)
				: null;

			if (actionsKey == null) {
				continue;
			}

			for (final NodeInterface mapping : Iterables.toList(candidate.getProperty(actionsKey))) {

				// Match on the ACTION verb, not the event: a form fires its persist on the form's
				// submit or a button's click, but the verb identifies it either way. create/update
				// are plain-form persists; control-process is an already process-aware submit.
				final String widgetAction = mapping.getProperty(actionKey);
				seenActions.add(String.valueOf(widgetAction));

				if (!"create".equals(widgetAction) && !"update".equals(widgetAction) && !"control-process".equals(widgetAction)) {
					continue;
				}

				// Normalise everything to completeWithSubject on this step -- the one correct
				// operation for a subject-bearing step, since it is idempotent (create-or-edit).
				mapping.setProperty(actionKey, "control-process");
				mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY), "completeWithSubject");
				mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY), "${current.id}");
				mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.TARGETS_ELEMENT_PROPERTY), step);
				mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), subjectType);

				if (amTraits.hasKey(ActionMappingTraitDefinition.TARGETS_ELEMENT_BPMN_ID_PROPERTY)) {

					mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.TARGETS_ELEMENT_BPMN_ID_PROPERTY), step.getBpmnId());
				}

				retargeted++;

				logger.info("BPMN page skeleton: form submit '{}' wired to completeWithSubject at step '{}'",
					widgetAction, step.getBpmnId());
			}
		}

		if (retargeted == 0) {

			// The form was inserted but its submit still only saves the subject -- the process
			// will not advance. Report precisely what was (and was not) found, because the cause
			// is one of a few distinct widget-structure problems.
			if (!seenActions.isEmpty()) {

				// Action mappings exist in the form, just not a persist verb we retarget.
				logger.warn("BPMN page skeleton: inserted a form for step '{}' but none of its action mappings is a create/update/control-process submit (found: {}); the process will not advance. The form's submit must persist the subject via 'create' or 'update', or already be a 'control-process' action.",
					step.getBpmnId(), seenActions);

			} else {

				// No action mapping in the form's own subtree at all. The most common cause is a
				// shared-template widget whose <form> lives in the shared component body rather
				// than in a per-instance <structr:template> block -- so its submit is not part of
				// this instance and, being shared across every form on every page, could not be
				// configured per step even if it were reachable.
				final DOMNode shared = formRoot.getSharedComponent();
				final boolean submitInSharedComponent = shared != null && hasTriggeredAction(shared);

				if (submitInSharedComponent) {

					logger.warn("BPMN page skeleton: inserted a form for step '{}' whose submit action lives in the SHARED COMPONENT, not in a per-instance template. Per-step wiring is impossible there (it is shared across every form). Structure the widget like 'Create Form': a <structr:template src=\"...\"> instance whose children hold the <form> with the submit action.",
						step.getBpmnId());

				} else {

					logger.warn("BPMN page skeleton: inserted a form for step '{}' but found no action mapping at all; the submit is likely an inline attribute that did not expand into an ActionMapping node. The process will not advance.",
						step.getBpmnId());
				}
			}
		}

		return retargeted;
	}

	/** True if {@code root} or any descendant carries at least one triggered ActionMapping. */
	private static boolean hasTriggeredAction(final DOMNode root) {

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

			final PropertyKey<Iterable<NodeInterface>> actionsKey = node.getTraits().key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY);
			if (!Iterables.toList(node.getProperty(actionsKey)).isEmpty()) {

				return true;
			}
		}

		return false;
	}

	/**
	 * Expansion parameters for a widget: its source plus its own {@code componentType} and
	 * {@code dimensions}, wrapped the way {@code Widget.expandWidget} expects them (a
	 * {@code JsonSingleInput} under the {@code config} key).
	 *
	 * <p>Passing the config is not optional for data-driven widgets: {@code expandWidget} copies
	 * these onto the appended roots, and a component without a dimension fails
	 * {@code ComponentConfiguration.checkCompatibility} the moment its configuration is created
	 * ("Component X has no dimension"). This mirrors what AppendWidgetCommand sends from the
	 * Insert Widget dialog.</p>
	 */
	private static Map<String, Object> expansionParameters(final Widget widget) {

		final Map<String, Object> parameters  = new HashMap<>();
		final JsonSingleInput singleInput     = new JsonSingleInput();
		final JsonInput input                 = new JsonInput();

		singleInput.add(input);

		parameters.put("source", widget.getSource());
		parameters.put("config", singleInput);

		if (widget.getComponentType() != null) {

			input.put("componentType", widget.getComponentType());
		}

		if (widget.getDimensions() != null) {

			input.put("dimensions", widget.getDimensions());
		}

		return parameters;
	}

	/** Depth-first search for the first descendant matching {@code predicate}, or null. */
	private static DOMNode findFirst(final DOMNode parent, final Predicate<DOMNode> predicate) {

		for (final DOMNode child : parent.getChildren()) {

			if (predicate.test(child)) {

				return child;
			}

			final DOMNode match = findFirst(child, predicate);
			if (match != null) {

				return match;
			}
		}

		return null;
	}

	/**
	 * Add the partial that launches an instance: a named div holding a button wired to the
	 * "start process" Event Action Mapping, with a navigate-to-{@code {result.url}} follow-up
	 * so the click lands the user on the new instance's page (the {@code start} operation
	 * returns that url -- see DOMElementTraitDefinition's control-process handler).
	 *
	 * <p>It goes INSIDE the start event's div when the process has one: that div is already
	 * the {@code no-instance} partial, so nesting keeps one launch area instead of two
	 * competing ones and needs no mapping of its own. A process without a start event gets
	 * the partial at the top of the wrapper, with its own {@code no-instance} mapping.</p>
	 */
	private static void createStartProcessPartial(final App app, final Page page, final DOMElement wrapper, final DOMElement startEventDiv,
	                                              final BpmnProcess process, final String processLabel, final String bpmnProcessId,
	                                              final Set<String> usedIds) throws FrameworkException {

		final DOMElement container = page.createElement("div");
		final String htmlId        = firstFree("start-process", usedIds::contains);

		if (startEventDiv != null) {

			startEventDiv.appendChild(container);

		} else {

			wrapper.appendChild(container);
			createVisibilityMapping(app, container, process, null, VisibilityMappingTraitDefinition.STATE_NO_INSTANCE, bpmnProcessId);
		}

		describe(container, "Start Process", htmlId, "bpmn-start-process sw-card-content");
		usedIds.add(htmlId);

		final DOMElement button  = page.createElement("button");
		final String buttonId    = firstFree(htmlId + "-button", usedIds::contains);

		container.appendChild(button);

		// Localizable too: the key is the literal label, so a Localization can translate it
		// without touching the page.
		button.appendChild(page.createTextNode("${localize('Start Process " + escapeForSingleQuotedLiteral(processLabel) + "')}"));
		describe(button, "Start Process Button", buttonId, "bpmn-start-process-button sw-button sw-button-primary");
		usedIds.add(buttonId);

		final NodeInterface mapping = app.create(StructrTraits.ACTION_MAPPING);
		final Traits amTraits       = mapping.getTraits();

		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.EVENT_PROPERTY),              "click");
		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.ACTION_PROPERTY),             "control-process");
		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY),  "start");
		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.CONTROLS_PROCESS_PROPERTY),   process);
		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY),  "navigate-to-url");
		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY),        "{result.url}");

		// Denormalized backup the importer's rewire pass matches on, so a re-imported
		// process keeps this button working (BpmnImporter#rewireExternalReferences).
		mapping.setProperty(amTraits.key(ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_PROPERTY), bpmnProcessId);

		button.setProperty(button.getTraits().key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY), List.of(mapping));
	}

	private static void createVisibilityMapping(final App app, final DOMElement domNode, final BpmnProcess process, final BpmnElement step,
	                                            final String state, final String bpmnProcessId) throws FrameworkException {

		final NodeInterface mapping = app.create(ProcessTraits.VISIBILITY_MAPPING);
		final Traits traits         = mapping.getTraits();

		mapping.setProperty(traits.key(VisibilityMappingTraitDefinition.DOM_NODE_PROPERTY),     domNode);
		mapping.setProperty(traits.key(VisibilityMappingTraitDefinition.VISIBLE_WHEN_PROPERTY), state);

		// A bound process changes what no-instance MEANS, so the launch partial must not have
		// one. With any process bound, the predicate is "the current user has no active
		// instance of this process" -- a query against the logged-in user, which is false for
		// an anonymous visitor and false again for someone who already started one, so the
		// start button silently vanishes. With no process bound it is the documented context
		// rule, "no instance in this page's render context": visible on the bare page for
		// everyone, hidden on /<page>/<instance-uuid>. That is what a launch partial wants.
		//
		// Binding cannot be expressed by the denormalized id alone either: evaluate() derives
		// the id FROM the relationship when one is set (VisibilityMappingTraitDefinition
		// #preferredBoundProcessId), so the relationship is what has to be left off.
		//
		// Step-scoped states are the opposite: they need both, the id being what verifies the
		// context instance actually belongs to this process, and both are what the importer's
		// re-import rewire pass matches on.
		final boolean contextScoped = VisibilityMappingTraitDefinition.STATE_NO_INSTANCE.equals(state)
			|| VisibilityMappingTraitDefinition.STATE_HAS_ACTIVE_INSTANCE.equals(state);

		// The STEP is always recorded when there is one: it says which step the partial belongs
		// to, it is what the step heading reads its live name from, and no predicate consults it
		// for the context-scoped states (evaluatePredicate only looks at boundProcessId and the
		// current user there), so it cannot change their meaning.
		if (step != null) {

			mapping.setProperty(traits.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY),         step);
			mapping.setProperty(traits.key(VisibilityMappingTraitDefinition.BOUND_STEP_BPMN_ID_PROPERTY), step.getBpmnId());
		}

		if (contextScoped) {

			return;
		}

		mapping.setProperty(traits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_PROPERTY),    process);
		mapping.setProperty(traits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_ID_PROPERTY), bpmnProcessId);
	}

	/** Set the tree label, the HTML id and a marker class on a generated element. */
	private static void describe(final DOMElement element, final String name, final String htmlId, final String cssClass) throws FrameworkException {

		element.setName(name);
		element.setIdAttribute(htmlId);
		element.setProperty(element.getTraits().key(DOMElementTraitDefinition._HTML_CLASS_PROPERTY), cssClass);
	}

	/**
	 * The heading text for a step div: the step's CURRENT name, so renaming the step in the
	 * editor changes the page too, passed through {@code localize()} so the label can be
	 * translated ({@code localize()} returns its key unchanged when no Localization exists).
	 *
	 * <p>The step comes from the {@code visibilityMapping} keyword, which walks up to the
	 * partial governing this part of the page and returns its binding (RenderContext, alongside
	 * {@code component}): no query, no embedded bpmnId that could go stale, and no dependence
	 * on a ProcessInstance being in the render context -- which the launch partial needs, since
	 * there is none there. The keyword reads mappings as superuser, so this works for a
	 * frontend user with no access to the mapping nodes themselves.</p>
	 *
	 * <p>StructrScript navigation stops at the first null (ActionContext#getReferencedProperty),
	 * so an unbound div yields null rather than an error, and {@code coalesce} then falls back
	 * to the literal name -- the heading is the signal that a step rendered at all, so it must
	 * not come out empty.</p>
	 */
	private static String stepHeadingScript(final String label) {

		return "${localize(coalesce(visibilityMapping.boundStep.bpmnName, '" + escapeForSingleQuotedLiteral(label) + "'))}";
	}

	/** Escape a value for embedding in a single-quoted script string literal. */
	private static String escapeForSingleQuotedLiteral(final String value) {

		return StringUtils.defaultString(value).replace("\\", "\\\\").replace("'", "\\'");
	}

	// ----- naming -----

	/** The most descriptive name available for the process. */
	private static String processLabel(final BpmnProcess process) {

		for (final String candidate : List.of(
			StringUtils.defaultString(process.getProcessName()),
			StringUtils.defaultString(process.getName()),
			StringUtils.defaultString(process.getProcessId()))) {

			if (!candidate.isEmpty()) {

				return candidate;
			}
		}

		return "process";
	}

	/**
	 * Slugify for use as an HTML id or page name, via {@link Functions#cleanString} -- the
	 * engine behind {@code $.clean()} and the platform's existing process-to-page-name
	 * convention: {@code DOMElementTraitDefinition} slugifies {@code processName} with it to
	 * build the {@code /<page-name>/<instance-uuid>} start-process URL. Using the same
	 * function is what keeps a generated page's name equal to the name that URL falls back
	 * to. It also owns the transliteration policy (German umlauts expanded, remaining
	 * accents folded to ASCII), so there is one place to argue about it.
	 *
	 * <p>Added here: never return empty. A name that slugifies to nothing (punctuation only,
	 * or a script the fold strips) falls back to the step's bpmnId, then to a constant, so
	 * every div still gets an id.</p>
	 */
	static String slug(final String raw, final String fallback) {

		final String slug = trimDashes(Functions.cleanString(raw));

		if (!slug.isEmpty()) {

			return slug;
		}

		final String fromFallback = trimDashes(Functions.cleanString(fallback));

		return fromFallback.isEmpty() ? "step" : fromFallback;
	}

	private static String trimDashes(final String s) {
		return s.replaceAll("(^-+)|(-+$)", "");
	}

	/**
	 * {@code candidate}, or the first of {@code candidate-2}, {@code candidate-3}, ... that
	 * {@code taken} rejects. Used for both html ids (unique within the page) and page names
	 * (unique in the database, because page names route requests).
	 */
	static String firstFree(final String candidate, final Predicate<String> taken) {

		String result = candidate;
		int suffix    = 2;

		while (taken.test(result)) {

			result = candidate + "-" + suffix++;
		}

		return result;
	}

	private static boolean pageExists(final App app, final String name) {

		try {

			return app.nodeQuery(StructrTraits.PAGE).name(name).getFirst() != null;

		} catch (final FrameworkException fex) {

			// Treat an unreadable name as taken: the suffix loop then moves on instead of
			// failing the whole generation, and a colliding page name would be caught by
			// the Page uniqueness constraint anyway.
			logger.warn("BPMN page skeleton: could not check whether page '{}' exists, assuming it does", name, fex);
			return true;
		}
	}
}
