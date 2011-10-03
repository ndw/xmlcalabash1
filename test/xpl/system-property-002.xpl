<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
<p:input port="source"/>
<p:output port="result"/>

<p:string-replace match="/doc/unknown/@value">
  <p:with-option name="replace" select="concat('&#34;',p:system-property('p:unknown-value'), '&#34;')"/>
</p:string-replace>

</p:declare-step>