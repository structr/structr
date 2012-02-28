/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

function formatValue(key, obj) {

    if (obj == null) {
        return '<input name="' + key + '" type="text" value="">';
    } else if (obj.constructor === Object) {

        return '<input name="' + key + '" type="text" value="' + JSON.stringify(obj) + '">';

    } else if (obj.constructor === Array) {
        var out = '';
        $(obj).each(function(i,v) {
            //console.log(v);
            out += JSON.stringify(v);
        });

        return '<textarea name="' + key + '">' + out + '</textarea>';

    } else {
        return '<input name="' + key + '" type="text" value="' + obj + '">';

    }
    
//return '<input name="' + key + '" type="text" value="' + formatValue(data.result[key]) + '">';
}

function formatKey(text) {
    var result = '';
    for (var i=0; i<text.length; i++) {
        var c = text.charAt(i);
        if (c == c.toUpperCase()) {
            result += ' ' + c;
        } else {
            result += (i==0 ? c.toUpperCase() : c);
        }
    }
    return result;
}

function deleteAll(button, type, callback) {
    var types = plural(type);
    var element = $('#' + types);
    if (isDisabled(button)) return;
    var con = confirm('Delete all ' + types + '?');
    if (!con) return;
    $.ajax({
        url: rootUrl + type.toLowerCase(),
        type: "DELETE",
        headers: headers,
        success: function(data) {
            $(element).children("." + type).each(function(i, child) {
                $(child).remove();
            });
            enable(button);
            if (callback) callback();
        }
    });
}

function deleteNode(button, entity) {
    buttonClicked = button;
    if (isDisabled(button)) return;
    var con = Structr.confirmation('Delete ' + entity.type.toLowerCase() + ' \'' + entity.name + '\' <span class="id">' + entity.id + '</span>?', function() {
        //	var toSend = '{ "command" : "DELETE" , "id" : "' + entity.id + '", "data" : { "callback" : "' + callback + '" } }';
        var toSend = '{ "command" : "DELETE" , "id" : "' + entity.id + '" }';
        //if (debug) console.log(toSend);
        if (send(toSend)) {
            disable(button);
            $.unblockUI();
        }
    });
}

function isDisabled(button) {
    return $(button).data('disabled');
}

function disable(button, callback) {
    var b = $(button);
    b.data('disabled', true);
    b.addClass('disabled');
    if (callback) {
        b.off('click');
        b.on('click', callback);
        b.data('disabled', false);
    //enable(button, callback);
    }
}

function enable(button, func) {
    var b = $(button);
    b.data('disabled', false);
    b.removeClass('disabled');
    if (func) {
        b.off('click');
        b.on('click', func);
    }
}

function setPosition(parentId, nodeUrl, pos) {
    var toPut = '{ "' + parentId + '" : ' + pos + ' }';
    //console.log(toPut);
    $.ajax({
        url: nodeUrl + '/in',
        type: 'PUT',
        async: false,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        headers: headers,
        data: toPut,
        success: function(data) {
        //appendElement(parentId, elementId, data);
        }
    });
}

function refresh(parentId, id) {
    $('.' + parentId + '_ ' + id + '_ > div.nested').remove();
    showSubEntities(parentId, id);
}



var keyEventBlocked = true;
var keyEventTimeout;


function getIdFromClassString(classString) {
    var classes = classString.split(' ');
    var id;
    $(classes).each(function(i,v) {
        var len = v.length;
        if (v.substring(len-1, len) == '_') {
            id = v.substring(0,v.length-1);
        }
    });
    return id;
}

function getId(element) {
    return getIdFromClassString(element.attr('class'));
}

function lastPart(id, separator) {
    if (!separator) {
        separator = '_';
    }
    if (id) {
        return id.substring(id.lastIndexOf(separator)+1);
    }
    return '';
}
                                            
function sortArray(arrayIn, sortBy) {
    var arrayOut = arrayIn.sort(function(a,b) {
        return sortBy.indexOf(a.id) > sortBy.indexOf(b.id);
    });
    return arrayOut;
}
  
