package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class UpdateOperation implements InvertibleModificationOperation {

	private String treeIndex     = null;
	private DOMNode existingNode = null;
	private DOMNode newNode      = null;
	
	public UpdateOperation(final String treeIndex, final DOMNode existingNode, final DOMNode newNode) {
		
		this.treeIndex    = treeIndex;
		this.existingNode = existingNode;
		this.newNode      = newNode;
	}

	public String getTreeIndex() {
		return treeIndex;
	}

	public DOMNode getExistingNode() {
		return existingNode;
	}

	public DOMNode getNewNode() {
		return newNode;
	}

	@Override
	public String toString() {
		return "Update " + existingNode + " at " + treeIndex + " with " + newNode;
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
