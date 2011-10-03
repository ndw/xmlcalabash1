<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:ex="http://example.com/">
<p:output port="result"/>
<p:input port="parameters" kind="parameter"/>
<p:serialization port="result" method="xml" indent="true"/>

<p:exec command="git-log-summary" result-is-xml="true">
  <p:input port="source">
    <p:empty/>
  </p:input>
</p:exec>

<p:xslt>
  <p:input port="source" select="/c:result/*"/>
  <p:input port="stylesheet">
    <p:document href="git-summarize.xsl"/>
  </p:input>
</p:xslt>

</p:declare-step>
