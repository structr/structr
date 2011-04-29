/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
		AbstractNode templates = createNode("Folder", "Templates");
		linkNodes(this, templates, RelType.HAS_CHILD);

			Template blogTemplate = (Template)createNode("Template", "BlogTemplate");
			blogTemplate.setContent(getBlogTemplateContent());
			linkNodes(templates, blogTemplate, RelType.HAS_CHILD);

			Template pageTemplate = (Template)createNode("Template", "PageTemplate");
			pageTemplate.setContent(getPageTemplateContent());
			linkNodes(templates, pageTemplate, RelType.HAS_CHILD);

			Template textFieldTemplate = (Template)createNode("Template", "TextFieldTemplate");
			textFieldTemplate.setContent(getTextFieldTemplateContent());
			linkNodes(templates, textFieldTemplate, RelType.HAS_CHILD);

			Template submitButtonTemplate = (Template)createNode("Template", "SubmitTemplate");
			submitButtonTemplate.setContent(getSubmitTemplateContent());
			linkNodes(templates, submitButtonTemplate, RelType.HAS_CHILD);

			Template textAreaTemplate = (Template)createNode("Template", "TextAreaTemplate");
			textAreaTemplate.setContent(getTextAreaTemplateContent());
			linkNodes(templates, textAreaTemplate, RelType.HAS_CHILD);

			Template blogAdminTemplate = (Template)createNode("Template", "BlogAdminTemplate");
			blogAdminTemplate.setContent(getBlogAdminTemplateContent());
			linkNodes(templates, blogAdminTemplate, RelType.HAS_CHILD);

			Template addCommentTemplate = (Template)createNode("Template", "AddCommentTemplate");
			addCommentTemplate.setContent(getAddCommentTemplateContent());
			linkNodes(templates, addCommentTemplate, RelType.HAS_CHILD);

			Template commentTemplate = (Template)createNode("Template", "CommentTemplate");
			commentTemplate.setContent(getCommentTemplateContent());
			linkNodes(templates, commentTemplate, RelType.HAS_CHILD);


// blog page
		AbstractNode blogPage = createNode("HomePage", "blog");
		linkNodes(this, blogPage, RelType.HAS_CHILD);

			HtmlSource blogPageContent = (HtmlSource)createNode("HtmlSource", "content");
			blogPageContent.setContent("%{list}");
			linkNodes(blogPage, blogPageContent, RelType.HAS_CHILD);

				AppList blogPageList = (AppList)createNode("AppList", "list");
				linkNodes(blogPageContent, blogPageList, RelType.HAS_CHILD);

					AppNodeView blogPageNodeView = (AppNodeView)createNode("AppNodeView", "view");
					blogPageNodeView.setProperty("followRelationship", "HAS_CHILD");
					linkNodes(blogPageList, blogPageNodeView, RelType.HAS_CHILD);


// admin page
		AbstractNode adminPage = createNode("Page", "admin");
		linkNodes(this, adminPage, RelType.HAS_CHILD);

			AppList adminPageList = (AppList)createNode("AppList", "list");
			linkNodes(adminPage, adminPageList, RelType.HAS_CHILD);

// editor page
		AbstractNode editorPage = createNode("Page", "editor");
		linkNodes(this, editorPage, RelType.HAS_CHILD);

		AbstractNode editorForm = createNode("AppForm", "form");
		linkNodes(editorPage, editorForm, RelType.HAS_CHILD);

			AbstractNode editorFormLoader = createNode("AppNodeLoader", "loader");
			editorFormLoader.setProperty("idSource", "id");
			linkNodes(editorForm, editorFormLoader, RelType.HAS_CHILD);

			AbstractNode editorContentField = createNode("TextField", "content");
			linkNodes(editorForm, editorContentField, RelType.HAS_CHILD);

			AbstractNode editorSubmitButton = createNode("SubmitButton", "submit");
			linkNodes(editorForm, editorSubmitButton, RelType.HAS_CHILD);

			AbstractNode editorFormActions = createNode("AppActionContainer", "actions");
			linkNodes(editorForm, editorFormActions, RelType.HAS_CHILD);

				AbstractNode editorFormCreator = createNode("AppNodeCreator", "save");
				editorFormCreator.setProperty("targetType", "DataNode");
				linkNodes(editorFormActions, editorFormCreator, RelType.HAS_CHILD);

