/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity.html;

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.core.property.CollectionProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class P extends HtmlElement {

	public static final CollectionProperty<Content>  contents  = new CollectionProperty<Content>(Content.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<A>        as        = new CollectionProperty<A>(A.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Abbr>     abbrs     = new CollectionProperty<Abbr>(Abbr.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Area>     areas     = new CollectionProperty<Area>(Area.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Audio>    audios    = new CollectionProperty<Audio>(Audio.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<B>        bs        = new CollectionProperty<B>(B.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Bdi>      bdis      = new CollectionProperty<Bdi>(Bdi.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Bdo>      bdos      = new CollectionProperty<Bdo>(Bdo.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Br>       brs       = new CollectionProperty<Br>(Br.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Button>   buttons   = new CollectionProperty<Button>(Button.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Canvas>   canvases  = new CollectionProperty<Canvas>(Canvas.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Cite>     cites     = new CollectionProperty<Cite>(Cite.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Code>     codes     = new CollectionProperty<Code>(Code.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Command>  commands  = new CollectionProperty<Command>(Command.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Datalist> datalists = new CollectionProperty<Datalist>(Datalist.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Del>      dels      = new CollectionProperty<Del>(Del.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Dfn>      dfns      = new CollectionProperty<Dfn>(Dfn.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Em>       ems       = new CollectionProperty<Em>(Em.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Embed>    embeds    = new CollectionProperty<Embed>(Embed.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<I>        is        = new CollectionProperty<I>(I.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Iframe>   iframes   = new CollectionProperty<Iframe>(Iframe.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Img>      imgs      = new CollectionProperty<Img>(Img.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Input>    inputs    = new CollectionProperty<Input>(Input.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Ins>      inss      = new CollectionProperty<Ins>(Ins.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Kbd>      kbds      = new CollectionProperty<Kbd>(Kbd.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Keygen>   keygens   = new CollectionProperty<Keygen>(Keygen.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Label>    labels    = new CollectionProperty<Label>(Label.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Map>      maps      = new CollectionProperty<Map>(Map.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Mark>     marks     = new CollectionProperty<Mark>(Mark.class, RelType.CONTAINS, Direction.OUTGOING);
// ???	public static final CollectionProperty<Math>     maths     = new CollectionProperty<Math>(Math.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Meter>    meters    = new CollectionProperty<Meter>(Meter.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Noscript> noscripts = new CollectionProperty<Noscript>(Noscript.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Object>   objects   = new CollectionProperty<Object>(Object.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Output>   outputs   = new CollectionProperty<Output>(Output.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Progress> progresss = new CollectionProperty<Progress>(Progress.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Q>        qs        = new CollectionProperty<Q>(Q.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Ruby>     rubys     = new CollectionProperty<Ruby>(Ruby.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<S>        ss        = new CollectionProperty<S>(S.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Samp>     samps     = new CollectionProperty<Samp>(Samp.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Script>   scripts   = new CollectionProperty<Script>(Script.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Select>   selects   = new CollectionProperty<Select>(Select.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Small>    smalls    = new CollectionProperty<Small>(Small.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Span>     spans     = new CollectionProperty<Span>(Span.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Strong>   strongs   = new CollectionProperty<Strong>(Strong.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Sub>      subs      = new CollectionProperty<Sub>(Sub.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Sup>      sups      = new CollectionProperty<Sup>(Sup.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Textarea> textareas = new CollectionProperty<Textarea>(Textarea.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Time>     times     = new CollectionProperty<Time>(Time.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<U>        us        = new CollectionProperty<U>(U.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Var>      vars      = new CollectionProperty<Var>(Var.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Video>    videos    = new CollectionProperty<Video>(Video.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Wbr>      wbrs      = new CollectionProperty<Wbr>(Wbr.class, RelType.CONTAINS, Direction.OUTGOING);
}
