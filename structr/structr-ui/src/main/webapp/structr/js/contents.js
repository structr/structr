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

var contents, editor, contentType;

$(document).ready(function() {
    Structr.registerModule('contents', _Contents);
    Structr.classes.push('content');
});

var _Contents = {

    icon : 'icon/page_white.png',
    add_icon : 'icon/page_white_add.png',
    delete_icon : 'icon/page_white_delete.png',
	
    init : function() {
    //Structr.classes.push('content');
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
            $('.add_content_icon', main).on('click', function(e) {
                e.stopPropagation();
                var entity = {};
                entity.type = 'Content';
                Command.create(entity);
            });
        }
    },

    show : function() {
        if (palette) {
            palette.children().first().before('<div class="elementGroup"><h3>Content</h3><div class="draggable content" id="add_content">content</div></div>');
            $('#add_content', palette).draggable({
                iframeFix: true,
                revert: 'invalid',
                containment: 'body',
                zIndex: 1,
                helper: 'clone'
            });
        }

        return Command.list('Content');
    },

    appendContentElement : function(content, parentId, componentId, pageId, treeAddress) {
        if (debug) console.log('Contents.appendContentElement', content, parentId, componentId, pageId, treeAddress);

        if (treeAddress) {
            if (debug) console.log('Contents.appendContentElement: tree address', treeAddress);
            parent = $('#_' + treeAddress);
        } else {
            parent = Structr.findParent(parentId, componentId, pageId, contents);
        }
        
        if (!parent) return false;
        
        if (debug) console.log(parent);

        //	var abbrContent = (content.content ? content.content.substring(0,36) + '&hellip;': '&nbsp;');

        //var nameOrContent = content.content ? content.content : content.name;
        
        var parentPath = getElementPath(parent);
        var id = parentPath + '_' + parent.children('.node').length;
        
        parent.append('<div id="_' + id + '" class="node content ' + content.id + '_">'
            + '<img class="typeIcon" src="'+ _Contents.icon + '">'
            + '<div class="content_">' + escapeTags(content.content) + '</div> <span class="id">' + content.id + '</span>'
            //	    + '<b class="content_">' + content.content + '</b>'
            + '</div>');
        
        var pos = parent.children('.' + content.id + '_').length-1;
        if (debug) console.log('pos', content.id, pos);
        
        //var div = Structr.node(content.id, parentId, componentId, pageId, pos);
        var div = $('#_' + id);

        div.append('<img title="Delete content \'' + content.name + '\'" alt="Delete content \'' + content.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, content);
        });

        div.append('<img title="Edit ' + content.name + ' [' + content.id + ']" alt="Edit ' + content.name + ' [' + content.id + ']" class="edit_icon button" src="icon/pencil.png">');
        $('.edit_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            var text = self.parent().find('.content_').text();
            Structr.dialog('Edit content of ' + content.id, function() {
                if (debug) console.log('content saved')
            }, function() {
                if (debug) console.log('cancelled')
            });
            _Contents.editContent(this, content, text, $('#dialogBox .dialogText'));
        });
        
        $('.content_', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            var text = self.parent().find('.content_').text();
            Structr.dialog('Edit content of ' + content.id, function() {
                if (debug) console.log('content saved')
            }, function() {
                if (debug) console.log('cancelled')
            });
            _Contents.editContent(this, content, text, $('#dialogBox .dialogText'));
        });
        
        _Entities.appendEditPropertiesIcon(div, content);
        _Entities.appendAccessControlIcon(div, content);

        return div;
    },

    editContent : function (button, entity, text, element) {
        if (isDisabled(button)) return;
        var div = element.append('<div class="editor"></div>');
        if (debug) console.log(div);
        var contentBox = $('.editor', element);
        contentType = contentType ? contentType : entity.contentType;
        //alert(contentType);
        var text1, text2, timer;
        editor = CodeMirror(contentBox.get(0), {
            value: unescapeTags(text),
            mode:  contentType,
            lineNumbers: true,
            onChange: function(cm, changes) {
                
//                window.clearTimeout(timer);
//                
//                var element = $( '.' + entity.id + '_')[0];
//                
//                text1 = $(element).children('.content_').text();
//                text2 = editor.getValue();
//                
//                if (!text1) text1 = '';
//                if (!text2) text2 = '';
//		
//                if (debug) console.log('Element', element);
//                if (debug) console.log(text1);
//                if (debug) console.log(text2);
//                
//                if (text1 == text2) return;
//                editorCursor = cm.getCursor();
//                if (debug) console.log(editorCursor);
//
//                //timer = window.setTimeout(function() {
//                Command.patch(entity.id, text1, text2);
//            //}, 5000);
				
            }
        });
        
        element.append('<button id="editorSave">Save</button>');
        $('#editorSave', element).on('click', function() {
            window.clearTimeout(timer);
                
            var contentNode = $( '.' + entity.id + '_')[0];
                
            text1 = $(contentNode).children('.content_').text();
            text2 = editor.getValue();
                
            if (!text1) text1 = '';
            if (!text2) text2 = '';
		
            if (debug) {
                console.log('Element', contentNode);
                console.log(text1);
                console.log(text2);
            }
                
            if (text1 == text2) return;
//            editorCursor = cm.getCursor();
//            if (debug) console.log(editorCursor);

            //timer = window.setTimeout(function() {
            Command.patch(entity.id, text1, text2);
        //}, 5000);
            
        });
        
        $('#dialogBox .dialogMeta').append('<table class="props ' + entity.id + '_"></table>');
        var t = $('.props', $('#dialogBox .dialogMeta'));
        
        t.append('<tr><td><label for="contentTypeSelect">Content-Type:</label></td><td>'
            + '<select class="contentType_" id="contentTypeSelect">'
            + '<option value="text/plain">text/plain</option>'
            + '<option value="text/html">text/html</option>'
            + '<option value="text/css">text/css</option>'
            + '<option value="text/javascript">text/javascript</option>'
            + '<option value="text/markdown">text/markdown</option>'
            + '<option value="text/textile">text/textile</option>'
            + '<option value="text/mediawiki">text/mediawiki</option>'
            + '<option value="text/tracwiki">text/tracwiki</option>'
            + '<option value="text/confluence">text/confluence</option>'
            + '</select>'
            + '</td></tr>');
        
        Command.getProperty(entity.id, 'contentType', '#dialogBox');
        var select = $('#contentTypeSelect', t);
        select.on('change', function() {
            contentType = select.val();
            Command.setProperty(entity.id, 'contentType', contentType);
        });
        
        t.append('<tr><td><label for="data-key">Data Key:</label></td><td><input id="dataKey" class="data-key_" name="data-key" size="20" value=""></td></tr>');
        Command.getProperty(entity.id, 'data-key', '#dialogBox');
        var dataKeyInput = $('#dataKey', t);
        dataKeyInput.on('blur', function() {
            Command.setProperty(entity.id, 'data-key', dataKeyInput.val());
        });

        _Entities.appendSimpleSelection($('#dialogBox .dialogMeta'), entity, 'type_definitions', 'Data Type', 'typeDefinitionId');

        editor.id = entity.id;

    }
};