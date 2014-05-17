/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
    icon: 'icon/page_white.png',
    comment_icon: 'icon/comment.png',
    add_icon: 'icon/page_white_add.png',
    delete_icon: 'icon/page_white_delete.png',
    appendContentElement: function(content, refNode) {
        log('Contents.appendContentElement', content, refNode);

        var parent;

        if (content.parent && content.parent.id) {
            parent = Structr.node(content.parent.id);
            _Entities.ensureExpanded(parent);
        } else {
            parent = elements;
        }

        if (!parent)
            return false;

        var isActiveNode = content.hideOnIndex || content.hideOnDetail || content.hideConditions || content.showConditions || content.dataKey;

        var html = '<div id="id_' + content.id + '" class="node content ' + (isActiveNode ? ' activeNode' : 'staticNode') + '">'
                + '<img class="typeIcon" src="' + (content.type === 'Comment' ? _Contents.comment_icon : _Contents.icon) + '">'
                + '<div class="content_">' + escapeTags(content.content) + '</div> <span class="id">' + content.id + '</span>'
                + '</div>';

        if (refNode) {
            refNode.before(html);
        } else {
            parent.append(html);
        }

        var div = Structr.node(content.id);

        _Dragndrop.makeSortable(div);
        _Dragndrop.makeDroppable(div);

        _Entities.appendAccessControlIcon(div, content);

        div.append('<img title="Delete content \'' + content.name + '\'" alt="Delete content \'' + content.name + '\'" class="delete_icon button" src="' + Structr.delete_icon + '">');
        $('.delete_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Entities.deleteNode(this, content);
        });

        div.append('<img title="Edit Content" alt="Edit Content of ' + content.id + '" class="edit_icon button" src="icon/pencil.png">');
        $('.edit_icon', div).on('click', function(e) {
            e.stopPropagation();
            _Contents.openEditContentDialog(this, content);
            return false;
        });

        $('.content_', div).on('click', function(e) {
            e.stopPropagation();
            _Contents.openEditContentDialog(this, content);
            return false;
        });

        _Entities.appendEditPropertiesIcon(div, content);

        return div;
    },
    openEditContentDialog: function(btn, entity) {
        var self = $(btn);
        var text = self.parent().find('.content_').text();
        Structr.dialog('Edit content of ' + entity.id, function() {
            log('content saved')
        }, function() {
            log('cancelled')
        });
        _Contents.editContent(this, entity, text, dialogText);
    },
    editContent: function(button, entity, text, element) {
        if (isDisabled(button))
            return;
        var div = element.append('<div class="editor"></div>');
        log(div);
        var contentBox = $('.editor', element);
        contentType = contentType ? contentType : entity.contentType;
        //alert(contentType);
        var text1, text2, timer;
        editor = CodeMirror(contentBox.get(0), {
            value: text,
            mode: contentType,
            lineNumbers: true
        });
        editor.focus();
        Structr.resize();

        dialogBtn.append('<button id="editorSave" disabled="disabled" class="disabled">Save</button>');
        dialogBtn.append('<button id="saveAndClose" disabled="disabled" class="disabled"> Save and close</button>');

        dialogSaveButton = $('#editorSave', dialogBtn);
        var saveAndClose = $('#saveAndClose', dialogBtn);

        saveAndClose.on('click', function(e) {
            e.stopPropagation();
            dialogSaveButton.click();
            setTimeout(function() {
                dialogSaveButton.remove();
                saveAndClose.remove();
                dialogCancelButton.click();
            }, 500);
        });

        editor.on('change', function(cm, change) {

            var contentNode = Structr.node(entity.id)[0];

            text1 = $(contentNode).children('.content_').text();
            text2 = editor.getValue();

            if (text1 === text2) {
                dialogSaveButton.prop("disabled", true).addClass('disabled');
                saveAndClose.prop("disabled", true).addClass('disabled');
            } else {
                dialogSaveButton.prop("disabled", false).removeClass('disabled');
                saveAndClose.prop("disabled", false).removeClass('disabled');
            }
        });

        dialogSaveButton.on('click', function(e) {
            e.stopPropagation();

            var contentNode = Structr.node(entity.id)[0];

            text1 = $(contentNode).children('.content_').text();
            text2 = editor.getValue();

            if (!text1)
                text1 = '';
            if (!text2)
                text2 = '';

            if (debug) {
                console.log('Element', contentNode);
                console.log('text1', text1);
                console.log('text2', text2);
            }

            if (text1 === text2)
                return;
            Command.patch(entity.id, text1, text2, function() {
                dialogMsg.html('<div class="infoBox success">Content saved.</div>');
                $('.infoBox', dialogMsg).delay(2000).fadeOut(200);
                _Pages.reloadPreviews();
                dialogSaveButton.prop("disabled", true).addClass('disabled');
                saveAndClose.prop("disabled", true).addClass('disabled');
            });

        });

        //_Entities.appendBooleanSwitch(dialogMeta, entity, 'editable', 'Editable', 'If enabled, data fields in this content element are editable in edit mode.');

        var values = ['text/plain', 'text/html', 'text/css', 'text/javascript', 'text/markdown', 'text/textile', 'text/mediawiki', 'text/tracwiki', 'text/confluence'];

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