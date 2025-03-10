/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.flow.engine;

import org.structr.common.error.FrameworkException;
import org.structr.flow.api.FlowHandler;
import org.structr.flow.api.FlowResult;
import org.structr.flow.impl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 */
public class ForEachHandler implements FlowHandler {

	@Override
	public FlowNode handle(final Context context, final FlowNode flowNode) throws FlowException {

		final FlowForEach flowElement   = flowNode.as(FlowForEach.class);
		final FlowDataSource dataSource = flowElement.getDataSource();

		if (dataSource != null) {

			final FlowEngine engine = new FlowEngine(context);
			final FlowNode loopBody = flowElement.getLoopBody();

			if (loopBody != null) {

				final Object data = dataSource.get(context);

				// Special handling for FlowAggregate to ensure it's properly reset for nested loops.
				FlowNode element = loopBody;

				final Context cleanedLoopContext = new Context(context);
				traverseAndEvaluate(element, (el) -> {
					if (el instanceof FlowAggregate) {
						cleanedLoopContext.setData(((FlowAggregate) el).getUuid(), null);
					}
				});

				Context loopContext = new Context(cleanedLoopContext);

				if (data instanceof Iterable) {

					for (final Object o : ((Iterable) data)) {

						// Provide current element data for loop context and write evaluation result into main context data for this loop element
						loopContext.setData(flowElement.getUuid(), o);
						try {

							final FlowResult result = engine.execute(loopContext, loopBody);
							final FlowError error = result.getError();
							if (error != null) {

								if (error.getCause() != null) {

									loopContext.clearError();
									if (error.getCause() instanceof FlowException) {

										throw (FlowException)error.getCause();
									} else {

										throw new FrameworkException(422, "Unexpected exception in FlowForEach loop body.", error.getCause());
									}
								} else {

									loopContext.clearError();
									throw new FrameworkException(422, "Unexpected exception in FlowForEach loop body. " + error.getMessage());
								}
							}

						} catch (FrameworkException ex) {

							final FlowException flowException = new FlowException(ex, flowElement);
							FlowExceptionHandler exceptionHandler = flowElement.getExceptionHandler(loopContext);

							if (exceptionHandler != null) {

								try {

									engine.handleException(loopContext, flowException, flowElement);

									// Handle returns issued by FlowReturn within a loop-nested exception handler
									if (loopContext.hasResult()) {

										context.setResult(loopContext.getResult());
									}

									continue;
								} catch (FrameworkException handlingException) {

									throw new FlowException(handlingException, flowElement);
								}
							}

							throw flowException;

						}
						loopContext = openNewContext(context, loopContext, flowElement);

						// Break when an intermediate result or error occurs
						if (context.hasResult() || context.hasError()) {
							break;
						}
					}

				} else {

					// Provide current element data for loop context and write evaluation result into main context data for this loop element
					loopContext.setData(flowElement.getUuid(), data);

					try {

						engine.execute(loopContext, loopBody);
					} catch (FrameworkException ex) {

						throw new FlowException(ex, flowElement);
					}
				}

				for (Map.Entry<String,Object> entry : getAggregationData(loopContext, flowElement).entrySet()) {
					context.setData(entry.getKey(), entry.getValue());
				}
				context.setData(flowElement.getUuid(), data);

			}

		}

		return flowElement.next();
	}

	private Map<String,Object> getAggregationData(final Context context, final FlowNode flowElement) {
		Map<String,Object> aggregateData = new HashMap<>();

		FlowNode currentElement = ((FlowForEach)flowElement).getLoopBody();

		traverseAndEvaluate(currentElement, (el) -> {
			if (el instanceof FlowAggregate) {

				aggregateData.put(((FlowAggregate) el).getUuid(), context.getData(((FlowAggregate) el).getUuid()));
			}
		});

		return aggregateData;
	}

	private Context openNewContext(final Context context, Context loopContext, final FlowNode flowElement) {
		final Context newContext = new Context(context);

		for (Map.Entry<String,Object> entry : getAggregationData(loopContext, flowElement).entrySet()) {

			newContext.setData(entry.getKey(), entry.getValue());
		}

		return newContext;
	}

	private void traverseAndEvaluate(final FlowNode element, final Consumer<FlowNode> consumer) {

		if (element != null) {

			consumer.accept(element);

			if (element instanceof FlowDecision) {

				final FlowDecision decision = (FlowDecision)element;

				FlowNode decisionElement = decision.getTrueElement();
				if (decisionElement != null) {

					traverseAndEvaluate(decisionElement, consumer);
				}

				decisionElement = decision.getFalseElement();
				if (decisionElement != null) {

					traverseAndEvaluate(decisionElement, consumer);
				}

			} else {

				if (element.next() != null) {

					traverseAndEvaluate(element.next(), consumer);
				}
			}
		}
	}

}
