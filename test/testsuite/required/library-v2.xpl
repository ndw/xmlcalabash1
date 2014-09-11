<p:library xmlns:p="http://www.w3.org/ns/xproc"
	   xmlns:c="http://www.w3.org/ns/xproc-step"
	   xmlns:err="http://www.w3.org/ns/xproc-error"
           version='2.0'>

  <p:declare-step type="ex:test" xmlns:ex="http://example.com/ns/xproc">
    <p:output port="result"/>
    <p:identity>
      <p:input port="source">
        <p:inline>
          <doc/>
        </p:inline>
      </p:input>
      <p:with-option name="new-option" select="'foo'"/>
    </p:identity>
  </p:declare-step>

</p:library>
