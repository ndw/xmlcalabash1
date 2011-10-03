<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
<p:input port="source"/>
<p:output port="result" sequence="true">
  <p:pipe step="id" port="result"/>
  <p:document href="http://tests.xproc.org/tests/doc/document.xml"/>
</p:output>

<p:identity name="id"/>
</p:declare-step>