package org.structr.web.common;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.Importer;
import org.structr.web.diff.CreateOperation;
import org.structr.web.diff.DeleteOperation;
import org.structr.web.diff.InvertibleModificationOperation;
import org.structr.web.diff.MoveOperation;
import org.structr.web.diff.UpdateOperation;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.w3c.dom.Node;

/**
 *
 * @author Christian Morgner
 */
public class DiffTest extends StructrUiTest {

	public void testDiff() {
		
		try (final Tx tx = app.tx()) {

			final Page sourcePage   = createTestPage();

			final String sourceHtml   = renderPage(sourcePage);
			final String modifiedHtml = modifyHtml(sourceHtml);
			
			System.out.println("###############################################################################################");
			System.out.println("Original HTML:");
			System.out.println(sourceHtml);
			System.out.println("#############################");
			print(sourcePage, 0, 0);
			
			System.out.println("###############################################################################################");
			System.out.println("Modified HTML:");
			System.out.println(modifiedHtml);
			
			// parse page from modified source
			final Page modifiedPage = parsePage(modifiedHtml);
			System.out.println("#############################");
			print(modifiedPage, 0, 0);
			
			final List<InvertibleModificationOperation> changeSet = diff(sourcePage, modifiedPage);
			
			System.out.println("Changes:");
			for (final InvertibleModificationOperation op : changeSet) {

				System.out.println(op.toString());

				// execute operation
				op.apply(app, sourcePage);
			}
			
			final String updatedHtml = renderPage(sourcePage);
			System.out.println("###############################################################################################");
			System.out.println("Updated HTML:");
			System.out.println(updatedHtml);
			System.out.println("#############################");
			print(sourcePage, 0, 0);
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}
	}
	
	private List<InvertibleModificationOperation> diff(final Page sourcePage, final Page modifiedPage) {

		final List<InvertibleModificationOperation> changeSet = new LinkedList<>();
		final Map<String, DOMNode> indexMappedExistingNodes   = new LinkedHashMap<>();
		final Map<String, DOMNode> hashMappedExistingNodes    = new LinkedHashMap<>();
		final Map<String, DOMNode> indexMappedNewNodes        = new LinkedHashMap<>();
		final Map<String, DOMNode> hashMappedNewNodes         = new LinkedHashMap<>();
		
		collectNodes(sourcePage, indexMappedExistingNodes, hashMappedExistingNodes);
		collectNodes(modifiedPage, indexMappedNewNodes, hashMappedNewNodes);
		
		// iterate over existing nodes and try to find deleted ones
		for (final Iterator<Entry<String, DOMNode>> it = indexMappedExistingNodes.entrySet().iterator(); it.hasNext();) {
			
			final Entry<String, DOMNode> existingNodeEntry = it.next();
			final String treeIndex                         = existingNodeEntry.getKey();
			final DOMNode existingNode                     = existingNodeEntry.getValue();
			final String existingHash                      = existingNode.getIdHash();
			
			// check for deleted nodes ignoring Page nodes
			if (!hashMappedNewNodes.containsKey(existingHash) && !(existingNode instanceof Page)) {
				
				changeSet.add(new DeleteOperation(treeIndex, existingNode));
				
				// remove node from change set
				//it.remove();
			}
		}

		// iterate over new nodes and try to find new ones
		for (final Iterator<Entry<String, DOMNode>> it = indexMappedNewNodes.entrySet().iterator(); it.hasNext();) {
			
			final Entry<String, DOMNode> newNodeEntry = it.next();
			final String treeIndex                    = newNodeEntry.getKey();
			final DOMNode newNode                     = newNodeEntry.getValue();
			
			// if newNode is a content element, do not rely on local hash property
			String newHash = newNode.getProperty(DOMNode.dataHashProperty);
			if (newHash == null) {
				newHash = newNode.getIdHash();
			}
			
			// check for deleted nodes ignoring Page nodes
			if (!hashMappedExistingNodes.containsKey(newHash) && !(newNode instanceof Page)) {
				
				changeSet.add(new CreateOperation(treeIndex, null, newNode));
				
				// remove node from change set
				// it.remove();
			}
		}

		// compare all new nodes with all existing nodes
		for (final Entry<String, DOMNode> newNodeEntry : indexMappedNewNodes.entrySet()) {
			
			final String newTreeIndex = newNodeEntry.getKey();
			final DOMNode newNode     = newNodeEntry.getValue();

			for (final Entry<String, DOMNode> existingNodeEntry : indexMappedExistingNodes.entrySet()) {

				final String existingTreeIndex = existingNodeEntry.getKey();
				final DOMNode existingNode     = existingNodeEntry.getValue();
				int equalityBitmask            = 0;

				if (newTreeIndex.equals(existingTreeIndex)) {
					equalityBitmask |= 1;
				}

				if (newNode.getIdHash().equals(existingNode.getIdHash())) {
					equalityBitmask |= 2;
				}

				if (newNode.contentEquals(existingNode)) {
					equalityBitmask |= 4;
				}

				// System.out.println(existingTreeIndex + " / " + newTreeIndex + ": " + equalityBitmask);

				switch (equalityBitmask) {

					case 7:	// same tree index (1), same node (2), same content (4) => node is completely unmodified
						break;

					case 6:	// same content (2), same node (4), NOT same tree index => node has moved
						changeSet.add(new MoveOperation(existingTreeIndex, newTreeIndex, existingNode));
						break;

					case 5:	// same tree index (1), NOT same node, same content (5) => node was deleted and restored, maybe the identification information was lost
						// TODO: how to handle this?
						break;

					case 4:	// NOT same tree index, NOT same node, same content (4) => different node, content is equal by chance?
						// TODO: what to do here?
						break;

					case 3:	// same tree index, same node, NOT same content => node was modified but not moved
						changeSet.add(new UpdateOperation(existingTreeIndex, existingNode, newNode));
						break;

					case 2:	// NOT same tree index, same node (2), NOT same content => node was moved and changed

						// FIXME: order is important here?
						changeSet.add(new UpdateOperation(existingTreeIndex, existingNode, newNode));
						changeSet.add(new MoveOperation(existingTreeIndex, newTreeIndex, existingNode));
						break;

					case 1:	// same tree index (1), NOT same node, NOT same content => ignore
						break;

					case 0:	// NOT same tree index, NOT same node, NOT same content => ignore
						break;
				}
			}
		}
		
		return changeSet;
	}

	
	private String modifyHtml(final String sourceHtml) {

		final StringBuilder modifiedHtml = new StringBuilder(sourceHtml.replace("Initial", "Modified"));
		final int insertPosition         = modifiedHtml.indexOf("</body>");
		
		modifiedHtml.insert(insertPosition, "<div><h3>Another paragraph with title</h3><p>This is the paragraph text</p></div>\n");

		final int delStart = modifiedHtml.indexOf("<h3");
		final int delEnd   = modifiedHtml.indexOf("</h3>");
		
		modifiedHtml.replace(delStart, delEnd+5, "");
		
		return modifiedHtml.toString();

	}

