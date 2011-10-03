<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
    
      <p:xslt>
        <p:input port="source">
          <p:inline>
            <doc/>
          </p:inline>
        </p:input>
        <p:input port="stylesheet">
          <p:inline>
            <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
              <xsl:param name="foo"/>
              <xsl:template match="/">
                <result><xsl:value-of select="$foo"/></result>
              </xsl:template>
            </xsl:stylesheet>
          </p:inline>
        </p:input>
        <p:with-param name="foo" select="'bar'">
          <p:empty/>
        </p:with-param>
      </p:xslt>

    </p:declare-step>