function followIds(resourceId, entity) {
    var resId = resourceId.toString();
    var entityId = (entity ? entity.id : resourceId);
    var url = rootUrl + entityId + '/' + 'out';
    //console.log(url);
    var ids = [];
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        async: false,
        headers: headers,
        success: function(data) {
            //console.log(data);
            if (!data || data.length == 0 || !data.result) return;
            var out = data.result;
            if ($.isArray(out)) {
                //for (var i=0; i<out.length; i++) {
                $(out).each(function(i, rel) {
                    var pos = rel[resId];
                    if (pos) {
                        //console.log('pos: ' + pos);
                        ids[pos] = rel.endNodeId;
                    //console.log('ids[' + pos + ']: ' + ids[pos]);
                    }
                });
            } else {
          
                if (out[resId]) {
                    //console.log('out[resId]: ' + out[resId]);
                    ids[out[resId]] = out.endNodeId;
                //console.log('ids[' + out[resId] + ']: ' + out.endNodeId);
                }
            }
        }
    });
    //console.log('resourceId: ' + resourceId + ', nodeId: ' + resourceId);
    //console.log(ids);
    return ids;
}
  
function isIn(id, ids) {
    return ($.inArray(id, ids) > -1);
}

function escapeTags(str) {
	if (!str) return str;
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function unescapeTags(str) {
	if (!str) return str;
    return str.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
}


////(function( $, undefined ) {
//
//    $.widget("ui.droppable")._drop = function(event, custom) {
////        _drop: function(event,custom) {
//
//            var draggable = custom || $.ui.ddmanager.current;
//            if (!draggable || (draggable.currentItem || draggable.element)[0] == this.element[0]) return false; // Bail if draggable and droppable are same element
//
//
//
//            var childrenIntersection = false;
//            this.element.find(":data(droppable)").not(".ui-draggable-dragging").each(function() {
//                var inst = $.data(this, 'droppable');
//                console.log('*********************** _drop ***********************************');
//                if(
//                    inst.options.greedy
//                    && !inst.options.disabled
//                    && inst.options.scope == draggable.options.scope
//                    && inst.accept.call(inst.element[0], (draggable.currentItem || draggable.element))
//                    && $.ui.intersect(draggable, $.extend(inst, {
//                        offset: inst.element.offset()
//                    }), inst.options.tolerance, inst.options.iframeOffset)
//                    ) {
//                    childrenIntersection = true;
//                    return false;
//                }
//            });
//            if(childrenIntersection) return false;
//
//            if(this.accept.call(this.element[0],(draggable.currentItem || draggable.element))) {
//                if(this.options.activeClass) this.element.removeClass(this.options.activeClass);
//                if(this.options.hoverClass) this.element.removeClass(this.options.hoverClass);
//                this._trigger('drop', event, this.ui(draggable));
//                return this.element;
//            }
//
//            return false;
//        }
//
////    }
////);
////});
//
//$.ui.ddmanager.drop = function(draggable, event) {
//    console.log('########################## dropped ###########################');
//
//    var dropped = false;
//    $.each($.ui.ddmanager.droppables[draggable.options.scope] || [], function() {
//
//        if(!this.options) return;
//        if (!this.options.disabled && this.visible && $.ui.intersect(draggable, this, this.options.tolerance, this.options.iframeOffset))
//            dropped = this._drop.call(this, event) || dropped;
//
//        if (!this.options.disabled && this.visible && this.accept.call(this.element[0],(draggable.currentItem || draggable.element))) {
//            this.isout = 1;
//            this.isover = 0;
//            this._deactivate.call(this, event);
//        }
//
//    });
//    return dropped;
//
//}
//
//$.ui.ddmanager.drag = function(draggable, event) {
//
//    //If you have a highly dynamic page, you might try this option. It renders positions every time you move the mouse.
//    if(draggable.options.refreshPositions) $.ui.ddmanager.prepareOffsets(draggable, event);
//
//    //Run through all droppables and check their positions based on specific tolerance options
//    $.each($.ui.ddmanager.droppables[draggable.options.scope] || [], function() {
//
//        //console.log('dropppable', this);
//        console.log('greedy child? ' + this.greedyChild);
//        if(this.options.disabled || this.greedyChild || !this.visible) return;
//        var intersects = $.ui.intersect(draggable, this, this.options.tolerance, this.options.iframeOffset);
//
//        var c = !intersects && this.isover == 1 ? 'isout' : (intersects && this.isover == 0 ? 'isover' : null);
//        console.log('c', c);
//        if(!c) return;
//        //console.log('c', c);
//
//        var parentInstance;
//        if (this.options.greedy) {
//            var parent = this.element.parents(':data(droppable):eq(0)');
//            if (parent.length) {
//                parentInstance = $.data(parent[0], 'droppable');
//                parentInstance.greedyChild = (c == 'isover' ? 1 : 0);
//            }
//        }
//
//        // we just moved into a greedy child
//        if (parentInstance && c == 'isover') {
//            parentInstance['isover'] = 0;
//            parentInstance['isout'] = 1;
//            parentInstance._out.call(parentInstance, event);
//        }
//
//        this[c] = 1;
//        this[c == 'isout' ? 'isover' : 'isout'] = 0;
//        this[c == "isover" ? "_over" : "_out"].call(this, event);
//
//        // we just moved out of a greedy child
//        if (parentInstance && c == 'isout') {
//            parentInstance['isout'] = 0;
//            parentInstance['isover'] = 1;
//            parentInstance._over.call(parentInstance, event);
//        }
//    });
//
//}

//$.ui.intersect = function(draggable, droppable, toleranceMode, iframeOffset) {
//
//    var x1,x2,y1,y2,l,r,t,b;
//
//    if (!droppable.offset) return false;
//
//    if (iframeOffset) {
//        x1 = (draggable.positionAbs || draggable.position.absolute).left - iframeOffset.left;
//        y1 = (draggable.positionAbs || draggable.position.absolute).top - iframeOffset.top;
//        x2 = x1 + draggable.helperProportions.width;
//        y2 = y1 + draggable.helperProportions.height;
////        x2 -= iframeOffset.left;
////        y2 -= iframeOffset.top;
////        l -= iframeOffset.left;
////        r -= iframeOffset.left;
////        t -= iframeOffset.top;
////        b -= iframeOffset.top;
//
//        l = droppable.offset.left;
//        t = droppable.offset.top;
//
////        l = droppable.offset.left - iframeOffset.left;
////        t = droppable.offset.top - iframeOffset.top;
//
//    } else {
//        x1 = (draggable.positionAbs || draggable.position.absolute).left;
//        y1 = (draggable.positionAbs || draggable.position.absolute).top;
//        x2 = x1 + draggable.helperProportions.width;
//        y2 = y1 + draggable.helperProportions.height;
//        l = droppable.offset.left;
//        t = droppable.offset.top;
//    }
//
//    r = l + droppable.proportions.width;
//    b = t + droppable.proportions.height;
//
//
//
//    var returnValue;
//
//    switch (toleranceMode) {
//        case 'fit':
//            returnValue = (l <= x1 && x2 <= r
//                && t <= y1 && y2 <= b);
//            break;
//        case 'intersect':
//            returnValue = (l < x1 + (draggable.helperProportions.width / 2) // Right Half
//                && x2 - (draggable.helperProportions.width / 2) < r // Left Half
//                && t < y1 + (draggable.helperProportions.height / 2) // Bottom Half
//                && y2 - (draggable.helperProportions.height / 2) < b ); // Top Half
//            break;
//        case 'pointer':
//            var draggableLeft = ((draggable.positionAbs || draggable.position.absolute).left + (draggable.clickOffset || draggable.offset.click).left),
//            draggableTop = ((draggable.positionAbs || draggable.position.absolute).top + (draggable.clickOffset || draggable.offset.click).top),
//            isOver = $.ui.isOver(draggableTop, draggableLeft, t, l, droppable.proportions.height, droppable.proportions.width);
//            returnValue = isOver;
//            break;
//        case 'touch':
//            returnValue = (
//                (y1 >= t && y1 <= b) ||	// Top edge touching
//                (y2 >= t && y2 <= b) ||	// Bottom edge touching
//                (y1 < t && y2 > b)		// Surrounded vertically
//                ) && (
//                (x1 >= l && x1 <= r) ||	// Left edge touching
//                (x2 >= l && x2 <= r) ||	// Right edge touching
//                (x1 < l && x2 > r)		// Surrounded horizontally
//                );
//            break;
//        default:
//            returnValue = false;
//            break;
//    }
//    //$('#offset').append('x1,y1,x2,y2 => intersect? ' + x1 + ',' + y1 + ',' + x2 + ',' + y2 + ' => ' + returnValue);
//    if (returnValue) console.log('droppable,x1,y1,x2,y2 => intersect? ' + droppable.element.attr('structr_element_id') + ',' + x1 + ',' + y1 + ',' + x2 + ',' + y2 + ':' + toleranceMode);
//
////    if (returnValue) {
////        console.log('draggable', draggable);
////        console.log('droppable', droppable);
////        console.log('toleranceMode', toleranceMode);
////        console.log('iframeOffset', iframeOffset);
////    }
//
//    return returnValue;
//
//};

$.fn.reverse = [].reverse;