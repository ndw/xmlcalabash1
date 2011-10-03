<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:input port="source" sequence="true"/>
      <p:output port="result"/>

      <p:viewport match="para">
        <p:identity>
          <p:input port="source">
            <p:inline><foo/></p:inline>
          </p:input>
        </p:identity>
      </p:viewport>

    </p:declare-step>