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

import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;
import org.structr.process.traits.definitions.TaskInstanceTraitDefinition;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Human-task lifecycle tests for the {@link org.structr.process.engine.ProcessEngine}:
 * performer resolution (humanPerformer / potentialOwner / initiator fallback) and the
 * claim / release / decline / delegate / assign / make-available / cancel / complete
 * operations, including their authorization rules.
 */
public class ProcessEngineTaskTest extends AbstractProcessEngineTest {

	// ------------------------------------------------------------------
	// Performer resolution
	// ------------------------------------------------------------------

	@Test
	public void testHumanPerformerAssignsToInitiator() throws Exception {

		final String procUuid    = importProcess("/engine-human-task.bpmn");
		final NodeInterface init  = createUser("initiator");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engineAs(init).startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(instId), "Task_Fill");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertEquals(init.getUuid(), assigneeOf(task).getUuid());
			assertEquals(TaskInstanceTraitDefinition.SET_BY_BPMN, task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY)));
			tx.success();
		}
	}

	@Test
	public void testPotentialOwnerCreatesAvailableTaskWithCandidates() throws Exception {

		createGroup("Reviewers");
		final String procUuid = importProcess("/engine-candidate-task.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_AVAILABLE, taskStatus(task));
			assertTrue("the Reviewers group should be a candidate assignee", candidateAssigneeIds(task).size() == 1);
			tx.success();
		}
	}

	@Test
	public void testDefaultAssigneeFromInitiatorFallback() throws Exception {

		// simple-approval's user task has no performers; with the flag set the task
		// falls back to the initiator.
		final String procUuid = importProcess("/simple-approval.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface proc = app.getNodeById(procUuid);
			proc.setProperty(proc.getTraits().key(BpmnProcessTraitDefinition.DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY), true);
			tx.success();
		}

		final NodeInterface init = createUser("initiator");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engineAs(init).startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(instId), "UserTask_1");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertEquals(init.getUuid(), assigneeOf(task).getUuid());
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Claim
	// ------------------------------------------------------------------

	@Test
	public void testClaimByGroupMember() throws Exception {

		final Ctx c = startCandidateTask();

		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(c.instId), "Task_Review");
			engineAs(c.member).claimTask(task);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertEquals(c.member.getUuid(), assigneeOf(task).getUuid());
			assertEquals(TaskInstanceTraitDefinition.SET_BY_SELF, task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY)));
			tx.success();
		}
	}

	@Test
	public void testClaimByNonMemberFails() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface outsider = createUser("outsider");

		try (final Tx tx = app.tx()) {

			try {

				engineAs(outsider).claimTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
				fail("expected 403 claiming as a non-candidate");

			} catch (final FrameworkException expected) {

				assertEquals(403, expected.getStatus());
			}

			tx.success();
		}
	}

	@Test
	public void testClaimAlreadyReservedFails() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface member2 = createUser("member2");

		addToGroup(c.group, member2);

		try (final Tx tx = app.tx()) {

			engineAs(c.member).claimTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			try {

				engineAs(member2).claimTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"));
				fail("expected 422 claiming a task that is no longer available");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Release
	// ------------------------------------------------------------------

	@Test
	public void testReleaseReturnsTaskToPool() throws Exception {

		final Ctx c = startCandidateTask();

		try (final Tx tx = app.tx()) {

			engineAs(c.member).claimTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engineAs(c.member).releaseTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_AVAILABLE, taskStatus(task));
			assertNull(assigneeOf(task));
			tx.success();
		}
	}

	@Test
	public void testReleaseByNonAssigneeFails() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface member2 = createUser("member2");

		addToGroup(c.group, member2);

		try (final Tx tx = app.tx()) {

			engineAs(c.member).claimTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			try {

				engineAs(member2).releaseTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"));
				fail("expected 403 releasing a task held by someone else");

			} catch (final FrameworkException expected) {

				assertEquals(403, expected.getStatus());
			}

			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Decline
	// ------------------------------------------------------------------

	@Test
	public void testDeclineRecordsVoteWithoutChangingStatus() throws Exception {

		final Ctx c = startCandidateTask();

		try (final Tx tx = app.tx()) {

			engineAs(c.member).declineTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_AVAILABLE, taskStatus(task));
			assertTrue(declinedByIds(task).contains(c.member.getUuid()));
			tx.success();
		}
	}

	@Test
	public void testClaimSupersedesPriorDecline() throws Exception {

		final Ctx c = startCandidateTask();

		try (final Tx tx = app.tx()) {

			engineAs(c.member).declineTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engineAs(c.member).claimTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertFalse("claim should remove the caller from declinedBy", declinedByIds(task).contains(c.member.getUuid()));
			tx.success();
		}
	}

	@Test
	public void testDeclineByNonCandidateFails() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface outsider = createUser("outsider");

		try (final Tx tx = app.tx()) {

			try {

				engineAs(outsider).declineTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
				fail("expected 403 declining as a non-candidate");

			} catch (final FrameworkException expected) {

				assertEquals(403, expected.getStatus());
			}

			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Delegate
	// ------------------------------------------------------------------

	@Test
	public void testDelegateFromAssignee() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface delegate = createUser("delegate");

		try (final Tx tx = app.tx()) {

			engineAs(c.member).claimTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engineAs(c.member).delegateTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(delegate.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertEquals(delegate.getUuid(), assigneeOf(task).getUuid());
			assertEquals(TaskInstanceTraitDefinition.SET_BY_DELEGATION, task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY)));
			tx.success();
		}
	}

	@Test
	public void testDelegateAvailableTaskByCandidate() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface delegate = createUser("delegate");

		try (final Tx tx = app.tx()) {

			engineAs(c.member).delegateTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(delegate.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertEquals(delegate.getUuid(), assigneeOf(task).getUuid());
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Administrative assign / make-available / cancel
	// ------------------------------------------------------------------

	@Test
	public void testAssignTaskToArbitraryUser() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface other = createUser("non-candidate");

		try (final Tx tx = app.tx()) {

			engine().assignTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(other.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_RESERVED, taskStatus(task));
			assertEquals(other.getUuid(), assigneeOf(task).getUuid());
			assertEquals(TaskInstanceTraitDefinition.SET_BY_ADMIN, task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.ASSIGNEE_SET_BY_PROPERTY)));
			tx.success();
		}
	}

	@Test
	public void testAssignTaskRevokesPreviousAssigneeGrant() throws Exception {

		// Reassigning a task has to take the previous assignee's access away with it. The engine
		// grants read+write to whoever holds a task, so without the revoke the previous assignee
		// keeps both on a task that is now somebody else's - and nothing in the task itself shows
		// it, because status and assignee are already correct.
		final Ctx c                = startCandidateTask();
		final NodeInterface first  = createUser("first-assignee");
		final NodeInterface second = createUser("second-assignee");

		try (final Tx tx = app.tx()) {

			engine().assignTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(first.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");

			assertTrue("assignee must hold read on their task",  directGrant(task, first).contains(Permission.read.name()));
			assertTrue("assignee must hold write on their task", directGrant(task, first).contains(Permission.write.name()));

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().assignTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(second.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");

			assertEquals(second.getUuid(), assigneeOf(task).getUuid());

			assertTrue("previous assignee must keep no direct grant on a task that is no longer theirs", directGrant(task, first).isEmpty());
			assertFalse("previous assignee must lose write", task.as(AccessControllable.class).isGranted(Permission.write, userContext(first)));

			assertTrue("new assignee must hold read",  directGrant(task, second).contains(Permission.read.name()));
			assertTrue("new assignee must hold write", directGrant(task, second).contains(Permission.write.name()));

			tx.success();
		}
	}

	@Test
	public void testMakeTaskAvailableRevokesPreviousAssigneeGrant() throws Exception {

		// Same rule when the task goes back into the pool: the holder is no longer the holder.
		final Ctx c               = startCandidateTask();
		final NodeInterface other = createUser("non-candidate");

		try (final Tx tx = app.tx()) {

			engine().assignTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(other.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().makeTaskAvailable(anyTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");

			assertTrue("released assignee must keep no direct grant", directGrant(task, other).isEmpty());
			assertFalse("released assignee must lose write", task.as(AccessControllable.class).isGranted(Permission.write, userContext(other)));

			tx.success();
		}
	}

	/**
	 * The permissions directly granted on a node, i.e. the Security relationship itself. Deliberately
	 * not isGranted(): read also reaches a task indirectly, because TaskInstanceOfProcess propagates
	 * read In with PropagationMode.Add, so a participant who keeps read on the ProcessInstance keeps
	 * read on its tasks. That is the design - having taken part in a process lets you see it - and it
	 * is write, plus the direct grant, that reassignment has to take away.
	 */
	private Set<String> directGrant(final NodeInterface node, final NodeInterface user) throws FrameworkException {

		final Security security = node.as(AccessControllable.class).getSecurityRelationship(user.as(Principal.class));

		return security == null ? Set.of() : security.getPermissions();
	}

	@Test
	public void testMakeTaskAvailableClearsAssignee() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface other = createUser("non-candidate");

		try (final Tx tx = app.tx()) {

			engine().assignTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"), app.getNodeById(other.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().makeTaskAvailable(anyTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = anyTaskAt(app.getNodeById(c.instId), "Task_Review");
			assertEquals(TaskInstanceTraitDefinition.STATUS_AVAILABLE, taskStatus(task));
			assertNull(assigneeOf(task));
			tx.success();
		}
	}

	@Test
	public void testCancelTaskDoesNotAdvanceProcess() throws Exception {

		final Ctx c = startCandidateTask();

		try (final Tx tx = app.tx()) {

			engine().cancelTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(c.instId);
			final NodeInterface task = anyTaskAt(inst, "Task_Review");

			assertEquals(TaskInstanceTraitDefinition.STATUS_CANCELLED, taskStatus(task));
			assertNotNull(task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.CANCELLED_TIME_PROPERTY)));
			// The instance keeps running (admin must intervene explicitly), no tokens active.
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			assertEquals(0, tokenCount(inst, org.structr.process.traits.definitions.ProcessTokenTraitDefinition.STATUS_WAITING));
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Complete
	// ------------------------------------------------------------------

	@Test
	public void testCompleteAfterClaimAdvancesProcess() throws Exception {

		final Ctx c = startCandidateTask();

		try (final Tx tx = app.tx()) {

			engineAs(c.member).claimTask(openTaskAt(app.getNodeById(c.instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engineAs(c.member).completeTask(anyTaskAt(app.getNodeById(c.instId), "Task_Review"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(c.instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			assertEquals(TaskInstanceTraitDefinition.STATUS_COMPLETED, taskStatus(anyTaskAt(inst, "Task_Review")));
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Authorization gates on the admin JavaMethod entry points
	//
	// The other tests call the engine directly, which has no permission gate.
	// These drive the actual JavaMethod entry points (via invokeMethod) to verify
	// the accessControl checks that guard the admin operations.
	// ------------------------------------------------------------------

	@Test
	public void testCancelMethodRequiresAccessControl() throws Exception {

		final Ctx c = startCandidateTask();
		final String taskId;

		try (final Tx tx = app.tx()) {

			taskId = openTaskAt(app.getNodeById(c.instId), "Task_Review").getUuid();
			tx.success();
		}

		// The candidate member has read+write but NOT accessControl -> the 'cancel'
		// method's gate rejects with 403 (a direct engine call would have allowed it).
		try (final Tx tx = app.tx()) {

			try {

				invokeMethod(userContext(c.member), app.getNodeById(taskId), "cancel", Map.of(), false);
				fail("expected 403 invoking 'cancel' without accessControl");

			} catch (final FrameworkException expected) {

				assertEquals(403, expected.getStatus());
			}

			tx.success();
		}

		// The super user holds accessControl -> the method proceeds and cancels the task.
		try (final Tx tx = app.tx()) {

			invokeMethod(securityContext, app.getNodeById(taskId), "cancel", Map.of(), false);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(TaskInstanceTraitDefinition.STATUS_CANCELLED, taskStatus(app.getNodeById(taskId)));
			tx.success();
		}
	}

	@Test
	public void testMakeAvailableAndAssignTaskRequireAccessControl() throws Exception {

		final Ctx c = startCandidateTask();
		final NodeInterface other = createUser("non-admin-target");

		final String taskId;

		try (final Tx tx = app.tx()) {

			taskId = openTaskAt(app.getNodeById(c.instId), "Task_Review").getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			try {

				invokeMethod(userContext(c.member), app.getNodeById(taskId), "makeAvailable", Map.of(), false);
				fail("expected 403 invoking 'makeAvailable' without accessControl");

			} catch (final FrameworkException expected) {

				assertEquals(403, expected.getStatus());
			}

			try {

				invokeMethod(userContext(c.member), app.getNodeById(taskId), "assignTask", Map.of("assignee", other.getUuid()), false);
				fail("expected 403 invoking 'assignTask' without accessControl");

			} catch (final FrameworkException expected) {

				assertEquals(403, expected.getStatus());
			}

			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// completeWithSubject: get-or-create semantics
	// ------------------------------------------------------------------

	@Test
	public void testCompleteWithSubjectCreatesSubjectWhenAbsent() throws Exception {

		createSubjectType("Claim", "title");

		final NodeInterface init = createUser("initiator");
		final String procUuid    = importProcess("/engine-human-task.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engineAs(init).startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(instId), "Task_Fill");
			invokeMethod(userContext(init), task, "completeWithSubject", Map.of("subjectType", "Claim", "title", "first"), false);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = subjectOf(app.getNodeById(instId));
			assertNotNull("completeWithSubject with no existing subject must create one", subject);
			assertTrue("created subject should be of the declared type", subject.is("Claim"));
			assertEquals("first", subject.getProperty(subject.getTraits().key("title")));
			assertEquals("exactly one subject node should exist", 1, app.nodeQuery("Claim").getAsList().size());
			tx.success();
		}
	}

	@Test
	public void testCompleteWithSubjectUpdatesExistingSubjectInsteadOfReplacing() throws Exception {

		createSubjectType("Claim", "title");

		final NodeInterface init = createUser("initiator");
		final String procUuid    = importProcess("/engine-human-task.bpmn");

		final String instId;
		final String existingSubjectId;

		try (final Tx tx = app.tx()) {

			instId = engineAs(init).startProcess(app.getNodeById(procUuid), null).getUuid();

			// simulate an earlier step having already created the subject
			final NodeInterface existing = app.create("Claim");
			existing.setProperty(existing.getTraits().key("title"), "original");
			final NodeInterface instance = app.getNodeById(instId);
			instance.setProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY), existing);
			existingSubjectId = existing.getUuid();

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(instId), "Task_Fill");
			invokeMethod(userContext(init), task, "completeWithSubject", Map.of("subjectType", "Claim", "title", "edited"), false);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = subjectOf(app.getNodeById(instId));
			assertNotNull(subject);
			assertEquals("the existing subject must be kept, not replaced", existingSubjectId, subject.getUuid());
			assertEquals("its fields must be updated from the form", "edited", subject.getProperty(subject.getTraits().key("title")));
			assertEquals("no second subject node may be created", 1, app.nodeQuery("Claim").getAsList().size());
			tx.success();
		}
	}

	/** Create a minimal subject SchemaNode with one String property, usable as a process subject. */
	private void createSubjectType(final String typeName, final String propertyName) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface type = app.create(StructrTraits.SCHEMA_NODE, typeName);
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), propertyName),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), type)
			);
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	/** A started candidate-task instance with its group and one member. */
	private static final class Ctx {

		final String instId;
		final NodeInterface group;
		final NodeInterface member;
		Ctx(final String instId, final NodeInterface group, final NodeInterface member) {

			this.instId = instId; this.group = group; this.member = member;
		}
	}

	private Ctx startCandidateTask() throws FrameworkException {

		final NodeInterface group  = createGroup("Reviewers");
		final NodeInterface member = createUser("reviewer");

		addToGroup(group, member);
		final String procUuid = importProcess("/engine-candidate-task.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		return new Ctx(instId, group, member);
	}

	private Set<String> candidateAssigneeIds(final NodeInterface task) throws FrameworkException {

		final Set<String> ids = new HashSet<>();

		for (final NodeInterface n : collect(task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.CANDIDATE_ASSIGNEES_PROPERTY)))) {

			ids.add(n.getUuid());
		}

		return ids;
	}

	private Set<String> declinedByIds(final NodeInterface task) throws FrameworkException {

		final Set<String> ids = new HashSet<>();

		for (final NodeInterface n : collect(task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.DECLINED_BY_PROPERTY)))) {

			ids.add(n.getUuid());
		}

		return ids;
	}
}
