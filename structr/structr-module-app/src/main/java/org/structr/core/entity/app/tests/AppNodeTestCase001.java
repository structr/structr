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

package org.structr.core.entity.app.tests;

import org.structr.core.entity.ApplicationNode;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Template;
import org.structr.core.entity.web.HtmlSource;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeTestCase001 extends ApplicationNode
{
	@Override
	public void buildTestCase()
	{

// templates folder
		AbstractNode templates = createNode(this, "Folder", "Templates");


// templates
		Template pageTemplate = (Template)createNode(templates, "Template" , "PageTemplate");
		pageTemplate.setContent(getPageTemplateSource());

		Template inputTemplate = (Template)createNode(templates, "Template" , "InputTemplate");
		inputTemplate.setContent(getInputTemplateSource());

		Template buttonTemplate = (Template)createNode(templates, "Template" , "ButtonTemplate");
		buttonTemplate.setContent(getButtonTemplateSource());

		Template listItemTemplate = (Template)createNode(templates, "Template" , "ListItemTemplate");
		listItemTemplate.setContent(getListItemTemplateSource());


// page 1
		AbstractNode overview = createNode(this, "Page", "overview", pageTemplate);

		HtmlSource tableNode = (HtmlSource)createNode(overview, "HtmlSource", "table");
		tableNode.setContent(getTableTemplateSource());

		AbstractNode listNode = createNode(tableNode, "AppList", "listNode", listItemTemplate);

// page 2
		AbstractNode editor = createNode(this, "Page", "editor", pageTemplate);

		AbstractNode form = createNode(editor, "AppForm", "form1");

		AbstractNode loader = createNode(form, "AppNodeLoader", "loader");
		loader.setProperty("loaderSourceParameter", "param");

		AbstractNode name = createNode(form, "TextField", "name", inputTemplate);
		name.setProperty("label", "Name");

		AbstractNode surname = createNode(form, "TextField", "surname", inputTemplate);
		surname.setProperty("label", "Surname");

		AbstractNode location = createNode(form, "TextField", "location", inputTemplate);
		location.setProperty("label", "Location");

		AbstractNode creator = createNode(form, "AppNodeCreator", "creator");
		creator.setProperty("targetType", "DataNode");

		AbstractNode deleter = createNode(form, "AppNodeDeleter", "delete");

		AbstractNode submitButton = createNode(form, "SubmitButton", "submit", buttonTemplate);
		submitButton.setProperty("label", "Submit");
		linkNodes(form, creator, RelType.SUBMIT);


// Data folder
		AbstractNode dataFolder = createNode(this, "Folder", "Data");		// the data folder
		linkNodes(listNode, dataFolder, RelType.DATA);			// the list source


// source/dest relationships
		linkNodes(creator, overview, RelType.SUCCESS_DESTINATION);
		linkNodes(creator, editor, RelType.ERROR_DESTINATION);
		linkNodes(creator, dataFolder, RelType.CREATE_DESTINATION);

		linkNodes(deleter, overview, RelType.SUCCESS_DESTINATION);
		linkNodes(deleter, overview, RelType.ERROR_DESTINATION);

// data relationships
		linkNodes(loader, creator, RelType.DATA);			// ensures that an edited object is not created again, but saved instead
		linkNodes(loader, deleter, RelType.DATA);

		linkNodes(loader, name, RelType.DATA);				// input from loader to name field
		linkNodes(loader, surname, RelType.DATA);			// input from loader to surname field
		linkNodes(loader, location, RelType.DATA);			// input from loader to location field

		linkNodes(name, creator, RelType.DATA);				// output from name field to creator
		linkNodes(surname, creator, RelType.DATA);			// output from surname field to creator
		linkNodes(location, creator, RelType.DATA);			// output from location field to creator


		// loader  >--DATA-->   name	 >--DATA-->  creator
		// loader  >--DATA-->  surname	 >--DATA-->  creator
		// loader  >--DATA-->  location	 >--DATA-->  creator
	}


	














	private String getTableTemplateSource()
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append("<table border='1' cellspacing='0' cellpadding='5'>\n");
		ret.append("%{listNode}\n");
		ret.append("</table>\n");

		return(ret.toString());
	}


	private String getListItemTemplateSource()
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append("<#setting number_format='0' />\n");
		ret.append("<tr>\n");
		ret.append("<td>${Template.getProperty(\"name\")}</td>\n");
		ret.append("<td>${Template.getProperty(\"surname\")}</td>\n");
		ret.append("<td>${Template.getProperty(\"location\")}</td>\n");
		ret.append("<td><a href='editor/form1/delete?param=${Template.id}'>delete</a></td>\n");
		ret.append("</tr>\n");
		ret.append("</p>\n");

		return(ret.toString());
	}

	private String getButtonTemplateSource()
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append("<p class='title'><input type='submit' value='${SubmitButton.label}' /></p>");

		return(ret.toString());
	}

	private String getInputTemplateSource()
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append("<#if TextField.label ?? >\n");
		ret.append("<p class='title'>\n");
		ret.append("${TextField.label}\n");
		ret.append("</p>\n");
		ret.append("</#if>\n");
		ret.append("<#if TextField.errorMessage ?? >\n");
		ret.append("<p class='error'>\n");
		ret.append("${TextField.errorMessage}\n");
		ret.append("</p>\n");
		ret.append("</#if>\n");
		ret.append("<p>\n");
		ret.append("<input type='text' name='${TextField.name}' <#if TextField.value ?? >value='${TextField.value}'</#if> />\n");
		ret.append("</p>\n");

		return(ret.toString());
	}

	private String getPageTemplateSource()
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append("<html>\n");
		ret.append("<head>\n");
		ret.append("<title>structr-module-app test 001</title>\n");
		ret.append("<style type='text/css'>\n");
		ret.append("a { margin-right:20px; text-decoration:none; }\n");
		ret.append("a:hover { text-decoration:underline; }\n");
		ret.append("p { margin:0px; padding:0px; }\n");
		ret.append("div { margin:0px; margin-bottom:20px; padding:0px; }\n");
		ret.append(".listItem a { color:#ff8800; font-weight:bold; }\n");
		ret.append(".title { margin-top:20px; font-weight:bold; }\n");
		ret.append(".error { color:#cc0000; }\n");
		ret.append("</style>\n");
		ret.append("</head>\n");
		ret.append("<body>\n");
		ret.append("<h2>TestCase 001</h2>\n");
		ret.append("<div>\n");
		ret.append("<a href='overview'>Overview</a>\n");
		ret.append("<a href='editor'>Create</a>\n");
		ret.append("</div>\n");
		ret.append("<#if ErrorMessage?? >\n");
		ret.append("<div>\n");
		ret.append("Fehler: ${ErrorMessage}\n");
		ret.append("</div>\n");
		ret.append("</#if>\n");
		ret.append("%{*}\n");
		ret.append("</body>\n");
		ret.append("</html>\n");

		return(ret.toString());
	}
}
