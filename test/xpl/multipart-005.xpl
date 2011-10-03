<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      <p:http-request>
        <p:input port="source">
          <p:inline>
            <c:request method="get" detailed="true" href="http://tests.xproc.org/service/fixed-multipart"/>
          </p:inline>
        </p:input>
      </p:http-request>

      <p:choose>
        <p:when test="/c:response/c:header[@name='Content-Type' and @value='multipart/related; boundary=&#34;=-=-=-=-=&#34;']">
          <p:delete match="c:header"/>
        </p:when>
        <p:otherwise>
          <p:identity>
            <p:input port="source">
              <p:inline>
                <failure>Expected headers not present.</failure>
              </p:inline>
            </p:input>
          </p:identity>
        </p:otherwise>
      </p:choose>
    </p:declare-step>