package org.structr.function;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class RenderFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_RENDER    = "Usage: ${render(node)} or ${render(nodes)}. Example: ${render(get(this, \"children\"))}";
	public static final String ERROR_MESSAGE_RENDER_JS = "Usage: ${{Structr.render(node)}} or ${{Structr.render(nodes)}}. Example: ${{Structr.render(Structr.get('this').children)}}";

	@Override
	public String getName() {
		return "render()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length == 1) {

			RenderContext innerCtx = new RenderContext((RenderContext)ctx);

			if (sources[0] instanceof DOMNode) {

				((DOMNode)sources[0]).render(innerCtx, 0);

			} else if (sources[0] instanceof Collection) {

				for (final Object obj : (Collection)sources[0]) {

					if (obj instanceof DOMNode) {
						((DOMNode)obj).render(innerCtx, 0);
					}

				}

			}

			return StringUtils.join(innerCtx.getBuffer().getQueue(), "");
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_RENDER_JS : ERROR_MESSAGE_RENDER);
	}

	@Override
	public String shortDescription() {
		return "Renders the children of the current node";
	}

}
