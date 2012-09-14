<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" version="1.0">
<p:serialization port="result" method="xml" indent="true"/>

<p:xquery>
  <p:input port="query">
    <p:data href="xpc2.xqy"/>
  </p:input>
</p:xquery>

<p:xslt version="2.0">
  <p:input port="stylesheet">
    <p:document href="xpc2.xsl"/>
  </p:input>
</p:xslt>

</p:pipeline>
