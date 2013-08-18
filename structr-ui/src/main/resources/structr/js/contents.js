/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

var _Contents = {

    icon : 'icon/page_white.png',
    add_icon : 'icon/page_white_add.png',
    delete_icon : 'icon/page_white_delete.png',
	
    appendContentElement : function(content, refNode) {
        log('Contents.appendContentElement', content, refNode);

        var parent;
        
        if (content.parent && content.parent.id) {
            parent = Structr.node(content.parent.id);
            _Entities.ensureExpanded(parent);
        } else {
            parent = elements;
        }
        
        if (!parent) return false;
        
        var html = '<div id="id_' + content.id + '" class="node content">'
        + '<img class="typeIcon" src="'+ _Contents.icon + '">'
        + '<div class="content_">' + escapeTags(content.content) + '</div> <span class="id">' + content.id + '</span>'
        + '</div>';
        
        if (refNode) {
            refNode.before(html);
        } else {
            parent.append(html);
        }
        
        var div = Structr.node(content.id);
    
        var page = div.closest( '.page')[0];
        if (!page && pages) {
            div.draggable({
                revert: 'invalid',
                //containment: '#pages',
                stack: 'div',
                //helper: 'clone',
                start: function(event, ui) {
                    $(this).draggable(disable);
                }
            });
        }
        
        _Entities.appendAccessControlIcon(div, content);

        div.append('<img title="Delete content \'' + content.name + '\'" alt="Delete content \'' + content.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, content);
        });

        div.append('<img title="Edit Content" alt="Edit Content of ' + content.id + '" class="edit_icon button" src="icon/pencil.png">');
        $('.edit_icon', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            var text = self.parent().find('.content_').text();
            Structr.dialog('Edit content of ' + content.id, function() {
                log('content saved')
            }, function() {
                log('cancelled')
            });
            _Contents.editContent(this, content, text, dialogText);
        });
        
        $('.content_', div).on('click', function(e) {
            e.stopPropagation();
            var self = $(this);
            var text = self.parent().find('.content_').text();
            Structr.dialog('Edit content of ' + content.id, function() {
                log('content saved')
            }, function() {
                log('cancelled')
            });
            _Contents.editContent(this, content, text, dialogText);
        });
        
        _Entities.appendEditPropertiesIcon(div, content);

        return div;
    },

    editContent : function (button, entity, text, element) {
        if (isDisabled(button)) return;
        var div = element.append('<div class="editor"></div>');
        log(div);
        var contentBox = $('.editor', element);
        contentType = contentType ? contentType : entity.contentType;
        //alert(contentType);
        var text1, text2, timer;
        editor = CodeMirror(contentBox.get(0), {
            value: unescapeTags(text),
            mode:  contentType,
            lineNumbers: true
        });
        editor.focus();
        
        dialogBtn.append('<button id="editorSave">Save</button>');
        dialogBtn.append('<button id="saveAndClose"> Save and close</button>');
        
        var saveBtn = $('#editorSave', dialogBtn);
        var saveAndClose = $('#saveAndClose', dialogBtn);
        
        $('button#saveAndClose', dialogBtn).on('click', function(e) {
            e.stopPropagation();
            saveBtn.click();
            setTimeout(function() {
                dialogCancelButton.click();
            }, 100);
            saveBtn.remove();
            saveAndClose.remove();
            
        });

        saveBtn.on('click', function() {
     
            var contentNode = Structr.node(entity.id)[0];
                
            text1 = $(contentNode).children('.content_').text();
            text2 = editor.getValue();
                
            if (!text1) text1 = '';
            if (!text2) text2 = '';
		
            if (debug) {
                console.log('Element', contentNode);
                console.log('text1', text1);
                console.log('text2', text2);
            }
                
            if (text1 === text2) return;
            Command.patch(entity.id, text1, text2, function() {
                _Pages.reloadPreviews();
                dialogMsg.html('<div class="infoBox success">Content saved.</div>');
                $('.infoBox', dialogMsg).delay(2000).fadeOut(200);
            });
            
        });

        dialogMeta.append('<div class="' + entity.id + '_"><button class="switch disabled editable_">Editable</button> If enabled, data fields in this content element are editable in edit mode.</div>');
        var editableSwitch = $('.editable_');
        _Entities.changeBooleanAttribute(editableSwitch, entity.editable);
        editableSwitch.on('click', function(e) {
            e.stopPropagation();
            entity.setProperty('editable', editableSwitch.hasClass('disabled'), false, function() {
                _Entities.changeBooleanAttribute(editableSwitch, entity.editable);
                blinkGreen(editableSwitch);
            });
        });
        
        var values = [ 'text/plain', 'text/html', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence'];
        
        dialogMeta.append('<label for="contentTypeSelect">Content-Type:</label><select class="contentType_" id="contentTypeSelect"></select>');
        var select = $('#contentTypeSelect', dialogMeta);
        $.each(values, function(i, type) {
            select.append('<option ' + (type === entity.contentType ? 'selected' : '') + ' value="' + type + '">' + type + '</option>');
        });
        select.on('change', function() {
            contentType = select.val();
            entity.setProperty('contentType', contentType, false, function() {
                _Pages.reloadPreviews();
            });
        });
        
        editor.id = entity.id;

    }
};