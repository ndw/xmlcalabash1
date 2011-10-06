<?xml version='1.0' encoding='UTF-8'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:sa="http://xproc.org/ns/syntax-annotations"
                xmlns:ss="http://xproc.org/ns/syntax-summary"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:rng="http://relaxng.org/ns/structure/1.0"
                xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
                xmlns:f="http://docbook.org/xslt/ns/extension"
                exclude-result-prefixes="sa xs e rng ss f" version="2.0">

<xsl:strip-space elements="rng:*"/>

<!-- ============================================================ -->

<!--
<xsl:template match="/">
  <xsl:variable name="pattern" as="element()">
    <e:rng-pattern name="Viewport"/>
  </xsl:variable>

  <xsl:apply-templates select="$pattern"/>
</xsl:template>
-->

<xsl:template match="e:rng-fragment">
  <xsl:param name="class"/>
  <xsl:call-template name="e:rng-pattern">
    <xsl:with-param name="schema">
      <xsl:copy-of select="*"/>
    </xsl:with-param>
    <xsl:with-param name="class" select="$class"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="e:rng-pattern" name="e:rng-pattern">
  <xsl:param name="schema"/>
  <xsl:param name="class"/>

  <xsl:variable name="pattern" select="@name"/>

  <xsl:variable name="schemafile" select="'../docs/schemas/xproc.rng'"/>

  <xsl:variable name="theschema" as="document-node()">
    <xsl:choose>
      <xsl:when test="$schema">
	<xsl:sequence select="$schema"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:sequence select="document($schemafile)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="rngpat" select="$theschema/rng:grammar/rng:define[@name=$pattern]"/>

  <xsl:if test="not($rngpat) or not($rngpat/rng:element)">
    <xsl:message>
      <xsl:text>Warning: Can't make syntax summary for </xsl:text>
      <xsl:value-of select="$pattern"/>
    </xsl:message>
  </xsl:if>

  <xsl:variable name="summary-body" as="element()*">
    <xsl:apply-templates select="$rngpat/rng:element/*">
      <xsl:with-param name="schema" select="$theschema" tunnel="yes"/>
    </xsl:apply-templates>
  </xsl:variable>

  <!--
  <xsl:result-document href="rng.xml" method="xml" indent="yes">
    <rng>
      <xsl:copy-of select="$summary-body"/>
    </rng>
  </xsl:result-document>
  -->

  <xsl:variable name="summary" as="element()*">
    <ss:element-summary name="{$rngpat/rng:element/@name}">
     <xsl:if test="@xml:id and not($class='step-vocabulary')">
      <!-- xml:id support added by HST
           The class test avoids duplicates in the summary appendix-->
      <xsl:attribute name="xml:id" select="@xml:id"/>
     </xsl:if>
      <xsl:choose>
	<xsl:when test="not($rngpat/rng:element/@name)">
	  <!-- special case -->
	  <xsl:choose>
	    <xsl:when test="ancestor::db:section[@xml:id='p.atomic']"
		      xmlns:db="http://docbook.org/ns/docbook">
	      <xsl:attribute name="prefix" select="'p'"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:attribute name="prefix" select="'pfx'"/>
	    </xsl:otherwise>
	  </xsl:choose>
	  <xsl:attribute name="class" select="'language-construct'"/>
	</xsl:when>
	<xsl:when test="not($rngpat/@sa:class) or $rngpat/@sa:class = 'language-construct'">
	  <xsl:attribute name="prefix" select="'p'"/>
	  <xsl:attribute name="class" select="'language-construct'"/>
	</xsl:when>
	<xsl:when test="$rngpat/@sa:class = 'language-example'">
	  <xsl:attribute name="prefix" select="'p'"/>
	  <xsl:attribute name="class" select="$rngpat/@sa:class"/>
	</xsl:when>
	<xsl:when test="$rngpat/@sa:class = 'step-vocabulary'">
	  <xsl:attribute name="prefix" select="'c'"/>
	  <xsl:attribute name="class" select="$rngpat/@sa:class"/>
	</xsl:when>
	<xsl:when test="$rngpat/@sa:class = 'error-vocabulary'">
	  <xsl:attribute name="prefix" select="'c'"/>
	  <xsl:attribute name="class" select="$rngpat/@sa:class"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:attribute name="prefix" select="'x'"/>
	</xsl:otherwise>
      </xsl:choose>
      <xsl:copy-of select="$summary-body[self::ss:attribute]"/>
      <xsl:if test="$summary-body[not(self::ss:attribute)]">
	<ss:content-model>
	  <xsl:copy-of select="$summary-body[not(self::ss:attribute)]"/>
	</ss:content-model>
      </xsl:if>
    </ss:element-summary>
  </xsl:variable>

