<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
      <p:output port="result"/>
      <p:option name="opt"/>

      <p:choose>
        <p:when test="$opt = 'value'">
	  <p:xpath-context><p:empty/></p:xpath-context>
          <p:identity>
            <p:input port="source">
              <p:inline><success/></p:inline>
            </p:input>
          </p:identity>
        </p:when>
        <p:otherwise>
          <p:identity>
            <p:input port="source">
              <p:inline><failure/></p:inline>
            </p:input>
          </p:identity>
        </p:otherwise>
      </p:choose>
    </p:declare-step>