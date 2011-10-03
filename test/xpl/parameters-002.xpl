<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
      <p:input port="source"/>
      <p:output port="result"/>
      
      <p:parameters name="params">
        <p:input port="parameters">
          <p:pipe step="main" port="source"/>
        </p:input>
      </p:parameters>

      <!-- parameters are inherently unordered, but we want to force
           an order so that the test comes out right... -->
      
      <p:identity name="pick1">
        <p:input port="source" select="/c:param-set/c:param[@name='param1']">
          <p:pipe step="params" port="result"/>
        </p:input>
      </p:identity>
      
      <p:identity name="pick2">
        <p:input port="source" select="/c:param-set/c:param[@name='param2']">
          <p:pipe step="params" port="result"/>
        </p:input>
      </p:identity>
      
      <p:wrap-sequence wrapper="c:param-set">
        <p:input port="source">
          <p:pipe step="pick1" port="result"/>
          <p:pipe step="pick2" port="result"/>
        </p:input>
      </p:wrap-sequence>
      
    </p:declare-step>