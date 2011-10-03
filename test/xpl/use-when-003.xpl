<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <ex:unsupported-step xmlns:ex="http://example.org/ns/pipelines" p:use-when="false()"/>

      <p:identity>
        <p:input port="source">
          <p:inline>
            <success/>
          </p:inline>
        </p:input>
      </p:identity>

    </p:declare-step>