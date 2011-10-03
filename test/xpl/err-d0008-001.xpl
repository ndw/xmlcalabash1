<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
		<p:input port="source" sequence="true"/>
		<p:output port="result" sequence="true"/>
		
		<p:variable name="value" select="/doc/a"/>

		<p:string-replace match="//doc" replace="concat('test-',$value)"/>
		
    </p:declare-step>