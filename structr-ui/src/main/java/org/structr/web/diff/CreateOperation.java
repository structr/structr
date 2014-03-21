package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class CreateOperation extends InvertibleModificationOperation {

	private String newTreeIndex = null;
	private DOMNode newNode     = null;
	
	public CreateOperation(final DOMNode newNode, final String newTreeIndex) {
		
		this.newTreeIndex = newTreeIndex;
		this.newNode      = newNode;
	}

	@Override
	public String toString() {
		
		return "Create " + newNode.getIdHashOrProperty() + " at " + newTreeIndex;
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page page) throws FrameworkException {
		
		final InsertPosition insertPosition = findInsertPosition(page, newTreeIndex);
		if (insertPosition != null) {
			
			final DOMNode parent  = insertPosition.getParent();
			final DOMNode sibling = insertPosition.getSibling();

			if (sibling != null) {
				
				System.out.println("Inserting at " + parent.getIdHashOrProperty() + " before " + sibling.getIdHashOrProperty());
				
			} else {
				
				System.out.println("Inserting at " + parent.getIdHashOrProperty());
			}
			
			page.adoptNode(newNode);
			
			parent.insertBefore(newNode, sibling);
			
		}
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}
}
