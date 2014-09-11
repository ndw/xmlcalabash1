<p:library version='1.0'
           xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:ex="http://example.com/steps"
           xmlns:ex1="http://example.com/steps1"
           xmlns:ex2="http://example.com/steps2"
           exclude-inline-prefixes="p ex">

  <p:declare-step type="ex:foo" exclude-inline-prefixes="ex1">
    <p:output port="result"/>
    <p:identity>
      <p:input port="source">
        <p:inline><doc/></p:inline>
      </p:input>
    </p:identity>
  </p:declare-step>

</p:library>