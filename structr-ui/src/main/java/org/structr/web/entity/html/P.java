/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.neo4j.graphdb.Direction;

import org.structr.web.common.RelType;
import org.structr.core.property.CollectionProperty;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class P extends DOMElement {

	public static final CollectionProperty<Content>  contents  = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<A>        as        = new CollectionProperty<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Abbr>     abbrs     = new CollectionProperty<Abbr>("abbrs", Abbr.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Area>     areas     = new CollectionProperty<Area>("areas", Area.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Audio>    audios    = new CollectionProperty<Audio>("audios", Audio.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<B>        bs        = new CollectionProperty<B>("bs", B.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Bdi>      bdis      = new CollectionProperty<Bdi>("bdis", Bdi.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Bdo>      bdos      = new CollectionProperty<Bdo>("bdos", Bdo.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Br>       brs       = new CollectionProperty<Br>("brs", Br.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Button>   buttons   = new CollectionProperty<Button>("buttons", Button.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Canvas>   canvases  = new CollectionProperty<Canvas>("canvases", Canvas.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Cite>     cites     = new CollectionProperty<Cite>("cites", Cite.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Code>     codes     = new CollectionProperty<Code>("codes", Code.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Command>  commands  = new CollectionProperty<Command>("commands", Command.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Datalist> datalists = new CollectionProperty<Datalist>("datalists", Datalist.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Del>      dels      = new CollectionProperty<Del>("dels", Del.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Dfn>      dfns      = new CollectionProperty<Dfn>("dfns", Dfn.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Em>       ems       = new CollectionProperty<Em>("ems", Em.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Embed>    embeds    = new CollectionProperty<Embed>("embeds", Embed.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<I>        is        = new CollectionProperty<I>("is", I.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Iframe>   iframes   = new CollectionProperty<Iframe>("iframes", Iframe.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Img>      imgs      = new CollectionProperty<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Input>    inputs    = new CollectionProperty<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Ins>      inss      = new CollectionProperty<Ins>("inss", Ins.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Kbd>      kbds      = new CollectionProperty<Kbd>("kbds", Kbd.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Keygen>   keygens   = new CollectionProperty<Keygen>("keygens", Keygen.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Label>    labels    = new CollectionProperty<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Map>      maps      = new CollectionProperty<Map>("maps", Map.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Mark>     marks     = new CollectionProperty<Mark>("marks", Mark.class, RelType.CONTAINS, Direction.OUTGOING, false);
// ???	public static final CollectionProperty<Math>     maths     = new CollectionProperty<Math>("maths", Math.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Meter>    meters    = new CollectionProperty<Meter>("meters", Meter.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Noscript> noscripts = new CollectionProperty<Noscript>("noscripts", Noscript.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Object>   objects   = new CollectionProperty<Object>("objects", Object.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Output>   outputs   = new CollectionProperty<Output>("outputs", Output.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Progress> progresss = new CollectionProperty<Progress>("progresss", Progress.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Q>        qs        = new CollectionProperty<Q>("qs", Q.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Ruby>     rubys     = new CollectionProperty<Ruby>("rubys", Ruby.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<S>        ss        = new CollectionProperty<S>("ss", S.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Samp>     samps     = new CollectionProperty<Samp>("samps", Samp.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Script>   scripts   = new CollectionProperty<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Select>   selects   = new CollectionProperty<Select>("selects", Select.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Small>    smalls    = new CollectionProperty<Small>("smalls", Small.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Span>     spans     = new CollectionProperty<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Strong>   strongs   = new CollectionProperty<Strong>("strongs", Strong.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Sub>      subs      = new CollectionProperty<Sub>("subs", Sub.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Sup>      sups      = new CollectionProperty<Sup>("sups", Sup.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Textarea> textareas = new CollectionProperty<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Time>     times     = new CollectionProperty<Time>("times", Time.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<U>        us        = new CollectionProperty<U>("us", U.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Var>      vars      = new CollectionProperty<Var>("vars", Var.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Video>    videos    = new CollectionProperty<Video>("videos", Video.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Wbr>      wbrs      = new CollectionProperty<Wbr>("wbrs", Wbr.class, RelType.CONTAINS, Direction.OUTGOING, false);
}
