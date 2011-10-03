<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      
      <p:parameters name="parameters"/>

      <p:count>
        <p:input port="source" select="//c:param">
          <p:pipe step="parameters" port="result"/>
        </p:input>
      </p:count>

    </p:declare-step>