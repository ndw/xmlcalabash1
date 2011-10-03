<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:split-sequence>
  <p:input port="source">
    <p:pipe step="pipeline" port="source"/>
    <p:inline><ex:stylesheet xmlns:ex="http://example.com/ex"/></p:inline>
    <p:pipe step="pipeline" port="source"/>
    <p:inline><ex:stylesheet xmlns:ex="http://example.com/ex"/></p:inline>
  </p:input>
  <p:with-option xmlns:ex2="http://example.com/ex" name="test" select="'/ex2:*'"/>
</p:split-sequence>

<p:count/>

</p:pipeline>