	private String renderPage(final Page page) throws FrameworkException {
		
		final RenderContext ctx = new RenderContext(null, null, RenderContext.EditMode.RAW, Locale.GERMAN);
		final TestBuffer buffer = new TestBuffer();
		ctx.setBuffer(buffer);
		page.render(securityContext, ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}
	
	private Page createTestPage() throws FrameworkException {
		
		final Page page = app.create(Page.class, new NodeAttribute(Page.name, "Test"));
		
		Node html = page.createElement("html");
		Node head = page.createElement("head");
		Node title = page.createElement("title");
		Node titleText = page.createTextNode("${capitalize(page.name)}");
		
		page.appendChild(html);
		html.appendChild(head);
		head.appendChild(title);
		title.appendChild(titleText);
		
		Node body = page.createElement("body");
		html.appendChild(body);

		{
			Node h1 = page.createElement("h1");
			body.appendChild(h1);

			Node h1Text = page.createTextNode("${capitalize(page.name)}");
			h1.appendChild(h1Text);
		}

		{
			Node h2 = page.createElement("h2");
			body.appendChild(h2);

			Node h2Text = page.createTextNode("H2Text");
			h2.appendChild(h2Text);
		}

		{
			Node h3 = page.createElement("h3");
			body.appendChild(h3);

			Node h3Text = page.createTextNode("H3Text");
			h3.appendChild(h3Text);
		}
		
		Node div = page.createElement("div");
		body.appendChild(div);
		
		Node divText = page.createTextNode("Initial body text");
		div.appendChild(divText);
		
		return page;
	}
	
	private void collectNodes(final Page page, final Map<String, DOMNode> indexMappedNodes, final Map<String, DOMNode> hashMappedNodes) {
		collectNodes(page, indexMappedNodes, hashMappedNodes, 0, 0);
	}
	
	private void collectNodes(final DOMNode node, final Map<String, DOMNode> indexMappedNodes, final Map<String, DOMNode> hashMappedNodes, final int depth, final int currentPosition) {
		
		int position = currentPosition;

		// store node with its tree index
		indexMappedNodes.put("[" + depth + ":" + currentPosition + "]", node);
		
		// store node with its data hash
		String dataHash = node.getProperty(DOMNode.dataHashProperty);
		if (dataHash == null) {
			dataHash = node.getIdHash();
		}
		
		hashMappedNodes.put(dataHash, node);

		// recurse
		for (final DOMChildren childRel : node.getChildRelationships()) {
			collectNodes(childRel.getTargetNode(), indexMappedNodes, hashMappedNodes, depth+1, position++);
		}
	}
	
	private Page parsePage(final String source) throws FrameworkException {

		final Importer importer = new Importer(securityContext, source, null, "source", 0, true, true);
		
		Page page  = null;
		
		try (final Tx tx = app.tx()) {
			
			page   = app.create(Page.class, new NodeAttribute<>(Page.name, "Test"));
			
			if (importer.parse()) {
				
				importer.createChildNodesWithHtml(page, page, "");
			}
			
			tx.success();
			
		}
		
		return page;
	}
	
	private void print(final DOMNode node, final int depth, final int currentPosition) {
		
		int position = currentPosition;
		
		for (int i=0; i<depth; i++) {
			
			System.out.print("    ");
		}
		
		System.out.println(node.getProperty(GraphObject.type) + "[" + depth + ":" + currentPosition + "]");
		
		for (final DOMChildren childRel : node.getChildRelationships()) {
			
			print(childRel.getTargetNode(), depth+1, position++);
		}
	}
	
	private static class TestBuffer extends AsyncBuffer {
		
		private StringBuilder buf = new StringBuilder();
		
		@Override
		public AsyncBuffer append(final String s) {
			buf.append(s);
			return this;
		}
		
		public StringBuilder getBuffer() {
			return buf;
		}

		@Override
		public void flush() {
		}

		@Override
		public void onWritePossible() throws IOException {
		}
	}
}



/*
		// first step: detect nodes that have changed.
		for (final Entry<String, DOMNode> entry : nodesFromSourcePage.entrySet()) {
			
			final String sourceIndex = entry.getKey();
			final DOMNode sourceNode = entry.getValue();
			
			if (nodesFromModifiedPage.containsKey(sourceIndex)) {
				
				// check equality
				final DOMNode targetNode = nodesFromModifiedPage.get(sourceIndex);
				if (sourceNode.isSameNode(targetNode)) {
					
					modifiedNodes.add(sourceIndex);
					
				} else {
					
				}
				
			} else {
				
				deletedNodes.add(sourceIndex);
			}
		}

		for (final Entry<String, DOMNode> entry : nodesFromModifiedPage.entrySet()) {
			
			final String targetIndex = entry.getKey();
			final DOMNode targetNode = entry.getValue();
			
			if (nodesFromSourcePage.containsKey(targetIndex)) {
				
				// check equality
				final DOMNode sourceNode = nodesFromModifiedPage.get(targetIndex);
				if (!sourceNode.isSameNode(targetNode)) {
					
					modifiedNodes.add(targetIndex);
				}
				
			} else {
				
				additionalNodes.add(targetIndex);
			}
		}
		
		// additionalNodes contains all node indexes that are not present
		// in the source page, but we don't know where exactly the new
		// nodes were added to the page, so we check all additional nodes
		// for equality with existing nodes
		for (final Entry<String, DOMNode> entry : nodesFromSourcePage.entrySet()) {
			
			final String existingNodePosition = entry.getKey();
			final DOMNode existingNode        = entry.getValue();
			
			// iterate over additional nodes
			for (final String newNodePosition : additionalNodes) {
				
				final DOMNode newNode = nodesFromModifiedPage.get(newNodePosition);
				
				if (existingNode.isSameNode(newNode)) {
					
					// this node was moved from existingNodePosition to newNodePosition
					changeSet.add(new MoveOperation(existingNodePosition, newNodePosition, existingNode));
					
				} else {
					
					// this node was inserted at newNodePosition
					changeSet.add(new InsertOperation(newNodePosition, newNode));
					
				}
			}
			
			// iterate over deleted nodes
			for (final String deleteNodePosition : additionalNodes) {
				
				final DOMNode newNode = nodesFromModifiedPage.get(deleteNodePosition);
				
				if (existingNode.isSameNode(newNode)) {
					
					// this node was moved from existingNodePosition to newNodePosition
					changeSet.add(new MoveOperation(existingNodePosition, deleteNodePosition, existingNode));
					
				} else {
					
					// this node was inserted at newNodePosition
					changeSet.add(new InsertOperation(deleteNodePosition, newNode));
					
				}
			}
		}

 */