<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
		xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
		exclude-result-prefixes="e xs"
                version="2.0">

<xsl:strip-space elements="e:*"/>

<xsl:template match="e:element-syntax">
  <p class="element-syntax {if (e:in-category/@name)
                           then concat('element-syntax-', e:in-category/@name)
	                   else 'element-syntax'}">
    <xsl:choose>
      <xsl:when test="e:in-category/@name = 'other-step'">
	<code>
	  <em>
	    <xsl:text>&lt;pfx:</xsl:text><xsl:value-of select="@name"/>
	  </em>
	  <xsl:apply-templates mode="top"/>
	</code>
      </xsl:when>
      <xsl:when test="e:in-category/@name = 'step-vocabulary'">
	<code>
	  <xsl:text>&lt;c:</xsl:text><xsl:value-of select="@name"/>
	  <xsl:apply-templates mode="top"/>
	</code>
      </xsl:when>
      <xsl:when test="e:in-category/@name = 'error-vocabulary'">
	<code>
	  <xsl:text>&lt;err:</xsl:text><xsl:value-of select="@name"/>
	  <xsl:apply-templates mode="top"/>
	</code>
      </xsl:when>
      <xsl:otherwise>
	<code>
	  <xsl:text>&lt;p:</xsl:text><xsl:value-of select="@name"/>
	  <xsl:apply-templates mode="top"/>
	</code>
      </xsl:otherwise>
    </xsl:choose>
  </p>
</xsl:template>

<xsl:template match="e:in-category">
  <xsl:text>&lt;!-- Category: </xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text> --&gt;</xsl:text>
  <br/>
</xsl:template>

<xsl:template match="e:sequence|e:choice|e:model|e:element|e:text" mode="top">
  <xsl:text>&gt;</xsl:text>
  <br/>
  <xsl:text>&#160;&#160;&#160;</xsl:text>
  <xsl:apply-templates select="."/>
  <br/>

  <xsl:choose>
    <xsl:when test="../e:in-category/@name = 'other-step'">
      <em>
	<xsl:text>&lt;/pfx:</xsl:text>
	<xsl:value-of select="../@name"/>
      </em>
      <xsl:text>&gt;</xsl:text>
    </xsl:when>
    <xsl:when test="../e:in-category/@name = 'step-vocabulary'">
      <xsl:text>&lt;/c:</xsl:text>
      <xsl:value-of select="../@name"/>
      <xsl:text>&gt;</xsl:text>
    </xsl:when>
    <xsl:when test="../e:in-category/@name = 'error-vocabulary'">
      <xsl:text>&lt;/err:</xsl:text>
      <xsl:value-of select="../@name"/>
      <xsl:text>&gt;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>&lt;/p:</xsl:text>
      <xsl:value-of select="../@name"/>
      <xsl:text>&gt;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>

<xsl:template match="e:sequence|e:choice">
  <xsl:call-template name="group"/>
  <xsl:text>(</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>)</xsl:text>
  <xsl:call-template name="repeat"/>
</xsl:template>

<xsl:template match="e:model">
  <xsl:call-template name="group"/>
  <var><xsl:value-of select="@name"/></var>
  <xsl:call-template name="repeat"/>
</xsl:template>

<xsl:template match="e:text">#PCDATA</xsl:template>

<xsl:template match="e:element">
  <xsl:variable name="category" select="ancestor::e:element-syntax/e:in-category/@name"/>
  <xsl:variable name="pfx" as="xs:string">
    <xsl:choose>
      <xsl:when test="$category = 'step-vocabulary'">c</xsl:when>
      <xsl:when test="$category = 'error-vocabulary'">err</xsl:when>
      <xsl:otherwise>p</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="idpfx" as="xs:string?">
    <xsl:choose>
      <xsl:when test="$category = 'step-vocabulary'">cv</xsl:when>
      <xsl:when test="$category = 'error-vocabulary'">err</xsl:when>
      <xsl:otherwise>p</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$idpfx = ''">
      <xsl:call-template name="group"/>
      <xsl:value-of select="$pfx"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:call-template name="repeat"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="not(key('id',concat($idpfx, '.',@name)))">
	<xsl:message>
	  <xsl:text>No description of </xsl:text>
	  <xsl:value-of select="@name"/>
	  <xsl:text> (</xsl:text>
	  <xsl:value-of select="concat($idpfx, '.',@name)"/>
	  <xsl:text>)</xsl:text>
	</xsl:message>
      </xsl:if>

      <xsl:call-template name="group"/>
      <a href="#{$idpfx}.{@name}">
	<xsl:value-of select="$pfx"/>
	<xsl:text>:</xsl:text>
	<xsl:value-of select="@name"/>
      </a>
      <xsl:call-template name="repeat"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="group">
  <xsl:if test="position()>1">
    <xsl:choose>
      <xsl:when test="parent :: e:sequence">, </xsl:when>
      <xsl:when test="parent :: e:choice"> | </xsl:when>
    </xsl:choose>
    <br/>
    <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template name="repeat">
  <xsl:choose>
   <xsl:when test="@repeat='one-or-more'">
    <xsl:text>+</xsl:text>
   </xsl:when>
   <xsl:when test="@repeat='zero-or-more'">
    <xsl:text>*</xsl:text>
   </xsl:when>
   <xsl:when test="@repeat='zero-or-one'">
    <xsl:text>?</xsl:text>
   </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="e:empty" mode="top">
  <xsl:text>&#160;/&gt;</xsl:text>
</xsl:template>

<xsl:template match="e:attribute" mode="top">
  <br/>
  <xsl:text>&#160;&#160;</xsl:text>
  <xsl:choose>
    <xsl:when test="@required='yes'">
      <b><xsl:value-of select="@name"/></b>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="@name"/><xsl:text>?</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text> = </xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="e:data-type">
  <xsl:if test="position()>1"> | </xsl:if>
  <var><xsl:value-of select="@name"/></var>
</xsl:template>

<xsl:template match="e:constant">
  <xsl:if test="position()>1"> | </xsl:if>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@value"/>
  <xsl:text>"</xsl:text>
</xsl:template>

<xsl:template match="e:attribute-value-template">
  <xsl:text>{ </xsl:text>
  <xsl:apply-templates/>
  <xsl:text> }</xsl:text>
</xsl:template>

<xsl:template match="e:allowed-parents" mode="top"/>

<xsl:template match="e:allowed-parents">
  <p><i>Permitted parent elements:</i></p>
  <ul>
    <xsl:apply-templates/>
    <xsl:if test="not(*)"><li>None</li></xsl:if>
  </ul>
</xsl:template>

<xsl:template match="e:parent">
  <li><code>p:<xsl:value-of select="@name"/></code></li>
</xsl:template>

<xsl:template match="e:parent-category[@name='sequence-constructor']">
  <li>any XSLT element whose content model is <i>sequence constructor</i></li>
  <li>any literal result element</li>
</xsl:template>

</xsl:stylesheet>
