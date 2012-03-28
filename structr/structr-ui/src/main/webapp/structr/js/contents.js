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

var contents, editor;

$(document).ready(function() {
    Structr.registerModule('contents', _Contents);
});

var _Contents = {

    icon : 'icon/page_white.png',
    add_icon : 'icon/page_white_add.png',
    delete_icon : 'icon/page_white_delete.png',
	
    init : function() {
        Structr.classes.push('content');
    },

    onload : function() {
        if (debug) console.log('onload');
        if (palette) palette.remove();
        main.append('<div id="contents"></div>');

        contents = $('#contents');
        _Contents.refresh();
        contents.show();
    },

    refresh : function() {
        contents.empty();
        if (_Contents.show()) {
            contents.append('<button class="add_content_icon button"><img title="Add Content" alt="Add Content" src="' + _Contents.add_icon + '"> Add Content</button>');
            $('.add_content_icon', main).on('click', function() {
                _Contents.addContent(this);
            });
        }
    },

    show : function() {
        if (palette) {
            palette.append('<div class="elementGroup"><h3>Content</h3><div class="draggable content" id="add_content">content</div></div>');
            $('#add_content', palette).draggable({
                iframeFix: true,
                revert: 'invalid',
                containment: 'body',
                zIndex: 1,
                helper: 'clone'
            });
        }

        return _Entities.showEntities('Content');
    },

    appendContentElement : function(content, parentId, resourceId) {
        if (debug) console.log('Contents.appendContentElement: parentId: ' + parentId + ', resourceId: ' + resourceId);

        var parent = Structr.findParent(parentId, resourceId, contents);

        var text = (content.content ? content.content.substring(0,200) : '-- empty --');

        parent.append('<div class="node content ' + content.id + '_">'
            + '<img class="typeIcon" src="'+ _Contents.icon + '">'
            + '<b class="content_">' + text + '</b> <span class="id">' + content.id + '</span>'
            + '</div>');
        var div = $('.' + content.id + '_', parent);

        div.append('<img title="Edit ' + content.name + ' [' + content.id + ']" alt="Edit ' + content.name + ' [' + content.id + ']" class="edit_icon button" src="icon/pencil.png">');
        $('.edit_icon', div).on('click', function() {
            Structr.dialog('Edit content of ' + content.id, function() {
                console.log('content saved')
            }, function() {
                console.log('cancelled')
            });
            _Contents.editContent(this, content, 'all', $('#dialogText'));
        });

        div.append('<img title="Delete content \'' + content.name + '\'" alt="Delete content \'' + content.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function() {
            _Contents.deleteContent(this, content);
        });
        //        div.append('<img class="add_icon button" title="Add Element" alt="Add Element" src="icon/add.png">');
        //        $('.add_icon', div).on('click', function() {
        //            Resources.addElement(this, resource);
        //        });
        $('b', div).on('click', function() {
            Structr.dialog('Edit Properties of ' + content.id, function() {
                console.log('save')
            }, function() {
                console.log('cancelled')
            });
            _Entities.showProperties(this, content, 'all', $('#dialogText'));
        });

        //        $.ajax({
        //            url: rootUrl + 'contents/' + content.id + '/out',
        //            dataType: 'json',
        //            contentType: 'application/json; charset=utf-8',
        //            success: function(data) {
        //                if (debug) console.log(data);
        //                $(data).each(function(i,v) {
        //                    if (debug) console.log(v); //TODO: display information about relationship
        //                });
        //
        //            }
        //        });
        return div;
    },

    addContent : function(button) {
        return _Entities.add(button, 'Content');
    },

    deleteContent : function(button, content) {
        if (debug) console.log('delete content ' + content);
        deleteNode(button, content);
    },
	
    patch : function(id, patch) {
        var command = '{ "command" : "PATCH" , "id" : "' + id + '", "data" : { "patch" : ' + patch + '} }';
        if (debug) console.log(command);
        return send(command);
    },

    editContent : function (button, entity, view, element) {
        if (isDisabled(button)) return;
        var div = element.append('<div class="editor"></div>');
        if (debug) console.log(div);
        //var div = $('.' + resourceId + '_ .' + contentId + '_');
        var contentBox = $('.editor', element);
        //        disable(button, function() {
        //            contentBox.remove();
        //            enable(button, function() {
        //                //editContent(button, resourceId, contentId);
        //                });
        //        });
        editor = CodeMirror(contentBox.get(0), {
            value: unescapeTags(entity.content),
            mode:  "htmlmixed",
            lineNumbers: true,
            onChange: function(cm, changes) {
				
				var text1 = entity.content;
				var text2 = editor.getValue();
				
				var p = dmp.patch_make(text1, text2);
				var strp = dmp.patch_toText(p);
				console.log(strp, $.quoteString(strp));
				_Contents.patch(entity.id, $.quoteString(strp));
//
//                var selection = window.getSelection();
//                console.log(selection);
//                if (selection.rangeCount) {
//					console.log(selection.rangeCount);
//                    selStart = selection.getRangeAt(selection.rangeCount).startOffset;
//                    selEnd = selection.getRangeAt(selection.rangeCount).endOffset;
//                    console.log(selStart, selEnd);
//                }





                //var content = escapeTags(editor.getValue());
                //_Entities.setProperty(entity.id, 'content', content);
            }
        });

        editor.id = entity.id;


    //        var url = rootUrl + 'content' + '/' + entity.id;
    //        if (debug) console.log(url);
    //        $.ajax({
    //            url: url,
    //            dataType: 'json',
    //            contentType: 'application/json; charset=utf-8',
    //            headers: headers,
    //            success: function(data) {
    //                codeMirror = CodeMirror(contentBox.get(0), {
    //                    value: unescapeTags(data.result.content),
    //                    mode:  "htmlmixed",
    //                    lineNumbers: true,
    //                    onKeyEvent: function() {
    //                        //console.log(keyEventBlocked);
    //                        if (keyEventBlocked) {
    //                            clearTimeout(keyEventTimeout);
    //                            keyEventTimeout = setTimeout(function() {
    //                                var fromCodeMirror = escapeTags(codeMirror.getValue());
    //                                var content = $.quoteString(fromCodeMirror);
    //                                var data = '{ "content" : ' + content + ' }';
    //                                //console.log(data);
    //                                $.ajax({
    //                                    url: url,
    //                                    //async: false,
    //                                    type: 'PUT',
    //                                    dataType: 'json',
    //                                    contentType: 'application/json; charset=utf-8',
    //                                    headers: headers,
    //                                    data: data,
    //                                    success: function(data) {
    //                                        _Resources.reloadPreviews();
    //                                        keyEventBlocked = true;
    //                                    //enable(button);
    //                                    }
    //                                });
    //                            }, 500);
    //                            return;
    //                        }
    //                    }
    //                });
    //            }
    //        });
    }
};