<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:viewport match="para">
        <p:viewport-source>
          <p:inline>
            <doc>
              <para>Some paragraph.</para>
              <para>Some paragraph.</para>
              <para>Some paragraph.</para>
              <para>
                <para>Nested paragraph.</para>
              </para>
            </doc>
          </p:inline>
        </p:viewport-source>
        <p:identity>
          <p:input port="source">
            <p:inline><foo/></p:inline>
          </p:input>
        </p:identity>
      </p:viewport>

    </p:declare-step>