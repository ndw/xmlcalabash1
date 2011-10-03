<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns="http://example.net/" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
    <p:output port="result"/>
    <p:xslt>
      <p:input port="source">
        <p:inline>
          <doc/>
        </p:inline>
      </p:input>
      <p:with-param name="param" select="'foo'"/>
      <p:input port="stylesheet">
        <p:inline>
          <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
            <xsl:param name="param"/>
            <xsl:template match="/*">
              <doc value="{$param}"/>
            </xsl:template>
          </xsl:stylesheet>
        </p:inline>
      </p:input>
    </p:xslt>
  </p:declare-step>