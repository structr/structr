package org.structr.web.test.entity;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.Content;
import org.structr.web.entity.Page;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 *
 * @author Christian Morgner
 */

public class ContentTest extends StructrUiTest {
	
	public void testSplitText() {
		
		Document document = getDocument();

		Text content = document.createTextNode("Dies ist ein Test");
		assertNotNull(content);
		
		Element div = document.createElement("div");
		assertNotNull(div);
		
		// add child
		div.appendChild(content);
		
		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());
		
		// test split method
		Content secondPart = (Content)content.splitText(8);
		assertNotNull(secondPart);
		
		assertEquals("Dies ist", content.getData());
		assertEquals(" ein Test", secondPart.getData());
	}

	public void testIsElementContentWhitespace() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		assertEquals(true, content.isElementContentWhitespace());
	}

	public void testGetWholeText() {
		
		// TODO: implement me
	}

	public void testReplaceWholeText() {
		
		// TODO: implement me
	}

	public void testGetData() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());
	}

	public void testSetData() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());
		
	}

	public void testGetLength() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		assertEquals(17, content.getLength());
	}

	public void testSubstringData() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		String substringData = content.substringData(5, 3);
		assertEquals("ist", substringData);
	}

	public void testAppendData() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist");
		assertEquals("Dies ist", content.getData());
		
		content.appendData(" ein Test");
		assertEquals("Dies ist ein Test", content.getData());		
	}

	public void testInsertData() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ein Test");
		assertEquals("Dies ein Test", content.getData());
		
		content.insertData(5, "ist ");
		assertEquals("Dies ist ein Test", content.getData());		
	}

	public void testDeleteData() {
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());
		
		content.deleteData(5, 4);
		assertEquals("Dies ein Test", content.getData());		
	}

	public void testReplaceData(){
		
		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());
		
		content.replaceData(5, 3, "war");
		assertEquals("Dies war ein Test", content.getData());		
	}

	private Document getDocument() {
		
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
	
	private Content getContentNode() {
		
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
