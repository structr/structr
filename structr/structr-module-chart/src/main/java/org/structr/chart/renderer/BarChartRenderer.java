package org.structr.chart.renderer;

import java.awt.Color;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.structr.chart.entity.CategoryChart;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 *
 * @author Christian Morgner
 */
public class BarChartRenderer extends ChartRenderer implements NodeRenderer<CategoryChart>
{
	public void renderNode(StructrOutputStream out, CategoryChart currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
		String categoryAxisLabel = currentNode.getCategroyAxisLabel();
		String valueAxisLabel = currentNode.getValueAxisLabel();

		String categoryPropertyName = currentNode.getStringProperty(CategoryChart.Key.categoryProperty);
		String valuePropertyName = currentNode.getStringProperty(CategoryChart.Key.valueProperty);

		boolean recursive = currentNode.getRecursive();
		int width = currentNode.getWidth();
		int height = currentNode.getHeight();

		PlotOrientation orientation = PlotOrientation.HORIZONTAL;
		if(currentNode.getOrientation() != null)
		{
			String orientationFromNode = currentNode.getOrientation();
			
			if("vertical".equals(orientationFromNode))	orientation = PlotOrientation.VERTICAL;
			if("horizontal".equals(orientationFromNode))	orientation = PlotOrientation.HORIZONTAL;
		}

		// collect data
		for(StructrRelationship rel : currentNode.getOutgoingDataRelationships())
		{
			AbstractNode endNode = rel.getEndNode();
			for(AbstractNode child : endNode.getDirectChildNodes())
			{
				collectData(categoryPropertyName, valuePropertyName, child, dataSet, recursive);
			}
		}

		CategoryAxis domainAxis = new CategoryAxis(categoryAxisLabel);
		currentNode.configureCategoryAxis(domainAxis);

		ValueAxis valueAxis = new NumberAxis(valueAxisLabel);
		currentNode.configureValueAxis(valueAxis);

		CategoryItemRenderer renderer = currentNode.getCategoryItemRenderer();
		CategoryPlot plot = new CategoryPlot(dataSet, domainAxis, valueAxis, renderer);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOrientation(orientation);

		// allow node to override plot settings
		currentNode.configurePlot(plot);

		JFreeChart chart = new JFreeChart(plot);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(Color.WHITE);

		// allow node to override chart settings
		currentNode.configureChart(chart);

		// write image
		writeImage(out, chart.createBufferedImage(width, height));
	}

	public String getContentType(CategoryChart currentNode)
	{
		return("image/png");
	}

	private void collectData(String categoryPropertyName, String valuePropertyName, AbstractNode startNode, DefaultCategoryDataset dataSet, boolean recursive)
	{
		Object categoryProperty = startNode.getProperty(categoryPropertyName);
		Object valueProperty = startNode.getProperty(valuePropertyName);

		String categoryValue = categoryPropertyName;
		String valueValue = valuePropertyName;

		if(categoryProperty != null)
		{
			categoryValue = categoryProperty.toString();
		}

		if(valueProperty != null)
		{
			valueValue = valueProperty.toString();
		}

		// add value
		incrementValue(dataSet, categoryValue, valueValue);

		if(recursive)
		{
			for(AbstractNode child : startNode.getDirectChildNodes())
			{
				collectData(categoryPropertyName, valuePropertyName, child, dataSet, recursive);
			}
		}
	}

	private void incrementValue(DefaultCategoryDataset dataSet, String categoryValue, String valueValue)
	{
		try
		{
			dataSet.incrementValue(1, categoryValue, valueValue);

		} catch(Throwable t)
		{
			// value does not yet exist
			dataSet.addValue(1, categoryValue, valueValue);
		}
	}
}
