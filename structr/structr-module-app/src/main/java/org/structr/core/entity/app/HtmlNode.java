package org.structr.core.entity.app;

import java.util.Map;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.renderer.HtmlRenderer;

/**
 *
 * @author Christian Morgner
 */
public abstract class HtmlNode extends AbstractNode
{
	public abstract boolean hasContent(final HtmlRenderer renderer, final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId);
	public abstract void doBeforeRendering(final HtmlRenderer renderer, final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId);
	public abstract void renderContent(final HtmlRenderer renderer, final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId);

	@Override
	public String getIconSrc()
	{
		return("/images/tag.png");
	}

	@Override
	public void initializeRenderers(final Map<RenderMode, NodeRenderer> rendererMap)
	{
		rendererMap.put(RenderMode.Default, new HtmlRenderer());
	}
}
