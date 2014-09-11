<p:library xmlns:p="http://www.w3.org/ns/xproc"
	   xmlns:c="http://www.w3.org/ns/xproc-step"
	   xmlns:err="http://www.w3.org/ns/xproc-error">

  <p:declare-step type="ex:test" version="1.0" xmlns:ex="http://example.com/ns/xproc">
    <p:output port="result"/>
    <p:identity>
      <p:input port="source">
        <p:inline>
          <doc/>
        </p:inline>
      </p:input>
    </p:identity>
  </p:declare-step>

</p:library>
