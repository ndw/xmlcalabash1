<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="2.0">
  <p:output port="result"/>
 
  <p:identity name="identity">
    <p:input port="source">
      <p:inline>
        <doc/>
      </p:inline>
    </p:input>
  </p:identity>

  <p:sink/>

  <p:count>
    <p:input port="source">
      <p:pipe step="identity" port="new-output-port"/>
    </p:input>
  </p:count>

</p:declare-step>