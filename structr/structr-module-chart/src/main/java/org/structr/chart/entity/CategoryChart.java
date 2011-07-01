package org.structr.chart.entity;

import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.structr.chart.renderer.BarChartRenderer;
import org.structr.common.PropertyKey;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;

/**
 *
 * @author Christian Morgner
 */
public abstract class CategoryChart extends Chart
{
	public enum Key implements PropertyKey
	{
		orientation, categoryAxisLabel, valueAxisLabel,
		categoryProperty, valueProperty, recursive
	}

	public abstract CategoryItemRenderer getCategoryItemRenderer();
	public abstract void configureCategoryAxis(CategoryAxis categoryAxis);
	public abstract void configureValueAxis(ValueAxis valueAxis);
	public abstract void configurePlot(CategoryPlot plot);
	public abstract void configureChart(JFreeChart chart);
	
	public String getOrientation()
	{
		return(getStringProperty(Key.orientation));
	}

	public String getCategroyAxisLabel()
	{
		return(getStringProperty(Key.categoryAxisLabel));
	}

	public String getValueAxisLabel()
	{
		return(getStringProperty(Key.valueAxisLabel));
	}

	public boolean getRecursive()
	{
		return(getBooleanProperty(Key.recursive));
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
	{
		renderers.put(RenderMode.Default, new BarChartRenderer());
		renderers.put(RenderMode.Direct, new BarChartRenderer());
	}
}
