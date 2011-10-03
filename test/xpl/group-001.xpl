<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      <p:group name="grp">
	<p:output port="result"/>
        <p:count/>
      </p:group>
      <p:identity>
        <p:input port="source">
          <p:pipe step="grp" port="result"/>
        </p:input>
      </p:identity>
    </p:pipeline>