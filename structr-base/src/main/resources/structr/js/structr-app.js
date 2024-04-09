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
/* Chosen v1.1.0 | (c) 2011-2013 by Harvest | MIT License, https://github.com/harvesthq/chosen/blob/master/LICENSE.md */
!function(){var a,AbstractChosen,Chosen,SelectParser,b,c={}.hasOwnProperty,d=function(a,b){function d(){this.constructor=a}for(var e in b)c.call(b,e)&&(a[e]=b[e]);return d.prototype=b.prototype,a.prototype=new d,a.__super__=b.prototype,a};SelectParser=function(){function SelectParser(){this.options_index=0,this.parsed=[]}return SelectParser.prototype.add_node=function(a){return"OPTGROUP"===a.nodeName.toUpperCase()?this.add_group(a):this.add_option(a)},SelectParser.prototype.add_group=function(a){var b,c,d,e,f,g;for(b=this.parsed.length,this.parsed.push({array_index:b,group:!0,label:this.escapeExpression(a.label),children:0,disabled:a.disabled}),f=a.childNodes,g=[],d=0,e=f.length;e>d;d++)c=f[d],g.push(this.add_option(c,b,a.disabled));return g},SelectParser.prototype.add_option=function(a,b,c){return"OPTION"===a.nodeName.toUpperCase()?(""!==a.text?(null!=b&&(this.parsed[b].children+=1),this.parsed.push({array_index:this.parsed.length,options_index:this.options_index,value:a.value,text:a.text,html:a.innerHTML,selected:a.selected,disabled:c===!0?c:a.disabled,group_array_index:b,classes:a.className,style:a.style.cssText})):this.parsed.push({array_index:this.parsed.length,options_index:this.options_index,empty:!0}),this.options_index+=1):void 0},SelectParser.prototype.escapeExpression=function(a){var b,c;return null==a||a===!1?"":/[\&\<\>\"\'\`]/.test(a)?(b={"<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#x27;","`":"&#x60;"},c=/&(?!\w+;)|[\<\>\"\'\`]/g,a.replace(c,function(a){return b[a]||"&amp;"})):a},SelectParser}(),SelectParser.select_to_array=function(a){var b,c,d,e,f;for(c=new SelectParser,f=a.childNodes,d=0,e=f.length;e>d;d++)b=f[d],c.add_node(b);return c.parsed},AbstractChosen=function(){function AbstractChosen(a,b){this.form_field=a,this.options=null!=b?b:{},AbstractChosen.browser_is_supported()&&(this.is_multiple=this.form_field.multiple,this.set_default_text(),this.set_default_values(),this.setup(),this.set_up_html(),this.register_observers())}return AbstractChosen.prototype.set_default_values=function(){var a=this;return this.click_test_action=function(b){return a.test_active_click(b)},this.activate_action=function(b){return a.activate_field(b)},this.active_field=!1,this.mouse_on_container=!1,this.results_showing=!1,this.result_highlighted=null,this.allow_single_deselect=null!=this.options.allow_single_deselect&&null!=this.form_field.options[0]&&""===this.form_field.options[0].text?this.options.allow_single_deselect:!1,this.disable_search_threshold=this.options.disable_search_threshold||0,this.disable_search=this.options.disable_search||!1,this.enable_split_word_search=null!=this.options.enable_split_word_search?this.options.enable_split_word_search:!0,this.group_search=null!=this.options.group_search?this.options.group_search:!0,this.search_contains=this.options.search_contains||!1,this.single_backstroke_delete=null!=this.options.single_backstroke_delete?this.options.single_backstroke_delete:!0,this.max_selected_options=this.options.max_selected_options||1/0,this.inherit_select_classes=this.options.inherit_select_classes||!1,this.display_selected_options=null!=this.options.display_selected_options?this.options.display_selected_options:!0,this.display_disabled_options=null!=this.options.display_disabled_options?this.options.display_disabled_options:!0},AbstractChosen.prototype.set_default_text=function(){return this.default_text=this.form_field.getAttribute("data-placeholder")?this.form_field.getAttribute("data-placeholder"):this.is_multiple?this.options.placeholder_text_multiple||this.options.placeholder_text||AbstractChosen.default_multiple_text:this.options.placeholder_text_single||this.options.placeholder_text||AbstractChosen.default_single_text,this.results_none_found=this.form_field.getAttribute("data-no_results_text")||this.options.no_results_text||AbstractChosen.default_no_result_text},AbstractChosen.prototype.mouse_enter=function(){return this.mouse_on_container=!0},AbstractChosen.prototype.mouse_leave=function(){return this.mouse_on_container=!1},AbstractChosen.prototype.input_focus=function(){var a=this;if(this.is_multiple){if(!this.active_field)return setTimeout(function(){return a.container_mousedown()},50)}else if(!this.active_field)return this.activate_field()},AbstractChosen.prototype.input_blur=function(){var a=this;return this.mouse_on_container?void 0:(this.active_field=!1,setTimeout(function(){return a.blur_test()},100))},AbstractChosen.prototype.results_option_build=function(a){var b,c,d,e,f;for(b="",f=this.results_data,d=0,e=f.length;e>d;d++)c=f[d],b+=c.group?this.result_add_group(c):this.result_add_option(c),(null!=a?a.first:void 0)&&(c.selected&&this.is_multiple?this.choice_build(c):c.selected&&!this.is_multiple&&this.single_set_selected_text(c.text));return b},AbstractChosen.prototype.result_add_option=function(a){var b,c;return a.search_match?this.include_option_in_results(a)?(b=[],a.disabled||a.selected&&this.is_multiple||b.push("active-result"),!a.disabled||a.selected&&this.is_multiple||b.push("disabled-result"),a.selected&&b.push("result-selected"),null!=a.group_array_index&&b.push("group-option"),""!==a.classes&&b.push(a.classes),c=document.createElement("li"),c.className=b.join(" "),c.style.cssText=a.style,c.setAttribute("data-option-array-index",a.array_index),c.innerHTML=a.search_text,this.outerHTML(c)):"":""},AbstractChosen.prototype.result_add_group=function(a){var b;return a.search_match||a.group_match?a.active_options>0?(b=document.createElement("li"),b.className="group-result",b.innerHTML=a.search_text,this.outerHTML(b)):"":""},AbstractChosen.prototype.results_update_field=function(){return this.set_default_text(),this.is_multiple||this.results_reset_cleanup(),this.result_clear_highlight(),this.results_build(),this.results_showing?this.winnow_results():void 0},AbstractChosen.prototype.reset_single_select_options=function(){var a,b,c,d,e;for(d=this.results_data,e=[],b=0,c=d.length;c>b;b++)a=d[b],a.selected?e.push(a.selected=!1):e.push(void 0);return e},AbstractChosen.prototype.results_toggle=function(){return this.results_showing?this.results_hide():this.results_show()},AbstractChosen.prototype.results_search=function(){return this.results_showing?this.winnow_results():this.results_show()},AbstractChosen.prototype.winnow_results=function(){var a,b,c,d,e,f,g,h,i,j,k,l,m;for(this.no_results_clear(),e=0,g=this.get_search_text(),a=g.replace(/[-[\]{}()*+?.,\\^$|#\s]/g,"\\$&"),d=this.search_contains?"":"^",c=new RegExp(d+a,"i"),j=new RegExp(a,"i"),m=this.results_data,k=0,l=m.length;l>k;k++)b=m[k],b.search_match=!1,f=null,this.include_option_in_results(b)&&(b.group&&(b.group_match=!1,b.active_options=0),null!=b.group_array_index&&this.results_data[b.group_array_index]&&(f=this.results_data[b.group_array_index],0===f.active_options&&f.search_match&&(e+=1),f.active_options+=1),(!b.group||this.group_search)&&(b.search_text=b.group?b.label:b.html,b.search_match=this.search_string_match(b.search_text,c),b.search_match&&!b.group&&(e+=1),b.search_match?(g.length&&(h=b.search_text.search(j),i=b.search_text.substr(0,h+g.length)+"</em>"+b.search_text.substr(h+g.length),b.search_text=i.substr(0,h)+"<em>"+i.substr(h)),null!=f&&(f.group_match=!0)):null!=b.group_array_index&&this.results_data[b.group_array_index].search_match&&(b.search_match=!0)));return this.result_clear_highlight(),1>e&&g.length?(this.update_results_content(""),this.no_results(g)):(this.update_results_content(this.results_option_build()),this.winnow_results_set_highlight())},AbstractChosen.prototype.search_string_match=function(a,b){var c,d,e,f;if(b.test(a))return!0;if(this.enable_split_word_search&&(a.indexOf(" ")>=0||0===a.indexOf("["))&&(d=a.replace(/\[|\]/g,"").split(" "),d.length))for(e=0,f=d.length;f>e;e++)if(c=d[e],b.test(c))return!0},AbstractChosen.prototype.choices_count=function(){var a,b,c,d;if(null!=this.selected_option_count)return this.selected_option_count;for(this.selected_option_count=0,d=this.form_field.options,b=0,c=d.length;c>b;b++)a=d[b],a.selected&&(this.selected_option_count+=1);return this.selected_option_count},AbstractChosen.prototype.choices_click=function(a){return a.preventDefault(),this.results_showing||this.is_disabled?void 0:this.results_show()},AbstractChosen.prototype.keyup_checker=function(a){var b,c;switch(b=null!=(c=a.which)?c:a.keyCode,this.search_field_scale(),b){case 8:if(this.is_multiple&&this.backstroke_length<1&&this.choices_count()>0)return this.keydown_backstroke();if(!this.pending_backstroke)return this.result_clear_highlight(),this.results_search();break;case 13:if(a.preventDefault(),this.results_showing)return this.result_select(a);break;case 27:return this.results_showing&&this.results_hide(),!0;case 9:case 38:case 40:case 16:case 91:case 17:break;default:return this.results_search()}},AbstractChosen.prototype.clipboard_event_checker=function(){var a=this;return setTimeout(function(){return a.results_search()},50)},AbstractChosen.prototype.container_width=function(){return null!=this.options.width?this.options.width:""+this.form_field.offsetWidth+"px"},AbstractChosen.prototype.include_option_in_results=function(a){return this.is_multiple&&!this.display_selected_options&&a.selected?!1:!this.display_disabled_options&&a.disabled?!1:a.empty?!1:!0},AbstractChosen.prototype.search_results_touchstart=function(a){return this.touch_started=!0,this.search_results_mouseover(a)},AbstractChosen.prototype.search_results_touchmove=function(a){return this.touch_started=!1,this.search_results_mouseout(a)},AbstractChosen.prototype.search_results_touchend=function(a){return this.touch_started?this.search_results_mouseup(a):void 0},AbstractChosen.prototype.outerHTML=function(a){var b;return a.outerHTML?a.outerHTML:(b=document.createElement("div"),b.appendChild(a),b.innerHTML)},AbstractChosen.browser_is_supported=function(){return"Microsoft Internet Explorer"===window.navigator.appName?document.documentMode>=8:/iP(od|hone)/i.test(window.navigator.userAgent)?!1:/Android/i.test(window.navigator.userAgent)&&/Mobile/i.test(window.navigator.userAgent)?!1:!0},AbstractChosen.default_multiple_text="Select Some Options",AbstractChosen.default_single_text="Select an Option",AbstractChosen.default_no_result_text="No results match",AbstractChosen}(),a=jQuery,a.fn.extend({chosen:function(b){return AbstractChosen.browser_is_supported()?this.each(function(){var c,d;c=a(this),d=c.data("chosen"),"destroy"===b&&d?d.destroy():d||c.data("chosen",new Chosen(this,b))}):this}}),Chosen=function(c){function Chosen(){return b=Chosen.__super__.constructor.apply(this,arguments)}return d(Chosen,c),Chosen.prototype.setup=function(){return this.form_field_jq=a(this.form_field),this.current_selectedIndex=this.form_field.selectedIndex,this.is_rtl=this.form_field_jq.hasClass("chosen-rtl")},Chosen.prototype.set_up_html=function(){var b,c;return b=["chosen-container"],b.push("chosen-container-"+(this.is_multiple?"multi":"single")),this.inherit_select_classes&&this.form_field.className&&b.push(this.form_field.className),this.is_rtl&&b.push("chosen-rtl"),c={"class":b.join(" "),style:"width: "+this.container_width()+";",title:this.form_field.title},this.form_field.id.length&&(c.id=this.form_field.id.replace(/[^\w]/g,"_")+"_chosen"),this.container=a("<div />",c),this.is_multiple?this.container.html('<ul class="chosen-choices"><li class="search-field"><input type="text" value="'+this.default_text+'" class="default" autocomplete="off" style="width:25px;" /></li></ul><div class="chosen-drop"><ul class="chosen-results"></ul></div>'):this.container.html('<a class="chosen-single chosen-default" tabindex="-1"><span>'+this.default_text+'</span><div><b></b></div></a><div class="chosen-drop"><div class="chosen-search"><input type="text" autocomplete="off" /></div><ul class="chosen-results"></ul></div>'),this.form_field_jq.hide().after(this.container),this.dropdown=this.container.find("div.chosen-drop").first(),this.search_field=this.container.find("input").first(),this.search_results=this.container.find("ul.chosen-results").first(),this.search_field_scale(),this.search_no_results=this.container.find("li.no-results").first(),this.is_multiple?(this.search_choices=this.container.find("ul.chosen-choices").first(),this.search_container=this.container.find("li.search-field").first()):(this.search_container=this.container.find("div.chosen-search").first(),this.selected_item=this.container.find(".chosen-single").first()),this.results_build(),this.set_tab_index(),this.set_label_behavior(),this.form_field_jq.trigger("chosen:ready",{chosen:this})},Chosen.prototype.register_observers=function(){var a=this;return this.container.bind("mousedown.chosen",function(b){a.container_mousedown(b)}),this.container.bind("mouseup.chosen",function(b){a.container_mouseup(b)}),this.container.bind("mouseenter.chosen",function(b){a.mouse_enter(b)}),this.container.bind("mouseleave.chosen",function(b){a.mouse_leave(b)}),this.search_results.bind("mouseup.chosen",function(b){a.search_results_mouseup(b)}),this.search_results.bind("mouseover.chosen",function(b){a.search_results_mouseover(b)}),this.search_results.bind("mouseout.chosen",function(b){a.search_results_mouseout(b)}),this.search_results.bind("mousewheel.chosen DOMMouseScroll.chosen",function(b){a.search_results_mousewheel(b)}),this.search_results.bind("touchstart.chosen",function(b){a.search_results_touchstart(b)}),this.search_results.bind("touchmove.chosen",function(b){a.search_results_touchmove(b)}),this.search_results.bind("touchend.chosen",function(b){a.search_results_touchend(b)}),this.form_field_jq.bind("chosen:updated.chosen",function(b){a.results_update_field(b)}),this.form_field_jq.bind("chosen:activate.chosen",function(b){a.activate_field(b)}),this.form_field_jq.bind("chosen:open.chosen",function(b){a.container_mousedown(b)}),this.form_field_jq.bind("chosen:close.chosen",function(b){a.input_blur(b)}),this.search_field.bind("blur.chosen",function(b){a.input_blur(b)}),this.search_field.bind("keyup.chosen",function(b){a.keyup_checker(b)}),this.search_field.bind("keydown.chosen",function(b){a.keydown_checker(b)}),this.search_field.bind("focus.chosen",function(b){a.input_focus(b)}),this.search_field.bind("cut.chosen",function(b){a.clipboard_event_checker(b)}),this.search_field.bind("paste.chosen",function(b){a.clipboard_event_checker(b)}),this.is_multiple?this.search_choices.bind("click.chosen",function(b){a.choices_click(b)}):this.container.bind("click.chosen",function(a){a.preventDefault()})},Chosen.prototype.destroy=function(){return a(this.container[0].ownerDocument).unbind("click.chosen",this.click_test_action),this.search_field[0].tabIndex&&(this.form_field_jq[0].tabIndex=this.search_field[0].tabIndex),this.container.remove(),this.form_field_jq.removeData("chosen"),this.form_field_jq.show()},Chosen.prototype.search_field_disabled=function(){return this.is_disabled=this.form_field_jq[0].disabled,this.is_disabled?(this.container.addClass("chosen-disabled"),this.search_field[0].disabled=!0,this.is_multiple||this.selected_item.unbind("focus.chosen",this.activate_action),this.close_field()):(this.container.removeClass("chosen-disabled"),this.search_field[0].disabled=!1,this.is_multiple?void 0:this.selected_item.bind("focus.chosen",this.activate_action))},Chosen.prototype.container_mousedown=function(b){return this.is_disabled||(b&&"mousedown"===b.type&&!this.results_showing&&b.preventDefault(),null!=b&&a(b.target).hasClass("search-choice-close"))?void 0:(this.active_field?this.is_multiple||!b||a(b.target)[0]!==this.selected_item[0]&&!a(b.target).parents("a.chosen-single").length||(b.preventDefault(),this.results_toggle()):(this.is_multiple&&this.search_field.val(""),a(this.container[0].ownerDocument).bind("click.chosen",this.click_test_action),this.results_show()),this.activate_field())},Chosen.prototype.container_mouseup=function(a){return"ABBR"!==a.target.nodeName||this.is_disabled?void 0:this.results_reset(a)},Chosen.prototype.search_results_mousewheel=function(a){var b;return a.originalEvent&&(b=-a.originalEvent.wheelDelta||a.originalEvent.detail),null!=b?(a.preventDefault(),"DOMMouseScroll"===a.type&&(b=40*b),this.search_results.scrollTop(b+this.search_results.scrollTop())):void 0},Chosen.prototype.blur_test=function(){return!this.active_field&&this.container.hasClass("chosen-container-active")?this.close_field():void 0},Chosen.prototype.close_field=function(){return a(this.container[0].ownerDocument).unbind("click.chosen",this.click_test_action),this.active_field=!1,this.results_hide(),this.container.removeClass("chosen-container-active"),this.clear_backstroke(),this.show_search_field_default(),this.search_field_scale()},Chosen.prototype.activate_field=function(){return this.container.addClass("chosen-container-active"),this.active_field=!0,this.search_field.val(this.search_field.val()),this.search_field.focus()},Chosen.prototype.test_active_click=function(b){var c;return c=a(b.target).closest(".chosen-container"),c.length&&this.container[0]===c[0]?this.active_field=!0:this.close_field()},Chosen.prototype.results_build=function(){return this.parsing=!0,this.selected_option_count=null,this.results_data=SelectParser.select_to_array(this.form_field),this.is_multiple?this.search_choices.find("li.search-choice").remove():this.is_multiple||(this.single_set_selected_text(),this.disable_search||this.form_field.options.length<=this.disable_search_threshold?(this.search_field[0].readOnly=!0,this.container.addClass("chosen-container-single-nosearch")):(this.search_field[0].readOnly=!1,this.container.removeClass("chosen-container-single-nosearch"))),this.update_results_content(this.results_option_build({first:!0})),this.search_field_disabled(),this.show_search_field_default(),this.search_field_scale(),this.parsing=!1},Chosen.prototype.result_do_highlight=function(a){var b,c,d,e,f;if(a.length){if(this.result_clear_highlight(),this.result_highlight=a,this.result_highlight.addClass("highlighted"),d=parseInt(this.search_results.css("maxHeight"),10),f=this.search_results.scrollTop(),e=d+f,c=this.result_highlight.position().top+this.search_results.scrollTop(),b=c+this.result_highlight.outerHeight(),b>=e)return this.search_results.scrollTop(b-d>0?b-d:0);if(f>c)return this.search_results.scrollTop(c)}},Chosen.prototype.result_clear_highlight=function(){return this.result_highlight&&this.result_highlight.removeClass("highlighted"),this.result_highlight=null},Chosen.prototype.results_show=function(){return this.is_multiple&&this.max_selected_options<=this.choices_count()?(this.form_field_jq.trigger("chosen:maxselected",{chosen:this}),!1):(this.container.addClass("chosen-with-drop"),this.results_showing=!0,this.search_field.focus(),this.search_field.val(this.search_field.val()),this.winnow_results(),this.form_field_jq.trigger("chosen:showing_dropdown",{chosen:this}))},Chosen.prototype.update_results_content=function(a){return this.search_results.html(a)},Chosen.prototype.results_hide=function(){return this.results_showing&&(this.result_clear_highlight(),this.container.removeClass("chosen-with-drop"),this.form_field_jq.trigger("chosen:hiding_dropdown",{chosen:this})),this.results_showing=!1},Chosen.prototype.set_tab_index=function(){var a;return this.form_field.tabIndex?(a=this.form_field.tabIndex,this.form_field.tabIndex=-1,this.search_field[0].tabIndex=a):void 0},Chosen.prototype.set_label_behavior=function(){var b=this;return this.form_field_label=this.form_field_jq.parents("label"),!this.form_field_label.length&&this.form_field.id.length&&(this.form_field_label=a("label[for='"+this.form_field.id+"']")),this.form_field_label.length>0?this.form_field_label.bind("click.chosen",function(a){return b.is_multiple?b.container_mousedown(a):b.activate_field()}):void 0},Chosen.prototype.show_search_field_default=function(){return this.is_multiple&&this.choices_count()<1&&!this.active_field?(this.search_field.val(this.default_text),this.search_field.addClass("default")):(this.search_field.val(""),this.search_field.removeClass("default"))},Chosen.prototype.search_results_mouseup=function(b){var c;return c=a(b.target).hasClass("active-result")?a(b.target):a(b.target).parents(".active-result").first(),c.length?(this.result_highlight=c,this.result_select(b),this.search_field.focus()):void 0},Chosen.prototype.search_results_mouseover=function(b){var c;return c=a(b.target).hasClass("active-result")?a(b.target):a(b.target).parents(".active-result").first(),c?this.result_do_highlight(c):void 0},Chosen.prototype.search_results_mouseout=function(b){return a(b.target).hasClass("active-result")?this.result_clear_highlight():void 0},Chosen.prototype.choice_build=function(b){var c,d,e=this;return c=a("<li />",{"class":"search-choice"}).html("<span>"+b.html+"</span>"),b.disabled?c.addClass("search-choice-disabled"):(d=a("<a />",{"class":"search-choice-close","data-option-array-index":b.array_index}),d.bind("click.chosen",function(a){return e.choice_destroy_link_click(a)}),c.append(d)),this.search_container.before(c)},Chosen.prototype.choice_destroy_link_click=function(b){return b.preventDefault(),b.stopPropagation(),this.is_disabled?void 0:this.choice_destroy(a(b.target))},Chosen.prototype.choice_destroy=function(a){return this.result_deselect(a[0].getAttribute("data-option-array-index"))?(this.show_search_field_default(),this.is_multiple&&this.choices_count()>0&&this.search_field.val().length<1&&this.results_hide(),a.parents("li").first().remove(),this.search_field_scale()):void 0},Chosen.prototype.results_reset=function(){return this.reset_single_select_options(),this.form_field.options[0].selected=!0,this.single_set_selected_text(),this.show_search_field_default(),this.results_reset_cleanup(),this.form_field_jq.trigger("change"),this.active_field?this.results_hide():void 0},Chosen.prototype.results_reset_cleanup=function(){return this.current_selectedIndex=this.form_field.selectedIndex,this.selected_item.find("abbr").remove()},Chosen.prototype.result_select=function(a){var b,c;return this.result_highlight?(b=this.result_highlight,this.result_clear_highlight(),this.is_multiple&&this.max_selected_options<=this.choices_count()?(this.form_field_jq.trigger("chosen:maxselected",{chosen:this}),!1):(this.is_multiple?b.removeClass("active-result"):this.reset_single_select_options(),c=this.results_data[b[0].getAttribute("data-option-array-index")],c.selected=!0,this.form_field.options[c.options_index].selected=!0,this.selected_option_count=null,this.is_multiple?this.choice_build(c):this.single_set_selected_text(c.text),(a.metaKey||a.ctrlKey)&&this.is_multiple||this.results_hide(),this.search_field.val(""),(this.is_multiple||this.form_field.selectedIndex!==this.current_selectedIndex)&&this.form_field_jq.trigger("change",{selected:this.form_field.options[c.options_index].value}),this.current_selectedIndex=this.form_field.selectedIndex,this.search_field_scale())):void 0},Chosen.prototype.single_set_selected_text=function(a){return null==a&&(a=this.default_text),a===this.default_text?this.selected_item.addClass("chosen-default"):(this.single_deselect_control_build(),this.selected_item.removeClass("chosen-default")),this.selected_item.find("span").text(a)},Chosen.prototype.result_deselect=function(a){var b;return b=this.results_data[a],this.form_field.options[b.options_index].disabled?!1:(b.selected=!1,this.form_field.options[b.options_index].selected=!1,this.selected_option_count=null,this.result_clear_highlight(),this.results_showing&&this.winnow_results(),this.form_field_jq.trigger("change",{deselected:this.form_field.options[b.options_index].value}),this.search_field_scale(),!0)},Chosen.prototype.single_deselect_control_build=function(){return this.allow_single_deselect?(this.selected_item.find("abbr").length||this.selected_item.find("span").first().after('<abbr class="search-choice-close"></abbr>'),this.selected_item.addClass("chosen-single-with-deselect")):void 0},Chosen.prototype.get_search_text=function(){return this.search_field.val()===this.default_text?"":a("<div/>").text(a.trim(this.search_field.val())).html()},Chosen.prototype.winnow_results_set_highlight=function(){var a,b;return b=this.is_multiple?[]:this.search_results.find(".result-selected.active-result"),a=b.length?b.first():this.search_results.find(".active-result").first(),null!=a?this.result_do_highlight(a):void 0},Chosen.prototype.no_results=function(b){var c;return c=a('<li class="no-results">'+this.results_none_found+' "<span></span>"</li>'),c.find("span").first().html(b),this.search_results.append(c),this.form_field_jq.trigger("chosen:no_results",{chosen:this})},Chosen.prototype.no_results_clear=function(){return this.search_results.find(".no-results").remove()},Chosen.prototype.keydown_arrow=function(){var a;return this.results_showing&&this.result_highlight?(a=this.result_highlight.nextAll("li.active-result").first())?this.result_do_highlight(a):void 0:this.results_show()},Chosen.prototype.keyup_arrow=function(){var a;return this.results_showing||this.is_multiple?this.result_highlight?(a=this.result_highlight.prevAll("li.active-result"),a.length?this.result_do_highlight(a.first()):(this.choices_count()>0&&this.results_hide(),this.result_clear_highlight())):void 0:this.results_show()},Chosen.prototype.keydown_backstroke=function(){var a;return this.pending_backstroke?(this.choice_destroy(this.pending_backstroke.find("a").first()),this.clear_backstroke()):(a=this.search_container.siblings("li.search-choice").last(),a.length&&!a.hasClass("search-choice-disabled")?(this.pending_backstroke=a,this.single_backstroke_delete?this.keydown_backstroke():this.pending_backstroke.addClass("search-choice-focus")):void 0)},Chosen.prototype.clear_backstroke=function(){return this.pending_backstroke&&this.pending_backstroke.removeClass("search-choice-focus"),this.pending_backstroke=null},Chosen.prototype.keydown_checker=function(a){var b,c;switch(b=null!=(c=a.which)?c:a.keyCode,this.search_field_scale(),8!==b&&this.pending_backstroke&&this.clear_backstroke(),b){case 8:this.backstroke_length=this.search_field.val().length;break;case 9:this.results_showing&&!this.is_multiple&&this.result_select(a),this.mouse_on_container=!1;break;case 13:a.preventDefault();break;case 38:a.preventDefault(),this.keyup_arrow();break;case 40:a.preventDefault(),this.keydown_arrow()}},Chosen.prototype.search_field_scale=function(){var b,c,d,e,f,g,h,i,j;if(this.is_multiple){for(d=0,h=0,f="position:absolute; left: -1000px; top: -1000px; display:none;",g=["font-size","font-style","font-weight","font-family","line-height","text-transform","letter-spacing"],i=0,j=g.length;j>i;i++)e=g[i],f+=e+":"+this.search_field.css(e)+";";return b=a("<div />",{style:f}),b.text(this.search_field.val()),a("body").append(b),h=b.width()+25,b.remove(),c=this.container.outerWidth(),h>c-10&&(h=c-10),this.search_field.css({width:h+"px"})}},Chosen}(AbstractChosen)}.call(this);
$(function() { $('head').append('<link rel="stylesheet" type="text/css" href="/structr/css/lib/chosen.min.css">')});
/*
 *  Copyright (C) 2010-2018 Structr GmbH
 *
 *  This file is part of Structr <http://structr.org>.
 *
 *  Structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  Structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * JS library for interactive web applications built with Structr.
 * This file has to be present in a web page to make Structr widgets work.
 */

var structrRestUrl = '/structr/rest/';
var buttonSelector = '[data-structr-action]';
var altKey = false, ctrlKey = false, shiftKey = false, eKey = false;
// to set a non-default locale, set the global variable structrAppLocale
var structrAppLocale = structrAppLocale || 'en_EN';


$(function() {

	var s = new StructrApp(structrRestUrl, structrAppLocale);
	window.setDebugMode = function(enable) {
		s.setDebugMode(enable);
	};
	s.activateButtons(buttonSelector);
	$(document).trigger('structr-ready');
	s.hideNonEdit();

	$(window).on('keydown', function(e) {
		var k = e.which;
		if (k === 16)
			shiftKey = true;
		if (k === 17)
			altKey = true;
		if (k === 18)
			ctrlKey = true;
		if (k === 69)
			eKey = true;
	});

	$(window).on('keyup', function(e) {
		var k = e.which;
		if (k === 16)
			shiftKey = false;
		if (k === 17)
			altKey = false;
		if (k === 18)
			ctrlKey = false;
		if (k === 69)
			eKey = false;
	});

});

/**
 * Base class for Structr apps
 *
 * @param baseUrl
 * @param locale
 * @returns {StructrApp}
 */
function StructrApp(baseUrl, locale) {
	this.locale = locale || 'en_EN';
	this.lang = this.locale.split('_')[0];
	if (baseUrl) {
		structrRestUrl = baseUrl;
	}
	var s = this;
	var hideEditElements = {}; // store elements in edit mode
	var hideNonEditElements = {}; // store elements in not edit mode
	this.edit = false;
	this.data = {};
	this.btnLabel = undefined;

	this.labels = {
		en : {
			save                             : 'Save',
			saving                           : 'Saving',
			cancel                           : 'Cancel',
			edit                             : 'Edit',
			sucessfullyUpdated               : 'Successfully updated',
			successfullyCreatedNew           : 'Successfully created new',
			couldNotUpdate                   : 'Could not update',
			couldNotCreate                   : 'Could not create',
			checking                         : 'Checking...',
			wrongUsernameOrPassword          : 'Wrong username or password!',
			wrongTwoFactorCode               : 'Wrong two factor code!',
			invalidTwoFactorToken            : 'Two Factor login took too long',
			sessionLimitExceeded             : 'Max. number of sessions exceeded.',
			loginAttempts                    : 'Too many failed password attempts!',
			passwordChangeRequired           : 'Your password has not been changed for too long!',
			pleaseEnterEMail                 : 'Please enter your e-mail address!',
			processing                       : 'Processing...',
			checkYourInbox                   : 'Thanks! Please check your inbox.',
			success                          : 'Success!',
			passwordLinkSent                 : 'Link to reset password sent. Please check your inbox or spam folder.',
			areYouSure                       : 'Are you sure?',
			areYourSureToDelete              : 'Are you sure you want to delete',
			successfullyExecutedCustomAction : 'Successfully executed custom action',
			couldNotExecuteCustomAction      : 'Could not execute custom action',
			couldNotReadRelatedProperty      : 'Could not read related property',
			makeSureContainedInEntity        : 'Make sure it is contained in the\nentity\'s ui view and readable via REST.',
		},
		de : {
			save                             : 'Speichern',
			saving                           : 'Speichere ...',
			cancel                           : 'Abbrechen',
			edit                             : 'Bearbeiten',
			sucessfullyUpdated               : 'Erfolgreich gespeichert:',
			successfullyCreatedNew           : 'Neues Object erfolgreich erstellt vom Typ',
			couldNotUpdate                   : 'Konnte nicht speichern: ',
			couldNotCreate                   : 'Konnte nicht erstellen:',
			checking                         : 'Prüfe ...',
			wrongUsernameOrPassword          : 'Falscher Benutzername oder Passwort!',
			wrongTwoFactorCode               : 'Falscher Two Factor Code!',
			invalidTwoFactorToken            : 'Two Factor login hat zu lange gedauert',
			numberOfSessionsExceeded         : 'Maximale Anzahl erlaubter gleichzeitiger Sessions überschritten.',
			loginAttempts                    : 'Zu viele fehlgeschlagene Passwort-Eingaben!',
			passwordChangeRequired           : 'Sie haben Ihr Passwort zu lange nicht geändert!',
			pleaseEnterEMail                 : 'Bitte E-Mail-Adresse eingeben!',
			processing                       : 'In Bearbeitung ...',
			checkYourInbox                   : 'Danke! Bitte E-Mail-Eingang prüfen.',
			success                          : 'Erfolgreich!',
			passwordLinkSent                 : 'Link zum Passwort-Reset wurde verschickt. Bitte E-Mail-Eingang prüfen.',
			areYouSure                       : 'Sicher?',
			areYourSureToDelete              : 'Wirklich löschen?',
			successfullyExecutedCustomAction : 'Benutzerdefinierte Aktion erfolgreich ausgeführt:',
			couldNotExecuteCustomAction      : 'Konnte benutzerdefinierte Aktion nicht ausführen:',
			couldNotReadRelatedProperty      : 'Konnte entferntes Attribut nicht lesen:',
			makeSureContainedInEntity        : 'Stellen Sie sicher, dass es in der\nEntität und in deren ui-View enthalten ist\nsowie per REST lesbar ist.',
		}
	};

	/**
	 * Bind 'click' event to all Structr buttons
	 */
	this.activateButtons = function(sel) {
		$(document).on('click', sel, function(e) {
			e.preventDefault();
			e.stopPropagation();
			var btn = $(this);
			var enableBtnFunction = function () { enableButton(btn); };
			disableButton(btn);
			s.btnLabel = s.btnLabel || btn.text();
			var a = btn.attr('data-structr-action').split(':');
			var action = a[0], type = a[1], suffix = a[2];
			var reload = btn.attr('data-structr-reload') === 'true';
			var appendId = btn.attr('data-structr-append-id') === 'true';
			var returnUrl = btn.attr('data-structr-return');
			var attrString = btn.attr('data-structr-attributes');
			var attrs = (attrString ? attrString.split(',') : []).map(function(s) {
				return s.trim();
			});

			var id = btn.attr('data-structr-id');
			var container = $('[data-structr-id="' + id + '"]');

			if (action === 'create') {
				var data = s.collectData(btn, id, attrs, type, suffix, 'name');
				s.create(btn, type, data, reload, returnUrl, appendId, enableBtnFunction, enableBtnFunction);

			} else if (action === 'save') {
				s.saveAction(btn, id, attrs, type, suffix, reload, returnUrl, s.labels[s.lang].successfullyUpdated + ' ' + id, s.labels[s.lang].couldNotUpdate + ' ' + id, enableBtnFunction, function(revert) {
					if (revert) {
						var cancelEditButton = $('[data-structr-action="cancel-edit' + (type ? ':' + type : '') + '"]');
						s.cancelEditAction(cancelEditButton, id, attrs, type, suffix, reload, returnUrl);
					} else {
						enableButton(btn);
					}
				});

			} else if (action === 'edit') {
				s.editAction(btn, id, attrs, type, suffix, reload, returnUrl);

			} else if (action === 'cancel-edit') {
				s.cancelEditAction(btn, id, attrs, type, suffix, reload, returnUrl);

			} else if (action === 'delete') {
				var f = s.field($('[data-structr-attr="name"]', container));
				s.del(btn, id, type, (btn.attr('data-structr-confirm') === 'true'), reload, returnUrl, (f ? f.val : undefined));

			} else if (action === 'login') {
				s.loginAction(btn, id, attrs, reload, returnUrl, enableBtnFunction, enableBtnFunction);

			} else if (action === 'logout') {
				s.logoutAction(btn, id, attrs, reload, returnUrl, enableBtnFunction, enableBtnFunction);

			} else if (action === 'registration') {
				s.registrationAction(btn, id, attrs, reload, returnUrl, enableBtnFunction, enableBtnFunction);

			} else if (action === 'reset-password') {
				s.resetPasswordAction(btn, id, attrs, reload, returnUrl, enableBtnFunction, enableBtnFunction);

			} else {
				var data = s.collectData(btn, id, attrs, type, suffix);
				s.customAction(btn, id, type, (btn.attr('data-structr-confirm') === 'true'), action, data, reload, returnUrl, appendId, enableBtnFunction, enableBtnFunction);
			}
		});
	},
	this.getPossibleFields = function(container, suffix, type, key, paramKey) {
		paramKey = paramKey || 'attr';
		var possibleFields;
		if (typeof suffix === 'string' && suffix.length) {
			// if a suffix is given, use only input elements with that suffix
			possibleFields = $('[data-structr-' + paramKey + '="' + key + ':' + suffix + '"]');
		} else if (container.length) {
			// exactly one container
			possibleFields = container.find('[data-structr-' + paramKey + '="' + key + '"]');
		} else {
			// fallback: all fields by name
			possibleFields = $('[data-structr-' + paramKey + '="' + key + '"]');
		}

		if (possibleFields.length !== 1) {
			// none, or more than one field: try with type prefix
			if (container.length) {
				possibleFields = $('[data-structr-' + paramKey + '="' + type + '.' + key + '"]', container);
			} else {
				possibleFields = $('[data-structr-' + paramKey + '="' + type + '.' + key + '"]');
			}
		}
		if (possibleFields.length !== 1) {
			// if still not found, try both, type prefix and suffix
			if (container.length) {
				possibleFields = $('[data-structr-' + paramKey + '="' + type + '.' + key + ':' + suffix + '"]', container);
			} else {
				possibleFields = $('[data-structr-' + paramKey + '="' + type + '.' + key + ':' + suffix + '"]');
			}
		}

		if (paramKey === 'attr' && possibleFields.length === 0) {
			possibleFields = s.getPossibleFields(container, suffix, type, key, 'name');
		}

		return possibleFields;
	},
	this.container = function(btn, id) {
		var form = btn.parents('form');
		var container;
		if (form.length === 1) {
			container = form;
		} else {
			container = btn.parents('[data-structr-id="' + id + '"]');
		}
		if (container.length === 0) {
			s.debug('No parent form found - this can lead to undesired behaviour.');
		}
		return container;
	},
	this.collectData = function(btn, id, attrs, type, suffix, paramKey) {
		paramKey = paramKey || 'attr';
		var container = s.container(btn, id);

		if (!s.data[id]) s.data[id] = {};
		$.each(attrs, function(i, key) {

			var inp = s.getPossibleFields(container, suffix, type, key, paramKey);
			var f = s.field(inp, true);
			if (!f) return;

			if (key.contains('.')) {
				delete s.data[id][key];
				var prop = key.split('.');
				var local = prop[0];
				var related = prop[1];

				key = local;
				s.data[id][local] = {};
				s.data[id][local][related] = f.val;

			} else if (f.type === 'Boolean') {

				s.data[id][key] = (f.val === true ? true : false);

			} else if (f.type === 'Integer' || f.type === 'Long') {

				s.data[id][key] = parseInt(f.val) || f.val;

			} else if (f.type === 'Double') {

				s.data[id][key] = parseFloat(f.val) || f.val;

			} else if (f.type === 'String' || f.type === 'Date') {

				if (f.val && f.val.length) {
					s.data[id][key] = f.val;
				} else {
					s.data[id][key] = null;
				}
			} else if (f.type === 'Enum') {
				var val = $('option:selected', inp).val();
				s.data[id][key] = (val === '' ? null : val);

			} else {
				var ids = [];
				$('option:selected', inp).each(function() {
					var self = $(this);
					if (self.val()) {
						ids.push(self.val());
					}
				});

				if (!ids.length) {
					s.data[id][key] = null;
				} else if (!f.type.endsWith('[]')) {
					s.data[id][key] = ids[0];
				} else {
					s.data[id][key] = ids.map(function(id) {
						return {'id': id};
					});
				}
			}
		});

		return s.data[id];
	},
	this.editAction = function(btn, id, attrs, type, suffix, reload, returnUrl) {
		var container = s.container(btn, id);

		//show edit elements and hide non-edit elements
		s.hideEdit(container);

		$.each(attrs, function(i, key) {

			var el = s.getPossibleFields(container, suffix, type, key, 'attr');
			var f = s.field(el);

			// ignore invalid fields
			if (!f) return;

			f.id = id;
			if (!s.data[id]) s.data[id] = {};
			s.data[id][key] = f.val;
			var anchor = el[0].tagName.toLowerCase() === 'a' ? el : el.parent('a');
			if (anchor.length) {
				var href = anchor.attr('href');
				anchor.attr('href', '').css({textDecoration: 'none'}).off('click').on('click', function() {
					return false;
				});
				anchor.attr('data-structr-href', href);
			}

			// don't replace select elements
			var inp = s.input(el);
			if (inp && inp.is('select')) {
				return;
			}

			if (f.type === 'Boolean') {
				el.html(checkbox(f));
			} else if (f.type === 'String' || f.type === 'Integer' || f.type === 'Long' || f.type === 'Double' || f.type === 'Date') {
				if (f.format && f.format === 'multi-line') {
					el.html(textarea(f));
				} else {
					if (!f.val || f.val.indexOf('\n') === -1) {
						el.html(inputField(f));
					} else {
						el.html(textarea(f));
					}
				}
			} else if (f.type === 'Enum') {

				el.html(enumSelect(f));
				var sel = $('select[data-structr-id="' + f.id + '"][data-structr-name="' + f.key + '"]');
				sel.append('<option></option>');
				$.each(f.format.split(','), function(i, o) {
					o = o.trim();
					sel.append('<option value="' + o + '" ' + (o === f.val ? 'selected="selected"' : '') + '>' + o + '</option>');
				});
				sel.addClass(f['class']);
				activateSelectElement(sel, {allow_single_deselect: true});

			} else {
				if (f.type.endsWith('[]')) {
					el.html(multiSelect(f));
				} else {
					el.html(singleSelect(f));
				}
			}

			var inp = s.input(el);
			inp.addClass(f['class']);

			if (f.type !== 'Enum') {
				resizeInput(inp);
			}

			if (anchor.length) {
				inp.attr('data-structr-href', href);
			}

			inp.attr('data-structr-display-value', f.displayVal);

			inp.on('keyup', function(e) {
				if (f.type === 'String') {
					s.checkInput(e, s.field(inp), inp);
				}
			});

			if (f.type === 'Date') {
				var defaultSettings = true;
				var dateFormat = 'yy-mm-dd';
				var targetDateFormat = 'yyyy-MM-dd';
				var timeFormat = 'HH:mm:ssz';

				if (f.format) {
					// user-supplied format in attribute "data-structr-format"
					defaultSettings = false;

					var dateTimeFormat = f.format.split('\'T\'');
					dateFormat = dateTimeFormat[0];
					timeFormat = dateTimeFormat[1];
				}

				inp.on('mouseup', function(event) {
					event.preventDefault();
					var input = $(this);

					if (timeFormat && typeof input.datetimepicker === "function") {
						input.datetimepicker({
							dateFormat: dateFormat,
							timeFormat: timeFormat,
							separator: 'T'
						});
						input.datetimepicker('show');
					} else {
						input.datepicker({
							dateFormat: dateFormat,
							onClose: function() {
								if (defaultSettings === true && typeof moment === "function") {
									var newValue = input.val();
									var formattedValue = moment(newValue).formatWithJDF(targetDateFormat);
									input.val(formattedValue);
								}
							}
						});
						input.datepicker('show');
					}
					input.off('mouseup');
				});
			}

		});
		var clazz = btn.attr('data-structr-edit-class');
		var saveButton = $('<button class="' + clazz + '" data-structr-action="save' + (type ? ':' + type : '') + (suffix ? ':' + suffix : '') + '" data-structr-id="' + id
			+ '" data-structr-attributes="' + attrs.join(',')
			+ '" data-structr-reload="' + reload
			+ (returnUrl ? '" data-structr-return="' + returnUrl : '')
			+ '">' + s.labels[s.lang].save + '</button>');
		saveButton.data('structrErrorHandler', btn.data('structrErrorHandler'));
		saveButton.insertBefore(btn);
		saveButton.prop('class', btn.prop('class')).after(' ');
		saveButton.addClass(clazz);
		btn.addClass(clazz);
		enableButton(saveButton);
		btn.text(s.labels[s.lang].cancel).attr('data-structr-action', 'cancel-edit' + (type ? ':' + type : ''));
		enableButton(btn);
	},

	this.saveAction = function(btn, id, attrs, type, suffix, reload, returnUrl, successMsg, errorMsg, onSuccess, onError) {
		var data = s.collectData(btn, id, attrs, type, suffix);
		s.request(btn, 'PUT', structrRestUrl + (type ? type + '/' : '') + id, data, reload, returnUrl, false, successMsg, errorMsg, onSuccess, onError);
	},

	this.cancelEditAction = function(btn, id, attrs, type, suffix, reload, returnUrl) {
		if (reload) {
			redirectOrReload(reload, returnUrl);
		} else {
			var container = s.container(btn, id);
			$.each(attrs, function(i, key) {
				var inp = s.getPossibleFields(container, suffix, type, key, 'name');
				var f = s.field(inp);
				var href = inp.attr('data-structr-href');
				var anchor = inp.parent('a');
				if (href && anchor.length) {
					anchor.attr('href', href);
					anchor.removeAttr('data-structr-href');
					anchor.attr('href', '').css({textDecoration: ''}).on('click', function() {
						document.location.href = href;
					});
				}
				inp.replaceWith(f.displayVal);
			});
			// clear data
			if (type) {
				$('button[data-structr-id="' + id + '"][data-structr-action="save:' + type + '"]').remove();
				btn.text(s.btnLabel).attr('data-structr-action', 'edit:' + type);
			} else {
				$('button[data-structr-id="' + id + '"][data-structr-action="save"]').remove();
				btn.text(s.btnLabel).attr('data-structr-action', 'edit');
			}
			enableButton(btn);

			//hide non edit elements and show edit elements
			s.hideNonEdit(container);

			removeSelectContainer(container);
		}
	},

	this.loginAction = function(btn, id, attrs, reload, returnUrl) {

		var data = {};

		if (attrs && attrs.length === 2) {

			// checking if the data is user/pw or 2fa
			if (!attrs[0].includes("twoFactor")) {
				data['name'] = $('[data-structr-name="' + attrs[0] + '"]').val();
				data['password'] = $('[data-structr-name="' + attrs[1] + '"]').val();
			} else {
				data['twoFactorToken'] = $('[data-structr-name="' + attrs[0] + '"]').val();
				data['twoFactorCode'] = $('[data-structr-name="' + attrs[1] + '"]').val();
			}
		}

		var msgBox = $('#msg');
		if (msgBox && msgBox.length) {
			$('span', msgBox).remove();
		}

		var oldBtnText = disableButton(btn, s.labels[s.lang].checking);

		var app = this;

		var ajaxRequest = $.ajax({
			type: 'POST',
			method: 'POST',
			contentType: 'application/json',
			url: '/structr/rest/login',
			data: JSON.stringify(data),
			statusCode: {
				200: function(data) {
					btn.text(s.labels[s.lang].success);
					redirectOrReload(reload, returnUrl);
				},
				202: function(data) {
					returnUrl = ajaxRequest.getResponseHeader("twoFactorLoginPage") + "?token=" + ajaxRequest.getResponseHeader("token");
					var qrdata = ajaxRequest.getResponseHeader("qrdata");
					if (qrdata) {
						returnUrl += "&qrdata=" + qrdata;
					}
					btn.text(s.labels[s.lang].success);
					redirectOrReload(reload, returnUrl);
				},
				401: function() { //change message on button depending on reason for 401
					var buttonLabel = s.labels[s.lang].wrongUsernameOrPassword;

					var callbackFn = function () {
						btn.text(oldBtnText);
					};

					switch(ajaxRequest.getResponseHeader("reason")) {
						case "maxSessionLimitExceeded":
							buttonLabel = s.labels[s.lang].numberOfSessionsExceeded;
							break;
						case "loginAttempts":
							buttonLabel = s.labels[s.lang].loginAttempts;
							break;
						case "wrongTwoFactorCode":
							buttonLabel = s.labels[s.lang].wrongTwoFactorCode;
							break;
						case "passwordChangeRequired":
							buttonLabel = s.labels[s.lang].passwordChangeRequired;
							break;
						case "invalidTwoFactorToken":
							buttonLabel = s.labels[s.lang].invalidTwoFactorToken;
							callbackFn = function () {
								history.back();
							};
							break;
					}

					app.feedbackAction(msgBox, buttonLabel, 2000, btn, true, callbackFn);
				}
			}
		});
	},
	this.logoutAction = function(btn, id, attrs, reload, returnUrl) {
		disableButton(btn, s.labels[s.lang].processing);
		$.ajax({
			type: 'POST',
			method: 'POST',
			contentType: 'application/json',
			url: '/structr/rest/logout',
			data: JSON.stringify({}),
			statusCode: {
				200: function() {
					redirectOrReload(reload, returnUrl);
				}
			}
		});
	},
	this.registrationAction = function(btn, id, attrs, reload, returnUrl) {

		var data = this.collectValues(attrs);

		var msgBox = $('#msg');
		if (msgBox && msgBox.length) {
			$('span', msgBox).remove();
		}

		var oldBtnText = disableButton(btn, s.labels[s.lang].processing);
		var successText = s.labels[s.lang].checkYourInbox;

		var app = this;

		$.ajax({
			type: 'POST',
			method: 'POST',
			contentType: 'application/json',
			url: '/structr/rest/registration',
			data: JSON.stringify(data),
			statusCode: {
				200: function() {
					app.feedbackAction(msgBox, successText, 5000, btn, false, function () {
						enableButton(btn);
						btn.text(oldBtnText);
						redirectOrReload(reload);
					});
				},
				201: function() {
					app.feedbackAction(msgBox, successText, 5000, btn, false, function () {
						enableButton(btn);
						btn.text(oldBtnText);
						redirectOrReload(reload);
					});
				},
				400: function() {
					app.feedbackAction(msgBox, s.labels[s.lang].pleaseEnterEMail, 5000, btn, true, function () {
						btn.text(oldBtnText);
					});
				}
			}
		});
	},
	this.resetPasswordAction = function(btn, id, attrs, reload, returnUrl) {

		var data = this.collectValues(attrs);

		// trim and tranform to lowercase
		if (data.eMail) {
		    data.eMail = data.eMail.trim().toLowerCase();
		}

		var msgBox = $('#msg');
		if (msgBox && msgBox.length) {
			$('span', msgBox).remove();
		}

		var oldBtnText = disableButton(btn, s.labels[s.lang].processing);
		var successText = s.labels[s.lang].passwordLinkSent;

		var app = this;

		$.ajax({
			type: 'POST',
			method: 'POST',
			contentType: 'application/json',
			url: '/structr/rest/reset-password',
			data: JSON.stringify(data),
			statusCode: {
				200: function() {
					app.feedbackAction(msgBox, successText, 5000, btn, false, function () {
						enableButton(btn);
						btn.text(oldBtnText);
						redirectOrReload(reload);
					});
				},
				400: function() {
					app.feedbackAction(msgBox, s.labels[s.lang].pleaseEnterEMail, 1000, btn, true, function () {
						btn.text(oldBtnText);
					});
				}
			}
		});
	},
	this.input = function(elements) {
		var el = $(elements[0]);
		var inp;
		if (el.is('input') || el.is('textarea') || el.is('select')) {
			return el;
		} else {
			inp = el.children('textarea');
			if (inp.length) {
				return inp;
			} else {
				inp = el.children('input');
				if (inp.length) {
					return inp;
				} else {
					inp = el.children('select');
					if (inp.length) {
						return inp;
					} else {
						return null;
					}
				}
			}
		}
	},
	this.field = function(el, collect) {
		if (!el || !el.length) return;
		var displayVal  = el.attr('data-structr-display-value') || (el.is('select') ? null : el.html());
		var rawType     = el.attr('data-structr-type');
		var clazz       = el.attr('data-structr-edit-class');
		var query       = el.attr('data-structr-custom-options-query');
		var optionsKey  = el.attr('data-structr-options-key');
		var type        = rawType ? rawType.match(/^\S+/)[0] : 'String';
		var id          = el.attr('data-structr-id');
		var key         = el.attr('data-structr-attr');
		var rawVal      = !collect ? el.attr('data-structr-raw-value') : undefined;
		var placeholder = el.attr('data-structr-placeholder');
		var format      =  (rawType && rawType.contains(' ')) ? rawType.replace(type + ' ', '') : el.attr('data-structr-format');
		var val;
		if (type === 'Boolean') {
			if (el.is('input')) {
				val = el.is(':checked');
			} else {
				if (collect) {
					var inp = s.input(el);
					val = inp.is(':checked');
				} else {
					val = (rawVal === 'true') || (el.text() === 'true');
				}
			}
		} else {
			var inp = s.input(el);
			if (inp) {
				if (inp.is('select')) {
					val = inp.val();
				} else {
					val = rawVal || (inp.val() && inp.val().replace(/<br>/gi, '\n'));
				}
			} else {
				val = rawVal || el.html().replace(/<br>/gi, '\n');
			}
		}
		return {
			id: id,
			type: type,
			key: key,
			val: val,
			rawVal: rawVal,
			format: format,
			query: query,
			optionsKey: optionsKey,
			class: clazz,
			placeholder: placeholder,
			displayVal: displayVal
		};
	};

	this.create = function(btn, type, data, reload, returnUrl, appendId, successCallback, errorCallback) {
		s.request(btn, 'POST', structrRestUrl + type, data, reload, returnUrl, appendId, s.labels[s.lang].successfullyCreatedNew + ' ' + type, s.labels[s.lang].couldNotCreate + ' ' + type, successCallback, errorCallback);
	};

	this.customAction = function(btn, id, type, conf, action, data, reload, returnUrl, appendId, successCallback, errorCallback) {
		var sure = true;
		if (conf) {
			sure = confirm(s.labels[s.lang].areYouSure);
		}
		if (!conf || sure) {
			s.request(btn, 'POST', structrRestUrl + (type ? type + '/' : '') + (id ? id + '/' : '') + action, data, reload, returnUrl, appendId, s.labels[s.lang].successfullyExecutedCustomAction + ' ' + action, s.labels[s.lang].couldNotExecuteCustomAction + ' ' + type, successCallback, errorCallback);
		} else {
			enableButton(btn);
		}
	};

	this.request = function(btn, method, url, data, reload, returnUrl, appendId, successMsg, errorMsg, onSuccess, onError) {

		var errorHandlerFn;
		var errorHandlerName = btn.data('structrErrorHandler');
		if (window[errorHandlerName] && typeof window[errorHandlerName] === "function") {
			errorHandlerFn = window[errorHandlerName];
		}

		var dataString = JSON.stringify(data);

		var simpleAjaxErrorMessage = function(data, status, xhr) {
			if (errorHandlerFn) {
				errorHandlerFn(errorMsg, data);
			} else {
				s.dialog('error', errorMsg + ': ' + data.responseText);
			}
			if (onError) {
				onError();
			}
		};

		$.ajax({
			type: method,
			url: url,
			data: dataString,
			contentType: 'application/json; charset=utf-8',
			statusCode: {
				200: function(data) {
					s.dialog('success', successMsg);
					if (reload) {
						redirectOrReload(reload, returnUrl);
					} else {
						if (onSuccess) {
							onSuccess(data);
						}
					}
				},
				201: function(data) {
					s.dialog('success', successMsg);
					if (reload || appendId) {
						if (appendId) {
							if (!returnUrl) {
								returnUrl = document.location.href;
							}
							var isParameter = returnUrl.endsWith('=');
							var hasSlash    = returnUrl.endsWith('/');
							returnUrl = returnUrl + (isParameter || hasSlash ? '' : '/') + data.result[0];
						}
						redirectOrReload(reload, returnUrl);
					} else {
						if (onSuccess) {
							onSuccess();
						}
					}
				},
				400: simpleAjaxErrorMessage,
				401: simpleAjaxErrorMessage,
				403: simpleAjaxErrorMessage,
				404: function(data, status, xhr) {
					if (errorHandlerFn) {
						errorHandlerFn(errorMsg, data);
					} else {
						s.dialog('error', errorMsg + ': ' + data.responseText);
					}
					if (onError) {
						onError(data);
					}
				},
				422: function(data, status, xhr) {
					if (errorHandlerFn) {
						errorHandlerFn(errorMsg, data);
					} else {
						s.dialog('error', errorMsg + ': ' + data.responseText);
					}
					var revertEditMode = s.showFormErrors(btn, data.responseJSON);
					if (onError) {
						onError(revertEditMode);
					}
				},
				500: simpleAjaxErrorMessage
			}
		});
	},

	this.dialog = function(type, msg) {
		var el = $('[data-structr-dialog="' + type + '"]');
		el.addClass(type).html(msg).show().delay(2000).fadeOut(200);
	};

	this.showFormErrors = function(btn, msg) {
		var a = btn.attr('data-structr-action').split(':');
		var suffix = a[2];
		var id = btn.attr('data-structr-id');
		var container = s.container(btn, id);

		if (window.jQuery.validator) {
			var errorsToShow = {};
			var form;
			msg.errors.forEach(function(error) {
				var inp = s.getPossibleFields(container, suffix, error.type, error.property);
				var containsInputs = inp.find('input');
				if (containsInputs.length > 0) {
					inp = containsInputs;
				}
				inp.attr('name', error.type + '.' + error.property);
				errorsToShow[error.type + '.' + error.property] = error.token;
				form = inp.parents('form');
			});

			if (form) {
				var validator = form.validate();
				form.valid();
				validator.showErrors(errorsToShow);

				return false;
			}
		}

		return true;
	};

	this.add = function(id, sourceId, sourceType, relatedProperty) {

		var d = {};
		d[relatedProperty] = [ {'id': id} ];

		$.ajax({
			url: structrRestUrl + sourceType + '/' + sourceId + '/ui', method: 'GET', contentType: 'application/json',
			statusCode: {
				200: function(data) {

					if (data.result[relatedProperty] === undefined) {
						alert(s.labels[s.lang].couldNotReadRelatedProperty + '\n\n    ' + sourceType + '.' + relatedProperty + '\n\n' + s.labels[s.lang].makeSureContainedInEntity);
						return;
					}

					if (data.result[relatedProperty].length) {
						$.each(data.result[relatedProperty], function(i, obj) {
							d[relatedProperty].push({'id': obj.id});
						});
					}

					$.ajax({
						url: structrRestUrl + sourceType + '/' + sourceId, method: 'PUT', contentType: 'application/json',
						data: JSON.stringify(d),
						statusCode: {
							200: function() {
								window.location.reload();
							},
							400: function(xhr) {
								console.log(xhr);
							},
							422: function(xhr) {
								console.log(xhr);
							}
						}
					});

				}
			}
		});

	};

	this.remove = function(id, sourceId, sourceType, relatedProperty) {

		var d = {};
		d[relatedProperty] = [];

		$.ajax({
			url: structrRestUrl + sourceId + '/ui', method: 'GET', contentType: 'application/json',
			statusCode: {
				200: function(data) {

					if (data.result[relatedProperty] === undefined) {
						alert(s.labels[s.lang].couldNotReadRelatedProperty + '\n\n    ' + sourceType + '.' + relatedProperty + '\n\n' + s.labels[s.lang].makeSureContainedInEntity);
						return;
					}

					if (data.result[relatedProperty].length) {
						$.each(data.result[relatedProperty], function(i, obj) {
							if (obj.id !== id) {
								d[relatedProperty].push({'id': obj.id});
							}
						});
					}

					$.ajax({
						url: structrRestUrl + sourceId, method: 'PUT', contentType: 'application/json',
						data: JSON.stringify(d),
						statusCode: {
							200: function() {
								window.location.reload();
							},
							400: function(xhr) {
								console.log(xhr);
							},
							422: function(xhr) {
								console.log(xhr);
							}
						}
					});

				}
			}
		});

	};

	this.del = function(btn, id, type, conf, reload, returnUrl, name) {
		var sure = true;
		if (conf) {
			sure = confirm(s.labels[s.lang].areYourSureToDelete + ' ' + (name ? name : id) + '?');
		}
		if (!conf || sure) {
			$.ajax({
				url: structrRestUrl + (type ? type + '/' : '') + id, method: 'DELETE', contentType: 'application/json',
				statusCode: {
					200: function() {
						if (reload) {
							redirectOrReload(reload, returnUrl);
						} else {
							enableButton(btn);
						}
					}
				}
			});
		} else {
			enableButton(btn);
		}
	};

	this.save = function(f, b) {
		var obj = {};

		obj[f.key] = f.val;
		if (b) {
			b.html('<img src="/structr/img/al.gif"> ' + s.labels[s.lang].saving);
		}

		$.ajax({url: baseUrl + f.id, method: 'PUT', contentType: 'application/json', data: JSON.stringify(obj),
			statusCode: {
				200: function() {
					if (b) {
						b.text(s.labels[s.lang].success).remove();
					}
				}
			}
		});
	};

	this.checkInput = function(e, f, inp) {
		var k = e.which;

		if (f.type === 'String' && f.format === 'multi-line') {
			return;
		}

		if (isTextarea(inp[0])) {

			if (inp.val().indexOf('\n') === -1) {

				var parent = inp.parent();

				// No new line in textarea content => transform to input field
				f.val = inp.val();
				inp.replaceWith(inputField(f));
				inp = s.input(parent);

				inp.on('keyup', function(e) {
					s.checkInput(e, f, inp);
				});

				setCaretToEnd(inp[0]);

			}

		} else if (k === 13) {

			// Return key in input field => replace by textarea
			var parent = inp.parent();

			f.val = inp.val() + '\n';

			inp.replaceWith(textarea(f));
			inp = s.input(parent);

			inp.on('keyup', function(e) {
				s.checkInput(e, f, $(this));
			});

			setCaretToEnd(inp[0]);

		}

		inp.addClass(f['class']);

		resizeInput(inp);

	};
	this.appendSaveButton = function(b, p, inp, id, key) {

		// Remove existing save button
		if (b.length) {
			$('#save_' + id + '_' + key).remove();
		}

		inp.after('<button id="save_' + id + '_' + key + '">' + s.labels[s.lang].save + '</button>');
		$('#save_' + id + '_' + key).on('click', function() {
			var btn = $(this), inp = btn.prev();
			s.save(s.field(inp), btn);
		});

	};
	this.hideEdit = function(container) {

		this.replaceHiddenDivsWithStoredElements($('[data-structr-hide-id]', container), hideNonEditElements);
		this.replaceElementsWithHiddenDivs($('[data-structr-hide="edit"]', container), hideEditElements);

		$(document).trigger("structr-edit");
	};
	this.hideNonEdit = function(container) {

		if (container === undefined){

			this.replaceElementsWithHiddenDivs($('[data-structr-hide="non-edit"]'), hideNonEditElements);

		} else {

			this.replaceHiddenDivsWithStoredElements($('[data-structr-hide-id]', container), hideEditElements);
			this.replaceElementsWithHiddenDivs($('[data-structr-hide="non-edit"]', container), hideNonEditElements);

		}
	};
	this.replaceElementsWithHiddenDivs = function (el, elementStorage) {

		$.each(el, function(i, obj) {
			var random = Math.floor(Math.random()*1000000+1);
			elementStorage[random] = $(obj).clone(true,true);
			$(obj).replaceWith('<div style="display:none;" data-structr-hide-id="'+random+'"></div>');
		});
	};
	this.replaceHiddenDivsWithStoredElements = function (el, elementStorage) {

		$.each(el, function() {
			var id = $(this).attr('data-structr-hide-id');
			$(this).replaceWith(elementStorage[id]);
			delete elementStorage[id];
		});
	};
	this.feedbackAction = function (msgBox, messageText, delay, btn, shouldEnableButton, callback) {
		if (msgBox && msgBox.length) {
			$('#msg').append('<span>' + messageText + '</span>');
			$('#msg span').delay(delay).fadeOut(delay);
		} else {
			btn.text(messageText);
			if (typeof callback === "function") {
				window.setTimeout(callback, delay);
			}
		}
		if (shouldEnableButton) {
			enableButton(btn);
		}
	};
	this.collectValues = function (attrs) {
		var data = {};
		if (attrs && attrs.length) {
			attrs.forEach(function(attr) {
				data[attr] = $('[data-structr-name="' + attr + '"]').val();
			});
		}
		return data;
	};

	this.debugEnabled = false;
	this.setDebugMode = function (enable) {
		this.debugEnabled = (enable === true);
	};
	this.debug = function (message) {
		if (this.debugEnabled === true && console != undefined) {
			console.warn(message);
		}
	};
}

function resizeInput(inp) {

	var text = inp.val();
	// don't resize empty input elements with preset size
	if (!text || (!text.length && inp.attr('size'))) return;

	if (isTextarea(inp[0])) {

		var n = (text.match(/\n/g) || []).length;
		inp.attr('rows', n+2);

		var lines = text.split('\n');
		var c = lines.sort(function(a, b) {
			return b.length - a.length;
		})[0].length;

		inp.attr('cols', c+1);

	} else {

		inp.attr('size', text.length + 1);
	}
}

function urlParam(name) {
	name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
	var regexS = "[\\?&]" + name + "=([^&#]*)";
	var regex = new RegExp(regexS);
	var res = regex.exec(window.location.href);
	return (res && res.length ? res[1] : '');
}

String.prototype.capitalize = function() {
	return this.charAt(0).toUpperCase() + this.slice(1);
};

String.prototype.endsWith = function(suffix) {
	return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

String.prototype.parseIfJSON = function() {
	if (this.substring(0,1) === '{') {
		var cleaned = this.replace(/'/g, "\"");
		return JSON.parse(cleaned);
	}
	return this.toString();
};

String.prototype.splitAndTitleize = function(sep) {

	var res = new Array();
	var parts = this.split(sep);
	parts.forEach(function(part) {
		res.push(part.capitalize());
	});
	return res.join(' ');
};

String.prototype.toUnderscore = function() {
	return this.replace(/([A-Z])/g, function(m, a, offset) {
		return (offset > 0 ? '_' : '') + m.toLowerCase();
	});
};

if (typeof String.prototype.contains !== 'function') {
	String.prototype.contains = function(pattern) {
		return this.indexOf(pattern) > -1;
	};
}

function setCaretToEnd(el) {
	var l = el.value.length;
	if (document.selection) {
		el.focus();
		var o = document.selection.createRange();
		o.moveStart('character', -l);
		o.moveStart('character', l);
		o.moveEnd('character', 0);
		o.select();
	} else if (el.selectionStart || el.selectionStart === 0 || el.selectionStart === '0') {
		el.selectionStart = l;
		el.selectionEnd = l;
		el.focus();
	}
}

function escapeTags(str) {
	if (!str)
		return str;
	return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function isTextarea(el) {
	return el.nodeName.toLowerCase() === 'textarea';
}

function textarea(f) {
	return '<textarea data-structr-id="' + f.id + '" data-structr-type="' + f.type + '"' + (f['class'] ? ' data-structr-edit-class="' + f['class'] + '"' : '') + (f.format ? ' data-structr-format="' + f.format + '"' : '') + '" data-structr-name="' + f.key + '">' + f.val + '</textarea>';
}

function inputField(f) {
	var size = (f.val ? f.val.length : (f.type && f.type === 'Date' ? 25 : f.key.length));
	return '<input data-structr-id="' + f.id + '"' + (f['class'] ? ' data-structr-edit-class="' + f['class'] + '"' : '') + (f.format ? ' data-structr-format="' + f.format + '"' : '') + '" data-structr-name="' + f.key + '" data-structr-type="' + f.type + '" type="text" placeholder="' + (f.placeholder ? f.placeholder : '')
		+ '" data-structr-raw-value="' + escapeForHtmlAttributes(f.rawVal === 'null' ? '' : f.rawVal)
		+ '" value="' + escapeForHtmlAttributes(f.val === 'null' ? '' : f.val)
		+ '" size="' + size + '">';
}

function field(f) {
	return '<span type="text" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '"' + (f.format ? ' data-structr-format="' + f.format + '"' : '') + (f['class'] ? ' data-structr-edit-class="' + f['class'] + '"' : '') + ' data-structr-name="' + f.key + '">' + f.val + '</span>';
}

function checkbox(f) {
	return '<input type="checkbox" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '"' + (f.format ? ' data-structr-format="' + f.format + '"' : '') + (f['class'] ? ' data-structr-edit-class="' + f['class'] + '"' : '') + ' data-structr-name="' + f.key + '" ' + (f.val ? 'checked="checked"' : '') + '>';
}

function enumSelect(f) {
	return '<select data-structr-type="' + f.type + '"' + (f['class'] ? ' data-structr-edit-class="' + f['class'] + '"' : '') + ' data-structr-name="' + f.key + '" data-structr-id="' + f.id + '"></select>';
}

function activateSelectElement (el, config) {
	if ($().chosen) {
		el.chosen(config);
	} else if ($().select2) {
		el.select2(config);
	} else {
		// neither chosen nor select2 is available
	}
}

function removeSelectContainer (container) {
	if ($().chosen) {
		$('.chosen-container', container).remove();
	} else if ($().select2) {
		$('.select2-container', container).remove();
	} else {
		// neither chosen nor select2 is available
	}
}

function singleSelect(f) {
	var inp = '<select data-structr-type="' + f.type + '" data-structr-name="' + f.key + '" data-structr-id="' + f.id + '"></select>';
	var optionsKey = f.optionsKey || 'name';
	$.ajax({
		url: structrRestUrl + (f.query ? f.query : f.type + '/ui'), method: 'GET', contentType: 'application/json',
		statusCode: {
			200: function(data) {
				var sel = $('select[data-structr-id="' + f.id + '"][data-structr-name="' + f.key + '"]');
				sel.append('<option></option>');
				if (data.result && data.result.length) {
					$.each(data.result, function(i, o) {
						sel.append('<option value="' + o.id + '" ' + (o.id === f.val ? 'selected' : '') + '>' + o[optionsKey] + '</option>');
					});
					activateSelectElement(sel, {allow_single_deselect: true});
				}
			}
		}
	});
	return inp;
}

function multiSelect(f) {
	var inp = '<select data-structr-type="' + f.type + '" data-structr-name="' + f.key + '" data-structr-id="' + f.id + '" multiple="multiple"></select>';
	f.type = f.type.substring(0, f.type.length-2);
	var valIds = f.val.replace(/ /g, '').slice(1).slice(0, -1).split(',');
	var optionsKey = f.optionsKey || 'name';
	$.ajax({
		url: structrRestUrl + (f.query ? f.query : f.type + '/ui'), method: 'GET', contentType: 'application/json',
		statusCode: {
			200: function(data) {
				var sel = $('select[data-structr-id="' + f.id + '"][data-structr-name="' + f.key + '"]');
				if (data.result && data.result.length) {
					$.each(data.result, function(i, o) {
						sel.append('<option value="' + o.id + '" ' + (valIds.indexOf(o.id) > -1 ? 'selected' : '') + '>' + o[optionsKey] + '</option>');
					});
					activateSelectElement(sel, {});
				}
			}
		}
	});
	return inp;
}

function enableButton(btn) {
	btn.removeClass('disabled').removeAttr('disabled');
}

function disableButton(btn, text) {
	var oldBtnText = btn.text();
	btn.addClass('disabled').attr('disabled', 'disabled');
	if (text) {
		btn.text(text);
	}
	return oldBtnText;
}

function redirectOrReload(reload, returnUrl) {
	if (returnUrl) {
		window.location.href = returnUrl;
	} else if (reload) {
		window.location.reload();
	}
}

function escapeForHtmlAttributes(str, escapeWhitespace) {
	if (!(typeof str === 'string'))
		return str;
	var escapedStr = str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');

	return escapeWhitespace ? escapedStr.replace(/ /g, '&nbsp;') : escapedStr;
}


function lang() {
	if (navigator.language.toLowerCase() === 'en-us') { return ''; }
	else {
		var l = navigator.language.toLowerCase().split('-');
		if (l.length === 1) {
			if ($.datepicker.regional[l[0]] !== undefined) return l[0];
			else return '';
		} else if (l.length > 1) {
			if ($.datepicker.regional[l[0] + '-' + l[1].toUpperCase()] !== undefined) return l[0] + '-' + l[1].toUpperCase();
			else if ($.datepicker.regional[l[0]] !== undefined) return l[0];
			else return '';
		} else {
			return '';
		}
	}
}