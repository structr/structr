/*
 *  Copyright (C) 2011 Axel Morgner
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
    var con = Structr.confirmation('Delete ' + entity.name + ' [' + entity.id + ']?', function() {
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

function editContent(button, resourceId, contentId) {
    if (isDisabled(button)) return;
    var div = $('.' + resourceId + '_ .' + contentId + '_');
    div.append('<div class="editor"></div>');
    var contentBox = $('.editor', div);
    disable(button, function() {
        contentBox.remove();
        enable(button, function() {
            editContent(button, resourceId, contentId);
        });
    });
    var codeMirror;
    var url = rootUrl + 'content' + '/' + contentId;
    $.ajax({
        url: url,
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
        headers: headers,
        success: function(data) {
            codeMirror = CodeMirror(contentBox.get(0), {
                value: unescapeTags(data.result.content),
                mode:  "htmlmixed",
                lineNumbers: true,
                onKeyEvent: function() {
                    //console.log(keyEventBlocked);
                    if (keyEventBlocked) {
                        clearTimeout(keyEventTimeout);
                        keyEventTimeout = setTimeout(function() {
                            var fromCodeMirror = escapeTags(codeMirror.getValue());
                            var content = $.quoteString(fromCodeMirror);
                            var data = '{ "content" : ' + content + ' }';
                            //console.log(data);
                            $.ajax({
                                url: url,
                                //async: false,
                                type: 'PUT',
                                dataType: 'json',
                                contentType: 'application/json; charset=utf-8',
                                headers: headers,
                                data: data,
                                success: function(data) {
                                    refreshIframes();
                                    keyEventBlocked = true;
                                //enable(button);
                                }
                            });
                        }, 500);
                        return;
                    }
                }
            });
        }
    });
}

function refreshIframes() {
    $('.preview_box iframe').each(function() {
        this.contentDocument.location.reload(true);
    });
}

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
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function unescapeTags(str) {
    return str.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
}