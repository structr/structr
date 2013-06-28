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

var altKey = false, ctrlKey = false, shiftKey = false, eKey = false;

$(function() {
    var s = new StructrPage('/structr/rest/');
    console.log(urlParam('edit'))
    if (urlParam('edit') !== undefined) {
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
        //console.log('create button clicked', type);
        s.create(btn, type, null, sourceId, sourceType, relatedProperty);
    });

    $('.deleteButton').on('click', function() {
        var btn = $(this);
        var id = btn.attr('data-structr-id');
        //console.log('Delete', id);
        s.delete(id);
    });

    $(window).on('keydown', function(e) {
        var k = e.which;
        //console.log('before down', k, altKey, ctrlKey, shiftKey, eKey)
        if (k === 16)
            shiftKey = true;
        if (k === 17)
            altKey = true;
        if (k === 18)
            ctrlKey = true;
        if (k === 69)
            eKey = true;
        //console.log('after down', k, altKey, ctrlKey, shiftKey, eKey)
    });

    $(window).on('keyup', function(e) {
        var k = e.which;
        //console.log('before up', k, altKey, ctrlKey, shiftKey, eKey)
        if (k === 16)
            shiftKey = false;
        if (k === 17)
            altKey = false;
        if (k === 18)
            ctrlKey = false;
        if (k === 69)
            eKey = false;
        //console.log('after up', k, altKey, ctrlKey, shiftKey, eKey)
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
            val = el.val() ? el.val() : el.html().replace(/<br>/gi, '\n');
        }
        return {'id': id, 'type': type, 'key': key, 'val': val};
    };
    this.create = function(btn, type, data, sourceId, sourceType, relatedProperty) {
        console.log('Create', type, data, '/structr/rest/' + type.toUnderscore(), sourceId, relatedProperty);
        $.ajax({
            url: '/structr/rest/' + type.toUnderscore(), method: 'POST', contentType: 'application/json',
            data: JSON.stringify(data),
            statusCode: {
                201: function(x) {

                    if (sourceId && relatedProperty) {

                        var location = x.getResponseHeader('location');
                        var id = location.substring(location.lastIndexOf('/') + 1);
                        var d = JSON.parse('{"' + relatedProperty + '":[{"id":"' + id + '"}]}');


                        $.ajax({
                            url: '/structr/rest/' + sourceType.toUnderscore() + '/' + sourceId, method: 'GET', contentType: 'application/json',
                            statusCode: {
                                200: function(data) {
                                    //console.log(data.result);
                                    if (data.result && data.result[relatedProperty].length) {
                                        $.each(data.result[relatedProperty], function(i, obj) {
                                            d[relatedProperty].push({'id': obj.id});
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
                    btn.off('click');
                    var errors = JSON.parse(d.responseText).errors[type];
                    //console.log(btn, type, errors);
                    var data = {};
                    $.each(Object.keys(errors), function(i, key) {
                        var label = key.splitAndTitleize('_');
                        var msg = errors[key].join(',').splitAndTitleize('_');
                        $('<label class="structr-label">' + label + '</label> <input class="structr-input" type="text" data-structr-prop="' + key + '" placeholder="' + msg + '">').insertBefore(btn);
                    });

                    if (type === 'Image') {
                        console.log(window.File, window.FileReader, window.FileList, window.Blob);
                        if (window.File && window.FileReader && window.FileList && window.Blob) {

                            $('<div id="dropArea">Drag and drop image(s) here</div>').
                                    css({
                                        border: '1px solid #ccc',
                                        width: '8em',
                                        height: '3em'
                                    }).insertBefore(btn);

                            var drop = $('#dropArea');
                            //console.log(drop);

                            drop.on('dragover', function(event) {
                                event.originalEvent.dataTransfer.dropEffect = 'copy';
                                return false;
                            });
                            
                            drop.on('drop', function(event) {

                                if (!event.originalEvent.dataTransfer) {
                                    return;
                                }

                                event.stopPropagation();
                                event.preventDefault();

                                fileList = event.originalEvent.dataTransfer.files;

                                $(fileList).each(function(i, file) {
                                    $('[data-structr-prop=name]').val(file.name);
                                    var reader = new FileReader();
                                    reader.readAsBinaryString(file);

                                    reader.onload = function(f) {

                                        console.log('File was read into memory.');
                                        var binaryContent = f.target.result;
                                        console.log('uploadFile: binaryContent', binaryContent);
                                        
                                        data.imageData = 'data:' + file.type + ';base64,' + window.btoa(binaryContent);
                                        data.name = file.name;
                                        
                                        console.log(data);
                                        
                                        s.create(btn, type, data);

                                    }

                                });

                            });



                        }

                        //$('<label class="structr-label">Base64 Data</label> <input class="structr-input" type="text" data-structr-prop="imageData" placeholder="Paste here">').insertBefore(btn);
                    }

                    btn.on('click', function() {
                        $.each(btn.parent().children('.structr-input'), function() {
                            var inp = $(this);
                            var key = inp.attr('data-structr-prop');
                            var val = inp.val();
                            //console.log('collecting data', inp, key, val);
                            data[key] = val;
                        });
                        s.create(btn, type, data);
                    });

                }
            }
        });
    };
    this.delete = function(id) {
        //console.log('Delete', '/structr/rest/' + id);
        $.ajax({
            url: '/structr/rest/' + id, method: 'DELETE', contentType: 'application/json',
            statusCode: {200: function() {
                    window.location.reload();
                }}
        });
    };
    this.save = function(f, b) {
        //console.log(f, b);
        var obj = {};
        obj[f.key] = f.val;
        if (b)
            b.html('<img src="/structr/img/al.gif"> Saving');
        $.ajax({url: baseUrl + f.id, method: 'PUT', contentType: 'application/json', data: JSON.stringify(obj),
            statusCode: {
                200: function() {
                    if (b) {
                        b.text('Success!').remove();
                    }
                }
            }
        });
    };
    this.editable = function(enable) {
        if (enable === undefined)
            return s.edit;
        s.edit = enable;
        if (enable) {
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
                    if (f.val.indexOf('\n') === -1) {
                        i = '<input class="editField" type="text" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '" data-structr-key="' + f.key + '" value="' + (f.val === 'null' ? '' : f.val) + '" size="' + f.val.length + ' ">';
                    } else {
                        i = '<textarea data-structr-id="' + f.id + '" data-structr-key="' + f.key + '">' + (f.val === 'null' ? '' : f.val) + '\n' + '</textarea>';
                    }
                }

                sp.replaceWith(i);
                var input = $('[data-structr-id="' + f.id + '"][data-structr-key="' + f.key + '"]')
                input.css({fontFamily: 'sans-serif'});

                if (f.type === 'Boolean') {
                    input.on('change', function(e) {
                        s.save(s.field($(this)));
                    });
                } else {
                    input.on('keyup', function(e) {
                        s.checkInput(e, f, $(this));
                    });
                }
            });
        } else {
            $('.createButton').hide();
            $('.deleteButton').hide();
            $('[data-structr-type="Date"],[data-structr-type="String"],[data-structr-type="Integer"],[data-structr-type="Boolean"]').each(function() {
                var inp = $(this), f = s.field(inp), p = inp.parent('a');
                //console.log(inp, f, p);
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
    this.checkInput = function(e, f, inp) {
        var k = e.which, b = $('#save_' + f.id + '_' + f.key);
        var p = inp.parent('a');

        if (isTextarea(inp[0])) {

            // Check for line break
            if (inp.val().indexOf('\n') === -1) {

                inp.replaceWith('<input class="editField" type="text" data-structr-id="' + f.id + '" data-structr-type="' + f.type + '" data-structr-key="' + f.key + '" value="' + (inp.val() === 'null' ? '' : inp.val()) + '" size="' + inp.val().length + ' ">');
                inp = $('[data-structr-id="' + f.id + '"][data-structr-key="' + f.key + '"]');
                inp.on('keyup', function(e) {
                    s.checkInput(e, f, $(this));
                });

                setCaretToEnd(inp[0]);
            }

        } else {

            // Normal input field here
            if (k === 13) {

                if (shiftKey === true) {

                    // Shift-return in input field => make textarea and append line break

                    inp.replaceWith('<textarea data-structr-id="' + f.id + '" data-structr-key="' + f.key + '">' + inp.val() + '\n' + '</textarea>');
                    inp = $('[data-structr-id="' + f.id + '"][data-structr-key="' + f.key + '"]');
                    inp.on('keyup', function(e) {
                        s.checkInput(e, f, $(this));
                    });

                    inp.css({fontFamily: 'sans-serif'});

                    setCaretToEnd(inp[0]);

                } else {

                    // Return without shift => submit button if visible

                    if (b && b.length) {
                        b.click();
                        return true;
                    }

                }
            }
        }

        if (!(k === 13 && shiftKey) && ((k < 46 && k > 32) || (k > 9 && k < 32))) {
            //console.log('keyup', k, 'return')
            return true;
        }

        inp.prop('size', inp.val() ? inp.val().length : inp.text().length);

        if (!b.length) {
            (p.length ? p : inp).after(' <button class="saveButton" id="save_' + f.id + '_' + f.key + '">Save</button>');
            $('#save_' + f.id + '_' + f.key).on('click', function() {
                var btn = $(this), inp = btn.prev();
                s.save(s.field(inp), btn);
            });
        }

    }
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
        return (offset > 0 ? '_' : '') + m.toLowerCase();
    });
};

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