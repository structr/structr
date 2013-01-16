package org.structr.web.common;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.Document;

/**
 *
 * @author Christian Morgner
 */

public class DOMTest extends StructrUiTest {
	
	protected Document getDocument() {
		
		try {
			
			List<Page> pages = this.createTestNodes(Page.class, 1);

			if (!pages.isEmpty()) {
				
				return pages.get(0);
			}
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}

		return null;
		
		
	}
	
	protected Content getContentNode() {
		
		try {
			
			List<Content> contents = this.createTestNodes(Content.class, 1);

			if (!contents.isEmpty()) {
				
				return contents.get(0);
			}
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}

		return null;
	 }

}
