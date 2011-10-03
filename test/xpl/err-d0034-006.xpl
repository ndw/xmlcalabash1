<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:pack xmlns:foo="http://foo.com" wrapper="foo:bar" wrapper-prefix="baz">
        <p:input port="alternate">
          <p:empty/>
        </p:input>
      </p:pack>
    </p:pipeline>