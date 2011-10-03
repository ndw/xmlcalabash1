<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" version="1.0" name="pipeline">
      <p:input port="source" sequence="true"/>
      <p:input port="alt" sequence="true"/>
      <p:output port="result" sequence="true"/>
      
      <p:pack wrapper="wrapper">
        <p:input port="source">
          <p:pipe step="pipeline" port="source"/>
        </p:input>
        <p:input port="alternate">
          <p:pipe step="pipeline" port="alt"/>
        </p:input>
      </p:pack>
    </p:declare-step>