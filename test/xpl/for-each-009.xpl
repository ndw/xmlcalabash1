<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      
      <p:for-each name="loop">
        <p:iteration-source>
          <p:inline><foo/></p:inline>
          <p:inline><foo/></p:inline>
        </p:iteration-source>
        <p:output port="out">
          <p:inline><bar/></p:inline>
        </p:output>
        <p:sink/>
      </p:for-each>

      <p:wrap-sequence wrapper="wrapper"/>

    </p:declare-step>