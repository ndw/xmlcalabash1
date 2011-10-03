<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:variable name="element" select="'p'"/>

<p:filter>
  <p:with-option name="select" select="concat('//*[local-name(.) = &#34;',$element,'&#34;]')"/>
</p:filter>

</p:pipeline>