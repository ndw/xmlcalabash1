<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
      <p:input port="source"/>
      <p:output port="result">
        <p:pipe step="i1" port="result"/>
      </p:output>

      <p:identity name="i1">
        <p:input port="source">
          <p:pipe step="i2" port="result"/>
        </p:input>
      </p:identity>

      <p:identity name="i2">
        <p:input port="source">
          <p:pipe step="main" port="source"/>
        </p:input>
      </p:identity>
    </p:declare-step>