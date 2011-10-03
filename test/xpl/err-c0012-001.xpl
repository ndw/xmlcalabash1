<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://example.org/ns/pipelines" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:t="http://xproc.org/ns/testsuite" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
      <p:output port="result"/>
      <p:option name="path" required="true"/>
      
      <p:directory-list>
        <p:with-option name="path" select="$path">
          <p:empty/>
        </p:with-option>
      </p:directory-list>


    </p:declare-step>