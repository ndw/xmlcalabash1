<p:pipeline xmlns="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" name="pipeline" version="1.0">
    <p:identity name="stepname">
      <p:input port="source" primary="true">
	<p:pipe step="pipeline" port="source"/>
      </p:input>
    </p:identity>

    <p:count/>

  </p:pipeline>