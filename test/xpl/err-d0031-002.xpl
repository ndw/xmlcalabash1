<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
      <p:input port="parameters" kind="parameter"/>
      
      <p:parameters name="parameters">
        <p:input port="parameters">
          <p:pipe step="main" port="parameters"/>
        </p:input>
      </p:parameters>

    </p:declare-step>