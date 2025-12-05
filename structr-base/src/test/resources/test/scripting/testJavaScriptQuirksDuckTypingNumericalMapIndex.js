$.store('testStore', {
	'01': 'valueAtZeroOne',
	'2' : 'valueAtTwo'
});

let x = $.retrieve('testStore');

(x['2'] === 'valueAtTwo');
