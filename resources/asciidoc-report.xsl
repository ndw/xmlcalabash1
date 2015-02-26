<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:err="http://www.w3.org/ns/xproc-error"
		xmlns:tr="http://xproc.org/ns/testreport"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:f="http://nwalsh.com/ns/functions"
		exclude-result-prefixes="err tr xs"
                version="2.0">
<xsl:output method="text" encoding="utf-8" indent="no"/>
<xsl:strip-space elements="*"/>

<xsl:param name="show-pass" select="'1'"/>
<xsl:param name="show-partial" select="'1'"/>
<xsl:param name="show-fail" select="'1'"/>

<xsl:variable name="errors"
	      select="document('../src/main/resources/etc/error-list.xml')/err:error-list"/>

<xsl:template match="tr:test-report">
  <xsl:sequence select="f:h1(string(tr:title))"/>

  <xsl:variable name="total" select="count(//tr:pass)+count(//tr:fail)"/>
  <xsl:variable name="pass" select="count(//tr:pass)"/>

  <xsl:text>Passed </xsl:text>
  <xsl:value-of select="$pass"/>
  <xsl:text> of </xsl:text>
  <xsl:value-of select="$total"/>
  <xsl:text> tests</xsl:text>
  <xsl:if test="$total &gt; 0">
    <xsl:text> (</xsl:text>
    <xsl:value-of select="format-number($pass div $total*100.0, '0.00')"/>
    <xsl:text>%)</xsl:text>
  </xsl:if>
  <xsl:text> on </xsl:text>
  <xsl:apply-templates select="tr:date"/>
  <xsl:text>.</xsl:text>
  <xsl:text>&#10;&#10;</xsl:text>

  <xsl:text>:toc: right&#10;&#10;</xsl:text>

  <xsl:choose>
    <xsl:when test="$show-pass = '0'
                    and $show-partial = '0'
                    and $show-fail != '0'">
      <xsl:text>This report only shows failing tests.</xsl:text>
      <xsl:text>&#10;&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="$show-pass != '0'
                    and $show-partial = '0'
                    and $show-fail = '0'">
      <xsl:text>This report only shows passing tests.</xsl:text>
      <xsl:text>&#10;&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="$show-pass = '0'
                    and $show-partial != '0'
                    and $show-fail = '0'">
      <xsl:text>This report only shows partially passing tests.</xsl:text>
      <xsl:text>&#10;&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="$show-pass = '0'
                    and $show-partial != '0'
                    and $show-fail != '0'">
      <xsl:text>This report suppresses tests that pass entirely.</xsl:text>
      <xsl:text>&#10;&#10;</xsl:text>
    </xsl:when>
    <xsl:when test="$show-pass != '0'
                    and $show-partial != '0'
                    and $show-fail != '0'">
      <!-- nop -->
    </xsl:when>
    <xsl:when test="$show-pass = '0'
                    and $show-partial = '0'
                    and $show-fail = '0'">
      <xsl:text>This report shows no tests.</xsl:text>
      <xsl:text>&#10;&#10;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>This report shows tests that:</xsl:text>
      <xsl:text>&#10;&#10;</xsl:text>
      <xsl:if test="$show-pass != '0'">* Pass&#10;</xsl:if>
      <xsl:if test="$show-partial != '0'">* Pass partially&#10;</xsl:if>
      <xsl:if test="$show-fail != '0'">* Fails&#10;</xsl:if>
      <xsl:text>&#10;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:apply-templates select="*[not(self::tr:date)]"/>
</xsl:template>

<xsl:template match="tr:test-report/tr:title"/>

