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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.schema.action.ActionContext;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.BpmnElement;
import org.structr.process.traits.wrappers.BpmnElementTraitWrapper;
import java.util.Set;

/**
 * Trait definition for BpmnElement -- any BPMN element inside a process definition.
 * The bpmnElementType property identifies the BPMN element kind (startEvent, userTask,
 * exclusiveGateway, etc.).
 *
 * All BPMN content is stored as typed graph properties -- there is no raw XML storage.
 * Unknown/extra XML attributes are preserved in bpmnAttributes (JSON) for round-trip fidelity.
 */
public class BpmnElementTraitDefinition extends AbstractNodeTraitDefinition {

	// --- Core identity ---
	public static final String BPMN_ELEMENT_TYPE_PROPERTY   = "bpmnElementType";
	public static final String BPMN_NAME_PROPERTY           = "bpmnName";
	public static final String BPMN_ATTRIBUTES_PROPERTY     = "bpmnAttributes";

	// --- Content properties (promoted from XML child elements) ---
	public static final String DOCUMENTATION_PROPERTY       = "documentation";
	public static final String SCRIPT_CONTENT_PROPERTY      = "scriptContent";
	// Camunda inputOutput mappings, stored as JSON:
	// {"inputs":[{"name":..,"source":..}],"outputs":[{"name":..,"source":..}]}.
	// Applied around automatic-task execution (inputs before, outputs after).
	public static final String IO_MAPPINGS_PROPERTY         = "ioMappings";

	// --- Event definition properties ---
	public static final String EVENT_DEF_TYPE_PROPERTY      = "eventDefinitionType";
	public static final String EVENT_DEF_ID_PROPERTY        = "eventDefinitionId";
	public static final String EVENT_DEF_REF_PROPERTY       = "eventDefinitionRef";
	public static final String TIMER_TYPE_PROPERTY          = "timerType";
	public static final String TIMER_EXPRESSION_TYPE_PROPERTY = "timerExpressionType";
	public static final String TIMER_VALUE_PROPERTY         = "timerValue";

	// --- Relationship properties ---
	public static final String PROCESS_PROPERTY             = "process";
	public static final String PARENT_ELEMENT_PROPERTY      = "parentElement";
	public static final String CHILD_ELEMENTS_PROPERTY      = "childElements";
	public static final String CHILD_FLOWS_PROPERTY         = "childFlows";
	public static final String OUTGOING_FLOWS_PROPERTY      = "outgoingFlows";
	public static final String INCOMING_FLOWS_PROPERTY      = "incomingFlows";
	public static final String PERFORMERS_PROPERTY          = "performers";
	public static final String TASK_LISTENERS_PROPERTY      = "taskListeners";
	public static final String METHODS_PROPERTY             = "methods";
	public static final String VISIBILITY_MAPPINGS_PROPERTY = "visibilityMappings";
	public static final String CONTROL_ACTIONS_PROPERTY     = "controlActions";
	public static final String BOUND_CONFIGURATIONS_PROPERTY = "boundConfigurations";
	public static final String DI_SHAPE_PROPERTY            = "diShape";
	public static final String LANE_PROPERTY                = "lane";
	// Boundary event -> host activity (BPMN attachedToRef). The boundary
	// is the source of the relationship (Many-to-One); the host's inverse
	// is `attachedBoundaries` on this same trait.
	public static final String ATTACHED_TO_PROPERTY         = "attachedTo";
	public static final String ATTACHED_BOUNDARIES_PROPERTY = "attachedBoundaries";

