<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0">
<p:input port="parameters" kind="parameter"/>
<p:output port="result"/>
<p:serialization port="result" indent="true"/>

<p:identity>
  <p:input port="source">
    <p:inline>
      <doc>
        <para>Some <emph>text</emph> in a paragraph.</para>
      </doc>
    </p:inline>
  </p:input>
</p:identity>

<p:xslt template-name="root">
  <p:input port="stylesheet">
    <p:inline>
      <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                      version="2.0">

        <xsl:param name="text" required="yes"/>
        <xsl:param name="gv" required="yes"/>

        <xsl:template name="root">
          <root general-values="{$gv}">
            <xsl:sequence select="$text"/>
          </root>
        </xsl:template>
      </xsl:stylesheet>
    </p:inline>
  </p:input>
  <p:with-param name="text" select="/doc/para"/>
  <p:with-param name="gv" select="p:system-property('cx:general-values')"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"/>
</p:xslt>

</p:declare-step>
