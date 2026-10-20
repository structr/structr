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

import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ToExcelMigrationHandler;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

public class ExcelTest extends StructrUiTest {

	@Test
	public void testExcelFunctions() {

		try (final Tx tx = app.tx()) {

			final Principal admin = createAdminUser().as(Principal.class);
			final ActionContext actionContext = new ActionContext(SecurityContext.getInstance(admin, AccessMode.Backend));

			final String createExcelFileScript = """
					${
						setContent(
							create('File', 'name', 'excel_test.xlsx'),
							toExcel(
								find('User'), { view: 'public', sheetName: 'Users Public View' },
								find('User'), { properties: ['name', 'type', 'id'], sheetName: 'Users custom properties' },
								merge(
									{ name: 'Foo', 'test': 'str1', 'test2': 'str2' },
									{ name: 'Bar', 'test': 'str3', 'test2': 'str4' }
								), { properties: ['name', 'test', 'test2'], sheetName: 'Maps custom Properties' },
								merge(
									{ name: 'Foo', 'test': 'str1', 'test2': 'str2' },
									{ name: 'Bar', 'test': 'str3', 'test2': 'str4' }
								), { properties: ['name', 'test', 'test2'], sheetName: 'Silly Truncate', maxCellLength: 2, overflowMode: 't' },
							),
							'ISO-8859-1'
						)
					}
					""";

			Scripting.evaluate(actionContext, null, createExcelFileScript, "createExcelFileScript");

			final String readExcelFileScript = """
					${{
						let excelFile = $.find('File', 'name', 'excel_test.xlsx')[0];
					
						$.assert(!$.empty(excelFile), 422, 'Expected excel file to be present');
					
						{
							let data = $.fromExcel(excelFile, 'Users Public View');

							$.assert(data.length === 1, 422, 'Sheet "Users Public View" should contain exactly 1 entry');

							$.assert(data[0].id === $.me.id, 422, 'First entry in sheet "Users Public View" should have current users id');
							$.assert(data[0].name === $.me.name, 422, 'First entry in sheet "Users Public View" should have current users name');
							$.assert(data[0].type === $.me.type, 422, 'First entry in sheet "Users Public View" should have current users type');
						}

						{
							let data = $.fromExcel(excelFile, 'Users custom properties');

							$.assert(data.length === 1, 422, 'Sheet "Users custom properties" should contain exactly 1 entry');

							$.assert(data[0].id === $.me.id, 422, 'First entry in sheet "Users custom properties" should have current users id');
							$.assert(data[0].name === $.me.name, 422, 'First entry in sheet "Users custom properties" should have current users name');
							$.assert(data[0].type === $.me.type, 422, 'First entry in sheet "Users custom properties" should have current users type');
						}

						{
							let data = $.fromExcel(excelFile, 'Maps custom Properties');

							$.assert(data.length === 2, 422, 'Sheet "Maps custom Properties" should contain exactly 2 entries');

							$.assert(data[0].name === 'Foo', 422, 'First entry in sheet "Maps custom Properties" should have name="Foo".');
							$.assert(data[0].test === 'str1', 422, 'First entry in sheet "Maps custom Properties" should have test="str1".');
							$.assert(data[0].test2 === 'str2', 422, 'First entry in sheet "Maps custom Properties" should have test2="str2".');

							$.assert(data[1].name === 'Bar', 422, 'First entry in sheet "Maps custom Properties" should have name="Bar".');
							$.assert(data[1].test === 'str3', 422, 'First entry in sheet "Maps custom Properties" should have test="str3".');
							$.assert(data[1].test2 === 'str4', 422, 'First entry in sheet "Maps custom Properties" should have test2="str4".');
						}

						{
							let data = $.fromExcel(excelFile, 'Silly Truncate');

							$.assert(data.length === 2, 422, 'Sheet "Silly Truncate" should contain exactly 2 entries');

							$.assert(data[0].name === 'Fo', 422, 'First entry in sheet "Silly Truncate" should have name="Fo".');
							$.assert(data[0].test === 'st', 422, 'First entry in sheet "Silly Truncate" should have test="st".');
							$.assert(data[0].test2 === 'st', 422, 'First entry in sheet "Silly Truncate" should have test2="st".');

							$.assert(data[1].name === 'Ba', 422, 'First entry in sheet "Silly Truncate" should have name="Ba".');
							$.assert(data[1].test === 'st', 422, 'First entry in sheet "Silly Truncate" should have test="st".');
							$.assert(data[1].test2 === 'st', 422, 'First entry in sheet "Silly Truncate" should have test2="st".');
						}

						'Success';
					}}
					""";

			assertEquals("Expected 'Success' message for readExcelFile script. This depends on the StructrScript array syntax working inside the map syntax when creating the excel file", "Success", Scripting.evaluate(actionContext, null, readExcelFileScript, "readExcelFileScript"));

			tx.success();

		} catch(UnlicensedScriptException | FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testMigratedCallsProduceWorkbooks() {

		// the migration rewrites text; this is what shows the rewritten text actually works with the new function
		try (final Tx tx = app.tx()) {

			final Principal admin = createAdminUser().as(Principal.class);
			final ActionContext actionContext = new ActionContext(SecurityContext.getInstance(admin, AccessMode.Backend));

			final String structrScript = migrate("${setContent(create('File', 'name', 'migrated_structrscript.xlsx'), toExcel(find('User'), ['name', 'id'], true, false, '', 2, 't'), 'ISO-8859-1')}");
			final String javaScript    = migrate("${{ $.setContent($.create('File', 'name', 'migrated_javascript.xlsx'), $.toExcel($.find('User'), 'public'), 'ISO-8859-1'); }}");

			Scripting.evaluate(actionContext, null, structrScript, "structrScript");
			Scripting.evaluate(actionContext, null, javaScript, "javaScript");

			final String readExcelFileScript = """
					${{
						{
							let data = $.fromExcel($.find('File', 'name', 'migrated_structrscript.xlsx')[0], 'Sheet0');

							$.assert(data.length === 1, 422, 'StructrScript sheet should contain exactly 1 entry');
							$.assert(data[0].name === $.me.name.substring(0, 2), 422, 'StructrScript sheet should contain the truncated name');
							$.assert(data[0].id === $.me.id.substring(0, 2), 422, 'StructrScript sheet should contain the truncated id');
						}

						{
							let data = $.fromExcel($.find('File', 'name', 'migrated_javascript.xlsx')[0], 'Sheet0');

							$.assert(data.length === 1, 422, 'JavaScript sheet should contain exactly 1 entry');
							$.assert(data[0].id === $.me.id, 422, 'JavaScript sheet should contain the current users id');
							$.assert(data[0].type === $.me.type, 422, 'JavaScript sheet should contain the current users type');
						}

						'Success';
					}}
					""";

			assertEquals("Expected 'Success' message for readExcelFile script", "Success", Scripting.evaluate(actionContext, null, readExcelFileScript, "readExcelFileScript"));

			tx.success();

		} catch(UnlicensedScriptException | FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	private String migrate(final String source) {

		String migrated = source;

		for (final String call : ToExcelMigrationHandler.findCalls(source)) {

			final String rewritten = ToExcelMigrationHandler.rewrite(call);

			assertNotNull("expected " + call + " to be rewritten automatically", rewritten);

			migrated = migrated.replace(call, rewritten);
		}

		assertFalse("expected the source to change", source.equals(migrated));

		return migrated;
	}
}