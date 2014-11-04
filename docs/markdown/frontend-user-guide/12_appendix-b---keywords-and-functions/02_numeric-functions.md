<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>

<tr><td>add(value1, value2, ...)</td><td>Returns the sum of all values</td><td><code>${add(12, 34, 56)} => 102</code></td></tr>
<tr><td>subt(startValue, value1, ...)</td><td>Subtracts the second and all following values from the first</td><td><code>${subt(102, 34, 56)} => 12</code></td></tr>
<tr><td>mult(value1, value2, ...)</td><td>Returns the product of all values</td><td><code>${mult(1, 2, 3)} => 6</code></td></tr>
<tr><td>quot(startValue, value1, ...)</td><td>Returns the quotient of the two values</td><td><code>${quot(10, 4)} => 2.5</code></td></tr>
<tr><td>round(value, digits)</td><td>Rounds the given value to the given number of fractional digits</td><td><code>${round(10.4567, 2)} => 10.46</code></td></tr>
<tr><td>max(value1, value2)</td><td>Returns the maximum of the two values</td><td><code>${max(10, 20)} => 20</code></td></tr>
<tr><td>min(value1, value2)</td><td>Returns the minimum of the two values</td><td><code>${max(10, 20)} => 10</code></td></tr>
<tr><td>num(string)</td><td>Tries to convert the given value into a floating-point number</td><td><code>${num('123'} => 123.0</code></td></tr>
<tr><td>int(string)</td><td>Tries to convert the given value into an integer</td><td><code>${int('123'} => 123</code></td></tr>

</table>
