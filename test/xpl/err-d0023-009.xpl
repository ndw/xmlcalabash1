<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      <p:output port="result"/>
      <p:option name="opt" select="'pfx:para'"/>
      
      <p:delete xmlns:pfx="http://example.org">
        <p:input port="source">
          <p:inline>
            <doc>
              <pfx:para>Some text</pfx:para>
            </doc>
          </p:inline>
        </p:input>
        <p:with-option name="match" select="$opt"/>
      </p:delete>
    </p:declare-step>