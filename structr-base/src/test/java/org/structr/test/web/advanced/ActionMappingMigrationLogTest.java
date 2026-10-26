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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * The migration links each ActionMapping to the SchemaMethod it calls. A mapping that cannot be linked still works,
 * since the method is looked up by name at runtime, so the migration only reports a method name that matches nothing.
 */
public class ActionMappingMigrationLogTest extends StructrUiTest {

	@Test
	public void testValidMappingsWithoutLinkAreNotReported() {

		try {

			createSchema("ItemA", "ItemB");

			// called on objects of different types, so there is no single data type to declare
			final String polymorphic = createMapping("doSomething", null);
			final String expression  = createMapping("doSomething", "${current.type}");
			final String builtIn     = createMapping("doCSVImport", null);

			final List<String> messages = migrate();

			assertEquals("The migration reported a method that exists only on types although the mapping works", List.of(), messagesFor(messages, polymorphic));
			assertEquals("The migration reported a data type that is a script expression", List.of(), messagesFor(messages, expression));
			assertEquals("The migration reported a built-in Java method", List.of(), messagesFor(messages, builtIn));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMissingMethodIsReported() {

		try {

			final String missing = createMapping("doesNotExist", null);

			assertEquals("The migration did not report a method name that matches nothing exactly once", 1, messagesFor(migrate(), missing).size());

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----
	private void createSchema(final String... typeNames) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			for (final String typeName : typeNames) {

				schema.addType(typeName).addMethod("doSomething", "{ return 1; }");
			}

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}
	}

	private String createMapping(final String method, final String dataType) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.ACTION_MAPPING);

		try (final Tx tx = app.tx()) {

			final String id = app.create(StructrTraits.ACTION_MAPPING,
				new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), "method"),
				new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.METHOD_PROPERTY), method),
				new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), dataType)
			).getUuid();

			tx.success();

			return id;
		}
	}

	/**
	 * Runs the migration and returns what it logged at INFO or above.
	 */
	private List<String> migrate() throws FrameworkException {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(MigrationService.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			MigrationService.execute(false);

		} finally {

			logger.detachAppender(appender);
			appender.stop();
		}

		return appender.list.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.INFO)).map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
	}

	private List<String> messagesFor(final List<String> messages, final String id) {

		return messages.stream().filter(message -> message.contains(id)).collect(Collectors.toList());
	}
}
