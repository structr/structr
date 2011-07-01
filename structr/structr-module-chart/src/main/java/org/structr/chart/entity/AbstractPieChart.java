package org.structr.chart.entity;

import java.util.Map;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;
import org.structr.chart.renderer.PieChartRenderer;
import org.structr.common.PropertyKey;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractPieChart extends Chart
{
	public enum Key implements PropertyKey
	{
		label, valueProperty, recursive
	}

	public abstract PiePlot getPiePlot(PieDataset dataSet);

	public String getLabel()
	{
		return(getStringProperty(Key.label));
	}

	public String getValueProperty()
	{
		return(getStringProperty(Key.valueProperty));
	}

	public boolean getRecursive()
	{
		return(getBooleanProperty(Key.recursive));
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
	{
		renderers.put(RenderMode.Default, new PieChartRenderer());
		renderers.put(RenderMode.Direct, new PieChartRenderer());
	}

	@Override
	public String getIconSrc()
	{
		return ("/images/chart_pie.png");
	}
}
