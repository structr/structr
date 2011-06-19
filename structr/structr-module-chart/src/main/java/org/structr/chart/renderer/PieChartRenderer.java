package org.structr.chart.renderer;

import java.awt.Color;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.structr.chart.entity.AbstractPieChart;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 *
 * @author Christian Morgner
 */
public class PieChartRenderer extends ChartRenderer implements NodeRenderer<AbstractPieChart>
{
	public void renderNode(StructrOutputStream out, AbstractPieChart currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		String valuePropertyName = currentNode.getStringProperty(AbstractPieChart.Key.valueProperty);
		DefaultPieDataset dataSet = new DefaultPieDataset();

		boolean recursive = currentNode.getRecursive();
		int width = currentNode.getWidth();
		int height = currentNode.getHeight();

		// collect data
		for(StructrRelationship rel : currentNode.getOutgoingDataRelationships())
		{
			AbstractNode endNode = rel.getEndNode();
			for(AbstractNode child : endNode.getDirectChildNodes())
			{
				collectData(valuePropertyName, child, dataSet, recursive);
			}
		}

		PiePlot plot = currentNode.getPiePlot(dataSet);
		plot.setBackgroundPaint(Color.WHITE);

		JFreeChart chart = new JFreeChart(plot);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(Color.WHITE);

		// write image
		writeImage(out, chart.createBufferedImage(width, height));
	}

	public String getContentType(AbstractPieChart currentNode)
	{
		return("image/png");
	}

	private void collectData(String valuePropertyName, AbstractNode startNode, DefaultPieDataset dataSet, boolean recursive)
	{
		Object valueProperty = startNode.getProperty(valuePropertyName);
		String valueValue = valuePropertyName;

		if(valueProperty != null)
		{
			valueValue = valueProperty.toString();
		}

		// add value
		incrementValue(dataSet, valueValue);

		if(recursive)
		{
			for(AbstractNode child : startNode.getDirectChildNodes())
			{
				collectData(valuePropertyName, child, dataSet, recursive);
			}
		}
	}

	private void incrementValue(DefaultPieDataset dataSet, String valueValue)
	{
		try
		{
			Number number = dataSet.getValue(valueValue);
			dataSet.setValue(valueValue, number.intValue() + 1);

		} catch(Throwable t)
		{
			// value does not yet exist
			dataSet.setValue(valueValue, 1);
		}
	}
}
