<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:insert match="div" position="last-child">
  <p:input port="source">
    <p:pipe step="pipeline" port="source"/>
  </p:input>
  <p:input port="insertion">
    <p:inline><p>New after paragraph</p></p:inline>
  </p:input>
</p:insert>

</p:pipeline>