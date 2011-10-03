<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:split-sequence name="split" test="p" initial-only="true">
  <p:input port="source" select="/doc/*"/>
</p:split-sequence>

<p:sink/>

<p:count>
  <p:input port="source">
    <p:pipe step="split" port="not-matched"/>
  </p:input>
</p:count>

</p:pipeline>