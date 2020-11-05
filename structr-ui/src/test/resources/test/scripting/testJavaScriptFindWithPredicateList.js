function test() {

	let predicates = [];
	let equals     = $['find.equals'];
	let contains   = $['find.contains'];

	predicates.push(equals('aString', 'string01'));
	predicates.push(contains('aString', '3'));

	return $.find('TestOne', $.or(predicates), $.sort('aString'));
}

let _structrMainResult = test();