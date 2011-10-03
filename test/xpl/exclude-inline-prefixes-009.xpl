<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:ex="http://example.com/steps" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main" exclude-inline-prefixes="t ex">

  <p:output port="result"/>

  <p:declare-step type="ex:foo" exclude-inline-prefixes="c err">
    <p:output port="result"/>
    <p:identity>
      <p:input port="source">
        <p:inline><doc/></p:inline>
      </p:input>
    </p:identity>
  </p:declare-step>

  <ex:foo/>

  <p:wrap-sequence wrapper="wrapper"/>

  <p:escape-markup/>
</p:declare-step>