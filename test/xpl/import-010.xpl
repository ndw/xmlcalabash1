<p:declare-step xmlns:bar="http://bar.com" xmlns:baz="http://baz.com" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:import href="../lib/l6.xpl"/>
      <p:import href="../lib/pipeline2.xpl"/>

      <baz:test name="baz"/>
      <bar:test name="bar"/>

      <p:wrap-sequence wrapper="doc">
        <p:input port="source">
          <p:pipe step="baz" port="result"/>
          <p:pipe step="bar" port="result"/>
        </p:input>
      </p:wrap-sequence>

    </p:declare-step>