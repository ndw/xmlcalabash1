<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:cl="http://xmlcalabash.com/ns/library" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
<p:option name="count" required="true"/>

<p:import href="library.xpl"/>

<cl:fibonacci>
  <p:with-option name="number" select="$count"/>
</cl:fibonacci>

<p:count/>

</p:pipeline>