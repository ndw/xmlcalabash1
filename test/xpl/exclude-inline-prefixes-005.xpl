<p:declare-step xmlns="http://example.com/ns/test" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
  <p:output port="result"/>
  <p:identity>
    <p:input port="source">
      <p:inline exclude-inline-prefixes="#default t p err"><doc/></p:inline>
    </p:input>
  </p:identity>

  <p:choose xmlns="" xmlns:tmp="http://example.com/ns/test">
    <p:when test="/tmp:doc[namespace::c='http://www.w3.org/ns/xproc-step' and not(namespace::*[local-name()='t' or local-name()='p' or local-name()='err'])]">
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