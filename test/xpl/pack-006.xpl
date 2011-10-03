<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" version="1.0" name="pipeline">
      <p:input port="source" sequence="true"/>
      <p:input port="alt" sequence="true"/>
      <p:output port="result" sequence="true"/>
      
      <p:pack wrapper="wrapper" wrapper-namespace="http://baz.com" wrapper-prefix="baz">
        <p:input port="source">
          <p:pipe step="pipeline" port="source"/>
        </p:input>
        <p:input port="alternate">
          <p:pipe step="pipeline" port="alt"/>
        </p:input>
      </p:pack>

      <p:wrap-sequence wrapper="foo"/>

      <p:escape-markup/>

      <p:choose>
        <p:when test="contains(/foo, '&lt;baz:wrapper') and contains(/foo, 'xmlns:baz=&#34;http://baz.com&#34;')">
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