<xsl:template match="tr:date">
  <xsl:choose>
    <xsl:when test=". castable as xs:dateTime">
      <xsl:value-of select="format-dateTime(xs:dateTime(.),  '[D01] [MNn,*-3] [Y0001] at [h01]:[m01][Pn,*-1]')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="format-date(xs:date(.),  '[D01] [MNn,*-3] [Y0001]')"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="tr:processor">
  <xsl:sequence select="f:attr(('cols','&lt;h,&lt;,&lt;h,&lt;'))"/>
  <xsl:text>|=============================================&#10;</xsl:text>
  <xsl:text>4+&lt;h|Processor information&#10;</xsl:text>
  <xsl:text>|Name|</xsl:text>
  <xsl:value-of select="tr:name"/>
  <xsl:text>|Language|</xsl:text>
  <xsl:value-of select="tr:language"/>
  <xsl:text>&#10;</xsl:text>

  <xsl:text>|Vendor|</xsl:text>
  <xsl:value-of select="tr:vendor"/>
  <xsl:text>|XProc version|</xsl:text>
  <xsl:value-of select="tr:xproc-version"/>
  <xsl:text>&#10;</xsl:text>

  <xsl:text>|Vendor URI|</xsl:text>
  <xsl:value-of select="tr:vendor-uri"/>
  <xsl:text>|XPath version|</xsl:text>
  <xsl:value-of select="tr:xpath-version"/>
  <xsl:text>&#10;</xsl:text>

  <xsl:text>|Version|</xsl:text>
  <xsl:value-of select="tr:version"/>
  <xsl:text>|PSVI Supported|</xsl:text>
  <xsl:value-of select="tr:psvi-supported"/>
  <xsl:text>&#10;</xsl:text>
  <xsl:text>|=============================================&#10;&#10;</xsl:text>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="tr:episode" mode="procinfo">
  <xsl:sequence select="f:line(concat('Episode:: ', normalize-space(.)))"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="tr:test-suite">
  <xsl:sequence select="f:h2(string(tr:title))"/>

  <xsl:variable name="total" select="count(tr:pass)+count(tr:fail)"/>
  <xsl:variable name="pass" select="count(tr:pass)"/>

  <xsl:text>Passed </xsl:text>
  <xsl:value-of select="$pass"/>
  <xsl:text> of </xsl:text>
  <xsl:value-of select="$total"/>
  <xsl:text> tests</xsl:text>
  <xsl:if test="$total &gt; 0">
    <xsl:text> (</xsl:text>
    <xsl:value-of select="format-number($pass div $total*100.0, '0.00')"/>
    <xsl:text>%)</xsl:text>
  </xsl:if>
  <xsl:text>.</xsl:text>
  <xsl:text>&#10;&#10;</xsl:text>

  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="tr:test-suite/tr:title"/>

<xsl:template match="tr:pass">
  <xsl:if test="$show-pass != '0'">
    <xsl:call-template name="header"/>
    <xsl:call-template name="messages"/>
  </xsl:if>
</xsl:template>

<xsl:template match="tr:pass[tr:error]">
  <xsl:if test="$show-partial != '0'">
    <xsl:call-template name="header"/>

    <xsl:text>Wrong error: expected </xsl:text>
    <xsl:value-of select="tr:error/@expected"/>
    <xsl:text> but </xsl:text>
    <xsl:value-of select="tr:error"/>
    <xsl:text> was raised.</xsl:text>
    <xsl:sequence select="f:nl()"/>

    <xsl:call-template name="errors">
      <xsl:with-param name="errs"
		      select="(tr:error/@expected,tr:error)"/>
    </xsl:call-template>

    <xsl:call-template name="messages"/>
  </xsl:if>
</xsl:template>

<xsl:template match="tr:fail">
  <xsl:if test="$show-fail != '0'">
    <xsl:call-template name="header"/>

    <xsl:if test="tr:error">
      <xsl:text>Error: </xsl:text>
      <xsl:value-of select="tr:error"/>
      <xsl:text> was raised. </xsl:text>
      <xsl:sequence select="f:nl()"/>

      <xsl:call-template name="errors">
        <xsl:with-param name="errs"
		        select="(tr:error)"/>
      </xsl:call-template>
    </xsl:if>

    <xsl:if test="tr:expected|tr:actual">
      <xsl:sequence select="f:attr(('frame', 'topbot', 'cols', 'd&lt;,d&lt;'))"/>
      <xsl:sequence select="f:line('|====================')"/>
      <xsl:text>|Expected result:|Actual result:&#10;</xsl:text>
      <xsl:text>l|</xsl:text>
      <xsl:value-of select="tr:expected"/>
      <xsl:sequence select="f:nl()"/>
      <xsl:text>l|</xsl:text>
      <xsl:value-of select="tr:actual"/>
      <xsl:sequence select="f:nl()"/>
      <xsl:sequence select="f:line('|====================')"/>
      <xsl:sequence select="f:nl()"/>
    </xsl:if>

    <xsl:call-template name="messages"/>
  </xsl:if>
