<p:declare-step xmlns:test="http://www.test.com" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">

      <p:declare-step type="test:local-step">
        <p:option name="optional-no-default"/>
        
        <p:identity>
          <p:input port="source" select="$optional-no-default">
            <p:inline><doc/></p:inline>
          </p:input>
        </p:identity>
        
        <p:sink/>
      </p:declare-step>

      <test:local-step/>

    </p:declare-step>