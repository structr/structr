<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>

<tr><td>if(expression, trueExpression, falseExpression)</td><td>Evaluates the given expression and executes trueExpression or falseExpression depending on the evaluation</td><td><code>${if(true, 'string1', 'string2')} => string1</code></td></tr>
<tr><td>and(expr1, expr2, ...)</td><td>Returns true if all expressions are true</td><td><code>${and(true,true,true)} => true ; ${and(true,true,false)} => false</code></td></tr>
<tr><td>or(expr1, expr2, ...)</td><td>Returns true if at least one of the expressions is true</td><td><code>${or(false,true,false)} => true ; ${or(false,false,false)} => false</code></td></tr>
<tr><td>not(expr)</td><td>Returns false if the expression is true</td><td><code>${not(true)} => false</code></td></tr>
<tr><td>empty(string)</td><td>Returns true if the value is empty (empty string or null)</td><td><code>${empty('')} => true</code></td></tr>
<tr><td>equal(value1, value2)</td><td>Returns true if the values are equal</td><td><code>${equal('a','a')} => true</code></td></tr>
<tr><td>lt(value1, value2)</td><td>Returns true if the first value is lower than the second</td><td><code>${lt(10,20)} => true</code></td></tr>
<tr><td>lte(value1, value2)</td><td>Returns true if the first value is lower or equal than the second</td><td><code>${lte(10,10)} => true</code></td></tr>
<tr><td>gt(value1, value2)</td><td>Returns true if the first value is greater than the second</td><td><code>${gt(20,10)} => true</code></td></tr>
<tr><td>gte(value1, value2)</td><td>Returns true if the first value is greater or equal than the second</td><td><code>${gte(10,10)} => true</code></td></tr>
</table>
