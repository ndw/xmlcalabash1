<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
<p:output port="result"/>

<p:unescape-markup>
  <p:input port="source">
    <p:inline><wrapper>&lt;doc&gt;&lt;p&gt;foo&lt;/p&gt;&lt;/doc&gt;</wrapper></p:inline>
  </p:input>
</p:unescape-markup>

</p:declare-step>