</xsl:template>

<xsl:template match="tr:title">
  <xsl:choose>
    <xsl:when test="parent::tr:pass/tr:error">
      <xsl:sequence select="f:attr(('role','pass partial'))"/>
    </xsl:when>
    <xsl:when test="parent::tr:pass">
      <xsl:sequence select="f:attr(('role','pass'))"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="f:attr(('role','fail'))"/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="ancestor::tr:test-suite">
      <xsl:text>=== </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>== </xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="parent::tr:pass/tr:error">PASS </xsl:when>
    <xsl:when test="parent::tr:pass">PASS </xsl:when>
    <xsl:otherwise>FAIL </xsl:otherwise>
  </xsl:choose>

  <xsl:value-of select="."/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template name="header">
  <xsl:apply-templates select="tr:title"/>
  <xsl:sequence select="f:line(@uri)"/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template name="messages">
  <xsl:if test="tr:message">
    <xsl:choose>
      <xsl:when test="ancestor::tr:test-suite">
        <xsl:sequence select="f:h4(concat('Error message',
             if (count(tr:message) &gt; 1) then 's' else ''))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="f:h3(concat('Error message',
             if (count(tr:message) &gt; 1) then 's' else ''))"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:sequence select="f:nl()"/>
    <xsl:for-each select="tr:message">
      <xsl:text>* </xsl:text>
      <xsl:sequence select="f:line(normalize-space(.))"/>
    </xsl:for-each>
    <xsl:sequence select="f:nl()"/>
  </xsl:if>
</xsl:template>

<xsl:template name="errors">
  <xsl:param name="errs" as="xs:string+"/>

  <xsl:sequence select="f:nl()"/>
  <xsl:for-each select="$errs">
    <xsl:variable name="code" as="xs:string">
      <xsl:choose>
	<xsl:when test="contains(.,':')">
	  <xsl:value-of select="substring-after(.,':')"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:value-of select="."/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:text>* </xsl:text>
    <xsl:value-of select="$code"/>
    <xsl:text>: </xsl:text>
    <xsl:sequence
        select="f:line(normalize-space($errors/err:error[@code=$code]))"/>
  </xsl:for-each>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="f:h1">
  <xsl:param name="title" as="xs:string"/>
  <xsl:text>&#10;= </xsl:text>
  <xsl:value-of select="$title"/>
  <xsl:text>&#10;&#10;</xsl:text>
</xsl:function>

<xsl:function name="f:h2">
  <xsl:param name="title" as="xs:string"/>
  <xsl:text>&#10;== </xsl:text>
  <xsl:value-of select="$title"/>
  <xsl:text>&#10;&#10;</xsl:text>
</xsl:function>

<xsl:function name="f:h3">
  <xsl:param name="title" as="xs:string"/>
  <xsl:text>&#10;=== </xsl:text>
  <xsl:value-of select="$title"/>
  <xsl:text>&#10;&#10;</xsl:text>
</xsl:function>

<xsl:function name="f:h4">
  <xsl:param name="title" as="xs:string"/>
  <xsl:text>&#10;==== </xsl:text>
  <xsl:value-of select="$title"/>
  <xsl:text>&#10;&#10;</xsl:text>
</xsl:function>

<xsl:function name="f:attr">
  <xsl:param name="values" as="xs:string*"/>

  <xsl:text>[</xsl:text>

  <xsl:for-each select="$values">
    <xsl:if test="position() mod 2 = 1">
      <xsl:variable name="pos" select="position()"/>
      <xsl:if test="$pos &gt; 1">,</xsl:if>
      <xsl:value-of select="$values[$pos]"/>
      <xsl:text>=&quot;</xsl:text>
      <xsl:value-of select="$values[$pos+1]"/>
      <xsl:text>&quot;</xsl:text>
    </xsl:if>
  </xsl:for-each>

  <xsl:text>]</xsl:text>

  <xsl:text>&#10;</xsl:text>
</xsl:function>

<xsl:function name="f:line">
  <xsl:param name="text" as="xs:string"/>
  <xsl:value-of select="$text"/>
  <xsl:text>&#10;</xsl:text>
</xsl:function>

<xsl:function name="f:nl">
  <xsl:text>&#10;</xsl:text>
</xsl:function>

</xsl:stylesheet>
