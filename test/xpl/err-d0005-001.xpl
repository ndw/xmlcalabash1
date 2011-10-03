<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" name="main" version="1.0">
      <p:input port="source" sequence="true"/>
      <p:output port="result"/>

      <p:choose>
        <p:xpath-context>
          <p:pipe step="main" port="source"/>
        </p:xpath-context>
        <p:when test="1 = 0">
          <p:identity/>
        </p:when>
      </p:choose>
    </p:declare-step>