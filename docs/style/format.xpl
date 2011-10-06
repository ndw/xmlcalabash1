<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:l="http://xproc.org/library">
<p:input port="source"/>
<p:input port="parameters" kind="parameter"/>
<p:output port="result">
  <p:pipe step="xslt" port="result"/>
</p:output>

<p:serialization port="result" method="xhtml"/>

<p:xinclude/>

<p:xslt name="xslt">
  <p:input port="stylesheet">
    <p:document href="refhtml.xsl"/>
  </p:input>
  <p:with-param name="base.dir" select="'/projects/github/calabash/docs/build/'"/>
</p:xslt>

<p:for-each>
  <p:iteration-source>
    <p:pipe step="xslt" port="secondary"/>
  </p:iteration-source>
  <p:store method="xhtml">
    <p:with-option name="href" select="base-uri(/)"/>
  </p:store>
</p:for-each>

</p:declare-step>
