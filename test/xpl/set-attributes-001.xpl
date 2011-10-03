<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" version="1.0" name="pipeline">

<p:set-attributes match="*">
  <p:input port="attributes">
  <p:inline>
    <attributes xmlns:a="http://xproc.org/ns/testsuite/attributes" att="1" a:att="2">
      <a:foo/>
    </attributes>  
  </p:inline>
  </p:input>
</p:set-attributes>


</p:pipeline>