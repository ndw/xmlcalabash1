<p:library xmlns:p="http://www.w3.org/ns/xproc"
	   xmlns:test="http://xproc.org/ns/testsuite/test"
           version='1.0'>

 <p:declare-step type="test:step" name="test-step">
  <p:input port="source" sequence="true"/>
  <p:output port="result"/>
  <p:count/>
 </p:declare-step>

</p:library>
