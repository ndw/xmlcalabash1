<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:cl="http://xmlcalabash.com/ns/library" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
<p:option name="count" required="true"/>

<p:import href="library.xpl"/>

<cl:make-sequence>
  <p:with-option name="count" select="$count"/>
</cl:make-sequence>

<p:count/>

</p:pipeline>