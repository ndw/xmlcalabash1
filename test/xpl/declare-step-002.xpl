<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:mine="http://www.example.org/test/mine" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
	<p:declare-step name="test1" type="mine:identity">
	    <p:input port="source"/>
	    <p:output port="result"/>
	    <p:identity>
	        <p:input port="source">
	            <p:inline>
	                <inline_identity_test/>
	            </p:inline>
	        </p:input>
	    </p:identity>

	</p:declare-step>

	<mine:identity/>

	</p:pipeline>