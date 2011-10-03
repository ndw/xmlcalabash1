<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:xinclude fixup-xml-base="true">
	<p:log port="result" href="file:///tmp/out.xml"/>
      </p:xinclude>

      <p:choose>
        <p:when test="/document/para[2][contains(@xml:base, 'doc/xinclude/para.xml')]">
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
  
    </p:pipeline>