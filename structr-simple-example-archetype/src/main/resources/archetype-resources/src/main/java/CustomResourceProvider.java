/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.structr.rest.DefaultResourceProvider;

/**
 * Project-specific resource provider implementation.
 *
 * @author Christian Morgner
 */
public class CustomResourceProvider extends DefaultResourceProvider {

	/**
	 * This class exists so that Structr is able to identify
	 * the JAR file from which it runs. It is referenced in
	 * the structr.conf configuration file.
	 *
	 * Please do NOT remove this class, or Structr will not
	 * be able to scan and identify your entity classes etc.
	 */
}
