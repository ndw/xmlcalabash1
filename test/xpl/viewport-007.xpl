<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:viewport match="para">
        <p:output port="tmp" sequence="true">
          <p:inline><foo/></p:inline>
          <p:inline><bar/></p:inline>
        </p:output>
        <p:identity>
          <p:input port="source">
            <p:inline><baz/></p:inline>
          </p:input>
        </p:identity>
	<p:sink/>
      </p:viewport>
    </p:pipeline>