package org.structr.core.entity.app;

import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.renderer.HtmlRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public abstract class HtmlNode extends AbstractNode {

	static {

		EntityContext.registerPropertySet(HtmlNode.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	public abstract void doBeforeRendering(final HtmlRenderer renderer, final StructrOutputStream out,
		final AbstractNode startNode, final String editUrl, final Long editNodeId);

	public abstract void renderContent(final HtmlRenderer renderer, final StructrOutputStream out,
					   final AbstractNode startNode, final String editUrl, final Long editNodeId);

	@Override
	public void initializeRenderers(final Map<RenderMode, NodeRenderer> rendererMap) {

		rendererMap.put(RenderMode.Default,
				new HtmlRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/tag.png";
	}

	public abstract boolean hasContent(final HtmlRenderer renderer, final StructrOutputStream out,
					   final AbstractNode startNode, final String editUrl, final Long editNodeId);
}
