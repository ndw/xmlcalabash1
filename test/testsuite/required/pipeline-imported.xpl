<p:pipeline name="main" type="foo:imported" version='1.0'
            xmlns:p="http://www.w3.org/ns/xproc"
            xmlns:foo="http://acme.com/test">

  <p:import href="../lib/l1.xpl"/>

  <p:pipeline type="foo:nested">
    <p:identity/>
  </p:pipeline>

  <p:identity name="ident">
    <p:input port="source">
      <p:pipe step="main" port="source"/>
    </p:input>
  </p:identity>
  
</p:pipeline>