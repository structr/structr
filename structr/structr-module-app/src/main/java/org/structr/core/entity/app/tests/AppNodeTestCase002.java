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

import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Template;
import org.structr.core.entity.app.AppForm;
import org.structr.core.entity.app.AppList;
import org.structr.core.entity.app.AppNodeView;
import org.structr.core.entity.app.ApplicationNode;
import org.structr.core.entity.web.HtmlSource;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeTestCase002 extends ApplicationNode
{
	@Override
	public void buildTestCase()
	{
// templates folder
		AbstractNode templates = createNode(this, "Folder", "Templates");

			Template blogTemplate = (Template)createNode(templates, "Template", "BlogTemplate");
			blogTemplate.setContent(getBlogTemplateContent());

			Template pageTemplate = (Template)createNode(templates, "Template", "PageTemplate");
			pageTemplate.setContent(getPageTemplateContent());

			Template textFieldTemplate = (Template)createNode(templates, "Template", "TextFieldTemplate");
			textFieldTemplate.setContent(getTextFieldTemplateContent());

			Template submitButtonTemplate = (Template)createNode(templates, "Template", "SubmitTemplate");
			submitButtonTemplate.setContent(getSubmitTemplateContent());

			Template textAreaTemplate = (Template)createNode(templates, "Template", "TextAreaTemplate");
			textAreaTemplate.setContent(getTextAreaTemplateContent());

			Template blogAdminTemplate = (Template)createNode(templates, "Template", "BlogAdminTemplate");
			blogAdminTemplate.setContent(getBlogAdminTemplateContent());
			
			Template addCommentTemplate = (Template)createNode(templates, "Template", "AddCommentTemplate");
			addCommentTemplate.setContent(getAddCommentTemplateContent());

			Template commentTemplate = (Template)createNode(templates, "Template", "CommentTemplate");
			commentTemplate.setContent(getCommentTemplateContent());


// blog page
		AbstractNode blogPage = createNode(this, "HomePage", "blog", pageTemplate);

			HtmlSource blogPageContent = (HtmlSource)createNode(blogPage, "HtmlSource", "content");
			blogPageContent.setContent("%{list}");

				AppList blogPageList = (AppList)createNode(blogPageContent, "AppList", "list", blogTemplate);

					AppNodeView blogPageNodeView = (AppNodeView)createNode(blogPageList, "AppNodeView", "view", commentTemplate);
					blogPageNodeView.setProperty("followRelationship", "HAS_CHILD");


// admin page
		AbstractNode adminPage = createNode(this, "Page", "admin", pageTemplate);
		AppList adminPageList = (AppList)createNode(adminPage, "AppList", "list", blogAdminTemplate);

// editor page
		AbstractNode editorPage = createNode(this, "Page", "editor", pageTemplate);

		AbstractNode editorForm = createNode(editorPage, "AppForm", "form");

			AbstractNode editorFormLoader = createNode(editorForm, "AppNodeLoader", "loader");
			editorFormLoader.setProperty("idSource", "id");

			AbstractNode editorContentField = createNode(editorForm, "TextField", "content", textAreaTemplate);
			createNode(editorForm, "SubmitButton", "submit", submitButtonTemplate);
			
			AbstractNode editorFormActions = createNode(editorForm, "AppActionContainer", "actions");

				AbstractNode editorFormCreator = createNode(editorFormActions, "AppNodeCreator", "save");
				editorFormCreator.setProperty("targetType", "DataNode");

// add commen page
		AbstractNode addCommentPage = createNode(this, "Page", "addComment", pageTemplate);

			HtmlSource addCommentContentContainer = (HtmlSource)createNode(addCommentPage, "HtmlSource", "content");
			addCommentContentContainer.setContent("%{view}\n%{form}");

				AbstractNode addCommentNodeView = createNode(addCommentContentContainer, "AppNodeView", "view", addCommentTemplate);
				addCommentNodeView.setProperty("idSource", "id");

					AbstractNode addCommentChildView = createNode(addCommentNodeView, "AppNodeView", "childView", commentTemplate);
					addCommentChildView.setProperty("followRelationship", "HAS_CHILD");

				AbstractNode addCommentForm = createNode(addCommentContentContainer, "AppForm", "form");

					AbstractNode addCommentFormLoader = createNode(addCommentForm, "AppNodeLoader", "loader");
					addCommentFormLoader.setProperty("idSource", "id");

					AbstractNode addCommentCommentField = createNode(addCommentForm, "TextField", "comment", textFieldTemplate);
					createNode(addCommentForm, "SubmitButton", "submit", submitButtonTemplate);

					AbstractNode addCommentActions = createNode(addCommentForm, "AppActionContainer", "actions");

						AbstractNode addCommentCreator = createNode(addCommentActions, "AppNodeCreator", "save");
						addCommentCreator.setProperty("targetType", "DataNode");

						AbstractNode addCommentLinker = createNode(addCommentActions, "AppRelationshipCreator", "link");
						addCommentLinker.setProperty("targetRelType", "HAS_CHILD");


// blog entry folder
		AbstractNode blogEntries = createNode(this, "Folder", "BlogEntries");

// system folder
		AbstractNode system = createNode(this, "Folder", "System");
		AbstractNode timestamp = createNode(system, "AppTimestamp", "timestamp");

// relationships

		// blog page
		linkNodes(blogPageList, blogEntries, RelType.DATA);

		// admin page
		linkNodes(adminPageList, blogEntries, RelType.DATA);

		// editor page
		linkNodes(editorFormLoader, editorContentField, RelType.DATA);
		linkNodes(editorFormLoader, editorFormCreator, RelType.DATA);
		linkNodes(editorContentField, editorFormCreator, RelType.DATA);
		linkNodes(timestamp, editorFormCreator, RelType.DATA);

		// add blog entry actions
		linkNodes(editorFormCreator, blogEntries, RelType.CREATE_DESTINATION);
		linkNodes(editorFormActions, editorPage, RelType.ERROR_DESTINATION);
		linkNodes(editorFormActions, blogPage, RelType.SUCCESS_DESTINATION);

		// add comment page
		linkNodes(addCommentNodeView, blogEntries, RelType.DATA);

		linkNodes(addCommentFormLoader, addCommentCommentField, RelType.DATA);
		linkNodes(addCommentCommentField, addCommentCreator, RelType.DATA);

		linkNodes(addCommentFormLoader, addCommentLinker, RelType.DATA).setProperty("targetSlotName", "startNode");
		linkNodes(addCommentCreator, addCommentLinker, RelType.DATA).setProperty("targetSlotName", "endNode");

		// add comment actions
		linkNodes(addCommentActions, addCommentPage, RelType.ERROR_DESTINATION);
		linkNodes(addCommentActions, blogPage, RelType.SUCCESS_DESTINATION);

	}

	private String getBlogTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<#setting number_format=\"0\" />\n");
		ret.append("<#if Template.getProperty(\"timestamp\") ?? >\n");
		ret.append("<h2>${Template.getProperty(\"timestamp\")}</h2>\n");
		ret.append("</#if>\n");
		ret.append("<#if Template.getProperty(\"content\") ?? >\n");
		ret.append("${Template.getProperty(\"content\")}\n");
		ret.append("</#if>\n");
		ret.append("<p>\n");
		ret.append("<a href='addComment?id=${Template.id}'>Kommentar hinzuf&uuml;gen</a>\n");
		ret.append("</p>\n");

		return(ret.toString());
	}

	private String getPageTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<html>\n");
		ret.append("<head>\n");
		ret.append("<title>Mein Test-Blog</title>\n");
		ret.append("<style type=\"text/css\">\n");
		ret.append("input[type=\"text\"] { width:100%; }\n");
		ret.append("a { margin-right:10px; text-decoration:none; }\n");
		ret.append("a:hover { text-decoration:underline; }\n");
		ret.append("</style>\n");
		ret.append("</head>\n");
		ret.append("<body>\n");
		ret.append("<h1>Mein Test-Blog</h1>\n");
		ret.append("<p>\n");
		ret.append("<a href='blog'>&Uuml;bersicht</a>\n");
		ret.append("<a href='admin'>Administration</a>\n");
		ret.append("<a href='editor'>Neuer Eintrag</a>\n");
		ret.append("</p>\n");
		ret.append("%{*}\n");
		ret.append("</body>\n");
		ret.append("</html>\n");

		return(ret.toString());
	}

	private String getTextFieldTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<p>\n");
		ret.append("<input type=\"text\" name=\"${TextField.name}\" <#if TextField.value ?? >value=\"${TextField.value}\"</#if> />\n");
		ret.append("</p>\n");

		return(ret.toString());
	}

	private String getSubmitTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<input type=\"submit\" value=\"Speichern\" />\n");
		
		return(ret.toString());
	}

	private String getTextAreaTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<script type=\"text/javascript\" src=\"/t5s/ckeditor/ckeditor.js\"></script>\n");
		ret.append("<script type=\"text/javascript\">\n");
		ret.append("window.onload = function()\n");
		ret.append("{\n");
		ret.append("CKEDITOR.replace( 'content',\n");
		ret.append("{\n");
		ret.append("scayt_autoStartup         : false,\n");
		ret.append("disableNativeSpellChecker : true\n");
		ret.append("});\n");
		ret.append("}\n");
		ret.append("</script>\n");
		ret.append("<p>\n");
		ret.append("<textarea name=\"${TextField.name}\" id=\"content\">\n");
		ret.append("<#if TextField.value ?? >${TextField.value}</#if>\n");
		ret.append("</textarea>\n");
		ret.append("</p>\n");

	 return(ret.toString());
	}

	private String getBlogAdminTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<#setting number_format=\"0\" />\n");
		ret.append("<#if Template.getProperty(\"timestamp\") ?? >\n");
		ret.append("<h2>${Template.getProperty(\"timestamp\")}</h2>\n");
		ret.append("</#if>\n");
		ret.append("<#if Template.getProperty(\"content\") ?? >\n");
		ret.append("${Template.getProperty(\"content\")}\n");
		ret.append("</#if>\n");
		ret.append("<p style=\"text-align:right; padding-right:20px;\">\n");
		ret.append("<a href=\"editor?id=${Template.id}\">Bearbeiten</a>\n");
		ret.append("</p>\n");

		return(ret.toString());
	}

	private String getAddCommentTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<#setting number_format=\"0\" />\n");
		ret.append("<#if Template.getProperty(\"timestamp\") ?? >\n");
		ret.append("<h2>${Template.getProperty(\"timestamp\")}</h2>\n");
		ret.append("</#if>\n");
		ret.append("<#if Template.getProperty(\"content\") ?? >\n");
		ret.append("${Template.getProperty(\"content\")}\n");
		ret.append("</#if>\n");

		return(ret.toString());
	}

	private String getCommentTemplateContent()
	{
		StringBuilder ret = new StringBuilder();

		ret.append("<#if Template.getProperty(\"comment\") ?? >\n");
		ret.append("<p><b>Kommentar:</b>&nbsp;${Template.getProperty(\"comment\")}</p>\n");
		ret.append("</#if>\n");

		return(ret.toString());
	}
}
