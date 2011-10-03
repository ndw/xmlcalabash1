<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
<p:input port="source"/>
<p:output port="result"/>

<p:string-replace match="/doc/episode/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:episode'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/language/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:language'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/product-name/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:product-name'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/product-version/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:product-version'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/vendor/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:vendor'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/vendor-uri/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:vendor-uri'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/version/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:version'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/xpath-version/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:xpath-version'), '&#34;')"/>
</p:string-replace>

<p:string-replace match="/doc/psvi-supported/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:psvi-supported'), '&#34;')"/>
</p:string-replace>

<p:count>
  <p:input port="source" select="//*[@value='???']|//*[@value='']"/>
</p:count>

</p:declare-step>