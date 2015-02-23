<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
		exclude-result-prefixes="xs"
                version="2.0">

<xsl:output method="xml" encoding="utf-8" indent="no"
	    omit-xml-declaration="yes"/>

<xsl:param name="version.label" required="yes"/>

<xsl:template match="element()">
  <xsl:copy>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="comment()|processing-instruction()">
  <xsl:copy/>
</xsl:template>

<xsl:template match="text()">
  <xsl:value-of select="replace(., '\{VERSION\}', $version.label)"/>
</xsl:template>

<xsl:template match="attribute()">
  <xsl:attribute name="{node-name(.)}"
                 select="replace(., '\{VERSION\}', $version.label)"/>
</xsl:template>

</xsl:stylesheet>
