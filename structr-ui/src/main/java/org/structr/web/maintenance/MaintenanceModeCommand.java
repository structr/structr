/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.maintenance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.VersionHelper;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.Services;
import org.structr.core.StaticValue;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Localization;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.Security;
import org.structr.core.graph.*;
import org.structr.core.property.CypherProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.module.StructrModule;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.JavaScriptSource;
import org.structr.schema.export.StructrFunctionProperty;
import org.structr.schema.export.StructrMethodDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.export.StructrSchemaDefinition;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.AbstractMapComparator;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.AbstractMinifiedFile;
import org.structr.web.entity.ApplicationConfigurationDataNode;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.MinifiedCssFile;
import org.structr.web.entity.MinifiedJavaScriptFile;
import org.structr.web.entity.Site;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.maintenance.deploy.ComponentImportVisitor;
import org.structr.web.maintenance.deploy.FileImportVisitor;
import org.structr.web.maintenance.deploy.ImportFailureException;
import org.structr.web.maintenance.deploy.ImportPreconditionFailedException;
import org.structr.web.maintenance.deploy.PageImportVisitor;
import org.structr.web.maintenance.deploy.TemplateImportVisitor;
import org.structr.websocket.command.CreateComponentCommand;

public class MaintenanceModeCommand extends NodeServiceCommand implements MaintenanceCommand {

    private static final Logger logger                     = LoggerFactory.getLogger(MaintenanceModeCommand.class.getName());

    static {

        MaintenanceParameterResource.registerMaintenanceCommand("maintenanceMode", MaintenanceModeCommand.class);
    }

    @Override
    public void execute(Map<String, Object> parameters) throws FrameworkException {

        final String action = (String) parameters.get("action");
        boolean success     = false;

        if ("enable".equals(action)) {

            success = Services.getInstance().setMaintenanceMode(true);

        } else if ("disable".equals(action)) {

            success = Services.getInstance().setMaintenanceMode(false);

        } else {

            logger.warn("Unsupported action '{}'", action);
        }

        if (success) {

            final Map<String, Object> msgData = new HashMap();
            msgData.put(MaintenanceCommand.COMMAND_TYPE_KEY, "MAINTENANCE");
            msgData.put("enabled",                           Settings.MaintenanceModeEnabled.getValue());
            msgData.put("baseUrl",                           ActionContext.getBaseUrl(securityContext.getRequest(), true));
            TransactionCommand.simpleBroadcastGenericMessage(msgData, Predicate.all());
        }
    }

    @Override
    public boolean requiresEnclosingTransaction() {
        return false;
    }

    @Override
    public boolean requiresFlushingOfCaches() {
        return false;
    }
}
