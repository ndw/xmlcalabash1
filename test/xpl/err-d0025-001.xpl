<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">

    <p:parameters>
      <p:input port="parameters">
        <p:inline>
          <c:param xmlns:test="http://foo.com" name="test:bar" namespace="http://bar.com" value="baz"/>
        </p:inline>
      </p:input>
    </p:parameters>

  </p:declare-step>