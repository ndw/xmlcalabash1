<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:error xmlns:foo="http://foo.com" code="foo:bar" code-prefix="baz">
        <p:input port="source">
          <p:inline>
            <message>Bang!</message>
          </p:inline>
        </p:input>
      </p:error>
      <p:sink/>
    </p:declare-step>