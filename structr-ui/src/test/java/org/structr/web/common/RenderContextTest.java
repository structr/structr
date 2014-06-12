package org.structr.web.common;

import java.util.Date;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.parser.Functions;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */


public class RenderContextTest extends StructrUiTest {

	public void testVariableReplacement() {

		final Date now = new Date();
		Page page      = null;
		DOMNode html   = null;
		DOMNode head   = null;
		DOMNode body   = null;
		DOMNode title  = null;
		DOMNode h1     = null;
		DOMNode div1   = null;
		DOMNode p1     = null;
		DOMNode div2   = null;
		DOMNode p2     = null;
		DOMNode div3   = null;
		DOMNode p3     = null;
		DOMNode a      = null;
		DOMNode div4   = null;
		DOMNode p4     = null;

		try (final Tx tx = app.tx()) {

			page = Page.createNewPage(securityContext, "testpage");

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			html  = (DOMNode) page.createElement("html");
			head  = (DOMNode) page.createElement("head");
			body  = (DOMNode) page.createElement("body");
			title = (DOMNode) page.createElement("title");
			h1    = (DOMNode) page.createElement("h1");
			div1  = (DOMNode) page.createElement("div");
			p1    = (DOMNode) page.createElement("p");
			div2  = (DOMNode) page.createElement("div");
			p2    = (DOMNode) page.createElement("p");
			div3  = (DOMNode) page.createElement("div");
			p3    = (DOMNode) page.createElement("p");
			a     = (DOMNode) page.createElement("a");
			div4  = (DOMNode) page.createElement("div");
			p4    = (DOMNode) page.createElement("p");

			// add HTML element to page
			page.appendChild(html);

			// add HEAD and BODY elements to HTML
			html.appendChild(head);
			html.appendChild(body);

			// add TITLE element to HEAD
			head.appendChild(title);

			// add H1 element to BODY
			body.appendChild(h1);

			// add DIV element 1 to BODY
			body.appendChild(div1);
			div1.appendChild(p1);

			// add DIV element 2 to DIV
			div1.appendChild(div2);
			div2.appendChild(p2);

			// add DIV element 3 to DIV
			div2.appendChild(div3);
			div3.appendChild(p3);

			// add link to p3
			p3.appendChild(a);
			a.setProperty(LinkSource.linkable, page);

			body.appendChild(div4);
			div4.appendChild(p4);

			p4.setProperty(DOMElement.restQuery, "/divs");
			p4.setProperty(DOMElement.dataKey, "div");

			NodeList paragraphs = page.getElementsByTagName("p");
			assertEquals(p1, paragraphs.item(0));
			assertEquals(p2, paragraphs.item(1));
			assertEquals(p3, paragraphs.item(2));
			assertEquals(p4, paragraphs.item(3));

			// create users
			final User tester1 = app.create(User.class, new NodeAttribute<>(User.name, "tester1"), new NodeAttribute<>(User.eMail, "tester1@test.com"));
			final User tester2 = app.create(User.class, new NodeAttribute<>(User.name, "tester2"), new NodeAttribute<>(User.eMail, "tester2@test.com"));

			assertNotNull("User tester1 should exist.", tester1);
			assertNotNull("User tester2 should exist.", tester2);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final RenderContext ctx = new RenderContext();
			ctx.setPage(page);

			// test for "empty" return value
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${err}"));
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${this.error}"));
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${this.this.this.error}"));
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${parent.error}"));
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${this.owner}"));
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${parent.owner}"));

			// other functions are tested in the ActionContextTest in structr-core, see there.
			assertEquals("true", p1.replaceVariables(securityContext, ctx, "${true}"));
			assertEquals("false", p1.replaceVariables(securityContext, ctx, "${false}"));
			assertEquals("yes", p1.replaceVariables(securityContext, ctx, "${if(true, \"yes\", \"no\")}"));
			assertEquals("no", p1.replaceVariables(securityContext, ctx, "${if(false, \"yes\", \"no\")}"));
			assertEquals("true", p1.replaceVariables(securityContext, ctx, "${if(true, true, false)}"));
			assertEquals("false", p1.replaceVariables(securityContext, ctx, "${if(false, true, false)}"));

			assertNull(p1.replaceVariables(securityContext, ctx, "${if(true, null, \"no\")}"));
			assertNull(p1.replaceVariables(securityContext, ctx, "${null}"));

			assertEquals("Invalid replacement result", "/testpage?" + page.getUuid(), p1.replaceVariables(securityContext, ctx, "/${page.name}?${page.id}"));
			assertEquals("Invalid replacement result", "/testpage?" + page.getUuid(), a.replaceVariables(securityContext, ctx, "/${link.name}?${link.id}"));

			// these tests find single element => success
			assertEquals("Invalid replacement result", page.getUuid(), a.replaceVariables(securityContext, ctx, "${get(find('Page', 'name', 'testpage'), 'id')}"));
			assertEquals("Invalid replacement result", a.getUuid(), a.replaceVariables(securityContext, ctx, "${get(find('A'), 'id')}"));

			// this test finds multiple <p> elements => error
			assertEquals("Invalid replacement result", Functions.ERROR_MESSAGE_GET_ENTITY, a.replaceVariables(securityContext, ctx, "${get(find('P'), 'id')}"));

			// more complex replacement
			//assertEquals("Invalid replacement result", "", a.replaceVariables(securityContext, ctx, "${get(find('P'), 'id')}"));

			// String default value
			assertEquals("bar", p1.replaceVariables(securityContext, ctx, "${request.foo!bar}"));

			// Number default value (will be evaluated to a string)
			assertEquals("1", p1.replaceVariables(securityContext, ctx, "${page.position!1}"));

			// Number default value
			assertEquals("true", p1.replaceVariables(securityContext, ctx, "${equal(42, this.null!42)}"));


			final User tester1 = app.nodeQuery(User.class).andName("tester1").getFirst();
			final User tester2 = app.nodeQuery(User.class).andName("tester2").getFirst();

			assertNotNull("User tester1 should exist.", tester1);
			assertNotNull("User tester2 should exist.", tester2);

			final SecurityContext tester1Context = SecurityContext.getInstance(tester1, AccessMode.Backend);
			final SecurityContext tester2Context = SecurityContext.getInstance(tester2, AccessMode.Backend);

			// users
			assertEquals("tester1", p1.replaceVariables(tester1Context, ctx, "${me.name}"));
			assertEquals("tester2", p1.replaceVariables(tester2Context, ctx, "${me.name}"));

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

	}
}
