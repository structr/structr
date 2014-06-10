/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.schema.action;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestTwo;
import org.structr.core.graph.Tx;
import org.structr.core.parser.Functions;
import org.structr.core.property.ISO8601DateProperty;

/**
 *
 * @author Christian Morgner
 */


public class ActionContextTest extends StructrTest {

	public void testVariableReplacement() {

		final Date now                    = new Date();
		final SimpleDateFormat format1    = new SimpleDateFormat("dd.MM.yyyy");
		final SimpleDateFormat format2    = new SimpleDateFormat("HH:mm:ss");
		final SimpleDateFormat format3    = new SimpleDateFormat(ISO8601DateProperty.PATTERN);
		final String nowString1           = format1.format(now);
		final String nowString2           = format2.format(now);
		final String nowString3           = format3.format(now);
		final DecimalFormat numberFormat1 = new DecimalFormat("###0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final DecimalFormat numberFormat2 = new DecimalFormat("0000.0000", DecimalFormatSymbols.getInstance(Locale.GERMAN));
		final DecimalFormat numberFormat3 = new DecimalFormat("####", DecimalFormatSymbols.getInstance(Locale.SIMPLIFIED_CHINESE));
		final String numberString1        = numberFormat1.format(2.234);
		final String numberString2        = numberFormat2.format(2.234);
		final String numberString3        = numberFormat3.format(2.234);
		MailTemplate template             = null;
		TestOne testOne                   = null;
		TestTwo testTwo                   = null;
		TestThree testThree               = null;
		TestFour testFour                 = null;
		List<TestSix> testSixs            = null;

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode(TestOne.class);
			testTwo        = createTestNode(TestTwo.class);
			testThree      = createTestNode(TestThree.class);
			testFour       = createTestNode(TestFour.class);
			testSixs       = createTestNodes(TestSix.class, 20);

			// create mail template
			template = createTestNode(MailTemplate.class);
			template.setProperty(MailTemplate.name, "TEST");
			template.setProperty(MailTemplate.locale, "en_EN");
			template.setProperty(MailTemplate.text, "This is a template for ${this.name}");

			// check existance
			assertNotNull(testOne);

			testOne.setProperty(TestOne.name, "A-nice-little-name-for-my-test-object");
			testOne.setProperty(TestOne.anInt, 1);
			testOne.setProperty(TestOne.aString, "String");
			testOne.setProperty(TestOne.anotherString, "{\n\ttest: test,\n\tnum: 3\n}");
			testOne.setProperty(TestOne.replaceString, "${this.name}");
			testOne.setProperty(TestOne.aLong, 235242522552L);
			testOne.setProperty(TestOne.aDouble, 2.234);
			testOne.setProperty(TestOne.aDate, now);
			testOne.setProperty(TestOne.aBoolean, true);
			testOne.setProperty(TestOne.testTwo, testTwo);
			testOne.setProperty(TestOne.testThree, testThree);
			testOne.setProperty(TestOne.testFour,  testFour);
			testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);
			testOne.setProperty(TestOne.cleanTestString, "a<b>c.d'e?f(g)h{i}j[k]l+m/n–o\\p\\q|r's!t,u-v_w`x-y-zöäüßABCDEFGH");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(testOne, null);

			// test for "empty" return value
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${err}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.this.this.error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${parent.error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.owner}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.alwaysNull}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${parent.owner}"));

			assertEquals("${this} should evaluate to the current node", testOne.toString(), testOne.replaceVariables(securityContext, ctx, "${this}"));
			assertEquals("${parent} should evaluate to the context parent node", testOne.toString(), testOne.replaceVariables(securityContext, ctx, "${parent}"));

			assertEquals("${this} should evaluate to the current node", testTwo.toString(), testTwo.replaceVariables(securityContext, ctx, "${this}"));
			assertEquals("${parent} should evaluate to the context parent node", testOne.toString(), testTwo.replaceVariables(securityContext, ctx, "${parent}"));

			assertEquals("Invalid variable reference", testTwo.toString(),   testOne.replaceVariables(securityContext, ctx, "${this.testTwo}"));
			assertEquals("Invalid variable reference", testThree.toString(), testOne.replaceVariables(securityContext, ctx, "${this.testThree}"));
			assertEquals("Invalid variable reference", testFour.toString(),  testOne.replaceVariables(securityContext, ctx, "${this.testFour}"));

			assertEquals("Invalid variable reference", testTwo.getUuid(), testOne.replaceVariables(securityContext, ctx, "${this.testTwo.id}"));
			assertEquals("Invalid variable reference", testThree.getUuid(), testOne.replaceVariables(securityContext, ctx, "${this.testThree.id}"));
			assertEquals("Invalid variable reference", testFour.getUuid(), testOne.replaceVariables(securityContext, ctx, "${this.testFour.id}"));

			assertEquals("Invalid size result", "20", testOne.replaceVariables(securityContext, ctx, "${this.manyToManyTestSixs.size}"));

			try {

				testOne.replaceVariables(securityContext, ctx, "${(this.alwaysNull.size}");
				fail("A mismatched opening bracket should throw an exception.");

			} catch (FrameworkException fex) {
				assertEquals("Invalid expression: mismatched closing bracket after this.alwaysNull.size", fex.getMessage());
			}

			assertEquals("Invalid size result", "", testOne.replaceVariables(securityContext, ctx, "${this.alwaysNull.size}"));

