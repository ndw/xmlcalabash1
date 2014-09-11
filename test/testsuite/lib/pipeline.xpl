<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                type="foo:test" xmlns:foo="http://foo.com">
  <p:output port="result"/>

  <p:identity>
    <p:input port="source">
      <p:inline>
        <foo/>
      </p:inline>
    </p:input>
  </p:identity>

</p:declare-step>
