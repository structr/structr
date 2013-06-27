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

/**
 * JS library to enable in-page data editing in structr
 * 
 * The script tag to activate this file is automatically inserted into the
 * rendered page in edit mode (URL parameter 'edit=1')
 * 
 */

//var altKey, ctrlKey, shiftKey, eKey;

$(function() {
    var s = new StructrPage('/structr/rest/');

    if (urlParam('edit')) {
        s.editable(true);

        $('.editButton').text('Stop editing').on('click', function() {
            window.location.href = window.location.href.split('?')[0];
        });

    } else {
        $('.editButton').on('click', function() {
            if (!urlParam('edit')) {
                window.location.href = '?edit=1';
            }
        });
    }

    $('.createButton').on('click', function() {
        var btn = $(this);
        var type = btn.attr('data-structr-type');
        var sourceId = btn.attr('data-structr-source-id');
        var sourceType = btn.attr('data-structr-source-type');
        var relatedProperty = btn.attr('data-structr-related-property');
        console.log('create button clicked', type);
        s.create(btn, type, null, sourceId, sourceType, relatedProperty);
    });

    $('.deleteButton').on('click', function() {
        var btn = $(this);
        var id = btn.attr('data-structr-id');
        console.log('Delete', id);
        s.delete(id);
    });

//    $(window).on('keydown', function(e) {
//        var k = e.which;
//        if (k === 17) altKey = true;
//        if (k === 18) ctrlKey = true;
//        if (k === 16) shiftKey = true;
//        if (k === 69) eKey = true;
//    });
//
//    $(window).on('keyup', function(e) {
//        var k = e.which;
//        if (k === 17) altKey = false;
//        if (k === 18) ctrlKey = false;
//        if (k === 16) shiftKey = false;
//        if (k === 69) eKey = false;
//    });

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
    this.create = function(btn, type, data, sourceId, sourceType, relatedProperty) {
        console.log('Create', type, data, '/structr/rest/' + type.toUnderscore(), sourceId, relatedProperty);
        $.ajax({
            url: '/structr/rest/' + type.toUnderscore(), method: 'POST', contentType: 'application/json',
            data: JSON.stringify(data),
            statusCode: {
                201: function(x){
                    
                    if (sourceId && relatedProperty) {
                        
                        var location = x.getResponseHeader('location');
                        var id = location.substring(location.lastIndexOf('/') + 1);
                        var d = JSON.parse('{"' + relatedProperty + '":[{"id":"' + id + '"}]}');
                        
                        
                        $.ajax({
                            url: '/structr/rest/' + sourceType.toUnderscore() + '/' + sourceId, method: 'GET', contentType: 'application/json',
                            statusCode: {
                                200: function(data) {
                                    console.log(data.result);
                                    if (data.result && data.result[relatedProperty].length) {
                                        $.each(data.result[relatedProperty], function(i, obj) {
                                            d[relatedProperty].push({'id':obj.id});
                                        });
                                    }

                                    $.ajax({
                                        url: '/structr/rest/' + sourceType.toUnderscore() + '/' + sourceId, method: 'PUT', contentType: 'application/json',
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
                            
                        
                        
                    } else {
                        window.location.reload();
                    }
                    
                },
                400: function(d) {
                    console.log(d);
                },
                422: function(d) {
                    btn.parent().children('.structr-label').remove();
                    btn.parent().children('.structr-input').remove();
                    var errors = JSON.parse(d.responseText).errors[type];
                    console.log(btn, type, errors);
                    var data = {};
                    $.each(Object.keys(errors), function(i, key) {
                        var label = key.splitAndTitleize('_');
                        var msg = errors[key].join(',').splitAndTitleize('_');
                        $('<label class="structr-label">' + label + '</label> <input class="structr-input" type="text" data-structr-prop="' + key + '" placeholder="' + msg + '">').insertBefore(btn);
                    });

                    btn.on('click', function() {
                        $.each(btn.parent().children('.structr-input'), function() {
                            var inp = $(this);
                            var key = inp.attr('data-structr-prop');
                            var val = inp.val();
                            console.log('collecting data', inp, key, val);
                            d[key] = val;
                        });
                        s.create(btn, type, d);
                    });

                }
            }
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

function urlParam(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var res = regex.exec(window.location.href);
    return (res && res.length ? res[1] : '');
}

if (typeof String.prototype.capitalize !== 'function') {
    String.prototype.capitalize = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    };
}

if (typeof String.prototype.splitAndTitleize !== 'function') {
    String.prototype.splitAndTitleize = function(sep) {

        var res = new Array();
        var parts = this.split(sep);
        parts.forEach(function(part) {
            res.push(part.capitalize());
        })
        return res.join(" ");
    };
}

String.prototype.toUnderscore = function() {
    return this.replace(/([A-Z])/g, function(m, a, offset) {
        return (offset>0?'_':'') + m.toLowerCase();
    });
};