			assertEquals("Invalid variable reference", "1",            testOne.replaceVariables(securityContext, ctx, "${this.anInt}"));
			assertEquals("Invalid variable reference", "String",       testOne.replaceVariables(securityContext, ctx, "${this.aString}"));
			assertEquals("Invalid variable reference", "235242522552", testOne.replaceVariables(securityContext, ctx, "${this.aLong}"));
			assertEquals("Invalid variable reference", "2.234",        testOne.replaceVariables(securityContext, ctx, "${this.aDouble}"));

			// test with property
			assertEquals("Invalid md5() result", "27118326006d3829667a400ad23d5d98",  testOne.replaceVariables(securityContext, ctx, "${md5(this.aString)}"));
			assertEquals("Invalid usage message for md5()", Functions.ERROR_MESSAGE_MD5, testOne.replaceVariables(securityContext, ctx, "${md5()}"));
			assertEquals("Invalid upper() result", "27118326006D3829667A400AD23D5D98",  testOne.replaceVariables(securityContext, ctx, "${upper(md5(this.aString))}"));
			assertEquals("Invalid usage message for upper()", Functions.ERROR_MESSAGE_UPPER, testOne.replaceVariables(securityContext, ctx, "${upper()}"));
			assertEquals("Invalid upper(lower() result", "27118326006D3829667A400AD23D5D98",  testOne.replaceVariables(securityContext, ctx, "${upper(lower(upper(md5(this.aString))))}"));
			assertEquals("Invalid usage message for lower()", Functions.ERROR_MESSAGE_LOWER, testOne.replaceVariables(securityContext, ctx, "${lower()}"));

