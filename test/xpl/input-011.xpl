<p:declare-step xmlns:test="http://www.test.com" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      <p:declare-step type="test:foo">
        <p:input port="source" select="/doc/p">
          <p:inline>
            <doc>
              <p>Some text.</p>
            </doc>
          </p:inline>
        </p:input>
        <p:output port="result"/>
        <p:identity/>
      </p:declare-step>

      <test:foo>
        <p:input port="source">
          <p:inline>
            <doc>
              <p>Some other text.</p>
            </doc>
          </p:inline>
        </p:input>
      </test:foo>
     
    </p:declare-step>