<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:mine="http://www.example.org/test/mine" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
	<p:declare-step name="test1" type="mine:count">
	    <p:input port="source" sequence="true">
	      <p:inline>
		<nested_inline_test/>
	      </p:inline>	
	    </p:input>
	    <p:output port="result" sequence="true"/>
	    <p:count/>
	</p:declare-step>

	<mine:count>
	    <p:input port="source">
	        <p:inline>
	            <root1/>
	        </p:inline>
	        <p:inline>
	            <root2/>
	        </p:inline>
	    </p:input>
	</mine:count>

	</p:pipeline>