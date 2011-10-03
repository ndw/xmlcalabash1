<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      <p:input port="source"/>
      <p:output port="result"/>
      
      <p:try>
        <p:group>
          <p:delete>
            <p:with-option name="match" select="'h:del'">
              <p:namespaces xmlns:h="http://www.w3.org/1999/xhtml" except-prefixes="h"/>
            </p:with-option>
          </p:delete>
        </p:group>
        <p:catch>
          <p:identity>
            <p:input port="source">
              <p:inline>
                <message>Catch caught XPath failure.</message>
              </p:inline>
            </p:input>
          </p:identity>
        </p:catch>
      </p:try>
      
    </p:declare-step>