<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      <p:input port="source"/>
      <p:output port="result">
        <p:pipe step="group" port="result"/>
      </p:output>
      <p:output port="result2">
        <p:pipe step="group" port="result2"/>
      </p:output>

      <p:group name="group">
	<p:output port="result">
          <p:pipe step="outer-identity" port="result"/>
        </p:output>
	<p:output port="result2" primary="true"/>
        <p:identity/>
      </p:group>

      <p:identity name="outer-identity">
        <p:input port="source">
          <p:inline>
            <foo/>
          </p:inline>
        </p:input>
      </p:identity>
    </p:declare-step>