			assertEquals("Invalid md5() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${md5(this.alwaysNull)}"));
			assertEquals("Invalid upper() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${upper(this.alwaysNull)}"));
			assertEquals("Invalid lower() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lower(this.alwaysNull)}"));

			// test literal value as well
			assertEquals("Invalid md5() result", "cc03e747a6afbbcbf8be7668acfebee5",  testOne.replaceVariables(securityContext, ctx, "${md5(\"test123\")}"));

			assertEquals("Invalid lower() result", "string",       testOne.replaceVariables(securityContext, ctx, "${lower(this.aString)}"));
			assertEquals("Invalid upper() result", "STRING",       testOne.replaceVariables(securityContext, ctx, "${upper(this.aString)}"));

			// join
			assertEquals("Invalid join() result", "one,two,three", testOne.replaceVariables(securityContext, ctx, "${join(merge(\"one\", \"two\", \"three\"), \",\")}"));

			// concat
			assertEquals("Invalid concat() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${concat(\"one\", \"two\", \"three\")}"));
			assertEquals("Invalid concat() result", "oneStringthree", testOne.replaceVariables(securityContext, ctx, "${concat(\"one\", this.aString, \"three\")}"));
			assertEquals("Invalid concat() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${concat(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for concat()", Functions.ERROR_MESSAGE_CONCAT, testOne.replaceVariables(securityContext, ctx, "${concat()}"));

			// split
			assertEquals("Invalid split() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${concat(split(\"one,two,three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${concat(split(\"one;two;three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${concat(split(\"one;two;three\", \";\"))}"));
			assertEquals("Invalid split() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${split(this.alwaysNull)}"));
			assertEquals("Invalid usage message for split()", Functions.ERROR_MESSAGE_SPLIT, testOne.replaceVariables(securityContext, ctx, "${split()}"));

			// abbr
			assertEquals("Invalid abbr() result", "oneStringt…", testOne.replaceVariables(securityContext, ctx, "${abbr(concat(\"one\", this.aString, \"three\"), 10)}"));
			assertEquals("Invalid abbr() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${abbr(this.alwaysNull, 10)}"));
			assertEquals("Invalid usage message for abbr()", Functions.ERROR_MESSAGE_ABBR, testOne.replaceVariables(securityContext, ctx, "${abbr()}"));

			// capitalize..
			assertEquals("Invalid capitalize() result", "One_two_three", testOne.replaceVariables(securityContext, ctx, "${capitalize(concat(\"one_\", \"two_\", \"three\"))}"));
			assertEquals("Invalid capitalize() result", "One_Stringthree", testOne.replaceVariables(securityContext, ctx, "${capitalize(concat(\"one_\", this.aString, \"three\"))}"));
			assertEquals("Invalid capitalize() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${capitalize(this.alwaysNull)}"));
			assertEquals("Invalid usage message for capitalize()", Functions.ERROR_MESSAGE_CAPITALIZE, testOne.replaceVariables(securityContext, ctx, "${capitalize()}"));

			// titleize
			assertEquals("Invalid titleize() result", "One Two Three", testOne.replaceVariables(securityContext, ctx, "${titleize(concat(\"one_\", \"two_\", \"three\"), \"_\")}"));
			assertEquals("Invalid titleize() result", "One Stringthree", testOne.replaceVariables(securityContext, ctx, "${titleize(concat(\"one_\", this.aString, \"three\"), \"_\")}"));
			assertEquals("Invalid titleize() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${titleize(this.alwaysNull)}"));
			assertEquals("Invalid usage message for titleize()", Functions.ERROR_MESSAGE_TITLEIZE, testOne.replaceVariables(securityContext, ctx, "${titleize()}"));

			// num (explicit number conversion)
			assertEquals("Invalid num() result", "2.234", testOne.replaceVariables(securityContext, ctx, "${num(2.234)}"));
			assertEquals("Invalid num() result", "2.234", testOne.replaceVariables(securityContext, ctx, "${num(this.aDouble)}"));
			assertEquals("Invalid num() result", "1.0", testOne.replaceVariables(securityContext, ctx, "${num(this.anInt)}"));
			assertEquals("Invalid num() result", "", testOne.replaceVariables(securityContext, ctx, "${num(\"abc\")}"));
			assertEquals("Invalid num() result", "", testOne.replaceVariables(securityContext, ctx, "${num(this.aString)}"));
			assertEquals("Invalid num() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${num(this.alwaysNull)}"));
			assertEquals("Invalid usage message for num()", Functions.ERROR_MESSAGE_NUM, testOne.replaceVariables(securityContext, ctx, "${num()}"));

			// index_of
			assertEquals("Invalid index_of() result", "19", testOne.replaceVariables(securityContext, ctx, "${index_of(this.name, 'for')}"));
			assertEquals("Invalid index_of() result", "-1", testOne.replaceVariables(securityContext, ctx, "${index_of(this.name, 'entity')}"));
			assertEquals("Invalid index_of() result", "19", testOne.replaceVariables(securityContext, ctx, "${index_of('a-nice-little-name-for-my-test-object', 'for')}"));
			assertEquals("Invalid index_of() result", "-1", testOne.replaceVariables(securityContext, ctx, "${index_of('a-nice-little-name-for-my-test-object', 'entity')}"));

			// contains
			assertEquals("Invalid contains() result", "true", testOne.replaceVariables(securityContext, ctx, "${contains(this.name, 'for')}"));
			assertEquals("Invalid contains() result", "false", testOne.replaceVariables(securityContext, ctx, "${contains(this.name, 'entity')}"));
			assertEquals("Invalid contains() result", "true", testOne.replaceVariables(securityContext, ctx, "${contains('a-nice-little-name-for-my-test-object', 'for')}"));
			assertEquals("Invalid contains() result", "false", testOne.replaceVariables(securityContext, ctx, "${contains('a-nice-little-name-for-my-test-object', 'entity')}"));

			// substring
			assertEquals("Invalid substring() result", "for", testOne.replaceVariables(securityContext, ctx, "${substring(this.name, 19, 3)}"));
			assertEquals("Invalid substring() result", "", testOne.replaceVariables(securityContext, ctx, "${substring(this.name, -1, -1)}"));
			assertEquals("Invalid substring() result", "", testOne.replaceVariables(securityContext, ctx, "${substring(this.name, 100, -1)}"));
			assertEquals("Invalid substring() result", "", testOne.replaceVariables(securityContext, ctx, "${substring(this.name, 5, -2)}"));
			assertEquals("Invalid substring() result", "for", testOne.replaceVariables(securityContext, ctx, "${substring('a-nice-little-name-for-my-test-object', 19, 3)}"));
			assertEquals("Invalid substring() result", "", testOne.replaceVariables(securityContext, ctx, "${substring('a-nice-little-name-for-my-test-object', -1, -1)}"));
			assertEquals("Invalid substring() result", "", testOne.replaceVariables(securityContext, ctx, "${substring('a-nice-little-name-for-my-test-object', 100, -1)}"));
			assertEquals("Invalid substring() result", "", testOne.replaceVariables(securityContext, ctx, "${substring('a-nice-little-name-for-my-test-object', 5, -2)}"));

			// length
			assertEquals("Invalid length() result", "37", testOne.replaceVariables(securityContext, ctx, "${length(this.name)}"));
			assertEquals("Invalid length() result", "37", testOne.replaceVariables(securityContext, ctx, "${length('a-nice-little-name-for-my-test-object')}"));
			assertEquals("Invalid length() result", "4", testOne.replaceVariables(securityContext, ctx, "${length('test')}"));
			assertEquals("Invalid length() result", "", testOne.replaceVariables(securityContext, ctx, "${length(this.alwaysNull)}"));

			// clean (disabled for now, because literal strings pose problems in the matching process)
			assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoauabcdefgh", testOne.replaceVariables(securityContext, ctx, "${clean(this.cleanTestString)}"));
			assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoauabcdefgh", testOne.replaceVariables(securityContext, ctx, "${clean(get(this, \"cleanTestString\"))}"));
			assertEquals("Invalid clean() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${clean(this.alwaysNull)}"));
			assertEquals("Invalid usage message for clean()", Functions.ERROR_MESSAGE_CLEAN, testOne.replaceVariables(securityContext, ctx, "${clean()}"));

			// urlencode (disabled for now, because literal strings pose problems in the matching process)
			assertEquals("Invalid urlencode() result", "a%3Cb%3Ec.d%27e%3Ff%28g%29h%7Bi%7Dj%5Bk%5Dl%2Bm%2Fn%E2%80%93o%5Cp%5Cq%7Cr%27s%21t%2Cu-v_w%60x-y-z%C3%B6%C3%A4%C3%BC%C3%9FABCDEFGH", testOne.replaceVariables(securityContext, ctx, "${urlencode(this.cleanTestString)}"));
			assertEquals("Invalid urlencode() result", "a%3Cb%3Ec.d%27e%3Ff%28g%29h%7Bi%7Dj%5Bk%5Dl%2Bm%2Fn%E2%80%93o%5Cp%5Cq%7Cr%27s%21t%2Cu-v_w%60x-y-z%C3%B6%C3%A4%C3%BC%C3%9FABCDEFGH", testOne.replaceVariables(securityContext, ctx, "${urlencode(get(this, \"cleanTestString\"))}"));
			assertEquals("Invalid urlencode() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${urlencode(this.alwaysNull)}"));
			assertEquals("Invalid usage message for urlencode()", Functions.ERROR_MESSAGE_URLENCODE, testOne.replaceVariables(securityContext, ctx, "${urlencode()}"));

			// if etc.
			assertEquals("Invalid if() result", "true",  testOne.replaceVariables(securityContext, ctx,  "${if(\"true\", \"true\", \"false\")}"));
			assertEquals("Invalid if() result", "false", testOne.replaceVariables(securityContext, ctx,  "${if(\"false\", \"true\", \"false\")}"));
			assertEquals("Invalid usage message for if()", Functions.ERROR_MESSAGE_IF, testOne.replaceVariables(securityContext, ctx, "${if()}"));

			// empty
			assertEquals("Invalid empty() result", "true",  testOne.replaceVariables(securityContext, ctx,  "${empty(\"\")}"));
			assertEquals("Invalid empty() result", "false",  testOne.replaceVariables(securityContext, ctx, "${empty(\" \")}"));
			assertEquals("Invalid empty() result", "false",  testOne.replaceVariables(securityContext, ctx, "${empty(\"   \")}"));
			assertEquals("Invalid empty() result", "false",  testOne.replaceVariables(securityContext, ctx, "${empty(\"xyz\")}"));
			assertEquals("Invalid empty() result with null value", "true", testOne.replaceVariables(securityContext, ctx, "${empty(this.alwaysNull)}"));
			assertEquals("Invalid usage message for empty()", Functions.ERROR_MESSAGE_EMPTY, testOne.replaceVariables(securityContext, ctx, "${empty()}"));

			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(\"test\"), true, false)}"));
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(\"test\n\"), true, false)}"));

			// functions can NOT handle literal strings containing newlines  (disabled for now, because literal strings pose problems in the matching process)
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(\"\n\"), true, false)}"));
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(\"\n\"), \"true\", \"false\")}"));

			// functions CAN handle variable values with newlines!
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(this.anotherString), \"true\", \"false\")}"));

			// equal
			assertEquals("Invalid equal() result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(this.id, this.id)}"));
			assertEquals("Invalid equal() result", "false",  testOne.replaceVariables(securityContext, ctx, "${equal(\"1\", this.anInt)}"));
			assertEquals("Invalid equal() result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(1, this.anInt)}"));
			assertEquals("Invalid equal() result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(1.0, this.anInt)}"));
			assertEquals("Invalid equal() result", "false",  testOne.replaceVariables(securityContext, ctx, "${equal(this.anInt, \"1\")}"));
			assertEquals("Invalid equal() result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(this.anInt, 1)}"));
			assertEquals("Invalid equal() result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(this.anInt, 1.0)}"));
			assertEquals("Invalid equal() result", "false",  testOne.replaceVariables(securityContext, ctx, "${equal(this.aBoolean, \"true\")}"));
			assertEquals("Invalid equal() result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(this.aBoolean, true)}"));
			assertEquals("Invalid equal() result", "false",  testOne.replaceVariables(securityContext, ctx, "${equal(this.aBoolean, false)}"));

			// if + equal
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(this.id, this.id), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"abc\", \"abc\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(3, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"3\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(3.1414, 3.1414), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"3.1414\", \"3.1414\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(23.44242222243633337234623462, 23.44242222243633337234623462), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"23.44242222243633337234623462\", \"23.44242222243633337234623462\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(13, 013), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(13, \"013\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"13\", \"013\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"13\", \"00013\"), \"true\", \"false\")}"));
			assertEquals("Invalid usage message for equal()", Functions.ERROR_MESSAGE_EQUAL, testOne.replaceVariables(securityContext, ctx, "${equal()}"));

			// disabled: java StreamTokenizer can NOT handle scientific notation
