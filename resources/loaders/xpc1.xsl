<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cxp="http://xmlcalabash.com/ns/extensions/xpcparser"
                xmlns:p="http://www.w3.org/ns/xproc"
                exclude-result-prefixes="cxp"
                version="2.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"
	    omit-xml-declaration="yes"/>

<xsl:strip-space elements="*"/>

<xsl:template match="document">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="TOKEN | CName | quotedstr | xpcMarker | namespace"/>

<xsl:template match="pipeline">
  <p:pipeline version="{/document/xpcMarker/version}">
    <xsl:apply-templates select="/document" mode="namespaces"/>
    <xsl:apply-templates/>
  </p:pipeline>
</xsl:template>

<xsl:template match="pipelineBody">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="serialization">
  <p:serialization port="{cxp:quotedstr(quotedstr)}">
    <xsl:apply-templates select="withExtra"/>
  </p:serialization>
</xsl:template>

<xsl:template match="withExtra">
  <xsl:for-each select="attr">
    <!-- FIXME: what about names with prefixes! -->
    <xsl:attribute name="{QName}" select="cxp:quotedstr(quotedstr)"/>
  </xsl:for-each>
</xsl:template>

<xsl:template match="subpipeline">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="atomicStep[TOKEN]">
  <xsl:element name="p:{TOKEN}">
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<xsl:template match="atomicStep[CName]">
  <xsl:variable name="pfx" select="substring-before(CName, ':')"/>
  <xsl:variable name="ns" select="/document/namespace[prefix/NCName = $pfx]/quotedstr"/>

  <xsl:element name="{CName}" namespace="{cxp:quotedstr($ns)}">
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<xsl:template match="atomicStepBody">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="input">
  <p:input port="{cxp:quotedstr(quotedstr)}">
    <xsl:apply-templates/>
  </p:input>
</xsl:template>

<xsl:template match="binding">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="documentBinding">
  <p:document href="{cxp:quotedstr(quotedstr)}"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="cxp:quotedstr">
  <xsl:param name="quotedstr" as="xs:string"
             xmlns:xs="http://www.w3.org/2001/XMLSchema"/>

  <xsl:variable name="strip1" select="substring($quotedstr,2)"/>
  <xsl:variable name="str" select="substring($strip1, 1, string-length($strip1)-1)"/>

  <xsl:value-of select="$str"/>
</xsl:function>

<xsl:template match="document" mode="namespaces">
  <xsl:namespace name="p" select="'http://www.w3.org/ns/xproc'"/>

  <xsl:for-each select="namespace">
    <xsl:namespace name="{prefix}" select="cxp:quotedstr(quotedstr)"/>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
