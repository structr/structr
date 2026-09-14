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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Ticket 1588: storeParameterValues() sent every submitted field whose name happened to be a property
 * of the subject type to elevatedSubject.setProperties(SecurityContext.getSuperUserInstance(), ...),
 * excluding only "id" and "type". Superuser writes skip the readOnly and systemInternal checks, so the
 * declaration a property carries did not protect it either - a task form could set visibility, owner,
 * or on a Principal subject isAdmin and password.
 *
 * <p>What a step may write is already declared in the graph: SubjectTypeSynthesizer wires each user
 * task to its form view, and to a writable view whenever the vendor marked a field read-only.
 */
public class ProcessSubjectFieldTest extends AbstractProcessEngineTest {

	@Test
	public void testUndeclaredFieldsNeverReachTheSubject() throws Exception {

		final String instId = startFormsProcess();

		final Map<String, Object> submitted = new LinkedHashMap<>();

		submitted.put("title", "a title");
		submitted.put("amount", 5);

		// not part of any form of this process - and it decides who can see the node
		submitted.put("visibleToPublicUsers", true);

		try (final Tx tx = app.tx()) {

			engine().completeTask(anyTaskAt(app.getNodeById(instId), "Task_Antrag"), submitted);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = subjectOf(app.getNodeById(instId));

			assertNotNull("the process must have a subject", subject);

			assertEquals("a declared field must still be written", "a title", subject.getProperty(subject.getTraits().key("title")));
			assertFalse("an undeclared field must not reach the subject", subject.isVisibleToPublicUsers());

			tx.success();
		}
	}

	@Test
	public void testFieldsTheStepDeclaresReadOnlyAreNotWritten() throws Exception {

		final String instId = startFormsProcess();

		// step one declares title and amount, both writable
		try (final Tx tx = app.tx()) {

			engine().completeTask(anyTaskAt(app.getNodeById(instId), "Task_Antrag"), Map.of("title", "a title", "amount", 5));
			tx.success();
		}

		/* Step two shows amount but marks it read-only, so the synthesizer carved out
		   write_Task_Approval = "approved". Submitting amount there is the same kind of overreach as an
		   undeclared field: the form said the user may look at it, not change it. */
		try (final Tx tx = app.tx()) {

			engine().completeTask(anyTaskAt(app.getNodeById(instId), "Task_Approval"), Map.of("approved", true, "amount", 999));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface subject = subjectOf(app.getNodeById(instId));

			/* Object, not inline: getProperty() is generic, so String.valueOf(...) on its result lets the
			   compiler infer T as char[] and pick that overload - which then fails at runtime. */
			final Object approved = subject.getProperty(subject.getTraits().key("approved"));
			final Object amount   = subject.getProperty(subject.getTraits().key("amount"));

			assertEquals("the writable field of the step must be written", Boolean.TRUE, approved);
			assertEquals("a field the step declares read-only must keep its value", "5", String.valueOf(amount));

			tx.success();
		}
	}

	// ----- private methods -----
	private String startFormsProcess() throws FrameworkException {

		final String procUuid = importProcess("/camunda-forms.bpmn");

		try (final Tx tx = app.tx()) {

			// the type is synthesized from the process's Camunda forms during the import above
			final NodeInterface subject = app.create("Beschaffungsanforderung");
			final String instId         = engine().startProcess(app.getNodeById(procUuid), subject).getUuid();

			tx.success();

			return instId;
		}
	}
}
