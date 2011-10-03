<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:p="http://www.w3.org/ns/xproc" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
      <p:input port="source"/>
      
      <p:parameters name="params">
        <p:input port="parameters">
          <p:pipe step="main" port="source"/>
        </p:input>
      </p:parameters>
    </p:declare-step>