/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.chart.entity.tests;

import org.structr.chart.entity.AbstractPieChart;
import org.structr.chart.entity.CategoryChart;
import org.structr.chart.entity.Chart;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ApplicationNode;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppNodeTestCase003 extends ApplicationNode {

	@Override
	public void buildTestCase() {

		AbstractNode system       = createNode(this,
			"Folder",
			"sys");
		AbstractNode charts       = createNode(this,
			"Folder",
			"charts");
		AbstractNode pageTemplate = createNode(system,
			"Template",
			"template");

		pageTemplate.setProperty("content",
					 createPageTemplateContent());

		AbstractNode dataType = createNode(system,
						   "NodeType",
						   "DataType");

		dataType.setProperty("_location",
				     "java.lang.String");
		dataType.setProperty("_car",
				     "java.lang.String");

		AbstractNode barType = createNode(system,
						  "NodeType",
						  "BarType");

		barType.setProperty("_valueProperty",
				    "java.lang.String");
		barType.setProperty("_valueAxisLabel",
				    "java.lang.String");
		barType.setProperty("_categoryProperty",
				    "java.lang.String");
		barType.setProperty("_categoryAxisLabel",
				    "java.lang.String");
		barType.setProperty("_recursive",
				    "java.lang.Boolean");
		barType.setProperty("_orientation",
				    "java.lang.String");
		barType.setProperty("_width",
				    "java.lang.Integer");
		barType.setProperty("_height",
				    "java.lang.Integer");

		AbstractNode pieType = createNode(system,
						  "NodeType",
						  "PieType");

		pieType.setProperty("_valueProperty",
				    "java.lang.String");
		pieType.setProperty("_recursive",
				    "java.lang.Boolean");
		pieType.setProperty("_width",
				    "java.lang.Integer");
		pieType.setProperty("_height",
				    "java.lang.Integer");

		AbstractNode data = createNode(this,
					       "Folder",
					       "data");

		// create test set with 2 randomly distributed properties
		for (int i = 0; i < 50; i++) {

			String name       = "test" + ((i < 10)
						      ? "0"
						      : "") + i;
			AbstractNode node = createNode(data,
						       "DataNode",
						       name);

			linkNodes(node,
				  dataType,
				  RelType.CREATE_DESTINATION.TYPE);

			double rand1 = Math.random();
			double rand2 = Math.random();

			// ----- the location property -----
			if (rand1 <= 0.333) {

				node.setProperty("location",
						 "Dortmund");

			} else if (rand1 > 0.666) {

				node.setProperty("location",
						 "Berlin");

			} else {

				node.setProperty("location",
						 "Frankfurt");
			}

			// ----- the car property -----
			if (rand2 <= 0.333) {

				node.setProperty("car",
						 "Golf");

			} else if (rand2 > 0.666) {

				node.setProperty("car",
						 "Volvo");

			} else {

				node.setProperty("car",
						 "Audi");
			}
		}

		AbstractNode page = createNode(this,
					       "Page",
					       "index");

		linkNodes(page,
			  pageTemplate,
			  RelType.USE_TEMPLATE);

		AbstractNode htmlSource = createNode(page,
			"HtmlSource",
			"content");

		htmlSource.setProperty("content",
				       createHtmlSourceContent());

		AbstractNode pieChart = createNode(charts,
						   "PieChart",
						   "pieChart");

		pieChart.setProperty(Chart.Key.width,
				     400);
		pieChart.setProperty(Chart.Key.height,
				     400);
		pieChart.setProperty(AbstractPieChart.Key.recursive,
				     true);
		pieChart.setProperty(AbstractPieChart.Key.valueProperty,
				     "location");

		AbstractNode barChart = createNode(charts,
						   "BarChart",
						   "barChart");

		barChart.setProperty(Chart.Key.width,
				     600);
		barChart.setProperty(Chart.Key.height,
				     400);
		barChart.setProperty(CategoryChart.Key.recursive,
				     true);
		barChart.setProperty(CategoryChart.Key.valueProperty,
				     "location");
		barChart.setProperty(CategoryChart.Key.categoryProperty,
				     "car");
		barChart.setProperty(CategoryChart.Key.orientation,
				     "vertical");

		AbstractNode areaChart = createNode(charts,
			"AreaChart",
			"areaChart");

		areaChart.setProperty(Chart.Key.width,
				      600);
		areaChart.setProperty(Chart.Key.height,
				      400);
		areaChart.setProperty(CategoryChart.Key.recursive,
				      true);
		areaChart.setProperty(CategoryChart.Key.valueProperty,
				      "location");
		areaChart.setProperty(CategoryChart.Key.categoryProperty,
				      "car");
		areaChart.setProperty(CategoryChart.Key.orientation,
				      "vertical");

		AbstractNode stackedAreaChart = createNode(charts,
			"StackedAreaChart",
			"stackedAreaChart");

		stackedAreaChart.setProperty(Chart.Key.width,
					     600);
		stackedAreaChart.setProperty(Chart.Key.height,
					     400);
		stackedAreaChart.setProperty(CategoryChart.Key.recursive,
					     true);
		stackedAreaChart.setProperty(CategoryChart.Key.valueProperty,
					     "location");
		stackedAreaChart.setProperty(CategoryChart.Key.categoryProperty,
					     "car");
		stackedAreaChart.setProperty(CategoryChart.Key.orientation,
					     "vertical");

		AbstractNode lineChart = createNode(charts,
			"LineChart",
			"lineChart");

		lineChart.setProperty(Chart.Key.width,
				      600);
		lineChart.setProperty(Chart.Key.height,
				      400);
		lineChart.setProperty(CategoryChart.Key.recursive,
				      true);
		lineChart.setProperty(CategoryChart.Key.valueProperty,
				      "location");
		lineChart.setProperty(CategoryChart.Key.categoryProperty,
				      "car");
		lineChart.setProperty(CategoryChart.Key.orientation,
				      "vertical");
		linkNodes(pieChart,
			  data,
			  RelType.DATA);
		linkNodes(pieChart,
			  pieType,
			  RelType.TYPE);
		linkNodes(barChart,
			  data,
			  RelType.DATA);
		linkNodes(barChart,
			  barType,
			  RelType.TYPE);
		linkNodes(areaChart,
			  data,
			  RelType.DATA);
		linkNodes(areaChart,
			  barType,
			  RelType.TYPE);
		linkNodes(stackedAreaChart,
			  data,
			  RelType.DATA);
		linkNodes(stackedAreaChart,
			  barType,
			  RelType.TYPE);
		linkNodes(lineChart,
			  data,
			  RelType.DATA);
		linkNodes(lineChart,
			  barType,
			  RelType.TYPE);
	}

	private String createPageTemplateContent() {

		StringBuilder ret    = new StringBuilder(100);
		String lineSeparator = "\n";

		ret.append("<html>").append(lineSeparator);
		ret.append("<head><title>Test</title></head>").append(lineSeparator);
		ret.append("<body>").append(lineSeparator);
		ret.append("<h1>Chart test</h1>").append(lineSeparator);
		ret.append("<div>").append(lineSeparator);
		ret.append("%{*}").append(lineSeparator);
		ret.append("</div>").append(lineSeparator);
		ret.append("</body>").append(lineSeparator);
		ret.append("</html>").append(lineSeparator);

		return (ret.toString());
	}

	private String createHtmlSourceContent() {

		StringBuilder ret    = new StringBuilder(100);
		String lineSeparator = "\n";

		ret.append("<img src=\"charts/pieChart\" />").append(lineSeparator);
		ret.append("<img src=\"charts/barChart\" />").append(lineSeparator);
		ret.append("<img src=\"charts/areaChart\" />").append(lineSeparator);
		ret.append("<img src=\"charts/stackedAreaChart\" />").append(lineSeparator);
		ret.append("<img src=\"charts/lineChart\" />").append(lineSeparator);

		return (ret.toString());
	}
}