//			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(23.4462, 2.34462e1)}"));
//			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(0.00234462, 2.34462e-3)}"));
//			assertEquals("Invalid if(equal()) result with null value", "false",  testOne.replaceVariables(securityContext, ctx, "${equal(this.alwaysNull, 2.34462e-3)}"));
			assertEquals("Invalid if(equal()) result with null value", "false",  testOne.replaceVariables(securityContext, ctx, "${equal(0.00234462, this.alwaysNull)}"));
			assertEquals("Invalid if(equal()) result with null value", "true",  testOne.replaceVariables(securityContext, ctx, "${equal(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + add
			assertEquals("Invalid if(equal(add())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, add(1, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, add(\"1\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, add(1, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, add(1, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, add(1, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, add(\"1\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, add(1, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, add(1, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(20, add(\"10\", \"10\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(20, add(\"10\", \"010\")), \"true\", \"false\")}"));
			assertEquals("Invalid usage message for add()", Functions.ERROR_MESSAGE_ADD, testOne.replaceVariables(securityContext, ctx, "${add()}"));

			// add with null
			assertEquals("Invalid add() result with null value", "10.0",  testOne.replaceVariables(securityContext, ctx, "${add(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid add() result with null value", "11.0",  testOne.replaceVariables(securityContext, ctx, "${add(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid add() result with null value", "0.0",  testOne.replaceVariables(securityContext, ctx, "${add(this.alwaysNull, this.alwaysNull)}"));

			// if + lt
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lt(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lt(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// lt with null
			assertEquals("Invalid lt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lt(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid lt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lt(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid lt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lt(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for lt()", Functions.ERROR_MESSAGE_LT, testOne.replaceVariables(securityContext, ctx, "${lt()}"));

			// if + gt
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gt(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gt(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// gt with null
			assertEquals("Invalid gt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${gt(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid gt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${gt(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid gt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${gt(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for gt()", Functions.ERROR_MESSAGE_GT, testOne.replaceVariables(securityContext, ctx, "${gt()}"));

			// if + lte
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(lte(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(lte(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// lte with null
			assertEquals("Invalid lte() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lte(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid lte() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lte(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid lte() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${lte(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for lte()", Functions.ERROR_MESSAGE_LTE, testOne.replaceVariables(securityContext, ctx, "${lte()}"));

			// if + gte
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(gte(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(gte(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// gte with null
			assertEquals("Invalid gte() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${gte(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid gte() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${gte(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid gte() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${gte(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for gte()", Functions.ERROR_MESSAGE_GTE, testOne.replaceVariables(securityContext, ctx, "${gte()}"));

			// if + equal + subt
			assertEquals("Invalid if(equal(subt())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", subt(\"3\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, subt(\"3\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, subt(3, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, subt(\"3\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, subt(3, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, subt(3, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, subt(\"3\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, subt(3, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, subt(\"3\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, subt(3, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, subt(3, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(20, subt(\"30\", \"10\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(20, subt(\"30\", \"010\")), \"true\", \"false\")}"));

			// subt with null
			assertEquals("Invalid subt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${subt(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid subt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${subt(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid subt() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${subt(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for subt()", Functions.ERROR_MESSAGE_SUBT, testOne.replaceVariables(securityContext, ctx, "${subt()}"));

			// if + equal + mult
			assertEquals("Invalid if(equal(mult())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"6\", mult(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6, mult(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6, mult(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6, mult(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6, mult(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6, mult(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6.0, mult(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6.0, mult(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6.0, mult(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6.0, mult(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(6.0, mult(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(600, mult(\"30\", \"20\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(600, mult(\"30\", \"020\")), \"true\", \"false\")}"));

			// mult with null
			assertEquals("Invalid mult() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${mult(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid mult() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${mult(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid mult() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${mult(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for mult()", Functions.ERROR_MESSAGE_MULT, testOne.replaceVariables(securityContext, ctx, "${mult()}"));

			// if + equal + quot
			assertEquals("Invalid if(equal(quot())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"1.5\", quot(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(15, quot(\"30\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(15, quot(\"30\", \"02\")), \"true\", \"false\")}"));

			// quot with null
			assertEquals("Invalid quot() result with null value", "10.0",  testOne.replaceVariables(securityContext, ctx, "${quot(10, this.alwaysNull)}"));
			assertEquals("Invalid quot() result with null value", "10.0",  testOne.replaceVariables(securityContext, ctx, "${quot(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid quot() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${quot(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid quot() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${quot(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for quot()", Functions.ERROR_MESSAGE_QUOT, testOne.replaceVariables(securityContext, ctx, "${quot()}"));

			// if + equal + round
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", round(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", round(\"2.5\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", round(\"1.999999\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", round(\"2.499999\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(2.5, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(1.999999, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(2.499999, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(2, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.4, round(2.4, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.23, round(2.225234, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(1.9, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(2.5, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(1.999999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(2.499999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.999999, round(1.999999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.499999, round(2.499999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(1.999999999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, round(2, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.4, round(2.4, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.225234, round(2.225234, 8)), \"true\", \"false\")}"));

			// disabled because scientific notation is not supported :(
			//assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(0.00245, round(2.45e-3, 8)), \"true\", \"false\")}"));
			//assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(245, round(2.45e2, 8)), \"true\", \"false\")}"));

			// round with null
			assertEquals("Invalid round() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${round(\"10\")}"));
			assertEquals("Invalid round() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${round(this.alwaysNull)}"));
			assertEquals("Invalid round() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${round(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for round()", Functions.ERROR_MESSAGE_ROUND, testOne.replaceVariables(securityContext, ctx, "${round()}"));

			// if + equal + max
			assertEquals("Invalid if(equal(max())) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", max(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(max())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, max(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(max())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, max(1.9, 2)), \"true\", \"false\")}"));

			// max with null
			assertEquals("Invalid max() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${max(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid max() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${max(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid max() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${max(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for max()", Functions.ERROR_MESSAGE_MAX, testOne.replaceVariables(securityContext, ctx, "${max()}"));

			// if + equal + min
			assertEquals("Invalid if(equal(min())) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"1.9\", min(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(min())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.9, min(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(min())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1, min(1, 2)), \"true\", \"false\")}"));

			// min with null
			assertEquals("Invalid min() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${min(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid min() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${min(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid min() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${min(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for min()", Functions.ERROR_MESSAGE_MIN, testOne.replaceVariables(securityContext, ctx, "${min()}"));

			// date_format
			assertEquals("Invalid date_format() result", nowString1, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDate, \"" + format1.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", nowString2, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDate, \"" + format2.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", nowString3, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDate, \"" + format3.toPattern() + "\")}"));

			// date_format with null
			assertEquals("Invalid date_format() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${date_format(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid date_format() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${date_format(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for date_format()", Functions.ERROR_MESSAGE_DATE_FORMAT, testOne.replaceVariables(securityContext, ctx, "${date_format()}"));

			// date_format error messages
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_DATE_FORMAT, testOne.replaceVariables(securityContext, ctx, "${date_format()}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_DATE_FORMAT,  testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDouble)}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_DATE_FORMAT, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDouble, this.aDouble, this.aDouble)}"));

			// number_format error messages
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_NUMBER_FORMAT, testOne.replaceVariables(securityContext, ctx, "${number_format()}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_NUMBER_FORMAT, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble)}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_NUMBER_FORMAT, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, this.aDouble)}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_NUMBER_FORMAT, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, this.aDouble, \"\", \"\")}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", Functions.ERROR_MESSAGE_NUMBER_FORMAT, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, this.aDouble, \"\", \"\", \"\")}"));

			assertEquals("Invalid date_format() result", numberString1, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, \"en\", \"" + numberFormat1.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", numberString2, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, \"de\", \"" + numberFormat2.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", numberString3, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, \"zh\", \"" + numberFormat3.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result",   "123456.79", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"en\", \"0.00\")}"));
			assertEquals("Invalid date_format() result", "123456.7890", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"en\", \"0.0000\")}"));
			assertEquals("Invalid date_format() result",   "123456,79", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"de\", \"0.00\")}"));
			assertEquals("Invalid date_format() result", "123456,7890", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"de\", \"0.0000\")}"));
			assertEquals("Invalid date_format() result",   "123456.79", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"zh\", \"0.00\")}"));
			assertEquals("Invalid date_format() result", "123456.7890", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"zh\", \"0.0000\")}"));

			// number_format with null
			assertEquals("Invalid number_format() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${number_format(this.alwaysNull, this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid number_format() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${number_format(\"10\", this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid number_format() result with null value", "",  testOne.replaceVariables(securityContext, ctx, "${number_format(\"10\", \"de\", this.alwaysNull)}"));
			assertEquals("Invalid usage message for number_format()", Functions.ERROR_MESSAGE_NUMBER_FORMAT, testOne.replaceVariables(securityContext, ctx, "${number_format()}"));

			// not
			assertEquals("Invalid not() result", "true",  testOne.replaceVariables(securityContext, ctx, "${not(false)}"));
			assertEquals("Invalid not() result", "false", testOne.replaceVariables(securityContext, ctx, "${not(true)}"));
			assertEquals("Invalid not() result", "true",  testOne.replaceVariables(securityContext, ctx, "${not(\"false\")}"));
			assertEquals("Invalid not() result", "false", testOne.replaceVariables(securityContext, ctx, "${not(\"true\")}"));

			// not with null
			assertEquals("Invalid not() result with null value", "true", testOne.replaceVariables(securityContext, ctx, "${not(this.alwaysNull)}"));
			assertEquals("Invalid usage message for not()", Functions.ERROR_MESSAGE_NOT, testOne.replaceVariables(securityContext, ctx, "${not()}"));

			// and
			assertEquals("Invalid and() result", "true",  testOne.replaceVariables(securityContext, ctx, "${and(true, true)}"));
			assertEquals("Invalid and() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(true, false)}"));
			assertEquals("Invalid and() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(false, true)}"));
			assertEquals("Invalid and() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(false, false)}"));

			// and with null
			assertEquals("Invalid and() result with null value", "false", testOne.replaceVariables(securityContext, ctx, "${and(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for and()", Functions.ERROR_MESSAGE_AND, testOne.replaceVariables(securityContext, ctx, "${and(this.alwaysNull)}"));
			assertEquals("Invalid usage message for and()", Functions.ERROR_MESSAGE_AND, testOne.replaceVariables(securityContext, ctx, "${and()}"));

			// or
			assertEquals("Invalid or() result", "true",  testOne.replaceVariables(securityContext, ctx, "${or(true, true)}"));
			assertEquals("Invalid or() result", "true", testOne.replaceVariables(securityContext, ctx, "${or(true, false)}"));
			assertEquals("Invalid or() result", "true", testOne.replaceVariables(securityContext, ctx, "${or(false, true)}"));
			assertEquals("Invalid or() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(false, false)}"));

			// or with null
			assertEquals("Invalid or() result with null value", "false", testOne.replaceVariables(securityContext, ctx, "${or(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid usage message for or()", Functions.ERROR_MESSAGE_OR, testOne.replaceVariables(securityContext, ctx, "${or(this.alwaysNull)}"));
			assertEquals("Invalid usage message for or()", Functions.ERROR_MESSAGE_OR, testOne.replaceVariables(securityContext, ctx, "${or()}"));

			// get
			assertEquals("Invalid get() result", "1",  testOne.replaceVariables(securityContext, ctx, "${get(this, \"anInt\")}"));
			assertEquals("Invalid get() result", "String",  testOne.replaceVariables(securityContext, ctx, "${get(this, \"aString\")}"));
			assertEquals("Invalid get() result", "2.234",  testOne.replaceVariables(securityContext, ctx, "${get(this, \"aDouble\")}"));
			assertEquals("Invalid get() result", testTwo.toString(),  testOne.replaceVariables(securityContext, ctx, "${get(this, \"testTwo\")}"));
			assertEquals("Invalid get() result", testTwo.getUuid(),  testOne.replaceVariables(securityContext, ctx, "${get(get(this, \"testTwo\"), \"id\")}"));
			assertEquals("Invalid get() result", testSixs.get(0).getUuid(),  testOne.replaceVariables(securityContext, ctx, "${get(first(get(this, \"manyToManyTestSixs\")), \"id\")}"));
			assertEquals("Invalid usage message for get()", Functions.ERROR_MESSAGE_GET, testOne.replaceVariables(securityContext, ctx, "${get()}"));

			// first / last / nth
			assertEquals("Invalid first() result", testSixs.get( 0).toString(), testOne.replaceVariables(securityContext, ctx, "${first(this.manyToManyTestSixs)}"));
			assertEquals("Invalid last() result",  testSixs.get(19).toString(), testOne.replaceVariables(securityContext, ctx, "${last(this.manyToManyTestSixs)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 2).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs,  2)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 7).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs,  7)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 9).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs,  9)}"));
			assertEquals("Invalid ngth() result",  testSixs.get(12).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs, 12)}"));

			// first / last / nth with null
			assertEquals("Invalid first() result with null value", "", testOne.replaceVariables(securityContext, ctx, "${first(this.alwaysNull)}"));
			assertEquals("Invalid usage message for first()", Functions.ERROR_MESSAGE_FIRST, testOne.replaceVariables(securityContext, ctx, "${first()}"));
			assertEquals("Invalid last() result with null value",  "", testOne.replaceVariables(securityContext, ctx, "${last(this.alwaysNull)}"));
			assertEquals("Invalid usage message for last()", Functions.ERROR_MESSAGE_LAST, testOne.replaceVariables(securityContext, ctx, "${last()}"));
			assertEquals("Invalid nth() result with null value",   "", testOne.replaceVariables(securityContext, ctx, "${nth(this.alwaysNull,  2)}"));
			assertEquals("Invalid nth() result with null value",   "", testOne.replaceVariables(securityContext, ctx, "${nth(this.alwaysNull,  7)}"));
			assertEquals("Invalid nth() result with null value",   "", testOne.replaceVariables(securityContext, ctx, "${nth(this.alwaysNull,  9)}"));
			assertEquals("Invalid nth() result with null value",  "", testOne.replaceVariables(securityContext, ctx, "${nth(this.alwaysNull, 12)}"));
			assertEquals("Invalid nth() result with null value",  "", testOne.replaceVariables(securityContext, ctx, "${nth(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid nth() result with null value",  "", testOne.replaceVariables(securityContext, ctx, "${nth(this.alwaysNull, blah)}"));
			assertEquals("Invalid usage message for nth()", Functions.ERROR_MESSAGE_NTH, testOne.replaceVariables(securityContext, ctx, "${nth()}"));

			// each with null
			assertEquals("Invalid usage message for each()", Functions.ERROR_MESSAGE_EACH, testOne.replaceVariables(securityContext, ctx, "${each()}"));

			// get with null
			assertEquals("Invalid usage message for get()", Functions.ERROR_MESSAGE_GET, testOne.replaceVariables(securityContext, ctx, "${get()}"));

			// set with null
			assertEquals("Invalid usage message for set()", Functions.ERROR_MESSAGE_SET, testOne.replaceVariables(securityContext, ctx, "${set()}"));

			// geocode with null
			assertEquals("Invalid usage message for geocode()", Functions.ERROR_MESSAGE_GEOCODE, testOne.replaceVariables(securityContext, ctx, "${geocode()}"));

			// send_plaintex_mail with null
			assertEquals("Invalid usage message for send_plaintext_mail()", Functions.ERROR_MESSAGE_SEND_PLAINTEXT_MAIL, testOne.replaceVariables(securityContext, ctx, "${send_plaintext_mail()}"));

			// send_html_mail with null
			assertEquals("Invalid usage message for send_html_mail()", Functions.ERROR_MESSAGE_SEND_HTML_MAIL, testOne.replaceVariables(securityContext, ctx, "${send_html_mail()}"));

			// read with null
			assertEquals("Invalid usage message for each()", Functions.ERROR_MESSAGE_EACH, testOne.replaceVariables(securityContext, ctx, "${each()}"));

			// write with null
			assertEquals("Invalid usage message for write()", Functions.ERROR_MESSAGE_WRITE, testOne.replaceVariables(securityContext, ctx, "${write()}"));

			// append with null
			assertEquals("Invalid usage message for append()", Functions.ERROR_MESSAGE_APPEND, testOne.replaceVariables(securityContext, ctx, "${append()}"));

			// xml with null
			assertEquals("Invalid usage message for xml()", Functions.ERROR_MESSAGE_XML, testOne.replaceVariables(securityContext, ctx, "${xml()}"));

			// xpath with null
			assertEquals("Invalid usage message for xpath()", Functions.ERROR_MESSAGE_XPATH, testOne.replaceVariables(securityContext, ctx, "${xpath()}"));

			// find with null
			assertEquals("Invalid usage message for find()", Functions.ERROR_MESSAGE_FIND, testOne.replaceVariables(securityContext, ctx, "${find()}"));

			// do
			testOne.replaceVariables(securityContext, ctx, "${if(empty(this.alwaysNull), set(this, \"doResult\", true), set(this, \"doResult\", false))}");
			assertEquals("Invalid do() result", "true", testOne.replaceVariables(securityContext, ctx, "${this.doResult}"));

			testOne.replaceVariables(securityContext, ctx, "${if(empty(this.name), set(this, \"doResult\", true), set(this, \"doResult\", false))}");
			assertEquals("Invalid do() result", "false", testOne.replaceVariables(securityContext, ctx, "${this.doResult}"));

			// template method
			assertEquals("Invalid template() result", "This is a template for A-nice-little-name-for-my-test-object", testOne.replaceVariables(securityContext, ctx, "${template(\"TEST\", \"en_EN\", this)}"));

			// more complex tests
			testOne.replaceVariables(securityContext, ctx, "${each(split(\"setTestInteger1,setTestInteger2,setTestInteger3\"), set(this, data, 1))}");
			assertEquals("Invalid each() result", "1", testOne.replaceVariables(securityContext, ctx, "${get(this, \"setTestInteger1\")}"));
			assertEquals("Invalid each() result", "1", testOne.replaceVariables(securityContext, ctx, "${get(this, \"setTestInteger2\")}"));
			assertEquals("Invalid each() result", "1", testOne.replaceVariables(securityContext, ctx, "${get(this, \"setTestInteger3\")}"));

			// complex each expression, sets the value of "testString" to the concatenated IDs of all testSixs that are linked to "this"
			testOne.replaceVariables(securityContext, ctx, "${each(this.manyToManyTestSixs, set(this, \"testString\", concat(get(this, \"testString\"), data.id)))}");
			assertEquals("Invalid each() result", "640", testOne.replaceVariables(securityContext, ctx, "${length(this.testString)}"));

			assertEquals("Invalid if(equal()) result", "String",  testOne.replaceVariables(securityContext, ctx, "${if(empty(this.alwaysNull), titleize(this.aString, '-'), this.alwaysNull)}"));
			assertEquals("Invalid if(equal()) result", "String",  testOne.replaceVariables(securityContext, ctx, "${if(empty(this.aString), titleize(this.alwaysNull, '-'), this.aString)}"));

			assertNull("Invalid result for special null value", testOne.replaceVariables(securityContext, ctx, "${null}"));
			assertNull("Invalid result for special null value", testOne.replaceVariables(securityContext, ctx, "${if(equal(this.anInt, 15), \"selected\", null)}"));

			// tests from real-life examples
			assertEquals("Invalid replacement result", "tile plan ", testOne.replaceVariables(securityContext, ctx, "tile plan ${plan.bannerTag}"));

			// more tests with pre- and postfixes
			assertEquals("Invalid replacement result", "abcdefghijklmnop", testOne.replaceVariables(securityContext, ctx, "abcdefgh${blah}ijklmnop"));
			assertEquals("Invalid replacement result", "abcdefghStringijklmnop", testOne.replaceVariables(securityContext, ctx, "abcdefgh${this.aString}ijklmnop"));
			assertEquals("Invalid replacement result", "#String", testOne.replaceVariables(securityContext, ctx, "#${this.aString}"));
			assertEquals("Invalid replacement result", "doc_sections/"+ testOne.getUuid() + "/childSections?sort=pos", testOne.replaceVariables(securityContext, ctx, "doc_sections/${this.id}/childSections?sort=pos"));
			assertEquals("Invalid replacement result", "A Nice Little Name For My Test Object", testOne.replaceVariables(securityContext, ctx, "${titleize(this.name, '-')}"));
			assertEquals("Invalid replacement result", "STRINGtrueFALSE", testOne.replaceVariables(securityContext, ctx, "${upper(this.aString)}${lower(true)}${upper(false)}"));

			// test replace() method
			assertEquals("Invalid replace() result", "A-nice-little-name-for-my-test-object", testOne.replaceVariables(securityContext, ctx, "${replace(this.replaceString, this)}"));

			// test error method
			try {
				Actions.execute(securityContext, testTwo, "${error(\"base\", \"test1\")}");
				fail("error() should throw an exception.");

			} catch (FrameworkException fex) { }

			try {
				Actions.execute(securityContext, testTwo, "${error(\"base\", \"test1\", \"test2\")}");
				fail("error() should throw an exception.");

			} catch (FrameworkException fex) { }

			// test multiline statements
			assertEquals("Invalid replace() result", "equal", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, 2),\n    (\"equal\"),\n    (\"not equal\")\n)}"));
			assertEquals("Invalid replace() result", "not equal", testOne.replaceVariables(securityContext, ctx, "${if(equal(2, 3),\n    (\"equal\"),\n    (\"not equal\")\n)}"));

			assertEquals("Invalid keys() / join() result", "id,name,owner,type,createdBy,deleted,hidden,createdDate,lastModifiedDate,visibleToPublicUsers,visibleToAuthenticatedUsers,visibilityStartDate,visibilityEndDate", testOne.replaceVariables(securityContext, ctx, "${join(keys(this, 'ui'), ',')}"));

			// test default values
			assertEquals("Invalid string default value", "blah", testOne.replaceVariables(securityContext, ctx, "${this.alwaysNull!blah}"));
			assertEquals("Invalid numeric default value", "12", testOne.replaceVariables(securityContext, ctx, "${this.alwaysNull!12}"));

			// Number default value
			assertEquals("true", testOne.replaceVariables(securityContext, ctx, "${equal(42, this.alwaysNull!42)}"));

			// complex multi-statement tests
			testOne.replaceVariables(securityContext, ctx, "${(set(this, \"isValid\", true), each(this.manyToManyTestSixs, set(this, \"isValid\", and(this.isValid, equal(length(data.id, 32))))))}");
			assertEquals("Invalid multiline statement test result", "true", testOne.replaceVariables(securityContext, ctx, "${this.isValid}"));

			testOne.replaceVariables(securityContext, ctx, "${(set(this, \"isValid\", true), each(this.manyToManyTestSixs, set(this, \"isValid\", and(this.isValid, gte(now, data.createdDate)))))}");
			assertEquals("Invalid multiline statement test result", "true", testOne.replaceVariables(securityContext, ctx, "${this.isValid}"));

			testOne.replaceVariables(securityContext, ctx, "${(set(this, \"isValid\", false), each(this.manyToManyTestSixs, set(this, \"isValid\", and(this.isValid, gte(now, data.createdDate)))))}");
			assertEquals("Invalid multiline statement test result", "false", testOne.replaceVariables(securityContext, ctx, "${this.isValid}"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception");
		}
	}
}
