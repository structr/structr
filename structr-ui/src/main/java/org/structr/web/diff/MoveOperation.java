package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class MoveOperation extends InvertibleModificationOperation {

	private DOMNode originalNode = null;
	private String newTreeIndex  = null;
	
	public MoveOperation(final DOMNode originalNode, final String newTreeIndex) {

		this.newTreeIndex = newTreeIndex;
		this.originalNode = originalNode;
	}

	public DOMNode getOriginalNode() {
		return originalNode;
	}

	@Override
	public String toString() {
		return "Move " + originalNode.getIdHashOrProperty() + " to " + newTreeIndex;
	}
	
	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page page) throws FrameworkException {
		
		final InsertPosition insertPosition = findInsertPosition(page, newTreeIndex);
		if (insertPosition != null) {
			
			final DOMNode parent  = insertPosition.getParent();
			final DOMNode sibling = insertPosition.getSibling();
			
			parent.insertBefore(originalNode, sibling);
		}
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}
}
