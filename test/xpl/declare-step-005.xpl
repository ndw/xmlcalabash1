<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
	    <p:input port="source"/>
	    <p:output port="result"/>
	    <p:option name="test-option" select="'some string value'"/>
	    <p:identity>
	        <p:input port="source">
	            <p:inline>
	                <inline_identity_test/>
	            </p:inline>
	        </p:input>
	    </p:identity>
	</p:declare-step>