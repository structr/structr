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
package org.structr.test.cron;

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/** A configured task really runs on its schedule, and as the superuser. */
public class CronServiceTest extends StructrUiTest {

	private static final String TASK = "cronServiceTestTask";

	@Test
	public void testTaskRunsOnScheduleAsSuperuser() throws Exception {

		createFunction(TASK, "{ $.create('Group', { name: 'cron run by ' + $.me.name }); }");

		Settings.CronTasks.setValue(TASK);
		Settings.createSettingForValue(Settings.cronGroup, TASK + ".cronExpression", "* * * * * *");

		try {

			// the service reads its tasks when it starts
			Services.getInstance().shutdownService("CronService.default");
			Services.getInstance().startService("CronService.default");

			final String expected = "cron run by " + Settings.SuperUserName.getValue();
			List<NodeInterface> runs = List.of();

			// the service waits 5 s after startup, then the task runs every second
			for (int i = 0; i < 60 && runs.size() < 2; i++) {

				Thread.sleep(500);

				runs = groupsNamed(expected);
			}

			assertFalse("the task did not run within 30 s", runs.isEmpty());
			assertTrue("the task ran only once in 30 s, not every second", runs.size() >= 2);

		} finally {

			Settings.CronTasks.setValue("");

			Services.getInstance().shutdownService("CronService.default");
			Services.getInstance().startService("CronService.default");
		}
	}

	// ----- private methods -----
	private List<NodeInterface> groupsNamed(final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> groups = app.nodeQuery(StructrTraits.GROUP).name(name).getAsList();

			tx.success();

			return groups;
		}
	}

	private void createFunction(final String name, final String source) {

		final Traits traits = Traits.of(StructrTraits.SCHEMA_METHOD);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception creating the function: " + fex.getMessage());
		}
	}
}
