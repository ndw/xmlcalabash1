<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:f="http://docbook.org/xslt/ns/extension"
                xmlns:t="http://docbook.org/xslt/ns/template"
                xmlns:m="http://docbook.org/xslt/ns/mode"
                xmlns:tmpl="http://docbook.org/xslt/titlepage-templates"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="db f t m xs"
                version="2.0">

<xsl:import href="/projects/docbook/xslt20/xslt/base/html/chunk.xsl"/>
<xsl:import href="xproc.xsl"/>

<xsl:param name="refentry.separator" select="0"/>
<xsl:param name="resource.root" select="''"/>
<xsl:param name="html.stylesheets" select="'css/xproc.css'"/>
<xsl:param name="default.table.column.widths" select="0"/>

<xsl:param name="linenumbering" as="element()*">
<ln path="literallayout" everyNth="2" width="3" separator=" " padchar=" " minlines="3"/>
<ln path="programlisting" everyNth="2" width="3" separator=" " padchar=" " minlines="1"/>
<ln path="programlistingco" everyNth="2" width="3" separator=" " padchar=" " minlines="3"/>
<ln path="screen" everyNth="2" width="3" separator=" " padchar=" " minlines="3"/>
<ln path="synopsis" everyNth="2" width="3" separator=" " padchar=" " minlines="3"/>
<ln path="address" everyNth="0"/>
<ln path="epigraph/literallayout" everyNth="0"/>
</xsl:param>

<xsl:template name="t:user-titlepage-templates" as="element(tmpl:templates-list)?">
  <tmpl:templates-list>
    <tmpl:templates name="book">
      <tmpl:recto>
        <header tmpl:class="titlepage">
          <db:mediaobject/>
          <db:title/>
          <db:subtitle/>
          <db:corpauthor/>
          <db:authorgroup/>
          <db:author/>
          <db:editor/>
          <db:othercredit/>
          <db:releaseinfo/>
          <db:copyright/>
          <db:legalnotice/>
          <db:pubdate/>
          <db:revision/>
          <db:revhistory/>
          <db:abstract/>
        </header>
        <hr tmpl:keep="true"/>
      </tmpl:recto>
    </tmpl:templates>
  </tmpl:templates-list>
</xsl:template>

<xsl:function name="f:chunk-filename" as="xs:string">
  <xsl:param name="chunk" as="element()"/>

  <xsl:variable name="pifn" select="f:pi($chunk/processing-instruction('dbhtml'), 'filename')"/>

  <xsl:choose>
    <xsl:when test="$pifn != ''">
      <xsl:value-of select="$pifn"/>
    </xsl:when>
    <xsl:when test="$chunk/@xml:id">
      <xsl:value-of select="concat($chunk/@xml:id,'.html')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="concat('chunk-', local-name($chunk),
                            '-', generate-id($chunk), $html.ext)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<xsl:template name="t:user-header-content">
  <xsl:param name="node" select="."/>
  <xsl:param name="next" select="()"/>
  <xsl:param name="prev" select="()"/>
  <xsl:param name="up" select="()"/>
  <xsl:variable name="home" select="root($node)/*"/>

  <div class="navheader">
    <table border="0" cellpadding="0" cellspacing="0" width="100%"
           summary="Navigation table">
      <tr>
        <td align="left">
          <xsl:text>&#160;</xsl:text>
          <a title="{/db:book/db:info/db:title}" href="{f:href($node, $home)}">
            <img src="img/home.png" alt="Home" border="0"/>
          </a>
          <xsl:text>&#160;</xsl:text>

          <xsl:choose>
            <xsl:when test="exists($prev)">
              <a href="{f:href($node, $prev)}" title="{f:title-content($prev, false())}">
                <img src="img/prev.png" alt="Prev" border="0"/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <img src="img/xprev.png" alt="Prev" border="0"/>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>&#160;</xsl:text>

          <xsl:choose>
            <xsl:when test="count($up)>0">
              <a title="{f:title-content($up, false())}" href="{f:href($node, $up)}">
                <img src="img/up.png" alt="Up" border="0"/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <img src="img/xup.png" alt="Up" border="0"/>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>&#160;</xsl:text>

          <xsl:choose>
            <xsl:when test="count($next)>0">
              <a title="{f:title-content($next, false())}" href="{f:href($node, $next)}">
                <img src="img/next.png" alt="Next" border="0"/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <img src="img/xnext.png" alt="Next" border="0"/>
            </xsl:otherwise>
          </xsl:choose>
        </td>
        <td align="right">
          <i><xsl:value-of select="/db:book/db:info/db:title"/></i>
          <xsl:text> (Version </xsl:text>
          <xsl:value-of select="/db:book/db:info/db:releaseinfo[not(@role)]"/>
          <xsl:text>)</xsl:text>
        </td>
      </tr>
    </table>
  </div>
</xsl:template>

