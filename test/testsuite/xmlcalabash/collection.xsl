<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:x="http://example.com/ns/template"
		version="2.0">

<xsl:output method="xml" indent="yes"/>

<xsl:template name="x:root">
  <root>
    <xsl:value-of select="count(collection('http://xmlcalabash.com/collections/example'))"/>
  </root>
</xsl:template>

</xsl:stylesheet>
