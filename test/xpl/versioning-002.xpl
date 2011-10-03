<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="2.0">
  <p:output port="result"/>
 
  <p:identity name="id1">
    <p:input port="source">
      <p:inline>
        <doc1/>
      </p:inline>
    </p:input>
    <p:input port="new-input-port">
      <p:pipe step="id2" port="new-output-port"/>
    </p:input>
  </p:identity>

  <p:identity name="id2"/>

</p:declare-step>