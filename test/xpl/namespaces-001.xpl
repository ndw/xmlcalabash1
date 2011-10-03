<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:delete>
  <p:with-option name="match" select="'h:del'">
    <p:namespaces xmlns:h="http://www.w3.org/1999/xhtml"/>
  </p:with-option>
</p:delete>

</p:pipeline>