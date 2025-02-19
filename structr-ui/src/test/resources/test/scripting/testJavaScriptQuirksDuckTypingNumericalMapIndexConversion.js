function test() {

	let x = {};

	x['24'] = 'jack bauer';

	return $.toJson(x);
}

test();