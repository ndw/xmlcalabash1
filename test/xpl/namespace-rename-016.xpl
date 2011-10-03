<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
  <p:filter xmlns:n="http://www.example.com/" select="//n:parent"/>
  <p:namespace-rename from="http://www.example.com/ns"/>
</p:pipeline>