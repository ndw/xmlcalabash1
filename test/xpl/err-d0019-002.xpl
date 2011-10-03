<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
    <p:input port="source"/>
    <p:input port="alternate"/>
    <p:output port="result">
      <p:pipe step="step1" port="result"/>
    </p:output>
    <p:compare name="step1">
      <p:input port="source">
	<p:pipe step="main" port="source"/>
      </p:input>
      <p:input port="alternate">
	<p:pipe step="main" port="alternate"/>
      </p:input>
      <p:with-option name="fail-if-not-equal" select="'yes'">
	<p:empty/>
      </p:with-option>
    </p:compare>
  </p:declare-step>