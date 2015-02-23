<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:prof="http://xmlcalabash.com/ns/profile"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
		exclude-result-prefixes="prof xs"
                version="2.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
	    omit-xml-declaration="yes"/>

<xsl:strip-space elements="*"/>

<xsl:template match="prof:profile">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:choose>
      <xsl:when test="*/prof:time">
        <xsl:attribute name="total-time" select="prof:time"/>
        <xsl:attribute name="step-time" select="prof:time - sum(*/prof:time)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="step-time" select="prof:time"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates select="node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="prof:time"/>

<xsl:template match="attribute()|text()|comment()|processing-instruction()">
  <xsl:copy/>
</xsl:template>

</xsl:stylesheet>
