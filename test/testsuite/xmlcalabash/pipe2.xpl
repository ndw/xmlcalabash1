<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
		xmlns:cx="http://xmlcalabash.com/ns/extensions"
		exclude-inline-prefixes="cx"
		name="main">
<p:input port="source">
  <p:inline><doc>Congratulations! You've run a pipeline!</doc></p:inline>
</p:input>
<p:output port="result"/>
<cx:message message="debug me!"/>
</p:declare-step>