<!--
  <xsl:message>
    <xsl:copy-of select="$summary"/>
  </xsl:message>
-->

  <xsl:if test="not($class) or $rngpat/@sa:class = $class">
    <xsl:apply-templates select="$summary"/>
  </xsl:if>
</xsl:template>

<xsl:template match="rng:element">
  <xsl:param name="repeat" select="''"/>
  <ss:element name="{@name}" repeat="{$repeat}"/>
</xsl:template>

<xsl:template match="rng:anyName"/>

<xsl:template match="rng:optional">
  <xsl:choose>
    <xsl:when test="count(*) &gt; 1">
      <ss:group type="sequence" repeat="?">
	<xsl:apply-templates/>
      </ss:group>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates>
	<xsl:with-param name="repeat" select="'?'"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="rng:ref">
  <xsl:param name="repeat" select="''"/>
  <xsl:param name="schema" tunnel="yes"/>

  <xsl:variable name="pattern" select="@name"/>
  <xsl:variable name="rngpat" select="$schema/rng:grammar/rng:define[@name=$pattern]"/>

  <xsl:choose>
    <xsl:when test="$rngpat/@sa:ignore = 'yes'">
      <!-- nop -->
    </xsl:when>
    <xsl:when test="$rngpat/@sa:model">
      <ss:model name="{$rngpat/@sa:model}" repeat="{$repeat}"/>
    </xsl:when>
    <xsl:when test="$rngpat/@sa:element">
      <ss:element name="{$rngpat/@sa:element}" repeat="{$repeat}"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$rngpat/*">
	<xsl:with-param name="repeat" select="$repeat"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="rng:choice">
  <xsl:param name="repeat" select="''"/>

  <xsl:variable name="content" as="element()*">
    <xsl:for-each select="*">
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:if test="$content">
    <ss:group type="choice" repeat="{$repeat}">
      <xsl:sequence select="$content"/>
    </ss:group>
  </xsl:if>
</xsl:template>

<xsl:template match="rng:group">
  <xsl:param name="repeat" select="''"/>

  <ss:group type="sequence" repeat="{$repeat}">
    <xsl:for-each select="*">
      <xsl:apply-templates select="."/>
    </xsl:for-each>
  </ss:group>
</xsl:template>

<xsl:template match="rng:zeroOrMore">
  <xsl:apply-templates>
    <xsl:with-param name="repeat" select="'*'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="rng:oneOrMore">
  <xsl:apply-templates>
    <xsl:with-param name="repeat" select="'+'"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="rng:interleave">
  <xsl:param name="repeat" select="''"/>
  <ss:group type="interleave" repeat="{$repeat}">
    <xsl:apply-templates>
      <xsl:with-param name="repeat" select="$repeat"/>
    </xsl:apply-templates>
  </ss:group>
</xsl:template> 

<xsl:template match="rng:attribute[@name]" priority="10">
  <xsl:param name="schema" tunnel="yes"/>
  <xsl:param name="repeat" select="''"/>

  <ss:attribute name="{@name}" optional="{$repeat}">
    <xsl:attribute name="type">
      <xsl:choose>
	<xsl:when test="rng:data">
	  <xsl:apply-templates/>
	</xsl:when>
	<xsl:when test="rng:ref">
	  <xsl:variable name="pattern" select="rng:ref/@name"/>
	  <xsl:variable name="rngpat" select="$schema/rng:grammar/rng:define[@name=$pattern]"/>
	  <xsl:choose>
	    <xsl:when test="$rngpat/@sa:model">
	      <xsl:value-of select="$rngpat/@sa:model"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:message>
		<xsl:text>Warning: unsupported ref in attribute: </xsl:text>
		<xsl:value-of select="@name"/>
	      </xsl:message>
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:when>
	<xsl:when test="rng:choice/rng:value">
	  <xsl:for-each select="rng:choice/rng:value|rng:choice/rng:data">
	    <xsl:if test="position()&gt;1">|</xsl:if>
	    <xsl:choose>
	      <xsl:when test="self::rng:value">
		<xsl:value-of select="."/>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:text>xs:</xsl:text>
		<xsl:value-of select="@type"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:for-each>
	</xsl:when>
	<xsl:when test="rng:value">
	  <xsl:text>"</xsl:text>
	  <xsl:value-of select="rng:value"/>
	  <xsl:text>"</xsl:text>
	</xsl:when>
	<xsl:when test="rng:text or not(*)">
	  <xsl:text>string</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:message>
	    <xsl:text>Warning: unsupported content in attribute: </xsl:text>
	    <xsl:value-of select="@name"/>
	    <xsl:text> (</xsl:text>
	    <xsl:value-of select="ancestor::rng:element/@name"/>
	    <xsl:text>)</xsl:text>
	  </xsl:message>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </ss:attribute>
</xsl:template>

<xsl:template match="rng:attribute">
  <xsl:param name="schema" tunnel="yes"/>
  <xsl:param name="repeat" select="''"/>
  <!-- suppress -->
</xsl:template>

<xsl:template match="rng:data">
  <xsl:value-of select="@type"/>
</xsl:template> 

<xsl:template match="rng:empty"/>

<xsl:template match="rng:text">
  <ss:model name="string" repeat=""/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="ss:element-summary">
  <p>
    <xsl:sequence select="f:html-attributes(., @xml:id)"/>
    <xsl:attribute name="class">
      <xsl:text>element-syntax</xsl:text>
      <xsl:if test="@class">
	<xsl:text> element-syntax-</xsl:text>
	<xsl:value-of select="@class"/>
      </xsl:if>
    </xsl:attribute>
    <code>
      <xsl:text>&lt;</xsl:text>

      <xsl:choose>
	<xsl:when test="@name != ''">
	  <xsl:value-of select="@prefix"/>
	  <xsl:text>:</xsl:text>
	  <xsl:value-of select="if (contains(@name,':'))
				then substring-after(@name,':')
				else @name"/>
	</xsl:when>
	<xsl:when test=".//ss:model[@name='subpipeline']">
	  <var>
	    <xsl:value-of select="@prefix"/>
	    <xsl:text>:compound-step</xsl:text>
	  </var>
	</xsl:when>
	<xsl:otherwise>
	  <var>
	    <xsl:value-of select="@prefix"/>
	    <xsl:text>:atomic-step</xsl:text>
	  </var>
	</xsl:otherwise>
      </xsl:choose>

      <xsl:apply-templates select="ss:attribute"/>

      <xsl:choose>
	<xsl:when test="*[not(self::ss:attribute)]">
	  <xsl:text>&gt;</xsl:text>
	  <br/>
	  <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
	  <xsl:apply-templates select="*[not(self::ss:attribute)]"/>
	  <xsl:text>&lt;/</xsl:text>

	  <xsl:choose>
	    <xsl:when test="@name != ''">
	      <xsl:value-of select="@prefix"/>
	      <xsl:text>:</xsl:text>
	      <xsl:value-of select="if (contains(@name,':'))
				    then substring-after(@name,':')
				    else @name"/>
	    </xsl:when>
	    <xsl:when test=".//ss:model[@name='subpipeline']">
	      <var>
		<xsl:value-of select="@prefix"/>
		<xsl:text>:compound-step</xsl:text>
	      </var>
	    </xsl:when>
	    <xsl:otherwise>
	      <var>
		<xsl:value-of select="@prefix"/>
		<xsl:text>:atomic-step</xsl:text>
	      </var>
	    </xsl:otherwise>
	  </xsl:choose>

	  <xsl:text>&gt;</xsl:text>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:text>&#160;/&gt;</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
    </code>
  </p>
</xsl:template>

<xsl:template match="ss:attribute">
  <br/>
  <xsl:text>&#160;&#160;</xsl:text>
  <xsl:choose>
    <xsl:when test="@optional = ''">
      <strong>
	<xsl:value-of select="@name"/>
      </strong>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="@name"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:value-of select="@optional"/>
  <xsl:text> = </xsl:text>

  <!-- hack! -->
  <xsl:choose>
    <xsl:when test="contains(@type,'&quot;')">
      <xsl:value-of select="@type"/>
    </xsl:when>
    <xsl:otherwise>
      <var>
	<xsl:value-of select="@type"/>
      </var>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="ss:content-model">
  <xsl:apply-templates/>
  <br/>
</xsl:template>

<xsl:template match="ss:group">
  <xsl:choose>
    <xsl:when test="count(*) &gt; 1">
      <xsl:text>(</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:if test="count(*) &gt; 0">
    <xsl:value-of select="@repeat"/>
    <xsl:call-template name="separator"/>
  </xsl:if>
</xsl:template>

<xsl:template match="ss:model">
  <var>
    <xsl:value-of select="@name"/>
  </var>
  <xsl:value-of select="@repeat"/>
  <xsl:call-template name="separator"/>
</xsl:template>

<xsl:template match="ss:element">
  <xsl:variable name="prefix">
    <xsl:choose>
      <xsl:when test="ancestor::ss:element-summary/@class = 'language-example'">
	<xsl:text></xsl:text>
      </xsl:when>
      <xsl:when test="ancestor::ss:element-summary/@class = 'language-construct'">
	<xsl:text>p</xsl:text>
      </xsl:when>
      <xsl:when test="ancestor::ss:element-summary/@class = 'step-vocabulary'">
	<xsl:text>c</xsl:text>
      </xsl:when>
      <xsl:when test="ancestor::ss:element-summary/@class = 'error-vocabulary'">
	<xsl:text>c</xsl:text>
      </xsl:when>
      <xsl:otherwise>x</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="idpfx">
    <xsl:choose>
      <xsl:when test="ancestor::ss:element-summary/@class = 'language-example'
		      or starts-with(@name,'xs:')">
	<xsl:text></xsl:text>
      </xsl:when>
      <xsl:when test="ancestor::ss:element-summary/@class = 'language-construct'">
	<xsl:text>p.</xsl:text>
      </xsl:when>
      <xsl:when test="ancestor::ss:element-summary/@class = 'step-vocabulary'">
	<xsl:text>cv.</xsl:text>
      </xsl:when>
      <xsl:when test="ancestor::ss:element-summary/@class = 'error-vocabulary'">
	<xsl:text>cv.</xsl:text>
      </xsl:when>
      <xsl:otherwise>
	<xsl:text></xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="basename" select="if (contains(@name,':'))
			then substring-after(@name,':')
			else @name"/>

  <xsl:choose>
    <xsl:when test="$idpfx = ''">
      <var>
	<xsl:value-of select="$basename"/>
	<xsl:value-of select="@repeat"/>
      </var>
    </xsl:when>
    <xsl:otherwise>
      <a href="#{$idpfx}{$basename}">
	<xsl:value-of select="concat($prefix,':',$basename)"/>
      </a>
      <xsl:value-of select="@repeat"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:call-template name="separator"/>
</xsl:template>

<xsl:template name="separator">
  <xsl:choose>
    <xsl:when test="not(following-sibling::*)"/>
    <xsl:when test="parent::ss:group[@type='choice']">
      <xsl:text> | </xsl:text>
      <br/>
      <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      <xsl:for-each select="ancestor::ss:group">&#160;</xsl:for-each>
    </xsl:when>
    <xsl:when test="parent::ss:group[@type='interleave']">
      <xsl:text> &amp; </xsl:text>
      <br/>
      <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      <xsl:for-each select="ancestor::ss:group">&#160;</xsl:for-each>
    </xsl:when>
    <xsl:when test="parent::ss:group[@type='sequence']|parent::ss:content-model">
      <xsl:text>,</xsl:text>
      <br/>
      <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
      <xsl:for-each select="ancestor::ss:group">&#160;</xsl:for-each>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<!-- 
   <ss:element-summary name="pipeline" prefix="p">
      <ss:attribute name="name" type="QName" optional="yes"/>
      <ss:attribute name="p:ignore-prefixes" type="NMTOKENS" optional="yes"/>
      <ss:attribute name="xml:id" type="ID" optional="yes"/>
      <ss:attribute name="xml:base" type="anyURI" optional="yes"/>
      <ss:content-model>
         <ss:group type="zeroOrMore">
            <ss:group type="choice" optional="no">
               <ss:element name="input"/>
               <ss:element name="output"/>
               <ss:element name="parameter"/>
               <ss:element name="import"/>
               <ss:element name="declare-step"/>
               <ss:element name="p:doc"/>
            </ss:group>
         </ss:group>
         <ss:model name="subpipeline"/>
      </ss:content-model>
   </ss:element-summary>
-->

</xsl:stylesheet>
