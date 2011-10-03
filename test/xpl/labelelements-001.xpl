<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:label-elements/>

<p:for-each>
  <p:output port="result"/>
  <p:iteration-source select="//*[@xml:id]"/>
  <p:identity/>
</p:for-each>

<p:count/>

</p:pipeline>