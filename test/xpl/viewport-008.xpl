<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:viewport match="para">
        <p:output port="tmp" sequence="true">
          <p:inline><foo/></p:inline>
          <p:pipe step="identity1" port="result"/>
        </p:output>
        <p:identity name="identity1">
          <p:input port="source">
            <p:inline><bar/></p:inline>
          </p:input>
        </p:identity>
        <p:identity name="identity2">
          <p:input port="source">
            <p:inline><baz/></p:inline>
          </p:input>
        </p:identity>
	<p:sink/>
      </p:viewport>
    </p:pipeline>