<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:input port="source"/>
      <p:output port="result"/>
      
      <p:delete>
        <p:with-option name="match" select="'h:del'">
          <p:namespaces element="/*"/>
          <p:namespaces xmlns:h="http://foo.com"/>
          <p:inline>
            <h:html xmlns:h="http://www.w3.org/1999/xhtml"/>
          </p:inline>
        </p:with-option>
      </p:delete>
      
    </p:declare-step>