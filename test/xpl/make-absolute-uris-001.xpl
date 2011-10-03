<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:ex="http://xproc.org/ns/xproc/ex" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
  <p:input port="source">
    <p:document href="../doc/xml-base-test.xml"/>
  </p:input>
  <p:output port="result"/>

  <p:make-absolute-uris match="uri">
    <p:input port="source">
      <p:pipe step="main" port="source"/>
    </p:input>
  </p:make-absolute-uris>
</p:declare-step>