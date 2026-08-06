/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.datasources;

import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.StructrTransactionListener;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.example.ExampleDataProvider;
import org.structr.core.datasources.example.TypeAttributesProvider;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionPostProcess;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExampleData {

	private static final Map<String, Map<String, ExampleDataProvider>> DataProviders = new LinkedHashMap<>();

	static {

		DataProviders.put("attribute-names", Map.of("name-type-list", new TypeAttributesProvider()));
	}

	public static StructrTransactionListener postProcess(final String type, final List<String> attributes, final int number) {

		return new StructrTransactionListener() {

			@Override
			public void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) throws FrameworkException {
			}

			@Override
			public void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) {

				final ExampleDataProvider provider = ExampleData.of("attribute-names", "name-type-list", type);
				if (provider != null) {

					final List<Map<String, Object>> attributesList = provider.get(type);
					if (attributesList != null) {

						final App app = StructrApp.getInstance();

						try (final Tx tx = app.tx()) {

							// only create data if the type exists and has no instances yet
							if (Traits.exists(type) && app.nodeQuery(type).getFirst() == null) {

								final Traits traits = Traits.of(type);

								for (int i = 0; i < number; i++) {

									final NodeInterface instance = app.create(type, type + " #" + (i + 1));

									for (final String attribute : attributes) {

										if (traits.hasKey(attribute) && !"name".equals(attribute)) {

											final PropertyKey key = traits.key(attribute);
											final Object value    = key.getExampleValue(i);

											instance.setProperty(key, value);

										}
									}
								}
							}

							tx.success();

						} catch (Throwable t) {

							t.printStackTrace();
						}
					}
				}
			}

			@Override
			public void simpleBroadcast(final String messageName, final Map<String, Object> data, final Predicate<String> sessionIdPredicate) {
			}
		};
	}

	public static ExampleDataProvider of(final String typeOfExampleData, final String resultFormat, final String inputValue) {

		final Map<String, ExampleDataProvider> providers = DataProviders.get(typeOfExampleData);
		if (providers != null) {

			final ExampleDataProvider provider = providers.get(resultFormat);
			if (provider != null) {

				return provider;
			}
		}

		return null;
	}

	public static List<Map<String, Object>> get(final String typeOfExampleData, final String resultFormat, final String inputValue) {

		final ExampleDataProvider provider = ExampleData.of(typeOfExampleData, resultFormat, inputValue);
		if (provider != null) {

			return provider.get(inputValue);
		}

		return null;
	}
}
