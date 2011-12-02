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
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:g="http://nwalsh.com/ns/git-repo-info"
                exclude-result-prefixes="g xs"
                version="2.0">
  <xsl:output method="xml" indent="yes"/>
  <xsl:strip-space elements="*"/>

  <xsl:template match="g:git-repo-info">
    <xsl:variable name="commit-list" as="element(g:commit)*">
      <xsl:for-each-group select="g:commit" group-by="concat(g:date,g:committer,g:message)">
        <commit xmlns="http://nwalsh.com/ns/git-repo-info">
          <xsl:copy-of select="current-group()/g:file"/>
          <xsl:copy-of select="current-group()[1]/g:date"/>
          <xsl:copy-of select="current-group()[1]/g:hash"/>
          <xsl:copy-of select="current-group()[1]/g:committer"/>
          <xsl:copy-of select="current-group()[1]/g:committer-name"/>
          <xsl:copy-of select="current-group()[1]/g:committer-email"/>
          <xsl:copy-of select="current-group()[1]/g:message"/>
        </commit>
      </xsl:for-each-group>
    </xsl:variable>

    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates select="$commit-list">
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
