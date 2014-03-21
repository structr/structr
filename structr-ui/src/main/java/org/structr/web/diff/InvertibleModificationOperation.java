package org.structr.web.diff;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;

/**
 *
 * @author Christian Morgner
 */
public abstract class InvertibleModificationOperation {

	public abstract void apply(final App app, final Page page) throws FrameworkException;
	public abstract InvertibleModificationOperation revert();
	
	protected InsertPosition findInsertPosition(final Page page, final String treeIndex) {
		
		return collectNodes(page, 0, new LinkedHashMap<Integer, Integer>(), treeIndex);
	}
	
	private InsertPosition collectNodes(final DOMNode node, final int depth, final Map<Integer, Integer> childIndexMap, final String treeIndex) {
		
		Integer pos  = childIndexMap.get(depth);
		if (pos == null) {
			
			pos = 0;
		}
		
		int position = pos;
		childIndexMap.put(depth, ++position);

		// store node with its tree index
		if (treeIndex.equals("[" + depth + ":" + position + "]")) {
			return new InsertPosition((DOMNode)node.getParentNode(), (DOMNode)node.getNextSibling());
		}
		
		// recurse
		for (final DOMChildren childRel : node.getChildRelationships()) {
			
			final InsertPosition insertPosition = collectNodes(childRel.getTargetNode(), depth+1, childIndexMap, treeIndex);
			if (insertPosition != null) {
				
				return insertPosition;
			}
		}
		
		return null;
	}
	
	protected static class InsertPosition {
		
		private DOMNode parent = null;
		private DOMNode sibling = null;
		
		public InsertPosition(final DOMNode parent, final DOMNode sibling) {
			this.parent  = parent;
			this.sibling = sibling;
		}
		
		public DOMNode getParent() {
			return parent;
		}
		
		public DOMNode getSibling() {
			return sibling;
		}
	}
}
