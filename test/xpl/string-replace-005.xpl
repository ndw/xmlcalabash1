<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" version="1.0">
      <p:string-replace match="*/@version" replace="number(.)+1"/>
    </p:pipeline>