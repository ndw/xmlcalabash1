<p:library version="1.0"
           xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:test="http://xproc.org/ns/testsuite/test">

 <p:import href="l1.xpl"/>

 <p:declare-step type="test:step2" name="test-step">
  <p:input port="source" sequence="true"/>
  <p:output port="result"/>
   <!-- this tests that test:step is visible here -->
   <test:step/>
 </p:declare-step>

</p:library>
