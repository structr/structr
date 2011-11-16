package org.structr.core.renderer;

import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class NodeViewRenderer implements NodeRenderer<AbstractNode>
{
	private static final Logger logger = Logger.getLogger(NodeViewRenderer.class.getName());

	private static final String FOLLOW_RELATIONSHIP_KEY = "followRelationship";
	private static final String ID_SOURCE_KEY = "idSource";

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		AbstractNode sourceNode = loadNode(out.getSecurityContext(), out.getRequest(), currentNode);
		if(sourceNode != null)
		{
			doRendering(out, currentNode, sourceNode, editUrl, editNodeId);

		} else
		{
			logger.log(Level.WARNING, "sourceNode was null");
		}
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return ("text/html");
	}

	// ----- protected methods -----
	protected void doRendering(final StructrOutputStream out, final AbstractNode viewNode, final AbstractNode dataNode, final String editUrl, final Long editNodeId)
	{
		String templateSource = getTemplateFromNode(viewNode);
		StringWriter content = new StringWriter(100);

		viewNode.replaceByFreeMarker(out.getRequest(), templateSource, content, dataNode, editUrl, editNodeId);
		out.append(content.toString());

		List<AbstractNode> viewChildren = viewNode.getSortedDirectChildNodes();
		for(AbstractNode viewChild : viewChildren)
		{
			// 1. get desired display relationship from view node
			// 2. find requested child nodes from dataNode
			// 3. render nodes with doRendering (recursion)

			String followRel = viewChild.getStringProperty(FOLLOW_RELATIONSHIP_KEY);
			if(followRel != null)
			{
				RelationshipType relType = DynamicRelationshipType.withName(followRel);
				List<AbstractNode> dataChildren = dataNode.getDirectChildren(relType);

				for(AbstractNode dataChild : dataChildren)
				{
					doRendering(out, viewChild, dataChild, editUrl, editNodeId);
				}
			}

		}
	}

	// ----- private methods -----
	private AbstractNode loadNode(SecurityContext securityContext, HttpServletRequest request, AbstractNode node)
	{
		String idSourceParameter = node.getStringProperty(ID_SOURCE_KEY);
		String idSource = request.getParameter(idSourceParameter);

		logger.log(Level.INFO, "Got idSourceParameter {0}, parameter {1}, loading node..", new Object[] { idSourceParameter, idSource } );

		return ((AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(node, idSource));
	}

	private String getTemplateFromNode(final AbstractNode node)
	{
		String ret = "";

		if(node.hasTemplate())
		{
			ret = node.getTemplate().getContent();
		}

		return (ret);
	}
}
