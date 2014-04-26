package org.structr.web.common;

import java.util.Date;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.entity.LinkSource;
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

			NodeList divs = page.getElementsByTagName("p");
			assertEquals(p1, divs.item(0));
			assertEquals(p2, divs.item(1));
			assertEquals(p3, divs.item(2));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final RenderContext ctx = new RenderContext();
			ctx.setPage(page);

			// test for "empty" return value
			assertEquals("", p1.replaceVariables(securityContext, ctx, "${error}"));
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


		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

	}
}
