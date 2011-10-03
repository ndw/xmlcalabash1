<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

	<p:input port="source" sequence="true"/>
	<p:output port="result" sequence="true"/>

	   <p:identity name="step1">
	        <p:input port="source">
	              <p:inline>
	                  <root1/>
	              </p:inline>
	              <p:inline>
	                  <root2/>
	              </p:inline>
	              <p:inline>
	                  <root3/>
	              </p:inline>
	        </p:input>
	   </p:identity>

	</p:declare-step>