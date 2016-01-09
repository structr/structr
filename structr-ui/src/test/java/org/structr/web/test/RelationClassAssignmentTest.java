/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.TestCase.fail;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.relation.PageLink;
import org.w3c.dom.Text;

/**
 *
 *
 */
public class RelationClassAssignmentTest extends StructrUiTest {
	
	private static final Logger logger = Logger.getLogger(CreateSimplePageTest.class.getName());

	public void test01DOMChildren() {

		final String pageName	= "page-01";
		final String pageTitle	= "Page Title";
		final String bodyText	= "Body Text";
		
		final String h1ClassAttr = "heading";
		final String divClassAttr = "main";

		try (final Tx tx = app.tx()) {

			Page page = Page.createNewPage(securityContext, pageName);
			if (page != null) {
				
				DOMElement html  = (DOMElement) page.createElement("html");
				DOMElement head  = (DOMElement) page.createElement("head");
				DOMElement title = (DOMElement) page.createElement("title");
				Text titleText   = page.createTextNode(pageTitle);

				for (AbstractRelationship r : page.getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertTrue(r instanceof PageLink);
					
				}
				
				html.appendChild(head);
				
				for (AbstractRelationship r : head.getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertTrue(r instanceof DOMChildren);
					
				}

				head.appendChild(title);
				title.appendChild(titleText);

				for (AbstractRelationship r : ((DOMNode) titleText).getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertTrue(r instanceof DOMChildren);
					
				}

				
				
			}
			
			tx.success();
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
		
	

}
