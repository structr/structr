$.store('testStore', {
	'01': 'valueAtZeroOne',
	'2' : 'valueAtTwo'
});

let x = $.retrieve('testStore');

return (x['2'] === 'valueAtTwo');
