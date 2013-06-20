/*
 *  Copyright (C) 2010-2013 Axel Morgner
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

$(function() {
    var s = new StructrPage('/structr/rest/');

    if (urlParam('edit')) {
        s.editable(true);

        $('.editButton').text('Stop editing').on('click', function() {
            window.location.href = window.location.href.split('?')[0];
        });
    } else {
        $('.editButton').on('click', function() {
            var btn = $(this);
            if (!urlParam('edit')) {
                window.location.href = '?edit=1';
            }
        });
    }

    $('.deleteButton').on('click', function() {
        var btn = $(this);
        var id = btn.prop('id').substring(7);
        console.log('Delete', id);
        s.delete(id);
    });

});

function StructrPage(baseUrl) {
    var s = this;
    this.edit = false;
    this.field = function(el) {
        var type = el.attr('data-structr-type'), id = el.attr('data-structr-id'), key = el.attr('data-structr-key');
        var val;
        if (type === 'Boolean') {
            if (el.is('input')) {
                val = el.is(':checked');
            } else {
                val = el.text() === 'true';
            }
        } else {
            val = el.val() ? el.val() : el.text();
        }
        return {'id': id, 'type': type, 'key': key, 'val': val};
    };
    this.create = function(type, data) {
        console.log('Create', type, data, '/structr/rest/' + type.toLowerCase());
        $.ajax({
            url: '/structr/rest/' + type.toLowerCase(), method: 'POST', contentType: 'application/json',
            data: data,
            statusCode: {201: function() {
                    window.location.reload();
                }}
        });
    };
    this.delete = function(id) {
        console.log('Delete', '/structr/rest/' + id);
        $.ajax({
            url: '/structr/rest/' + id, method: 'DELETE', contentType: 'application/json',
            statusCode: {200: function() {
                    window.location.reload();
                }}
        });
    };
    this.save = function(f, b) {
        var obj = {};
        obj[f.key] = f.val;
        if (b)
            b.html('<img src="/structr/img/al.gif"> Saving');
        $.ajax({url: baseUrl + f.id, method: 'PUT', contentType: 'application/json', data: JSON.stringify(obj),
            statusCode: {200: function() {
                    if (b)
                        b.text('Success!').remove();
                    //window.setTimeout(function() {
                    //  window.history.pushState(null, null, window.location.href.split('?')[0]);
                    //  s.editable(false);
                    //}, 1500);
                }}});
    };
    this.editable = function(enable) {
        if (enable === undefined)
            return s.edit;
        s.edit = enable;
        if (enable) {
            console.log('enabling editable mode');
            $('.createButton').show();
            $('.deleteButton').show();
            $('[data-structr-type="Date"],[data-structr-type="String"],[data-structr-type="Integer"],[data-structr-type="Boolean"]').each(function() {
                if (!$(this).closest('body').length)
                    return;
                var sp = $(this), f = s.field(sp), p = sp.parent('a');
                if (p) {
                    p.attr('href', '').css({textDecoration: 'none'}).on('click', function() {
                        return false;
                    });
                }
                var i; //console.log(f.id, f.type, f.key, f.val);
                if (f.type === 'Boolean') {
                    i = '<input type="checkbox" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '" data-structr-key="' + f.key + '" ' + (f.val ? 'checked="checked"' : '') + '">';
                }
                else {
                    i = '<input class="editField" type="text" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '" data-structr-key="' + f.key + '" value="' + (f.val === 'null' ? '' : f.val) + '" size="' + f.val.length + ' ">';
                }
                sp.replaceWith(i);
                if (f.type === 'Boolean') {
                    $('[data-structr-id="' + f.id + '"][data-structr-key="' + f.key + '"]').on('change', function(e) {
                        s.save(s.field($(this)));
                    });
                } else {
                    $('[data-structr-id="' + f.id + '"][data-structr-key="' + f.key + '"]').on('keyup', function(e) {
                        console.log('keyup');
                        var k = e.which, b = $('#save_' + f.id + '_' + f.key);
                        if (k === 13 && b.length) {
                            b.click();
                            return;
                        }
                        if ((k < 46 && k > 32) || (k > 9 && k < 32))
                            return false;
                        var inp = $(this), p = inp.parent('a');
                        inp.prop('size', inp.val().length);
                        if (!b.length) {
                            (p.length ? p : inp).after(' <button class="saveButton" id="save_' + f.id + '_' + f.key + '">Save</button>');
                            $('#save_' + f.id + '_' + f.key).on('click', function() {
                                var btn = $(this), inp = btn.prev();
                                s.save(s.field(inp), btn);
                            });
                        }
                    });
                }
            });
        } else {
            $('.createButton').hide();
            $('.deleteButton').hide();
            $('[data-structr-type="Date"],[data-structr-type="String"],[data-structr-type="Integer"],[data-structr-type="Boolean"]').each(function() {
                var inp = $(this), f = s.field(inp), p = inp.parent('a');
                console.log(inp, f, p);
                inp.next('button').remove();
                if (p) {
                    p.attr('href', f.val).css({textDecoration: ''}).on('click', function() {
                        return true;
                    });
                }
                inp.replaceWith('<span type="text" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '" data-structr-key="' + f.key + '">' + f.val + '</span>');
            });
        }
        return s.edit;
    };
}
;
function urlParam(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var res = regex.exec(window.location.href);
    return (res && res.length ? res[1] : '');
}