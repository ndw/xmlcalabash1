<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:sink>
        <p:input port="source">
          <p:data href="../doc/textdata-utf8.data" wrapper-prefix="baz"/>
        </p:input>
      </p:sink>
    </p:declare-step>