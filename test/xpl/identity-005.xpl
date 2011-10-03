<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

	  <p:input port="source">
	        <p:inline>
	            <inline_test_success/>
	        </p:inline>
	  </p:input>    

	  <p:output port="result"/>

	  <p:identity/>

	</p:declare-step>