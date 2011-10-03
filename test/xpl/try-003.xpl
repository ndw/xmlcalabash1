<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
<p:input port="parameters" kind="parameter"/>
<p:input port="source">
  <p:inline><irrelevant/></p:inline>
</p:input>
<p:output port="result"/>

<p:try>
  <p:group>
    <p:xslt>
      <p:input port="stylesheet">
	<p:inline>
	  <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	    <xsl:template match="/">
	      <xsl:message terminate="yes">
		<xsl:text>This XSLT stylesheet always fails</xsl:text>
	      </xsl:message>
	    </xsl:template>
	  </xsl:stylesheet>
	</p:inline>
      </p:input>
    </p:xslt>
  </p:group>
  <p:catch name="catch">
    <p:choose>
      <p:when test="//c:error[contains(.,'This XSLT stylesheet always fails')]">
	<p:xpath-context>
	  <p:pipe step="catch" port="error"/>
	</p:xpath-context>
	<p:identity>
	  <p:input port="source">
	    <p:inline>
	      <message>Catch caught XSLT failure.</message>
	    </p:inline>
	  </p:input>
	</p:identity>
      </p:when>
      <p:otherwise>
	<p:identity>
	  <p:input port="source">
	    <p:inline>
	      <message>Catch caught XSLT failure, but not the message.</message>
	    </p:inline>
	  </p:input>
	</p:identity>
      </p:otherwise>
    </p:choose>
  </p:catch>
</p:try>
</p:declare-step>