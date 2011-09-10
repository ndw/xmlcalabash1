<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
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
    <xsl:choose>
      <xsl:when test="preceding-sibling::node()[1]/self::text()
                      and contains(preceding-sibling::text()[1], '&#10;')">
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>&#10;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:copy/>

    <xsl:choose>
      <xsl:when test="following-sibling::node()[1]/self::text()
                      and contains(following-sibling::text()[1], '&#10;')">
      </xsl:when>
      <xsl:when test="following-sibling::node()[1]/self::comment()
                      or following-sibling::node()[1]/self::processing-instruction()">
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>&#10;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='processing-instruction()'>
    <xsl:choose>
      <xsl:when test="preceding-sibling::node()[1]/self::text()
                      and contains(preceding-sibling::text()[1], '&#10;')">
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>&#10;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:copy/>

    <xsl:choose>
      <xsl:when test="following-sibling::node()[1]/self::text()
                      and contains(following-sibling::text()[1], '&#10;')">
      </xsl:when>
      <xsl:when test="following-sibling::node()[1]/self::comment()
                      or following-sibling::node()[1]/self::processing-instruction()">
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>&#10;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