// add commen page
		AbstractNode addCommentPage = createNode("Page", "addComment");
		linkNodes(this, addCommentPage, RelType.HAS_CHILD);

			HtmlSource addCommentContentContainer = (HtmlSource)createNode("HtmlSource", "content");
			addCommentContentContainer.setContent("%{view}\n%{form}");
			linkNodes(addCommentPage, addCommentContentContainer, RelType.HAS_CHILD);

				AbstractNode addCommentNodeView = createNode("AppNodeView", "view");
				addCommentNodeView.setProperty("idSource", "id");
				linkNodes(addCommentContentContainer, addCommentNodeView, RelType.HAS_CHILD);

					AbstractNode addCommentChildView = createNode("AppNodeView", "childView");
					addCommentChildView.setProperty("followRelationship", "HAS_CHILD");
					linkNodes(addCommentNodeView, addCommentChildView, RelType.HAS_CHILD);

				AbstractNode addCommentForm = createNode("AppForm", "form");
				linkNodes(addCommentContentContainer, addCommentForm, RelType.HAS_CHILD);

					AbstractNode addCommentFormLoader = createNode("AppNodeLoader", "loader");
					addCommentFormLoader.setProperty("idSource", "id");
					linkNodes(addCommentForm, addCommentFormLoader, RelType.HAS_CHILD);

					AbstractNode addCommentCommentField = createNode("TextField", "comment");
					linkNodes(addCommentForm, addCommentCommentField, RelType.HAS_CHILD);

					AbstractNode addCommentSubmitButton = createNode("SubmitButton", "submit");
					linkNodes(addCommentForm, addCommentSubmitButton, RelType.HAS_CHILD);

					AbstractNode addCommentActions = createNode("AppActionContainer", "actions");
					linkNodes(addCommentForm, addCommentActions, RelType.HAS_CHILD);

						AbstractNode addCommentCreator = createNode("AppNodeCreator", "save");
						addCommentCreator.setProperty("targetType", "DataNode");
						linkNodes(addCommentActions, addCommentCreator, RelType.HAS_CHILD);

						AbstractNode addCommentLinker = createNode("AppRelationshipCreator", "link");
						addCommentLinker.setProperty("targetRelType", "HAS_CHILD");
						linkNodes(addCommentActions, addCommentLinker, RelType.HAS_CHILD);




// blog entry folder
		AbstractNode blogEntries = createNode("Folder", "BlogEntries");
		linkNodes(this, blogEntries, RelType.HAS_CHILD);

// system folder
		AbstractNode system = createNode("Folder", "System");
		linkNodes(this, system, RelType.HAS_CHILD);

			AbstractNode timestamp = createNode("AppTimestamp", "timestamp");
			linkNodes(system, timestamp, RelType.HAS_CHILD);

// relationships

		// page templates
		linkNodes(blogPage, pageTemplate, RelType.USE_TEMPLATE);
		linkNodes(adminPage, pageTemplate, RelType.USE_TEMPLATE);
		linkNodes(editorPage, pageTemplate, RelType.USE_TEMPLATE);
		linkNodes(addCommentPage, pageTemplate, RelType.USE_TEMPLATE);

		// blog page
		linkNodes(blogPageList, blogTemplate, RelType.USE_TEMPLATE);
		linkNodes(blogPageList, blogEntries, RelType.DATA);
		linkNodes(blogPageNodeView, commentTemplate, RelType.USE_TEMPLATE);

		// admin page
		linkNodes(adminPageList, blogAdminTemplate, RelType.USE_TEMPLATE);
		linkNodes(adminPageList, blogEntries, RelType.DATA);

		// editor page
		linkNodes(editorFormLoader, editorContentField, RelType.DATA);
		linkNodes(editorFormLoader, editorFormCreator, RelType.DATA);

		linkNodes(editorContentField, editorFormCreator, RelType.DATA);
		linkNodes(editorContentField, textAreaTemplate, RelType.USE_TEMPLATE);
		linkNodes(editorSubmitButton, submitButtonTemplate, RelType.USE_TEMPLATE);

		linkNodes(timestamp, editorFormCreator, RelType.DATA);

		linkNodes(editorFormCreator, blogEntries, RelType.CREATE_DESTINATION);
		linkNodes(editorFormActions, editorPage, RelType.ERROR_DESTINATION);
		linkNodes(editorFormActions, blogPage, RelType.SUCCESS_DESTINATION);

		// add comment page
		linkNodes(addCommentNodeView, blogEntries, RelType.DATA);
		linkNodes(addCommentNodeView, addCommentTemplate, RelType.USE_TEMPLATE);
		linkNodes(addCommentChildView, commentTemplate, RelType.USE_TEMPLATE);

		linkNodes(addCommentCommentField, textFieldTemplate, RelType.USE_TEMPLATE);
		linkNodes(addCommentSubmitButton, submitButtonTemplate, RelType.USE_TEMPLATE);

		linkNodes(addCommentFormLoader, addCommentCommentField, RelType.DATA);
		linkNodes(addCommentCommentField, addCommentCreator, RelType.DATA);

		linkNodes(addCommentFormLoader, addCommentLinker, RelType.DATA).setProperty("targetSlotName", "startNode");
		linkNodes(addCommentCreator, addCommentLinker, RelType.DATA).setProperty("targetSlotName", "endNode");

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
