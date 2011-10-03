<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:choose>
        <p:xpath-context>
          <p:inline>
            <doc>
              <p>Para about XML</p>
            </doc>
          </p:inline>
        </p:xpath-context>
        <p:when test="//p[contains(., 'XML')]">
          <p:xpath-context>
            <p:inline>
              <doc>
                <p>Para about something else</p>
              </doc>
            </p:inline>
          </p:xpath-context>
          <p:identity>
            <p:input port="source">
              <p:inline>
                <result>Incorrect</result>
              </p:inline>
            </p:input>
          </p:identity>
        </p:when>
        <p:when test="//p[contains(., 'XML')]">
          <p:identity>
            <p:input port="source">
              <p:inline>
                <result>Correct</result>
              </p:inline>
            </p:input>
          </p:identity>
        </p:when>
        <p:otherwise>
          <p:identity>
            <p:input port="source">
              <p:inline>
                <result>Incorrect</result>
              </p:inline>
            </p:input>
          </p:identity>
        </p:otherwise>
      </p:choose>
    </p:declare-step>