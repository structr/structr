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
package org.structr.web.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EncryptedStringProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.relationship.StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry;

/**
 * Storage object for mount configuration data.
 */
public class StorageConfigurationEntry extends AbstractNode {

	public static final Property<StorageConfiguration> configurationProperty = new StartNode<>("configuration", StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry.class).partOfBuiltInSchema();

	public static final Property<String> nameProperty  = new StringProperty("name").partOfBuiltInSchema();
	public static final Property<String> valueProperty = new EncryptedStringProperty("value").partOfBuiltInSchema();

	public static final View uiView = new View(StorageConfigurationEntry.class, PropertyView.Ui,
		nameProperty, valueProperty, configurationProperty
	);

	public StorageConfiguration getConfiguration() {
		return getProperty(configurationProperty);
	}

	public void setName(final String name) throws FrameworkException {
		setProperty(nameProperty, name);
	}

	public String getValue() {
		return getProperty(valueProperty);
	}

	public void setValue(final String value) throws FrameworkException {
		setProperty(valueProperty, value);
	}

}