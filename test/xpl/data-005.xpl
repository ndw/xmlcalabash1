<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      
      <p:identity>
        <p:input port="source">
          <!-- no content-type information -->
          <p:data xmlns:test="http://test.com" href="../doc/textdata-utf8.data" wrapper="test:wrapper"/>
        </p:input>
      </p:identity>
    </p:declare-step>