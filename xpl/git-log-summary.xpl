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
    <p:inline>
      <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                      xmlns:g="http://nwalsh.com/ns/git-repo-info"
                      exclude-result-prefixes="g xs"
                      version="2.0">
        <xsl:template match="g:git-repo-info">
          <xsl:copy>
            <xsl:apply-templates select="g:commit">
              <xsl:sort select="g:date" order="descending"/>
            </xsl:apply-templates>
          </xsl:copy>
        </xsl:template>

        <xsl:template match="g:commit">
          <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
          </xsl:copy>
          <xsl:text>&#10;</xsl:text>
        </xsl:template>

        <xsl:template match="*">
          <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates/>
          </xsl:copy>
        </xsl:template>

        <xsl:template match="comment()|processing-instruction()|text()">
          <xsl:copy/>
        </xsl:template>
      </xsl:stylesheet>
    </p:inline>
  </p:input>
</p:xslt>

</p:declare-step>
