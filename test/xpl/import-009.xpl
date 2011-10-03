<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:import href="pipeline-imported.xpl"/>

      <p:choose xmlns:foo="http://acme.com/test" xmlns:test="http://xproc.org/ns/testsuite/test">
        <p:when test="p:step-available('foo:imported') and not(p:step-available('foo:nested')) and not(p:step-available('test:step'))">
          <p:identity>
            <p:input port="source">
              <p:inline>
                <success/>
              </p:inline>
            </p:input>
          </p:identity>
        </p:when>
        <p:otherwise>
          <p:identity>
            <p:input port="source">
              <p:inline>
                <failure/>
              </p:inline>
            </p:input>
          </p:identity>
        </p:otherwise>
      </p:choose>

    </p:declare-step>