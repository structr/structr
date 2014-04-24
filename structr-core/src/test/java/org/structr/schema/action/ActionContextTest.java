package org.structr.schema.action;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestTwo;
import org.structr.core.graph.Tx;
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

			// check existance
			assertNotNull(testOne);

			testOne.setProperty(TestOne.anInt, 1);
			testOne.setProperty(TestOne.aString, "String");
			testOne.setProperty(TestOne.anotherString, "{\n\ttest: test,\n\tnum: 3\n}");
			testOne.setProperty(TestOne.aLong, 235242522552L);
			testOne.setProperty(TestOne.aDouble, 2.234);
			testOne.setProperty(TestOne.aDate, now);
			testOne.setProperty(TestOne.testTwo, testTwo);
			testOne.setProperty(TestOne.testThree, testThree);
			testOne.setProperty(TestOne.testFour,  testFour);
			testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(testOne);

			// test for "empty" return value
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.this.this.error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${parent.error}"));
			assertEquals("Invalid expressions should yield and empty result", "", testOne.replaceVariables(securityContext, ctx, "${this.owner}"));
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

			assertEquals("Invalid size() result", "20", testOne.replaceVariables(securityContext, ctx, "${this.manyToManyTestSixs.size}"));

			assertEquals("Invalid variable reference", "1",            testOne.replaceVariables(securityContext, ctx, "${this.anInt}"));
			assertEquals("Invalid variable reference", "String",       testOne.replaceVariables(securityContext, ctx, "${this.aString}"));
			assertEquals("Invalid variable reference", "235242522552", testOne.replaceVariables(securityContext, ctx, "${this.aLong}"));
			assertEquals("Invalid variable reference", "2.234",        testOne.replaceVariables(securityContext, ctx, "${this.aDouble}"));

			// test with property
			assertEquals("Invalid md5() result", "27118326006d3829667a400ad23d5d98",  testOne.replaceVariables(securityContext, ctx, "${md5(this.aString)}"));
			assertEquals("Invalid upper() result", "27118326006D3829667A400AD23D5D98",  testOne.replaceVariables(securityContext, ctx, "${upper(md5(this.aString))}"));
			assertEquals("Invalid upper(lower() result", "27118326006D3829667A400AD23D5D98",  testOne.replaceVariables(securityContext, ctx, "${upper(lower(upper(md5(this.aString))))}"));

			// test literal value as well
			assertEquals("Invalid md5() result", "cc03e747a6afbbcbf8be7668acfebee5",  testOne.replaceVariables(securityContext, ctx, "${md5(\"test123\")}"));

			assertEquals("Invalid lower() result", "string",       testOne.replaceVariables(securityContext, ctx, "${lower(this.aString)}"));
			assertEquals("Invalid upper() result", "STRING",       testOne.replaceVariables(securityContext, ctx, "${upper(this.aString)}"));

			// join
			assertEquals("Invalid join() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${join(\"one\", \"two\", \"three\")}"));
			assertEquals("Invalid join() result", "oneStringthree", testOne.replaceVariables(securityContext, ctx, "${join(\"one\", this.aString, \"three\")}"));

			// split
			assertEquals("Invalid split() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${join(split(\"one,two,three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${join(split(\"one;two;three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", testOne.replaceVariables(securityContext, ctx, "${join(split(\"one;two;three\", \";\"))}"));

			// abbr
			assertEquals("Invalid abbr() result", "oneStringt…", testOne.replaceVariables(securityContext, ctx, "${abbr(join(\"one\", this.aString, \"three\"), 10)}"));

			// capitalize..
			assertEquals("Invalid capitalize() result", "One_two_three", testOne.replaceVariables(securityContext, ctx, "${capitalize(join(\"one_\", \"two_\", \"three\"))}"));
			assertEquals("Invalid capitalize() result", "One_Stringthree", testOne.replaceVariables(securityContext, ctx, "${capitalize(join(\"one_\", this.aString, \"three\"))}"));

			// titleize
			assertEquals("Invalid titleize() result", "One Two Three", testOne.replaceVariables(securityContext, ctx, "${titleize(join(\"one_\", \"two_\", \"three\"), \"_\")}"));
			assertEquals("Invalid titleize() result", "One Stringthree", testOne.replaceVariables(securityContext, ctx, "${titleize(join(\"one_\", this.aString, \"three\"), \"_\")}"));

			// num (explicit number conversion)
			assertEquals("Invalid num() result", "2.234", testOne.replaceVariables(securityContext, ctx, "${num(2.234)}"));
			assertEquals("Invalid num() result", "2.234", testOne.replaceVariables(securityContext, ctx, "${num(this.aDouble)}"));
			assertEquals("Invalid num() result", "1.0", testOne.replaceVariables(securityContext, ctx, "${num(this.anInt)}"));
			assertEquals("Invalid num() result", "", testOne.replaceVariables(securityContext, ctx, "${num(\"abc\")}"));
			assertEquals("Invalid num() result", "", testOne.replaceVariables(securityContext, ctx, "${num(this.aString)}"));

			// clean (disabled for now, because literal strings pose problems in the matching process)
			// assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoauabcdefgh", testOne.replaceVariables(securityContext, ctx, "${clean(\"a<b>c.d'e?f(g)h{i}j[k]l+m/n–o\\p\\q|r's!t,u-v_w`x-y-zöäüßABCDEFGH\")}"));

			// urlencode (disabled for now, because literal strings pose problems in the matching process)
			// assertEquals("Invalid urlencode() result", "a%3Cb%3Ec.d%27e%3Ff%28g%29h%7Bi%7Dj%5Bk%5Dl%2Bm%2Fn%E2%80%93o%5Cp%5Cq%7Cr%27s%21t%2Cu-v_w%60x-y-z%C3%B6%C3%A4%C3%BC%C3%9FABCDEFGH", testOne.replaceVariables(securityContext, ctx, "${urlencode(\"a<b>c.d'e?f(g)h{i}j[k]l+m/n–o\\p\\q|r's!t,u-v_w`x-y-zöäüßABCDEFGH\")}"));

			// if etc.
			assertEquals("Invalid if(empty()) result", "true",  testOne.replaceVariables(securityContext, ctx,  "${if(\"true\", \"true\", \"false\")}"));
			assertEquals("Invalid if(empty()) result", "false", testOne.replaceVariables(securityContext, ctx,  "${if(\"false\", \"true\", \"false\")}"));
			assertEquals("Invalid if(empty()) result", "true",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(\"\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(empty(\" \"), \"true\", \"false\")}"));
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(empty(\"   \"), \"true\", \"false\")}"));
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(empty(\"xyz\"), \"true\", \"false\")}"));

			// functions can NOT handle literal strings containing newlines  (disabled for now, because literal strings pose problems in the matching process)
			//assertEquals("Invalid if(empty()) result", "${if(empty(\"\n\"), \"true\", \"false\")}",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(\"\n\"), \"true\", \"false\")}"));

			// functions CAN handle variable values with newlines!
			assertEquals("Invalid if(empty()) result", "false",  testOne.replaceVariables(securityContext, ctx,  "${if(empty(this.anotherString), \"true\", \"false\")}"));

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

			// scientific notation
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(23.4462, 2.34462e1), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(0.00234462, 2.34462e-3), \"true\", \"false\")}"));

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

			// if + equal + quot
			assertEquals("Invalid if(equal(quot())) result", "false", testOne.replaceVariables(securityContext, ctx, "${if(equal(\"1.5\", quot(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.5, quot(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(15, quot(\"30\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(15, quot(\"30\", \"02\")), \"true\", \"false\")}"));

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

			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(0.00245, round(2.45e-3, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(245, round(2.45e2, 8)), \"true\", \"false\")}"));

			// if + equal + max
			assertEquals("Invalid if(equal(max())) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"2\", max(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(max())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2, max(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(max())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(2.0, max(1.9, 2)), \"true\", \"false\")}"));

			// if + equal + min
			assertEquals("Invalid if(equal(min())) result", "false",  testOne.replaceVariables(securityContext, ctx, "${if(equal(\"1.9\", min(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(min())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1.9, min(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(min())) result", "true",  testOne.replaceVariables(securityContext, ctx, "${if(equal(1, min(1, 2)), \"true\", \"false\")}"));

			// date_format
			assertEquals("Invalid date_format() result", nowString1, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDate, \"" + format1.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", nowString2, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDate, \"" + format2.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", nowString3, testOne.replaceVariables(securityContext, ctx, "${date_format(this.aDate, \"" + format3.toPattern() + "\")}"));

			// number_format
			assertEquals("Invalid date_format() result", numberString1, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, \"en\", \"" + numberFormat1.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", numberString2, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, \"de\", \"" + numberFormat2.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", numberString3, testOne.replaceVariables(securityContext, ctx, "${number_format(this.aDouble, \"zh\", \"" + numberFormat3.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result",   "123456.79", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"en\", \"0.00\")}"));
			assertEquals("Invalid date_format() result", "123456.7890", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"en\", \"0.0000\")}"));
			assertEquals("Invalid date_format() result",   "123456,79", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"de\", \"0.00\")}"));
			assertEquals("Invalid date_format() result", "123456,7890", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"de\", \"0.0000\")}"));
			assertEquals("Invalid date_format() result",   "123456.79", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"zh\", \"0.00\")}"));
			assertEquals("Invalid date_format() result", "123456.7890", testOne.replaceVariables(securityContext, ctx, "${number_format(123456.789012, \"zh\", \"0.0000\")}"));

			// not
			assertEquals("Invalid not() result", "true",  testOne.replaceVariables(securityContext, ctx, "${not(false)}"));
			assertEquals("Invalid not() result", "false", testOne.replaceVariables(securityContext, ctx, "${not(true)}"));
			assertEquals("Invalid not() result", "true",  testOne.replaceVariables(securityContext, ctx, "${not(\"false\")}"));
			assertEquals("Invalid not() result", "false", testOne.replaceVariables(securityContext, ctx, "${not(\"true\")}"));

			// and
			assertEquals("Invalid and() result", "true",  testOne.replaceVariables(securityContext, ctx, "${and(true, true)}"));
			assertEquals("Invalid and() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(true, false)}"));
			assertEquals("Invalid and() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(false, true)}"));
			assertEquals("Invalid and() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(false, false)}"));

			// or
			assertEquals("Invalid or() result", "true",  testOne.replaceVariables(securityContext, ctx, "${or(true, true)}"));
			assertEquals("Invalid or() result", "true", testOne.replaceVariables(securityContext, ctx, "${or(true, false)}"));
			assertEquals("Invalid or() result", "true", testOne.replaceVariables(securityContext, ctx, "${or(false, true)}"));
			assertEquals("Invalid or() result", "false", testOne.replaceVariables(securityContext, ctx, "${and(false, false)}"));

			// get
			assertEquals("Invalid get() result", "1",  testOne.replaceVariables(securityContext, ctx, "${get(this, \"anInt\")}"));
			assertEquals("Invalid get() result", "String",  testOne.replaceVariables(securityContext, ctx, "${get(this, \"aString\")}"));
			assertEquals("Invalid get() result", "2.234",  testOne.replaceVariables(securityContext, ctx, "${get(this, \"aDouble\")}"));
			assertEquals("Invalid get() result", testTwo.toString(),  testOne.replaceVariables(securityContext, ctx, "${get(this, \"testTwo\")}"));
			assertEquals("Invalid get() result", testTwo.getUuid(),  testOne.replaceVariables(securityContext, ctx, "${get(get(this, \"testTwo\"), \"id\")}"));
			assertEquals("Invalid get() result", testSixs.get(0).getUuid(),  testOne.replaceVariables(securityContext, ctx, "${get(first(get(this, \"manyToManyTestSixs\")), \"id\")}"));

			// first / last / nth
			assertEquals("Invalid first() result", testSixs.get( 0).toString(), testOne.replaceVariables(securityContext, ctx, "${first(this.manyToManyTestSixs)}"));
			assertEquals("Invalid last() result",  testSixs.get(19).toString(), testOne.replaceVariables(securityContext, ctx, "${last(this.manyToManyTestSixs)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 2).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs,  2)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 7).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs,  7)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 9).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs,  9)}"));
			assertEquals("Invalid ngth() result",  testSixs.get(12).toString(), testOne.replaceVariables(securityContext, ctx, "${nth(this.manyToManyTestSixs, 12)}"));

			// each with more complex logic
//			testOne.replaceVariables(securityContext, ctx, "${each(split(\"one,two,three\"), \"set(parent, this, 1)\")}");
//
//			assertEquals("Invalid each() result", "1", testOne.replaceVariables(securityContext, ctx, "${get(this, \"one\"}"));
//			assertEquals("Invalid each() result", "1", testOne.replaceVariables(securityContext, ctx, "${get(this, \"two\"}"));
//			assertEquals("Invalid each() result", "1", testOne.replaceVariables(securityContext, ctx, "${get(this, \"three\"}"));




		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		// TODO: test find() and mutating functions

	}
}



