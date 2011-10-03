<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result" sequence="true"/>
      
      <p:for-each name="loop">
        <p:iteration-source>
          <p:empty/>
        </p:iteration-source>
        <p:output port="out" sequence="true" primary="false">
          <p:pipe step="identity" port="result"/>
        </p:output>
        <p:identity name="identity"/>
      </p:for-each>

      <p:count>
        <p:input port="source">
          <p:pipe step="loop" port="out"/>
        </p:input>
      </p:count>
  
    </p:declare-step>