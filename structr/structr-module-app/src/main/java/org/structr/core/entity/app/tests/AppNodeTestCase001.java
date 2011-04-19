/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app.tests;

import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PlainText;

/**
 *
 * @author chrisi
 */
public class AppNodeTestCase001 extends AppNodeTestCase
{
	@Override
	public void buildTestCase()
	{

// templates folder
		AbstractNode templates = createNode("Folder", "Templates");
		linkNodes(this, templates, RelType.HAS_CHILD);


// templates
		PlainText pageTemplate = (PlainText)createNode("Template" , "PageTemplate");
		linkNodes(templates, pageTemplate, RelType.HAS_CHILD);
		pageTemplate.setContent(getPageTemplateSource());
		pageTemplate.setContentType("text/plain");

		PlainText inputTemplate = (PlainText)createNode("Template" , "InputTemplate");
		linkNodes(templates, inputTemplate, RelType.HAS_CHILD);
		inputTemplate.setContent(getInputTemplateSource());
		inputTemplate.setContentType("text/plain");

		PlainText buttonTemplate = (PlainText)createNode("Template" , "ButtonTemplate");
		linkNodes(templates, buttonTemplate, RelType.HAS_CHILD);
		buttonTemplate.setContent(getButtonTemplateSource());
		buttonTemplate.setContentType("text/plain");

		PlainText listItemTemplate = (PlainText)createNode("Template" , "ListItemTemplate");
		linkNodes(templates, listItemTemplate, RelType.HAS_CHILD);
		listItemTemplate.setContent(getListItemTemplateSource());
		listItemTemplate.setContentType("text/plain");


// page 1
		AbstractNode overview = createNode("Page", "overview");
		linkNodes(this, overview, RelType.HAS_CHILD);
		linkNodes(overview, pageTemplate, RelType.USE_TEMPLATE);

		AbstractNode listNode = createNode("AppList", "listNode");
		linkNodes(overview, listNode, RelType.HAS_CHILD);
		linkNodes(listNode, listItemTemplate, RelType.USE_TEMPLATE);

// page 2
		AbstractNode editor = createNode("Page", "editor");
		linkNodes(this, editor, RelType.HAS_CHILD);
		linkNodes(editor, pageTemplate, RelType.USE_TEMPLATE);

		AbstractNode form = createNode("AppForm", "form1");
		linkNodes(editor, form, RelType.HAS_CHILD);

		AbstractNode loader = createNode("AppNodeLoader", "loader");
		loader.setProperty("loaderSourceParameter", "param");
		linkNodes(form, loader, RelType.HAS_CHILD);

		AbstractNode name = createNode("TextField", "name");
		name.setProperty("label", "Name");
		linkNodes(form, name, RelType.HAS_CHILD);
		linkNodes(name, inputTemplate, RelType.USE_TEMPLATE);

		AbstractNode surname = createNode("TextField", "surname");
		surname.setProperty("label", "Surname");
		linkNodes(form, surname, RelType.HAS_CHILD);
		linkNodes(surname, inputTemplate, RelType.USE_TEMPLATE);

		AbstractNode location = createNode("TextField", "location");
		location.setProperty("label", "Location");
		linkNodes(form, location, RelType.HAS_CHILD);
		linkNodes(location, inputTemplate, RelType.USE_TEMPLATE);

		AbstractNode creator = createNode("AppNodeCreator", "creator");
		creator.setProperty("targetType", "DataNode");
		linkNodes(form, creator, RelType.HAS_CHILD);

		AbstractNode submitButton = createNode("SubmitButton", "submit");
		submitButton.setProperty("label", "Submit");
		linkNodes(form, submitButton, RelType.HAS_CHILD);
		linkNodes(form, creator, RelType.SUBMIT);
		linkNodes(submitButton, buttonTemplate, RelType.USE_TEMPLATE);


// Data folder
		AbstractNode dataFolder = createNode("Folder", "Data");		// the data folder
		linkNodes(this, dataFolder, RelType.HAS_CHILD);
		linkNodes(listNode, dataFolder, RelType.DATA);			// the list source


// source/dest relationships
		linkNodes(creator, overview, RelType.SUCCESS_DESTINATION);
		linkNodes(creator, editor, RelType.ERROR_DESTINATION);
		linkNodes(creator, dataFolder, RelType.CREATE_DESTINATION);


// data relationships
		linkNodes(loader, creator, RelType.DATA);			// ensures that an edited object is not created again, but saved instead

		linkNodes(loader, name, RelType.DATA);				// input from loader to name field
		linkNodes(loader, surname, RelType.DATA);			// input from loader to surname field
		linkNodes(loader, location, RelType.DATA);			// input from loader to location field

		linkNodes(name, creator, RelType.DATA);				// output from name field to creator
		linkNodes(surname, creator, RelType.DATA);			// output from surname field to creator
		linkNodes(location, creator, RelType.DATA);			// output from location field to creator


	}


	

















	private String getListItemTemplateSource()
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append("<#setting number_format='0' />\n");
		ret.append("<p class='listItem'>\n");
		ret.append("<a href='editor?param=${Template.id}'>\n");
		ret.append("<#if Template.getProperty(\"name\") ?? >${Template.getProperty(\"name\")}</#if>");
		ret.append(",&nbsp;");
		ret.append("<#if Template.getProperty(\"surname\") ?? >${Template.getProperty(\"surname\")}</#if>");
		ret.append("<#if Template.getProperty(\"location\") ?? >&nbsp;(${Template.getProperty(\"location\")})</#if>");
		ret.append("</a>\n");
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
