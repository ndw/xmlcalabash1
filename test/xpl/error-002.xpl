<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" xmlns:usrerr="http://xproc.org/ns/user-error" version="1.0">
      <p:output port="result"/>
      <p:try>
        <p:group>
          <p:error code="error" code-namespace="http://baz.com">
            <p:input port="source">
              <p:inline>
                <message>Bang!</message>
              </p:inline>
            </p:input>
          </p:error>
          <p:sink/>
          <p:identity>
            <p:input port="source">
              <p:inline>
                <failure-no-error/>
              </p:inline>
            </p:input>
          </p:identity>
        </p:group>
        <p:catch name="catch">
          <p:choose>
            <p:xpath-context>
              <p:pipe step="catch" port="error"/>
            </p:xpath-context>
            <p:when test="contains(//c:error/@code, ':error')">
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
        </p:catch>
      </p:try>
    </p:declare-step>