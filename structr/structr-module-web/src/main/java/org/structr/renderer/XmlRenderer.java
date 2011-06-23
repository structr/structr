package org.structr.renderer;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.Command;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.web.Xml;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.NodeRelationshipsCommand;

/**
 *
 * @author Christian Morgner
 */
public class XmlRenderer implements NodeRenderer<Xml>
{
	private final static String keyPrefix = "${";
	private final static String keySuffix = "}";

	@Override
	public void renderNode(StructrOutputStream out, Xml currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		if(currentNode.isVisible())
		{
			StringBuilder xml = new StringBuilder(currentNode.getXml());

			// start with first occurrence of key prefix
			int start = xml.indexOf(keyPrefix);

			while(start > -1)
			{

				int end = xml.indexOf(keySuffix, start + keyPrefix.length());
				String key = xml.substring(start + keyPrefix.length(), end);

				//System.out.println("Key to replace: '" + key + "'");

				StringBuilder replacement = new StringBuilder();

				// first, look for a property with name=key
				if(currentNode.getNode().hasProperty(key))
				{

					replacement.append(currentNode.getProperty(key));

				} else
				{

					Command nodeFactory = Services.command(NodeFactoryCommand.class);
					Command relsCommand = Services.command(NodeRelationshipsCommand.class);

					List<StructrRelationship> rels = (List<StructrRelationship>)relsCommand.execute(this, RelType.HAS_CHILD, Direction.OUTGOING);
					for(StructrRelationship r : rels)
					{

						AbstractNode s = (AbstractNode)nodeFactory.execute(r.getEndNode());

						if(key.equals(s.getName()))
						{
							s.renderNode(out, startNode, editUrl, editNodeId);
						}


					}

					rels = (List<StructrRelationship>)relsCommand.execute(this, RelType.LINK, Direction.OUTGOING);
					for(StructrRelationship r : rels)
					{

						AbstractNode s = (AbstractNode)nodeFactory.execute(r.getEndNode());

						if(key.equals(s.getName()))
						{
							s.renderNode(out, startNode, editUrl, editNodeId);
						}

					}


				}

				xml.replace(start, end + keySuffix.length(), replacement.toString());

				start = xml.indexOf(keyPrefix, end + keySuffix.length() + 1);

			}


			out.append(xml);
		}
	}

	@Override
	public String getContentType(Xml currentNode)
	{
		return ("text/xml");
	}
}
