<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:ex="http://xproc.org/ns/xproc/ex" xmlns:t="http://xproc.org/ns/testsuite" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:add-xml-base relative="true" all="true">
    <p:input port="source">
      <p:pipe step="main" port="source"/>
    </p:input>
  </p:add-xml-base>
</p:declare-step>