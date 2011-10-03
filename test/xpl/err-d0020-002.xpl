<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:input port="source"/>
      <p:store xmlns:test="http://www.example.com" name="store" href="file:///tmp/testout.xml" method="test:nonexistant"/>
    </p:declare-step>