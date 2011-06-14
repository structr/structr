package org.structr.chart.entity;

import java.util.Map;
import org.structr.chart.renderer.ChartRenderer;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;

/**
 *
 * @author Christian Morgner
 */
public class BarChart extends Chart
{
	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
	{
		renderers.put(RenderMode.Default, new ChartRenderer());
	}

	@Override
	public String getIconSrc()
	{
		return ("/images/chart_bar.png");
	}
}
