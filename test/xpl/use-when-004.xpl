<p:declare-step xmlns:ex="http://example.org/ns/pipelines" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:declare-step type="ex:supported-step">
        <p:output port="result"/>
        <p:identity>
          <p:input port="source">
            <p:inline>
              <success/>
            </p:inline>
          </p:input>
        </p:identity>
      </p:declare-step>

      <ex:supported-step p:use-when="true()"/>

    </p:declare-step>