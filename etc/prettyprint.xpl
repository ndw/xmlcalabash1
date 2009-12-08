<p:pipeline xmlns:p='http://www.w3.org/ns/xproc' version='1.0'>
<p:xslt>
  <p:input port='stylesheet'>
    <p:inline><xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
                xmlns:saxon='http://icl.com/saxon'
                exclude-result-prefixes='saxon'
                version='2.0'>

  <xsl:output method='xml' indent='yes' saxon:indent-spaces='2'/>

  <xsl:strip-space elements='*'/>

  <xsl:template match='*'>
    <xsl:copy>
      <xsl:copy-of select='@*'/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match='comment()'>
    <xsl:if test='not(preceding-sibling::node()[1][self::comment()])'>
      <xsl:text>&#10;</xsl:text>
      <xsl:text>&#10;</xsl:text>
    </xsl:if>
    <xsl:copy/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

  <xsl:template match='processing-instruction()'>
    <xsl:if test='not(preceding-sibling::node()[1][self::processing-instruction()])'>
      <xsl:text>&#10;</xsl:text>
    </xsl:if>
    <xsl:copy/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

</xsl:stylesheet></p:inline>
  </p:input>
</p:xslt>
</p:pipeline>
