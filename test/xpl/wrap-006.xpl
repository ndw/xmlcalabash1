<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
    <p:wrap xmlns:test="http://www.example.com/test" match="processing-instruction('foo')" group-adjacent="." wrapper="test:wrapper"/>
  </p:pipeline>