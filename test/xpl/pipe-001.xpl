<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:input port="source"/>
      <p:output port="result"/>
      
      <p:identity name="foo"/>
      <p:group>
        <p:group>
          <p:identity>
            <p:input port="source">
              <p:pipe step="foo" port="result"/>
            </p:input>
          </p:identity>
        </p:group>
      </p:group>
    </p:declare-step>