<xsl:template name="t:user-footer-content">
  <xsl:param name="node" select="."/>
  <xsl:param name="next" select="()"/>
  <xsl:param name="prev" select="()"/>
  <xsl:param name="up" select="()"/>
  <xsl:variable name="home" select="root($node)/*"/>

  <div class="navfooter">
    <table width="100%" summary="Navigation table">
      <tr>
        <td width="40%" align="left">
          <xsl:if test="count($prev)>0">
            <a title="{f:title-content($prev, false())}" href="{f:href($node, $prev)}">
              <img src="img/prev.png" alt="Prev" border="0"/>
            </a>
          </xsl:if>
          <xsl:text>&#160;</xsl:text>
        </td>
        <td width="20%" align="center">
          <xsl:choose>
            <xsl:when test="exists($home)">
              <a title="{f:title-content($home, false())}" href="{f:href($node, $home)}">
                <img src="img/home.png" alt="Home" border="0"/>
              </a>
            </xsl:when>
            <xsl:otherwise>&#160;</xsl:otherwise>
          </xsl:choose>
        </td>
        <td width="40%" align="right">
          <xsl:text>&#160;</xsl:text>
          <xsl:if test="count($next)>0">
            <a title="{f:title-content($next, false())}" href="{f:href($node, $next)}">
              <img src="img/next.png" alt="Next" border="0"/>
            </a>
          </xsl:if>
        </td>
      </tr>

      <tr>
        <td width="40%" align="left">
          <xsl:value-of select="f:title-content($prev, false())"/>
          <xsl:text>&#160;</xsl:text>
        </td>
        <td width="20%" align="center">
          <xsl:choose>
            <xsl:when test="count($up)>0">
              <a title="{f:title-content($up, false())}" href="{f:href($node, $up)}">
                <img src="img/up.png" alt="Up" border="0"/>
              </a>
            </xsl:when>
            <xsl:otherwise>&#160;</xsl:otherwise>
          </xsl:choose>
        </td>
        <td width="40%" align="right">
          <xsl:text>&#160;</xsl:text>
          <xsl:value-of select="f:title-content($next, false())"/>
        </td>
      </tr>
    </table>
  </div>
  <xsl:if test="$node/db:info/db:releaseinfo[@role='hash']">
    <div class="infofooter">
      <xsl:text>Last revised by </xsl:text>
      <xsl:value-of select="substring-before($node/db:info/db:releaseinfo[@role='author'],
                                             ' &lt;')"/>
      <xsl:text> on </xsl:text>
      <xsl:for-each select="$node/db:info/db:pubdate">
        <!-- hack: there should be only one -->
        <xsl:if test=". castable as xs:dateTime">
          <xsl:value-of select="format-dateTime(. cast as xs:dateTime,
                                                '[D1] [MNn,*-3] [Y0001]')"/>
        </xsl:if>
      </xsl:for-each>
      <xsl:text> </xsl:text>
      <span class="githash">
        <xsl:text>(git hash: </xsl:text>
        <xsl:value-of select="$node/db:info/db:releaseinfo[@role='hash']"/>
        <xsl:text>)</xsl:text>
      </span>
    </div>
  </xsl:if>
  <div class="copyrightfooter">
    <p>
      <a href="dbcpyright.html">Copyright</a>
      <xsl:text> &#xA9; </xsl:text>
      <xsl:for-each select="/db:book/db:info/db:copyright/db:year">
        <xsl:if test="position() &gt; 1">, </xsl:if>
        <xsl:value-of select="."/>
      </xsl:for-each>
      <xsl:text> Norman Walsh.</xsl:text>
    </p>
  </div>
</xsl:template>

<xsl:function name="f:title-content" as="node()*">
  <xsl:param name="node" as="node()?"/>
  <xsl:param name="allow-anchors" as="xs:boolean"/>
  <xsl:choose>
    <xsl:when test="empty($node)"/>
    <xsl:otherwise>
      <xsl:apply-templates select="$node" mode="m:title-content">
        <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<xsl:template match="db:releaseinfo[not(@role)]" mode="m:titlepage-mode">
  <p class="releaseinfo">
    <xsl:text>Reference version </xsl:text>
    <xsl:apply-templates/>
  </p>
</xsl:template>

<xsl:template match="db:releaseinfo[@role='xml-calabash-version']" mode="m:titlepage-mode">
  <p class="releaseinfo">
    <xsl:text>For XML Calabash version </xsl:text>
    <xsl:apply-templates/>
  </p>
</xsl:template>

<xsl:template match="db:pubdate" mode="m:titlepage-mode">
  <p class="pubdate">
    <xsl:text>Published </xsl:text>
    <xsl:value-of select='format-date(xs:date(.), "[D01] [MNn,*-3] [Y0001]")'/>
  </p>
  <br clear="all"/>
</xsl:template>

<xsl:template match="db:mediaobject" mode="m:titlepage-mode">
  <div class="cover">
    <xsl:apply-templates select="."/>
  </div>
</xsl:template>

</xsl:stylesheet>
