package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class MoveOperation implements InvertibleModificationOperation {

	private String originalTreeIndex = null;
	private String newTreeIndex      = null;
	private DOMNode originalNode     = null;
	
	public MoveOperation(final String originalTreeIndex, final String newTreeIndex, final DOMNode originalNode) {
		
		this.originalTreeIndex = originalTreeIndex;
		this.newTreeIndex      = newTreeIndex;
		this.originalNode      = originalNode;
	}

	public String getOriginalTreeIndex() {
		return originalTreeIndex;
	}

	public String getNewTreeIndex() {
		return newTreeIndex;
	}

	public DOMNode getOriginalNode() {
		return originalNode;
	}

	@Override
	public String toString() {
		return "Move " + originalNode + " from " + originalTreeIndex + " to " + newTreeIndex;
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
