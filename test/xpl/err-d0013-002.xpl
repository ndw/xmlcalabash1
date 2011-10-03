<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
<p:input port="source"/>
<p:output port="result"/>
<p:option xmlns:h="http://www.w3.org/1999/xhtml" name="irrelevant" select="'also irrelevant'"/>

<p:delete>
  <p:with-option name="match" select="'h:del'">
    <p:namespaces xmlns:h="http://www.w3.org/1999/xhtml"/>
    <p:namespaces xmlns:h="http://example.com/ns/h-something-else"/>
  </p:with-option>
</p:delete>

</p:declare-step>