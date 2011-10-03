<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      
      <p:choose>
        <p:xpath-context xmlns:foo="http://www.foo.com">
          <p:pipe step="pipeline" port="source"/>
        </p:xpath-context>
        <p:when xmlns:bar="http://www.bar.com" test="/foo:document/bar:title">
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
      
    </p:pipeline>