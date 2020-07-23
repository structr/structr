function test() {

	let x = {};

	x['24'] = 'jack bauer';

	return $.toJson(x);
}

let _structrMainResult = test();