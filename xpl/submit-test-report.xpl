<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
		xmlns:c="http://www.w3.org/ns/xproc-step"
		xmlns:cx="http://xmlcalabash.com/ns/extensions"
		exclude-inline-prefixes="c cx"
		name="main" version="1.0">
<p:input port="source"/>
<p:output port="result"/>
<p:option name="username" select="'calabash'"/>
<p:option name="password" required="true"/>

<p:documentation xmlns="http://www.w3.org/1999/xhtml">
<div>
  <p>This pipeline submits the XProc test suite results.</p>
</div>
</p:documentation>

<p:wrap wrapper="c:body" match="/"/>

<p:add-attribute match="c:body"
                 attribute-name="content-type"
                 attribute-value="application/xml"/>

<p:wrap wrapper="c:request" match="/"/>

<p:add-attribute attribute-name="password" match="/c:request">
  <p:with-option name="attribute-value" select="$password"/>
</p:add-attribute>

<p:add-attribute attribute-name="username" match="/c:request">
  <p:with-option name="attribute-value" select="$username"/>
</p:add-attribute>

<p:add-attribute attribute-name="href" match="/c:request">
  <p:with-option name="attribute-value"
                 select="'http://tests.xproc.org/results/submit/report'"/>
</p:add-attribute>

<p:set-attributes match="c:request">
  <p:input port="attributes">
    <p:inline>
      <c:request method="post" auth-method="Basic"
                 send-authorization="true"/>
    </p:inline>
  </p:input>
</p:set-attributes>

<p:http-request/>

</p:declare-step>
