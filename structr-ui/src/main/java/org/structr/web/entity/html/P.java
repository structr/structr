/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class P extends DOMElement {

//	public static final EndNodes<Content>  contents  = new EndNodes<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<A>        as        = new EndNodes<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Abbr>     abbrs     = new EndNodes<Abbr>("abbrs", Abbr.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Area>     areas     = new EndNodes<Area>("areas", Area.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Audio>    audios    = new EndNodes<Audio>("audios", Audio.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<B>        bs        = new EndNodes<B>("bs", B.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Bdi>      bdis      = new EndNodes<Bdi>("bdis", Bdi.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Bdo>      bdos      = new EndNodes<Bdo>("bdos", Bdo.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Br>       brs       = new EndNodes<Br>("brs", Br.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Button>   buttons   = new EndNodes<Button>("buttons", Button.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Canvas>   canvases  = new EndNodes<Canvas>("canvases", Canvas.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Cite>     cites     = new EndNodes<Cite>("cites", Cite.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Code>     codes     = new EndNodes<Code>("codes", Code.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Command>  commands  = new EndNodes<Command>("commands", Command.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Datalist> datalists = new EndNodes<Datalist>("datalists", Datalist.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Del>      dels      = new EndNodes<Del>("dels", Del.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Dfn>      dfns      = new EndNodes<Dfn>("dfns", Dfn.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Em>       ems       = new EndNodes<Em>("ems", Em.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Embed>    embeds    = new EndNodes<Embed>("embeds", Embed.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<I>        is        = new EndNodes<I>("is", I.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Iframe>   iframes   = new EndNodes<Iframe>("iframes", Iframe.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Img>      imgs      = new EndNodes<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Input>    inputs    = new EndNodes<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Ins>      inss      = new EndNodes<Ins>("inss", Ins.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Kbd>      kbds      = new EndNodes<Kbd>("kbds", Kbd.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Keygen>   keygens   = new EndNodes<Keygen>("keygens", Keygen.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Label>    labels    = new EndNodes<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Map>      maps      = new EndNodes<Map>("maps", Map.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Mark>     marks     = new EndNodes<Mark>("marks", Mark.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Meter>    meters    = new EndNodes<Meter>("meters", Meter.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Noscript> noscripts = new EndNodes<Noscript>("noscripts", Noscript.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Object>   objects   = new EndNodes<Object>("objects", Object.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Output>   outputs   = new EndNodes<Output>("outputs", Output.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Progress> progresss = new EndNodes<Progress>("progresss", Progress.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Q>        qs        = new EndNodes<Q>("qs", Q.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Ruby>     rubys     = new EndNodes<Ruby>("rubys", Ruby.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<S>        ss        = new EndNodes<S>("ss", S.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Samp>     samps     = new EndNodes<Samp>("samps", Samp.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Script>   scripts   = new EndNodes<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Select>   selects   = new EndNodes<Select>("selects", Select.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Small>    smalls    = new EndNodes<Small>("smalls", Small.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Span>     spans     = new EndNodes<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Strong>   strongs   = new EndNodes<Strong>("strongs", Strong.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Sub>      subs      = new EndNodes<Sub>("subs", Sub.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Sup>      sups      = new EndNodes<Sup>("sups", Sup.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Textarea> textareas = new EndNodes<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Time>     times     = new EndNodes<Time>("times", Time.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<U>        us        = new EndNodes<U>("us", U.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Var>      vars      = new EndNodes<Var>("vars", Var.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Video>    videos    = new EndNodes<Video>("videos", Video.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Wbr>      wbrs      = new EndNodes<Wbr>("wbrs", Wbr.class, RelType.CONTAINS, Direction.OUTGOING, false);
}
