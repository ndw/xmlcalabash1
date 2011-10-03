<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      <p:input port="source"/>
      <p:output port="result" sequence="true"/>

      <p:filter name="step1" select="/someunknownelement"/>
      
    </p:declare-step>