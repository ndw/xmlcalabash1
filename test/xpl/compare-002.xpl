<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
  <p:input port="source"/>
  <p:input port="alternate"/>
  <p:output port="result">
    <p:pipe step="compare" port="result"/>
  </p:output>

  <p:compare name="compare">
    <p:input port="source">
      <p:pipe step="main" port="source"/>
    </p:input>
    <p:input port="alternate">
      <p:pipe step="main" port="alternate"/>
    </p:input>
  </p:compare>
</p:declare-step>