<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      
      <p:identity>
        <p:input port="source">
          <!-- no content-type information -->
          <p:data href="../doc/textdata-utf8.data" wrapper="wrapper" wrapper-namespace="http://baz.com"/>
        </p:input>
      </p:identity>
    </p:declare-step>