/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import org.structr.api.service.LicenseManager;
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.files.url.StructrURLStreamHandlerFactory;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;
import org.structr.web.datasource.CypherGraphDataSource;
import org.structr.web.datasource.FunctionDataSource;
import org.structr.web.datasource.IdRequestParameterGraphDataSource;
import org.structr.web.datasource.RestDataSource;
import org.structr.web.function.*;
import org.structr.web.traits.definitions.*;
import org.structr.web.traits.definitions.dom.*;
import org.structr.web.traits.definitions.html.*;
import org.structr.web.traits.relationships.*;

import java.net.URL;
import java.util.Set;

/**
 */
public class UiModule implements StructrModule {

	static {

		URL.setURLStreamHandlerFactory(new StructrURLStreamHandlerFactory());
	}

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		DataSources.put(true, "ui", "idRequestParameterDataSource", new IdRequestParameterGraphDataSource("nodeId"));
		DataSources.put(true, "ui", "restDataSource",               new RestDataSource());
		DataSources.put(true, "ui", "cypherDataSource",             new CypherGraphDataSource());
		DataSources.put(true, "ui", "functionDataSource",           new FunctionDataSource(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY));

		StructrTraits.registerRelationshipType(StructrTraits.ABSTRACT_FILE_CONFIGURED_BY_STORAGE_CONFIGURATION,              new AbstractFileCONFIGURED_BYStorageConfiguration());
		StructrTraits.registerRelationshipType(StructrTraits.ACTION_MAPPING_PARAMETER_PARAMETER_MAPPING,                     new ActionMappingPARAMETERParameterMapping());
		StructrTraits.registerRelationshipType(StructrTraits.CSS_RULE_CONTAINS_CSS_RULE,                                     new CssRuleCONTAINSCssRule());
		StructrTraits.registerRelationshipType(StructrTraits.CSS_RULE_HAS_DECLARATION_CSS_DECLARATION,                       new CssRuleHAS_DECLARATIONCssDeclaration());
		StructrTraits.registerRelationshipType(StructrTraits.CSS_RULE_HAS_SELECTOR_CSS_SELECTOR,                             new CssRuleHAS_SELECTORCssSelector());
		StructrTraits.registerRelationshipType(StructrTraits.CSS_SEMANTIC_CLASS_MAPS_TO_CSS_SELECTOR,                        new CssSemanticClassMAPS_TOCssSelector());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_ELEMENT_INPUT_ELEMENT_PARAMETER_MAPPING,                    new DOMElementINPUT_ELEMENTParameterMapping());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_ELEMENT_RELOADS_DOM_ELEMENT,                                new DOMElementRELOADSDOMElement());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_ELEMENT_TRIGGERED_BY_ACTION_MAPPING,                        new DOMElementTRIGGERED_BYActionMapping());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_CONTAINS_DOM_NODE,                                     new DOMNodeCONTAINSDOMNode());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE,                        new DOMNodeCONTAINS_NEXT_SIBLINGDOMNode());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_FAILURE_TARGET_ACTION_MAPPING,                         new DOMNodeFAILURE_TARGETActionMapping());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_PAGE_PAGE,                                             new DOMNodePAGEPage());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING,           new DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING,           new DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING,                         new DOMNodeSUCCESS_TARGETActionMapping());
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_SYNC_DOM_NODE,                                         new DOMNodeSYNCDOMNode());
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_ABSTRACT_FILE,                                  new FolderCONTAINSAbstractFile());
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_FILE,                                           new FolderCONTAINSFile());
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_FOLDER,                                         new FolderCONTAINSFolder());
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_IMAGE,                                          new FolderCONTAINSImage());
		StructrTraits.registerRelationshipType(StructrTraits.IMAGE_PICTURE_OF_USER,                                          new ImagePICTURE_OFUser());
		StructrTraits.registerRelationshipType(StructrTraits.IMAGE_THUMBNAIL_IMAGE,                                          new ImageTHUMBNAILImage());
		StructrTraits.registerRelationshipType(StructrTraits.PAGE_HAS_PATH_PAGE_PATH,                                        new PageHAS_PATHPagePath());
		StructrTraits.registerRelationshipType(StructrTraits.LINK_SOURCE_LINK_LINKABLE,                                      new LinkSourceLINKLinkableTraitDefinition());
		StructrTraits.registerRelationshipType(StructrTraits.PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER,                    new PagePathHAS_PARAMETERPagePathParameter());
		StructrTraits.registerRelationshipType(StructrTraits.SITE_CONTAINS_PAGE,                                             new SiteCONTAINSPage());
		StructrTraits.registerRelationshipType(StructrTraits.STORAGE_CONFIGURATION_CONFIG_ENTRY_STORAGE_CONFIGURATION_ENTRY, new StorageConfigurationCONFIG_ENTRYStorageConfigurationEntryTraitDefinition());
		StructrTraits.registerRelationshipType(StructrTraits.USER_HOME_DIR_FOLDER,                                           new UserHOME_DIRFolder());
		StructrTraits.registerRelationshipType(StructrTraits.USER_WORKING_DIR_FOLDER,                                        new UserWORKING_DIRFolder());

		StructrTraits.registerNodeType(StructrTraits.ABSTRACT_FILE,                       new AbstractFileTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.ACTION_MAPPING,                      new ActionMappingTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.APPLICATION_CONFIGURATION_DATA_NODE, new ApplicationConfigurationDataNodeTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.COMMENT,                             new DOMNodeTraitDefinition(), new ContentTraitDefinition(), new CommentTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.COMPONENT,                           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new ComponentTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.CONTENT,                             new DOMNodeTraitDefinition(), new ContentTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.CSS_DECLARATION,                     new CssDeclarationTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.CSS_RULE,                            new CssRuleTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.CSS_SELECTOR,                        new CssSelectorTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.CSS_SEMANTIC_CLASS,                  new CssSemanticClassTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.DOM_NODE,                            new DOMNodeTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.DOM_ELEMENT,                         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FILE,                                new AbstractFileTraitDefinition(), new LinkableTraitDefinition(), new FileTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.IMAGE,                               new AbstractFileTraitDefinition(), new LinkableTraitDefinition(), new FileTraitDefinition(), new ImageTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.FOLDER,                              new AbstractFileTraitDefinition(), new FolderTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.LINKABLE,                            new LinkableTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.LINK_SOURCE,                         new LinkSourceTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.PAGE,                                new DOMNodeTraitDefinition(), new LinkableTraitDefinition(), new PageTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.PAGE_PATH,                           new PagePathTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.PAGE_PATH_PARAMETER,                 new PagePathParameterTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.PARAMETER_MAPPING,                   new ParameterMappingTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.SHADOW_DOCUMENT,                     new DOMNodeTraitDefinition(), new LinkableTraitDefinition(), new PageTraitDefinition(), new ShadowDocumentTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.SITE,                                new SiteTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.STORAGE_CONFIGURATION,               new StorageConfigurationTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.STORAGE_CONFIGURATION_ENTRY,         new StorageConfigurationEntryTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.TEMPLATE,                            new DOMNodeTraitDefinition(), new ContentTraitDefinition(), new TemplateTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.USER,                                new PrincipalTraitDefinition(), new UserTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.WIDGET,                              new WidgetTraitDefinition());

		// HTML elements
		StructrTraits.registerNodeType(StructrTraits.A,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new A());
		StructrTraits.registerNodeType(StructrTraits.ABBR,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Abbr());
		StructrTraits.registerNodeType(StructrTraits.ADDRESS,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.ADDRESS));
		StructrTraits.registerNodeType(StructrTraits.AREA,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Area());
		StructrTraits.registerNodeType(StructrTraits.ARTICLE,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.ARTICLE));
		StructrTraits.registerNodeType(StructrTraits.ASIDE,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.ASIDE));
		StructrTraits.registerNodeType(StructrTraits.AUDIO,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Audio());
		StructrTraits.registerNodeType(StructrTraits.BASE,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Base());
		StructrTraits.registerNodeType(StructrTraits.BDI,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Bdi());
		StructrTraits.registerNodeType(StructrTraits.BDO,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Bdo());
		StructrTraits.registerNodeType(StructrTraits.B,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new B());
		StructrTraits.registerNodeType(StructrTraits.BLOCKQUOTE,      new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.BLOCKQUOTE));
		StructrTraits.registerNodeType(StructrTraits.BODY,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.BODY));
		StructrTraits.registerNodeType(StructrTraits.BR,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Br());
		StructrTraits.registerNodeType(StructrTraits.BUTTON,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Button());
		StructrTraits.registerNodeType(StructrTraits.CANVAS,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Canvas());
		StructrTraits.registerNodeType(StructrTraits.CAPTION,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.CAPTION));
		StructrTraits.registerNodeType(StructrTraits.CITE,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Cite());
		StructrTraits.registerNodeType(StructrTraits.CODE,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Code());
		StructrTraits.registerNodeType(StructrTraits.COLGROUP,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.COLGROUP));
		StructrTraits.registerNodeType(StructrTraits.COL,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Col());
		StructrTraits.registerNodeType(StructrTraits.COMMAND,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Command());
		StructrTraits.registerNodeType(StructrTraits.DATA,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Data());
		StructrTraits.registerNodeType(StructrTraits.DATALIST,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.DATALIST));
		StructrTraits.registerNodeType(StructrTraits.DD,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.DD));
		StructrTraits.registerNodeType(StructrTraits.DEL,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.DEL));
		StructrTraits.registerNodeType(StructrTraits.DETAILS,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Details());
		StructrTraits.registerNodeType(StructrTraits.DFN,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Dfn());
		StructrTraits.registerNodeType(StructrTraits.DIALOG,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Dialog());
		StructrTraits.registerNodeType(StructrTraits.DIV,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.DIV));
		StructrTraits.registerNodeType(StructrTraits.DL,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.DL));
		StructrTraits.registerNodeType(StructrTraits.DT,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.DT));
		StructrTraits.registerNodeType(StructrTraits.EMBED,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Embed());
		StructrTraits.registerNodeType(StructrTraits.EM,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Em());
		StructrTraits.registerNodeType(StructrTraits.FIELDSET,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.FIELDSET));
		StructrTraits.registerNodeType(StructrTraits.FIGCAPTION,      new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.FIGCAPTION));
		StructrTraits.registerNodeType(StructrTraits.FIGURE,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.FIGURE));
		StructrTraits.registerNodeType(StructrTraits.FOOTER,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.FOOTER));
		StructrTraits.registerNodeType(StructrTraits.FORM,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Form());
		StructrTraits.registerNodeType(StructrTraits.G,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new G());
		StructrTraits.registerNodeType(StructrTraits.H1,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.H1));
		StructrTraits.registerNodeType(StructrTraits.H2,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.H2));
		StructrTraits.registerNodeType(StructrTraits.H3,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.H3));
		StructrTraits.registerNodeType(StructrTraits.H4,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.H4));
		StructrTraits.registerNodeType(StructrTraits.H5,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.H5));
		StructrTraits.registerNodeType(StructrTraits.H6,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.H6));
		StructrTraits.registerNodeType(StructrTraits.HEADER,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.HEADER));
		StructrTraits.registerNodeType(StructrTraits.HEAD,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.HEAD));
		StructrTraits.registerNodeType(StructrTraits.HGROUP,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.HGROUP));
		StructrTraits.registerNodeType(StructrTraits.HR,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Hr());
		StructrTraits.registerNodeType(StructrTraits.HTML,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Html());
		StructrTraits.registerNodeType(StructrTraits.IFRAME,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Iframe());
		StructrTraits.registerNodeType(StructrTraits.I,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new I());
		StructrTraits.registerNodeType(StructrTraits.IMG,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new Img());
		StructrTraits.registerNodeType(StructrTraits.INPUT,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Input());
		StructrTraits.registerNodeType(StructrTraits.INS,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.INS));
		StructrTraits.registerNodeType(StructrTraits.KBD,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Kbd());
		StructrTraits.registerNodeType(StructrTraits.KEYGEN,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Keygen());
		StructrTraits.registerNodeType(StructrTraits.LABEL,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Label());
		StructrTraits.registerNodeType(StructrTraits.LEGEND,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.LEGEND));
		StructrTraits.registerNodeType(StructrTraits.LI,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Li());
		StructrTraits.registerNodeType(StructrTraits.LINK,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new Link());
		StructrTraits.registerNodeType(StructrTraits.MAIN,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.MAIN));
		StructrTraits.registerNodeType(StructrTraits.MAP,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.MAP));
		StructrTraits.registerNodeType(StructrTraits.MARK,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Mark());
		StructrTraits.registerNodeType(StructrTraits.MENU,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.MENU));
		StructrTraits.registerNodeType(StructrTraits.META,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Meta());
		StructrTraits.registerNodeType(StructrTraits.METER,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.METER));
		StructrTraits.registerNodeType(StructrTraits.NAV,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.NAV));
		StructrTraits.registerNodeType(StructrTraits.NOSCRIPT,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.NOSCRIPT));
		StructrTraits.registerNodeType(StructrTraits.OBJECT,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new org.structr.web.traits.definitions.html.Object());
		StructrTraits.registerNodeType(StructrTraits.OL,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Ol());
		StructrTraits.registerNodeType(StructrTraits.OPTGROUP,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Optgroup());
		StructrTraits.registerNodeType(StructrTraits.OPTION,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Option());
		StructrTraits.registerNodeType(StructrTraits.OUTPUT,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.OUTPUT));
		StructrTraits.registerNodeType(StructrTraits.PARAM,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Param());
		StructrTraits.registerNodeType(StructrTraits.PICTURE,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.PICTURE));
		StructrTraits.registerNodeType(StructrTraits.P,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.P));
		StructrTraits.registerNodeType(StructrTraits.PRE,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Pre());
		StructrTraits.registerNodeType(StructrTraits.PROGRESS,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.PROGRESS));
		StructrTraits.registerNodeType(StructrTraits.Q,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Q());
		StructrTraits.registerNodeType(StructrTraits.RP,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Rp());
		StructrTraits.registerNodeType(StructrTraits.RT,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Rt());
		StructrTraits.registerNodeType(StructrTraits.RUBY,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Ruby());
		StructrTraits.registerNodeType(StructrTraits.SAMP,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Samp());
		StructrTraits.registerNodeType(StructrTraits.SCRIPT,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new Script());
		StructrTraits.registerNodeType(StructrTraits.SECTION,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.SECTION));
		StructrTraits.registerNodeType(StructrTraits.SELECT,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Select());
		StructrTraits.registerNodeType(StructrTraits.S,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new S());
		StructrTraits.registerNodeType(StructrTraits.SLOT,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Slot());
		StructrTraits.registerNodeType(StructrTraits.SMALL,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Small());
		StructrTraits.registerNodeType(StructrTraits.SOURCE,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Source());
		StructrTraits.registerNodeType(StructrTraits.SPAN,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Span());
		StructrTraits.registerNodeType(StructrTraits.STRONG,          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Strong());
		StructrTraits.registerNodeType(StructrTraits.STYLE,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Style());
		StructrTraits.registerNodeType(StructrTraits.SUB,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Sub());
		StructrTraits.registerNodeType(StructrTraits.SUMMARY,         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.SUMMARY));
		StructrTraits.registerNodeType(StructrTraits.SUP,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Sup());
		StructrTraits.registerNodeType(StructrTraits.TABLE,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.TABLE));
		StructrTraits.registerNodeType(StructrTraits.TBODY,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.TBODY));
		StructrTraits.registerNodeType(StructrTraits.TD,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Td());
		StructrTraits.registerNodeType(StructrTraits.TEMPLATEELEMENT, new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new TemplateElement());
		StructrTraits.registerNodeType(StructrTraits.TEXTAREA,        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Textarea());
		StructrTraits.registerNodeType(StructrTraits.TFOOT,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.TFOOT));
		StructrTraits.registerNodeType(StructrTraits.THEAD,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.THEAD));
		StructrTraits.registerNodeType(StructrTraits.TH,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Th());
		StructrTraits.registerNodeType(StructrTraits.TIME,            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Time());
		StructrTraits.registerNodeType(StructrTraits.TITLE,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.TITLE));
		StructrTraits.registerNodeType(StructrTraits.TRACK,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Track());
		StructrTraits.registerNodeType(StructrTraits.TR,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.TR));
		StructrTraits.registerNodeType(StructrTraits.U,               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new U());
		StructrTraits.registerNodeType(StructrTraits.UL,              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition(StructrTraits.UL));
		StructrTraits.registerNodeType(StructrTraits.VAR,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Var());
		StructrTraits.registerNodeType(StructrTraits.VIDEO,           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Video());
		StructrTraits.registerNodeType(StructrTraits.WBR,             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Wbr());
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new EscapeHtmlFunction());
		Functions.put(licenseManager, new EscapeXmlFunction());
		Functions.put(licenseManager, new UnescapeHtmlFunction());
		Functions.put(licenseManager, new StripHtmlFunction());
		Functions.put(licenseManager, new FromJsonFunction());
		Functions.put(licenseManager, new ToJsonFunction());
		Functions.put(licenseManager, new ToGraphObjectFunction());
		Functions.put(licenseManager, new IncludeFunction());
		Functions.put(licenseManager, new IncludeChildFunction());
		Functions.put(licenseManager, new RenderFunction());
		Functions.put(licenseManager, new SetDetailsObjectFunction());
		Functions.put(licenseManager, new ConfirmationKeyFunction());
		Functions.put(licenseManager, new ImportHtmlFunction());
		Functions.put(licenseManager, new ImportCssFunction());
		Functions.put(licenseManager, new RemoveDOMChildFunction());
		Functions.put(licenseManager, new ReplaceDOMChildFunction());
		Functions.put(licenseManager, new InsertHtmlFunction());
		Functions.put(licenseManager, new GetSourceFunction());
		Functions.put(licenseManager, new HasCssClassFunction());

		Functions.put(licenseManager, new SendHtmlMailFunction());
		Functions.put(licenseManager, new SendPlaintextMailFunction());
		Functions.put(licenseManager, new GetContentFunction());
		Functions.put(licenseManager, new SetContentFunction());
		Functions.put(licenseManager, new AppendContentFunction());
		Functions.put(licenseManager, new CopyFileContentsFunction());
		Functions.put(licenseManager, new SetSessionAttributeFunction());
		Functions.put(licenseManager, new GetSessionAttributeFunction());
		Functions.put(licenseManager, new RemoveSessionAttributeFunction());
		Functions.put(licenseManager, new IsLocaleFunction());

		Functions.put(licenseManager, new LogEventFunction());

		Functions.put(licenseManager, new HttpGetFunction());
		Functions.put(licenseManager, new HttpHeadFunction());
		Functions.put(licenseManager, new HttpPatchFunction());
		Functions.put(licenseManager, new HttpPostFunction());
		Functions.put(licenseManager, new HTTPPostMultiPartFunction());
		Functions.put(licenseManager, new HttpPutFunction());
		Functions.put(licenseManager, new HttpDeleteFunction());
		Functions.put(licenseManager, new AddHeaderFunction());
		Functions.put(licenseManager, new ClearHeadersFunction());
		Functions.put(licenseManager, new SetResponseHeaderFunction());
		Functions.put(licenseManager, new RemoveResponseHeaderFunction());
		Functions.put(licenseManager, new SetResponseCodeFunction());
		Functions.put(licenseManager, new GetRequestHeaderFunction());
		Functions.put(licenseManager, new ValidateCertificatesFunction());
		Functions.put(licenseManager, new GetCookieFunction());
		Functions.put(licenseManager, new SetCookieFunction());
		Functions.put(licenseManager, new FromXmlFunction());
		Functions.put(licenseManager, new CreateArchiveFunction());
		Functions.put(licenseManager, new CreateFolderPathFunction());
		Functions.put(licenseManager, new CreateZipFunction());
		Functions.put(licenseManager, new UnarchiveFunction());
		Functions.put(licenseManager, new ScheduleFunction());
		Functions.put(licenseManager, new MaintenanceFunction());
		Functions.put(licenseManager, new BarcodeFunction());
		Functions.put(licenseManager, new JobInfoFunction());
		Functions.put(licenseManager, new JobListFunction());
		Functions.put(licenseManager, new CreateAccessTokenFunction());
		Functions.put(licenseManager, new CreateAccessAndRefreshTokenFunction());

		Functions.put(licenseManager, new ApplicationStorePutFunction());
		Functions.put(licenseManager, new ApplicationStoreDeleteFunction());
		Functions.put(licenseManager, new ApplicationStoreGetFunction());
		Functions.put(licenseManager, new ApplicationStoreGetKeysFunction());
		Functions.put(licenseManager, new ApplicationStoreHasFunction());

		Functions.put(licenseManager, new RequestStorePutFunction());
		Functions.put(licenseManager, new RequestStoreDeleteFunction());
		Functions.put(licenseManager, new RequestStoreGetFunction());
		Functions.put(licenseManager, new RequestStoreGetKeysFunction());
		Functions.put(licenseManager, new RequestStoreHasFunction());

		Functions.put(licenseManager, new SendEventFunction());
		Functions.put(licenseManager, new BroadcastEventFunction());

		Functions.put(licenseManager, new GraphQLFunction());

		Functions.put(licenseManager, new SystemInfoFunction());
	}

	@Override
	public String getName() {
		return "ui";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("rest");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
