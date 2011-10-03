<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result" sequence="true"/>
      
      <p:for-each name="loop">
        <p:iteration-source>
          <p:inline><doc/></p:inline>
        </p:iteration-source>
        <p:output port="out" primary="true"/>
        <p:output port="out2" sequence="true" primary="false"/>
        <p:identity/>
      </p:for-each>

      <p:sink/>

      <p:count>
        <p:input port="source">
          <p:pipe step="loop" port="out2"/>
        </p:input>
      </p:count>
  
    </p:declare-step>