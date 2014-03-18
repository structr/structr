package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class DeleteOperation implements InvertibleModificationOperation {

	private String treeIndex     = null;
	private DOMNode existingNode = null;
	
	public DeleteOperation(final String treeIndex, final DOMNode existingNode) {
		
		this.treeIndex    = treeIndex;
		this.existingNode = existingNode;
	}

	public String getTreeIndex() {
		return treeIndex;
	}

	public DOMNode getExistingNode() {
		return existingNode;
	}

	@Override
	public String toString() {
		return "Delete " + existingNode + " at " + treeIndex;
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(Page page, DOMNode node) throws FrameworkException {
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}
}
