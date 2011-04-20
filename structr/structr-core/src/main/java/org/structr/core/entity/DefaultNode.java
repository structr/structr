/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity;

import java.util.Map;
import org.neo4j.graphdb.Node;
import org.structr.core.cloud.NodeDataContainer;

/**
 *
 * @author Christian Morgner
 */
public class DefaultNode extends AbstractNode
{
    public DefaultNode()
    {
	    super();
    }

    public DefaultNode(final Map<String, Object> properties)
    {
	    super(properties);
    }

    public DefaultNode(final NodeDataContainer data)
    {
	super(data);
    }

    public DefaultNode(final Node dbNode)
    {
        super(dbNode);
    }

    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
    {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible(user)) {
                out.append(getName());
            }

        }
    }

    @Override
    public String getIconSrc() {
        return "/images/error.png";
    }

    @Override
    public void onNodeCreation()
    {
    }

    @Override
    public void onNodeInstantiation()
    {
    }
}
