/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.test.traits.definitions.CsvTestOneTraitDefinition;
import org.structr.test.traits.definitions.CsvTestTwoTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class CsvFunctionsTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(CsvFunctionsTest.class.getName());

	@Override
	@BeforeMethod(firstTimeOnly = true)
	public void createSchema() {

		StructrTraits.registerNodeType("CsvTestOne", new CsvTestOneTraitDefinition());
		StructrTraits.registerNodeType("CsvTestTwo", new CsvTestTwoTraitDefinition());
	}

	@Test
	public void testCsvFunctions() {

		List<NodeInterface> csvTestOnes                = null;
		NodeInterface csvTestTwo                       = null;
		int index                                   = 0;

		try (final Tx tx = app.tx()) {

			csvTestOnes = createTestNodes("CsvTestOne", 5);
			csvTestTwo  = createTestNode("CsvTestTwo");

			final Traits localizationTraits                         = Traits.of("Localization");
			final Traits testOneTraits                              = Traits.of("CsvTestOne");
			final PropertyMap indexLocalizationProperties           = new PropertyMap();
			final PropertyMap nameLocalizationProperties            = new PropertyMap();
			final PropertyMap indexLocalizationPropertiesWithDomain = new PropertyMap();
			final PropertyMap nameLocalizationPropertiesWithDomain  = new PropertyMap();

			indexLocalizationProperties.put(localizationTraits.key("name"),                    "index");
			indexLocalizationProperties.put(localizationTraits.key("localizedName"),           "Localized INDEX");
			indexLocalizationProperties.put(localizationTraits.key("locale"),                  "en");

			nameLocalizationProperties.put(localizationTraits.key("name"),                     "name");
			nameLocalizationProperties.put(localizationTraits.key("localizedName"),            "Localized NAME");
			nameLocalizationProperties.put(localizationTraits.key("locale"),                   "en");

			indexLocalizationPropertiesWithDomain.put(localizationTraits.key("name"),          "index");
			indexLocalizationPropertiesWithDomain.put(localizationTraits.key("localizedName"), "Localized INDEX with DOMAIN");
			indexLocalizationPropertiesWithDomain.put(localizationTraits.key("locale"),        "en");
			indexLocalizationPropertiesWithDomain.put(localizationTraits.key("domain"),        "CSV TEST Domain");

			nameLocalizationPropertiesWithDomain.put(localizationTraits.key("name"),           "name");
			nameLocalizationPropertiesWithDomain.put(localizationTraits.key("localizedName"),  "Localized NAME with DOMAIN");
			nameLocalizationPropertiesWithDomain.put(localizationTraits.key("locale"),         "en");
			nameLocalizationPropertiesWithDomain.put(localizationTraits.key("domain"),         "CSV TEST Domain");

			app.create("Localization", indexLocalizationProperties);
			app.create("Localization", nameLocalizationProperties);
			app.create("Localization", indexLocalizationPropertiesWithDomain);
			app.create("Localization", nameLocalizationPropertiesWithDomain);

			for (final NodeInterface csvTestOne : csvTestOnes) {

				csvTestOne.setProperty(testOneTraits.key("name"), "CSV Test Node " + StringUtils.leftPad(Integer.toString(index+1), 4, "0"));
				csvTestOne.setProperty(testOneTraits.key("index"), index+1);

				if (index == 0) {
					// set string array on test four
					csvTestOne.setProperty(testOneTraits.key("stringArrayProperty"), new String[] { "one", "two", "three", "four" } );
				}

				csvTestOne.setProperty(testOneTraits.key("intArrayProperty"), new Integer[] { index, index+1, index+2, index+3 } );

				if (index == 2) {
					// set string array on test four
					csvTestOne.setProperty(testOneTraits.key("enumProperty"), "EnumValue2");
				}

				index++;
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);
			ctx.setLocale(Locale.ENGLISH);

			/**
			 * The expected results
			 */
			final String expectedDefaultCsv = "\"name\";\"index\";\"type\";\"stringArrayProperty\";\"enumProperty\"\n"
					+ "\"CSV Test Node 0001\";\"1\";\"CsvTestOne\";\"[\\\"one\\\", \\\"two\\\", \\\"three\\\", \\\"four\\\"]\";\"\"\n"
					+ "\"CSV Test Node 0002\";\"2\";\"CsvTestOne\";\"\";\"\"\n"
					+ "\"CSV Test Node 0003\";\"3\";\"CsvTestOne\";\"\";\"EnumValue2\"\n"
					+ "\"CSV Test Node 0004\";\"4\";\"CsvTestOne\";\"\";\"\"\n"
					+ "\"CSV Test Node 0005\";\"5\";\"CsvTestOne\";\"\";\"\"\n";
			final String expectedCsvWithNameAndIndex = "\"name\";\"index\"\n"
					+ "\"CSV Test Node 0001\";\"1\"\n"
					+ "\"CSV Test Node 0002\";\"2\"\n"
					+ "\"CSV Test Node 0003\";\"3\"\n"
					+ "\"CSV Test Node 0004\";\"4\"\n"
					+ "\"CSV Test Node 0005\";\"5\"\n";
			final String expectedCsvWithIndexAndName = "\"index\";\"name\"\n"
					+ "\"1\";\"CSV Test Node 0001\"\n"
					+ "\"2\";\"CSV Test Node 0002\"\n"
					+ "\"3\";\"CSV Test Node 0003\"\n"
					+ "\"4\";\"CSV Test Node 0004\"\n"
					+ "\"5\";\"CSV Test Node 0005\"\n";
			final String expectedCsvWithNameAndIndexAndCustomDelimiterPipe = "\"name\"|\"index\"\n"
					+ "\"CSV Test Node 0001\"|\"1\"\n"
					+ "\"CSV Test Node 0002\"|\"2\"\n"
					+ "\"CSV Test Node 0003\"|\"3\"\n"
					+ "\"CSV Test Node 0004\"|\"4\"\n"
					+ "\"CSV Test Node 0005\"|\"5\"\n";
			final String expectedCsvWithNameAndIndexAndCustomDelimiterXXX = "\"name\"X\"index\"\n"
					+ "\"CSV Test Node 0001\"X\"1\"\n"
					+ "\"CSV Test Node 0002\"X\"2\"\n"
					+ "\"CSV Test Node 0003\"X\"3\"\n"
					+ "\"CSV Test Node 0004\"X\"4\"\n"
					+ "\"CSV Test Node 0005\"X\"5\"\n";
			final String expectedCsvWithIndexAndNameAndSingleQuote = "'index';'name'\n"
					+ "'1';'CSV Test Node 0001'\n"
					+ "'2';'CSV Test Node 0002'\n"
					+ "'3';'CSV Test Node 0003'\n"
					+ "'4';'CSV Test Node 0004'\n"
					+ "'5';'CSV Test Node 0005'\n";
			final String expectedCsvWithIndexAndNameAndSingleQuoteAfterRoundTrip = "'index';'name'\n"
					+ "'1';'CSV Test Node 0001'\n"
					+ "'2';'CSV Test Node 0002'\n"
					+ "'3';'CSV Test Node 0003'\n"
					+ "'4';'CSV Test Node 0004'\n"
					+ "'5';'CSV Test Node 0005'\n";
			final String expectedCsvWithIndexAndNameAndSingleQuoteAndCRLF = "'index';'name'\r\n"
					+ "'1';'CSV Test Node 0001'\r\n"
					+ "'2';'CSV Test Node 0002'\r\n"
					+ "'3';'CSV Test Node 0003'\r\n"
					+ "'4';'CSV Test Node 0004'\r\n"
					+ "'5';'CSV Test Node 0005'\r\n";
			final String expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFNoHeader = "'1';'CSV Test Node 0001'\r\n"
					+ "'2';'CSV Test Node 0002'\r\n"
					+ "'3';'CSV Test Node 0003'\r\n"
					+ "'4';'CSV Test Node 0004'\r\n"
					+ "'5';'CSV Test Node 0005'\r\n";
			final String expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithExplicitHeader = "'index';'name'\r\n"
					+ "'1';'CSV Test Node 0001'\r\n"
					+ "'2';'CSV Test Node 0002'\r\n"
					+ "'3';'CSV Test Node 0003'\r\n"
					+ "'4';'CSV Test Node 0004'\r\n"
					+ "'5';'CSV Test Node 0005'\r\n";
			final String expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithLocalizedHeader = "'Localized INDEX';'Localized NAME'\r\n"
					+ "'1';'CSV Test Node 0001'\r\n"
					+ "'2';'CSV Test Node 0002'\r\n"
					+ "'3';'CSV Test Node 0003'\r\n"
					+ "'4';'CSV Test Node 0004'\r\n"
					+ "'5';'CSV Test Node 0005'\r\n";
			final String expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithLocalizedHeaderWithDomain = "'Localized INDEX with DOMAIN';'Localized NAME with DOMAIN'\r\n"
					+ "'1';'CSV Test Node 0001'\r\n"
					+ "'2';'CSV Test Node 0002'\r\n"
					+ "'3';'CSV Test Node 0003'\r\n"
					+ "'4';'CSV Test Node 0004'\r\n"
					+ "'5';'CSV Test Node 0005'\r\n";
			final String expectedCsvForCustomJavaScriptObjects = "\"id\";\"customField\";\"name\"\n"
					+ "\"abcd0001\";\"extra1\";\"my 1st custom object\"\n"
					+ "\"bcde0002\";\"extra2\";\"my 2nd custom object\"\n"
					+ "\"cdef0003\";\"extra3\";\"my 3rd custom object\"\n";
			final String expectedCsvForObjectsWithNewlineCharacters = "\"multi\"\n"
					+ "\"Multi\\nLine\\nTest\"\n";

			final String expectedCsvForIndexAndNameAndIntArray = "\"name\";\"index\";\"intArrayProperty\"\n"
					+ "\"CSV Test Node 0001\";\"1\";\"[\\\"0\\\", \\\"1\\\", \\\"2\\\", \\\"3\\\"]\"\n"
					+ "\"CSV Test Node 0002\";\"2\";\"[\\\"1\\\", \\\"2\\\", \\\"3\\\", \\\"4\\\"]\"\n"
					+ "\"CSV Test Node 0003\";\"3\";\"[\\\"2\\\", \\\"3\\\", \\\"4\\\", \\\"5\\\"]\"\n"
					+ "\"CSV Test Node 0004\";\"4\";\"[\\\"3\\\", \\\"4\\\", \\\"5\\\", \\\"6\\\"]\"\n"
					+ "\"CSV Test Node 0005\";\"5\";\"[\\\"4\\\", \\\"5\\\", \\\"6\\\", \\\"7\\\"]\"\n";

			/**
			 * First everything in StructrScript
			 */

			assertEquals(
					"Invalid result of default to_csv() call (StructrScript)",
					expectedDefaultCsv,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), 'csv')}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only name,index (StructrScript)",
					expectedCsvWithNameAndIndex,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('name', 'index'))}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only name,index (StructrScript)",
					expectedCsvWithIndexAndName,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'))}")
			);

			assertEquals(
					"Invalid result of to_csv() with delimiterChar = '|'. (StructrScript)",
					expectedCsvWithNameAndIndexAndCustomDelimiterPipe,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('name', 'index'), '|')}")
			);

			assertEquals(
					"Invalid result of to_csv() with delimiterChar = 'XXX'. Only first character should be used! (StructrScript)",
					expectedCsvWithNameAndIndexAndCustomDelimiterXXX,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('name', 'index'), 'XXX')}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name AND quote character = '  (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuote,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\")}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name AND quote character = ' AFTER round-trip through to_csv, from_csv and to_csv (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAfterRoundTrip,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(from_csv(to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\"), ';', \"'\"), merge('index', 'name'), ';', \"'\")}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLF,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\", '\\r\\n')}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and no header (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFNoHeader,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\", '\\r\\n', false)}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and explicit header (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithExplicitHeader,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\", '\\r\\n', true)}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and localized header (without domain) (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithLocalizedHeader,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\", '\\r\\n', true, true)}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and localized header (with domain) (StructrScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithLocalizedHeaderWithDomain,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('index', 'name'), ';', \"'\", '\\r\\n', true, true, 'CSV TEST Domain')}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only name,index,intArrayProperty (StructrScript)",
					expectedCsvForIndexAndNameAndIntArray,
					Scripting.replaceVariables(ctx, csvTestTwo, "${to_csv(find('CsvTestOne', sort('name')), merge('name', 'index', 'intArrayProperty'))}")
			);

			/**
			 * Then everything again in JavaScript
			 */

			assertEquals(
					"Invalid result of default Structr.to_csv() call (JavaScript)",
					expectedDefaultCsv,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), 'csv'))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only name,index (JavaScript)",
					expectedCsvWithNameAndIndex,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['name', 'index']))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only name,index (JavaScript)",
					expectedCsvWithIndexAndName,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name']))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() with delimiterChar = '|'. (JavaScript)",
					expectedCsvWithNameAndIndexAndCustomDelimiterPipe,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['name', 'index'], '|'))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() with delimiterChar = 'XXX'. Only first character should be used! (JavaScript)",
					expectedCsvWithNameAndIndexAndCustomDelimiterXXX,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['name', 'index'], 'XXX'))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name AND quote character = '  (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuote,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\"))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name AND quote character = ' AFTER round-trip through to_csv, from_csv and to_csv (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAfterRoundTrip,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.from_csv(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\"), ';', \"'\"), ['index', 'name'], ';', \"'\"))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLF,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\", '\\r\\n'))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and no header (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFNoHeader,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\", '\\r\\n', false))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and explicit header (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithExplicitHeader,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\", '\\r\\n', true))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and localized header (without domain) (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithLocalizedHeader,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\", '\\r\\n', true, true))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call with only index,name, singleQuoted and CRLF as recordSeparator and localized header (with domain) (JavaScript)",
					expectedCsvWithIndexAndNameAndSingleQuoteAndCRLFWithLocalizedHeaderWithDomain,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv(Structr.find('CsvTestOne', $.predicate.sort('name')), ['index', 'name'], ';', \"'\", '\\r\\n', true, true, 'CSV TEST Domain'))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call for a collection of custom objects (JavaScript)",
					expectedCsvForCustomJavaScriptObjects,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv([{id: 'abcd0001', name: 'my 1st custom object', customField: 'extra1'}, {id: 'bcde0002', name: 'my 2nd custom object', customField: 'extra2'}, {id: 'cdef0003', name: 'my 3rd custom object', customField: 'extra3'}], ['id', 'customField', 'name']))}}")
			);

			assertEquals(
					"Invalid result of Structr.to_csv() call for source objects with newlines (JavaScript)",
					expectedCsvForObjectsWithNewlineCharacters,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{Structr.print(Structr.to_csv([{multi:'Multi\\nLine\\nTest'}], ['multi']))}}")
			);

			assertEquals(
					"Invalid result of to_csv() call with only name,index,intArrayProperty (JavaScript)",
					expectedCsvForIndexAndNameAndIntArray,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{ $.print($.to_csv($.find('CsvTestOne', $.predicate.sort('name')), ['name', 'index', 'intArrayProperty'])) }}")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail(fex.getMessage());
		}


		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			final NodeInterface testTwo    = createTestNode("TestTwo");
			final NodeInterface testFive1 = createTestNode("TestFive");
			final NodeInterface testFive2 = createTestNode("TestFive");

			Scripting.replaceVariables(ctx, csvTestTwo, "${{ $.find('TestTwo', '" + testTwo.getUuid() + "').testFives.push($.find('TestFive', '" + testFive1.getUuid() + "')); }}");
			Scripting.replaceVariables(ctx, csvTestTwo, "${{ $.find('TestTwo', '" + testTwo.getUuid() + "').testFives.push($.find('TestFive', '" + testFive2.getUuid() + "')); }}");

			final String expectedIdAndTestFives = "\"id\";\"testFives\"\n"
					+ "\"" + testTwo.getUuid() + "\";\"[\\\"" + testFive1.getUuid() + "\\\", \\\"" + testFive2.getUuid() + "\\\"]\"\n";

			assertEquals(
					"Invalid result of to_csv() call with only id and linked property testFives (JavaScript)",
					expectedIdAndTestFives,
					Scripting.replaceVariables(ctx, csvTestTwo, "${{ $.print($.to_csv($.find('TestTwo'), ['id', 'testFives'])) }}")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail(fex.getMessage());
		}
	}

}
