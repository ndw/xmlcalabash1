<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:err="http://www.w3.org/ns/xproc-error"
		xmlns:tr="http://xproc.org/ns/testreport"
		xmlns="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
		exclude-result-prefixes="err tr xs"
                version="2.0">

<xsl:output method="xhtml" encoding="utf-8" indent="yes"/>

<xsl:preserve-space elements="*"/>

<xsl:variable name="errors"
	      select="document('/home/www/cache/error-list.xml')/err:error-list"/>

<xsl:template match="tr:test-report">
  <html>
    <head>
      <title>
	<xsl:value-of select="tr:title"/>
      </title>
      <link rel='stylesheet' type='text/css' href='http://tests.xproc.org/css/report.css' />
      <script type='text/javascript' src='http://tests.xproc.org/js/showtests.js'></script>
    </head>
    <body>
      <h1>
	<xsl:value-of select="tr:title"/>
      </h1>

      <xsl:variable name="total" select="count(//tr:pass)+count(//tr:fail)"/>
      <xsl:variable name="pass" select="count(//tr:pass)"/>

      <p>
        <span class="summary">
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
        </span>
      </p>

      <p><a id='hidepassed' href='javascript:hidepassed()'>Hide passed</a> /
         <a id="hidepartial" href="javascript:hidepartial()">Hide partial</a> /
         <a id='hidefailed' href='javascript:hidefailed()'>Hide failed</a></p>

      <xsl:apply-templates select="*[not(self::tr:date)]"/>
    </body>
  </html>
</xsl:template>

<xsl:template match="tr:test-report/tr:title"/>

<xsl:template match="tr:date">
  <span class="date" title="{.}">
    <xsl:choose>
      <xsl:when test=". castable as xs:dateTime">
	<xsl:value-of select="format-dateTime(xs:dateTime(.),  '[D01] [MNn,*-3] [Y0001] at [h01]:[m01][Pn,*-1]')"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="format-date(xs:date(.),  '[D01] [MNn,*-3] [Y0001]')"/>
      </xsl:otherwise>
    </xsl:choose>
  </span>
</xsl:template>

<xsl:template match="tr:processor">
  <div id="processor" class="processor">
    <div>
      <span class="title">Processor information</span>
      <span>&#160;&#160;(<a id='hideproc' href='javascript:hideprocessor()'>hide</a>)</span>
    </div>

    <dl>
      <xsl:apply-templates select="*" mode="procinfo"/>
    </dl>
  </div>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="tr:name" mode="procinfo">
  <dt>Name:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:vendor" mode="procinfo">
  <dt>Vendor:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:vendor-uri" mode="procinfo">
  <dt>Vendor URI</dt>
  <dd>
    <xsl:choose>
      <xsl:when test="starts-with(.,'http://tests.xproc.org/')">
	<a href="{substring-after(.,'http://tests.xproc.org')}">
	  <xsl:value-of select="."/>
	</a>
      </xsl:when>
      <xsl:otherwise>
	<a href="{.}">
	  <xsl:value-of select="."/>
	</a>
      </xsl:otherwise>
    </xsl:choose>
  </dd>
</xsl:template>

<xsl:template match="tr:version" mode="procinfo">
  <dt>Version:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:episode" mode="procinfo">
  <dt>Episode:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:language" mode="procinfo">
  <dt>Language:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:xproc-version" mode="procinfo">
  <dt>XProc Version:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:xpath-version" mode="procinfo">
  <dt>XPath Version:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<xsl:template match="tr:psvi-supported" mode="procinfo">
  <dt>PSVI Supported:</dt>
  <dd>
    <xsl:value-of select="."/>
  </dd>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="tr:test-suite">
  <div class="test-suite">
    <h2>
      <xsl:value-of select="tr:title"/>
    </h2>

    <xsl:variable name="total" select="count(tr:pass)+count(tr:fail)"/>
    <xsl:variable name="pass" select="count(tr:pass)"/>

      <p>
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
      </p>

    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="tr:test-suite/tr:title"/>

<xsl:template match="tr:pass">
  <div class='testcase'>
    <xsl:call-template name="header"/>
    <xsl:call-template name="messages"/>
  </div>
</xsl:template>

<xsl:template match="tr:pass[tr:error]">
  <div class='testcase'>
    <xsl:call-template name="header"/>

    <p>
      <xsl:text>Wrong error: expected </xsl:text>
      <xsl:value-of select="tr:error/@expected"/>
      <xsl:text> but </xsl:text>
      <xsl:value-of select="tr:error"/>
      <xsl:text> was raised.</xsl:text>
    </p>

    <xsl:call-template name="errors">
      <xsl:with-param name="errs"
		      select="(tr:error/@expected,tr:error)"/>
    </xsl:call-template>

    <xsl:call-template name="messages"/>
  </div>
</xsl:template>

<xsl:template match="tr:fail">
  <div class='testcase'>
    <xsl:call-template name="header"/>

    <xsl:if test="tr:error">
      <p>
	<xsl:text>Error: </xsl:text>
	<xsl:value-of select="tr:error"/>
	<xsl:text> was raised. </xsl:text>
      </p>

      <xsl:call-template name="errors">
	<xsl:with-param name="errs"
			select="(tr:error)"/>
      </xsl:call-template>
    </xsl:if>

    <xsl:if test="tr:expected|tr:actual">
      <table border="1" cellpadding="5" cellspacing="0">
	<tr>
	  <td>Expected result:</td>
	  <td>Actual result:</td>
	</tr>
	<tr>
	  <td valign="top" class="codelisting">
	    <pre>
	      <xsl:value-of select="tr:expected"/>
	    </pre>
	  </td>
	  <td valign="top" class="codelisting">
	    <pre>
	      <xsl:value-of select="tr:actual"/>
	    </pre>
	  </td>
	</tr>
      </table>
    </xsl:if>

    <xsl:call-template name="messages"/>
  </div>
</xsl:template>

<xsl:template match="tr:title">
  <h3>
    <xsl:choose>
      <xsl:when test="parent::tr:pass/tr:error">
	<span class="pass partial">PASS</span>
      </xsl:when>
      <xsl:when test="parent::tr:pass">
	<span class="pass">PASS</span>
      </xsl:when>
      <xsl:otherwise>
	<span class="fail">FAIL</span>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>&#160;</xsl:text>
    <xsl:value-of select="."/>
  </h3>
</xsl:template>

<xsl:template name="header">
  <xsl:apply-templates select="tr:title"/>
  <p>
    <a class="uri" href="{@uri}">
      <xsl:value-of select="@uri"/>
    </a>
  </p>
</xsl:template>

<xsl:template name="messages">
  <xsl:if test="tr:message">
    <h4>Error message<xsl:if test="count(tr:message)&gt;1">s</xsl:if></h4>
    <ul spacing="compact">
      <xsl:for-each select="tr:message">
	<li>
	  <xsl:value-of select="."/>
	</li>
      </xsl:for-each>
    </ul>
  </xsl:if>
</xsl:template>

<xsl:template name="errors">
  <xsl:param name="errs" as="xs:string+"/>

  <dl>
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
      <dt>
	<xsl:value-of select="$code"/>
      </dt>
      <dd>
	<xsl:value-of select="$errors/err:error[@code=$code]"/>
      </dd>
    </xsl:for-each>
  </dl>
</xsl:template>

</xsl:stylesheet>