	// --- Process / UI contract (UserTask elements only) ---
	// Structr-only declarations the process designer attaches to a UserTask
	// to drive process-bound widget rendering on the UI side. None of these
	// are part of the BPMN spec; they survive re-import via the importer's
	// rewire step (matched by bpmnId). Empty / unset on non-UserTask
	// elements; meaningless there. See the process / UI separation pillar
	// (project_process_ui_contract_pillar.md) for the design rationale.
	//   subjectType         - name of the SchemaNode the task operates on
	//                         (e.g. "LeaveRequest"). Read at render time by
	//                         process-bound widgets to derive their dataSource.
	//   subjectFormView     - optional SchemaView on subjectType naming the
	//                         fields the form should expose. When unset the
	//                         widget defaults to the standard view.
	//   subjectWritableView - optional SchemaView on subjectType naming the
	//                         subset of fields the task may write. Convention:
	//                         a subset of subjectFormView.
	//   instructions        - free-text help shown to the human user above
	//                         the form.
	public static final String SUBJECT_TYPE_PROPERTY          = "subjectType";
	public static final String SUBJECT_FORM_VIEW_PROPERTY     = "subjectFormView";
	public static final String SUBJECT_WRITABLE_VIEW_PROPERTY = "subjectWritableView";
	public static final String INSTRUCTIONS_PROPERTY          = "instructions";

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			BpmnElement.class, (traits, node) -> new BpmnElementTraitWrapper(traits, node)
		);
	}

	public BpmnElementTraitDefinition() {
		super(ProcessTraits.BPMN_ELEMENT);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		// Core identity
		final Property<String> bpmnElementType = new StringProperty(BPMN_ELEMENT_TYPE_PROPERTY).indexed();
		final Property<String> bpmnName        = new StringProperty(BPMN_NAME_PROPERTY).indexed();
		final Property<String> bpmnAttributes  = new StringProperty(BPMN_ATTRIBUTES_PROPERTY);

		// Content
		final Property<String> documentation   = new StringProperty(DOCUMENTATION_PROPERTY);
		final Property<String> scriptContent   = new StringProperty(SCRIPT_CONTENT_PROPERTY);
		final Property<String> ioMappings      = new StringProperty(IO_MAPPINGS_PROPERTY);

		// Event definitions
		final Property<String> eventDefType         = new StringProperty(EVENT_DEF_TYPE_PROPERTY);
		final Property<String> eventDefId           = new StringProperty(EVENT_DEF_ID_PROPERTY);
		final Property<String> eventDefRef          = new StringProperty(EVENT_DEF_REF_PROPERTY);
		final Property<String> timerType            = new StringProperty(TIMER_TYPE_PROPERTY);
		final Property<String> timerExpressionType  = new StringProperty(TIMER_EXPRESSION_TYPE_PROPERTY);
		final Property<String> timerValue           = new StringProperty(TIMER_VALUE_PROPERTY);

		// Relationships
		final Property<NodeInterface> process                    = new StartNode(traitsInstance, PROCESS_PROPERTY, ProcessTraits.BPMN_PROCESS_HAS_ELEMENT);
		final Property<NodeInterface> parentElement              = new StartNode(traitsInstance, PARENT_ELEMENT_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT);
		final Property<Iterable<NodeInterface>> childElements    = new EndNodes(traitsInstance, CHILD_ELEMENTS_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT);
		final Property<Iterable<NodeInterface>> childFlows       = new EndNodes(traitsInstance, CHILD_FLOWS_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_CHILD_FLOW);
		final Property<Iterable<NodeInterface>> outgoingFlows    = new StartNodes(traitsInstance, OUTGOING_FLOWS_PROPERTY, ProcessTraits.BPMN_SEQUENCE_FLOW_FROM);
		final Property<Iterable<NodeInterface>> incomingFlows    = new StartNodes(traitsInstance, INCOMING_FLOWS_PROPERTY, ProcessTraits.BPMN_SEQUENCE_FLOW_TO);
		final Property<Iterable<NodeInterface>> performers       = new EndNodes(traitsInstance, PERFORMERS_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_PERFORMER);
		final Property<Iterable<NodeInterface>> taskListeners    = new EndNodes(traitsInstance, TASK_LISTENERS_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_TASK_LISTENER);
		final Property<Iterable<NodeInterface>> methods          = new EndNodes(traitsInstance, METHODS_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_METHOD);
		// Inverse properties for relationships pointing INTO this BpmnElement.
		// Required so OneToMany.ensureCardinality can resolve the source side when
		// a VisibilityMapping or ActionMapping is reassigned (e.g. by the importer's
		// re-import rewire); without these, vm.setProperty(boundStep, newElem)
		// throws "missing StartNode(s) property".
		final Property<Iterable<NodeInterface>> visibilityMappings = new StartNodes(traitsInstance, VISIBILITY_MAPPINGS_PROPERTY, ProcessTraits.VISIBILITY_MAPPING_AT_BPMN_ELEMENT);
		final Property<Iterable<NodeInterface>> controlActions     = new StartNodes(traitsInstance, CONTROL_ACTIONS_PROPERTY,     StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT);
		// Inverse for ComponentConfiguration -[BOUND]-> BpmnElement so the
		// OneToMany cardinality check can resolve the source side when a
		// component's boundUserTask is reassigned. Lists every widget
		// currently bound to this UserTask (typically 0-N partials that
		// render the same step).
		final Property<Iterable<NodeInterface>> boundConfigurations = new StartNodes(traitsInstance, BOUND_CONFIGURATIONS_PROPERTY, StructrTraits.COMPONENT_CONFIGURATION_BOUND_BPMN_ELEMENT);
		final Property<NodeInterface> diShape                    = new StartNode(traitsInstance, DI_SHAPE_PROPERTY, ProcessTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT);
		// Inverse for BpmnLane -[HAS_FLOW_NODE]-> BpmnElement: each element
		// belongs to at most one lane (BPMN 2.0 rule). Required so
		// OneToMany.ensureCardinality on the lane->element rel can resolve
		// the source side without throwing "missing StartNode(s) property".
		final Property<NodeInterface> lane                       = new StartNode(traitsInstance, LANE_PROPERTY, ProcessTraits.BPMN_LANE_HAS_FLOW_NODE);
		// BPMN attachedToRef: a boundary event is attached to exactly one
		// host activity. Modelled as a self-referencing relationship on
		// BpmnElement so both ends share this trait. The boundary holds
		// the EndNode side (`attachedTo` -> the host); the host holds the
		// inverse StartNodes side (`attachedBoundaries`) so we can look
		// up "what boundaries does this task carry" without scanning.
		final Property<NodeInterface> attachedTo                 = new EndNode(traitsInstance, ATTACHED_TO_PROPERTY, ProcessTraits.BPMN_ELEMENT_ATTACHED_TO);
		final Property<Iterable<NodeInterface>> attachedBoundaries = new StartNodes(traitsInstance, ATTACHED_BOUNDARIES_PROPERTY, ProcessTraits.BPMN_ELEMENT_ATTACHED_TO);

		// Process / UI contract (UserTask elements only); see docstring above.
		final Property<String> subjectType         = new StringProperty(SUBJECT_TYPE_PROPERTY).indexed();
		final Property<String> subjectFormView     = new StringProperty(SUBJECT_FORM_VIEW_PROPERTY);
		final Property<String> subjectWritableView = new StringProperty(SUBJECT_WRITABLE_VIEW_PROPERTY);
		final Property<String> instructions        = new StringProperty(INSTRUCTIONS_PROPERTY);

		return newSet(bpmnElementType, bpmnName, bpmnAttributes,
			documentation, scriptContent, ioMappings,
			eventDefType, eventDefId, eventDefRef, timerType, timerExpressionType, timerValue,
			process, parentElement, childElements, childFlows, outgoingFlows, incomingFlows, performers, taskListeners, methods, visibilityMappings, controlActions, boundConfigurations, diShape, lane,
			attachedTo, attachedBoundaries,
			subjectType, subjectFormView, subjectWritableView, instructions);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, BPMN_ELEMENT_TYPE_PROPERTY, BPMN_NAME_PROPERTY,
				DOCUMENTATION_PROPERTY, SCRIPT_CONTENT_PROPERTY, EVENT_DEF_TYPE_PROPERTY,
				SUBJECT_TYPE_PROPERTY, SUBJECT_FORM_VIEW_PROPERTY, SUBJECT_WRITABLE_VIEW_PROPERTY, INSTRUCTIONS_PROPERTY),
			PropertyView.Ui, newSet(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, BPMN_ELEMENT_TYPE_PROPERTY, BPMN_NAME_PROPERTY, BPMN_ATTRIBUTES_PROPERTY,
				DOCUMENTATION_PROPERTY, SCRIPT_CONTENT_PROPERTY, IO_MAPPINGS_PROPERTY,
				EVENT_DEF_TYPE_PROPERTY, EVENT_DEF_ID_PROPERTY, EVENT_DEF_REF_PROPERTY,
				TIMER_TYPE_PROPERTY, TIMER_EXPRESSION_TYPE_PROPERTY, TIMER_VALUE_PROPERTY,
				PROCESS_PROPERTY, PARENT_ELEMENT_PROPERTY, CHILD_ELEMENTS_PROPERTY, CHILD_FLOWS_PROPERTY,
				OUTGOING_FLOWS_PROPERTY, INCOMING_FLOWS_PROPERTY, PERFORMERS_PROPERTY, TASK_LISTENERS_PROPERTY, METHODS_PROPERTY, DI_SHAPE_PROPERTY, LANE_PROPERTY,
				ATTACHED_TO_PROPERTY, ATTACHED_BOUNDARIES_PROPERTY,
				SUBJECT_TYPE_PROPERTY, SUBJECT_FORM_VIEW_PROPERTY, SUBJECT_WRITABLE_VIEW_PROPERTY, INSTRUCTIONS_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			// Replace humanPerformer / potentialOwner expressions and principal
			// lists on this element in one round trip. Each kind has two
			// inputs: an expression string (legacy / scripted authoring path)
			// and a list of Principal UUIDs (typed graph path). They aren't
			// mutually exclusive -- the engine prefers principals when present
			// and falls back to expression evaluation otherwise. Whichever is
			// non-empty causes the BpmnPerformer to be created / kept; if
			// BOTH are empty the performer is removed.
			//
			// Argument keys:
			//   humanPerformer            -> String expression
			//   potentialOwner            -> String expression
			//   humanPerformerPrincipals  -> List<UUID-string> | UUID-string | null
			//   potentialOwnerPrincipals  -> List<UUID-string> | UUID-string | null
			//
			// Generic <bpmn:performer> entries are left untouched: this method
			// only manages the two assignment kinds the editor surfaces.
			new JavaMethod("setPerformers", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final NodeInterface element = (NodeInterface) entity;
					final SecurityContext securityContext = actionContext.getSecurityContext();
					final App app = StructrApp.getInstance(securityContext);
					final Map<String, Object> args = arguments.toMap();

					final Object hpArg = args.get("humanPerformer");
					final Object poArg = args.get("potentialOwner");
					final String humanPerformer = (hpArg instanceof String) ? ((String) hpArg).trim() : "";
					final String potentialOwner = (poArg instanceof String) ? ((String) poArg).trim() : "";

					final List<NodeInterface> hpPrincipals = resolvePrincipalIds(app, args.get("humanPerformerPrincipals"));
					final List<NodeInterface> poPrincipals = resolvePrincipalIds(app, args.get("potentialOwnerPrincipals"));

					reconcilePerformer(app, element, BpmnPerformerTraitDefinition.KIND_HUMAN_PERFORMER, humanPerformer, hpPrincipals);
					reconcilePerformer(app, element, BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER, potentialOwner, poPrincipals);
					return null;
				}

				@Override
				public String getDescription() {
					return "Replace humanPerformer / potentialOwner configuration on this BpmnElement. Each kind accepts an expression string (\"${initiator}\" / \"user(alice)\" syntax) and a list of Principal UUIDs. The performer is removed when both are empty; otherwise a single BpmnPerformer of the kind is created / updated to carry both. Generic <performer> entries are not touched.";
				}
			}
		);
	}

	/**
	 * Coerce a {@code humanPerformerPrincipals} / {@code potentialOwnerPrincipals}
	 * argument into a list of resolved Principal nodes. Accepts a single
	 * UUID string, a list of UUID strings, or null. Unknown / unresolvable
	 * UUIDs are silently dropped: the editor passes ids it just rendered
	 * from the same graph, so a miss almost certainly means the picker
	 * raced with a delete and continuing without that principal is the
	 * right behaviour.
	 */
	private static List<NodeInterface> resolvePrincipalIds(final App app, final Object arg) throws FrameworkException {
		final List<NodeInterface> out = new LinkedList<>();
		if (arg == null) {

			return out;
		}
		final Iterable<?> source;
		if (arg instanceof Iterable) {

			source = (Iterable<?>) arg;

		} else if (arg instanceof Object[]) {

			source = java.util.Arrays.asList((Object[]) arg);

		} else if (arg instanceof String) {

			source = List.of((String) arg);

		} else {

			return out;
		}
		for (final Object item : source) {

			if (!(item instanceof String)) {
				continue;
			}
			final String id = ((String) item).trim();
			if (id.isEmpty()) {

				continue;
			}
			final NodeInterface node = app.getNodeById(id);
			if (node != null) {

				out.add(node);
			}
		}
		return out;
	}

	private static void reconcilePerformer(final App app, final NodeInterface element,
										   final String kind, final String expression,
										   final List<NodeInterface> principals) throws FrameworkException {

		final Traits elemTraits = element.getTraits();
		final Traits perfTraits = Traits.of(ProcessTraits.BPMN_PERFORMER);
		final PropertyKey<Iterable<NodeInterface>> performersKey  = elemTraits.key(PERFORMERS_PROPERTY);
		final PropertyKey<Iterable<NodeInterface>> principalsKey  = perfTraits.key(BpmnPerformerTraitDefinition.PRINCIPALS_PROPERTY);

		// Bucket existing performers by kind. Generic <performer> entries are
		// left as-is; only the two managed kinds are reconciled.
		final List<NodeInterface> ofKind = new LinkedList<>();
		final List<NodeInterface> others = new LinkedList<>();
		final Iterable<NodeInterface> existing = element.getProperty(performersKey);
		if (existing != null) {

			for (final NodeInterface p : existing) {

				final String pKind = p.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.KIND_PROPERTY));
				if (kind.equals(pKind)) {

					ofKind.add(p);
				}
				else                    others.add(p);
			}
		}

		final boolean hasExpr       = expression != null && !expression.isEmpty();
		final boolean hasPrincipals = principals != null && !principals.isEmpty();

		if (!hasExpr && !hasPrincipals) {

			// Remove all performers of this kind.
			for (final NodeInterface p : ofKind) app.delete(p);
			return;
		}

		if (ofKind.isEmpty()) {

			// Create a new BpmnPerformer of this kind and link via the rel.
			final NodeInterface created = app.create(ProcessTraits.BPMN_PERFORMER);
			created.setProperty(perfTraits.key(BpmnPerformerTraitDefinition.KIND_PROPERTY),       kind);
			created.setProperty(perfTraits.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY), hasExpr ? expression : null);
			created.setProperty(perfTraits.key(BpmnPerformerTraitDefinition.ELEMENT_PROPERTY),    element);
			created.setProperty(principalsKey, principals != null ? principals : List.of());
			return;
		}

		// One survivor gets the new state; extras get deleted.
		final NodeInterface keep = ofKind.get(0);
		keep.setProperty(perfTraits.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY), hasExpr ? expression : null);
		keep.setProperty(principalsKey, principals != null ? principals : List.of());

		boolean first = true;

		for (final NodeInterface p : ofKind) {

			// Skip the survivor (first element); delete the extras. Index-free so the
			// choice of List implementation stays O(n).
			if (first) {

				first = false;
				continue;
			}
			app.delete(p);
		}
		// `others` is read-only here -- they remain attached untouched.
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
