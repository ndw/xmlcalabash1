<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:f="http://nwalsh.com/ns/functions"
                xmlns:pr="http://xmlcalabash.com/ns/piperack"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
		exclude-result-prefixes="f pr xs"
                version="2.0">

<xsl:param name="format" select="'text'"/>

<xsl:template match="/">
  <xsl:choose>
    <xsl:when test="$format = 'text'">
      <xsl:result-document method="text">
        <xsl:apply-templates mode="text"/>
      </xsl:result-document>
    </xsl:when>
    <xsl:when test="$format = 'html'">
      <xsl:result-document method="html">
        <xsl:apply-templates mode="html"/>
      </xsl:result-document>
    </xsl:when>
    <xsl:when test="$format = 'json'">
      <xsl:result-document method="text">
        <xsl:apply-templates mode="json"/>
      </xsl:result-document>
    </xsl:when>
    <xsl:otherwise>
      <!-- $format = 'xml' -->
      <xsl:result-document method="xml">
        <xsl:sequence select="/"/>
      </xsl:result-document>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- == TEXT ========================================================== -->

<xsl:template match="pr:pipeline" mode="text">
  <xsl:value-of select="pr:uri"/>
  <xsl:choose>
    <xsl:when test="pr:has-run = 'true'"> has run</xsl:when>
    <xsl:otherwise> has not run</xsl:otherwise>
  </xsl:choose>
  <xsl:text>&#10;</xsl:text>
  <xsl:if test="pr:output">Outputs:&#10;</xsl:if>
  <xsl:apply-templates select="pr:output" mode="text"/>
  <xsl:if test="pr:input">Inputs:&#10;</xsl:if>
  <xsl:apply-templates select="pr:input" mode="text"/>
  <xsl:if test="pr:option">Options:&#10;</xsl:if>
  <xsl:apply-templates select="pr:option" mode="text"/>
  <xsl:if test="pr:parameter">Parameters:&#10;</xsl:if>
  <xsl:apply-templates select="pr:parameter" mode="text"/>
</xsl:template>

<xsl:template match="pr:input" mode="text">
  <xsl:text>  </xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>: </xsl:text>
  <xsl:if test="@primary = 'true'">
    <xsl:text>primary; </xsl:text>
  </xsl:if>
  <xsl:value-of select="@documents"/>
  <xsl:text> document(s) available&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:output" mode="text">
  <xsl:text>  </xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>: </xsl:text>
  <xsl:if test="@primary = 'true'">
    <xsl:text>primary; </xsl:text>
  </xsl:if>
  <xsl:value-of select="@documents"/>
  <xsl:text> document(s) remain&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:option|pr:parameter" mode="text">
  <xsl:text>  </xsl:text>
  <xsl:value-of select="pr:name"/>

  <xsl:if test="contains(pr:name, ':')">
    <xsl:text> (namespace = "</xsl:text>
    <xsl:value-of select="namespace-uri-for-prefix(substring-before(pr:name,':'), pr:name)"/>
    <xsl:text>")</xsl:text>
  </xsl:if>

  <xsl:choose>
    <xsl:when test="pr:value/@initialized='true'">
      <xsl:text> (initialized to general value)</xsl:text>
    </xsl:when>
    <xsl:when test="pr:value/@default='true'">
      <xsl:text> (no value provided; default will be used)</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>: </xsl:text>
      <xsl:value-of select="pr:value"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:status" mode="text">
  <xsl:text>XML Calabash version </xsl:text>
  <xsl:value-of select="pr:version"/>
  <xsl:text>, an XProc processor.&#10;</xsl:text>
  <xsl:text>Running on Saxon version </xsl:text>
  <xsl:value-of select="pr:saxon-version"/>
  <xsl:text>, </xsl:text>
  <xsl:value-of select="pr:saxon-edition"/>
  <xsl:text> edition.&#10;</xsl:text>
  <xsl:text>Copyright </xsl:text>
  <xsl:value-of select="pr:copyright"/>
  <xsl:text>&#10;</xsl:text>
  <xsl:apply-templates select="pr:message" mode="text"/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:message" mode="text">
  <xsl:value-of select="."/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:pipelines" mode="text">
  <xsl:for-each select="pr:pipeline">
    <xsl:value-of select="pr:uri"/>
    <xsl:text> (</xsl:text>
    <xsl:choose>
      <xsl:when test="pr:has-run = 'true'">
        <xsl:text>has run</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>has not run</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="pr:expires">
      <xsl:text>; expires: </xsl:text>
      <xsl:value-of select="pr:expires"/>
    </xsl:if>
    <xsl:text>)&#10;</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template match="pr:help" mode="text">
  <xsl:for-each select="pr:endpoint">
    <xsl:value-of select="pr:uri"/>
    <xsl:text> (</xsl:text>
    <xsl:value-of select="lower-case(pr:uri/@method)"/>
    <xsl:text>) - </xsl:text>
    <xsl:value-of select="pr:description"/>
    <xsl:text>&#10;</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template match="pr:error" mode="text">
  <xsl:value-of select="pr:message"/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:response" mode="text">
  <xsl:value-of select="pr:message"/>
  <xsl:if test="pr:expires">
    <xsl:text> (expires: </xsl:text>
    <xsl:value-of select="pr:expires"/>
    <xsl:text>)</xsl:text>
  </xsl:if>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="attribute()|comment()|processing-instruction()" mode="text">
  <!-- nop -->
</xsl:template>

<xsl:template match="text()" mode="text">
  <xsl:copy/>
</xsl:template>

<!-- == HTML ========================================================== -->

<xsl:template match="pr:pipeline" mode="html">
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Pipeline: <xsl:value-of select="pr:uri"/></title>
    </head>
    <body>
      <h1><xsl:value-of select="pr:uri"/></h1>
      <xsl:choose>
        <xsl:when test="pr:has-run = 'true'"><p>Has run.</p></xsl:when>
        <xsl:otherwise><p>Has not run.</p></xsl:otherwise>
      </xsl:choose>
      <xsl:if test="pr:output">
        <div class="outputs">
          <h2>Outputs</h2>
          <ul>
            <xsl:apply-templates select="pr:output" mode="html"/>
          </ul>
        </div>
      </xsl:if>
      <xsl:if test="pr:input">
        <div class="inputs">
          <h2>Inputs</h2>
          <ul>
            <xsl:apply-templates select="pr:input" mode="html"/>
          </ul>
        </div>
      </xsl:if>
      <xsl:if test="pr:option">
        <div class="options">
          <h2>Options</h2>
          <dl>
            <xsl:apply-templates select="pr:option" mode="html"/>
          </dl>
        </div>
      </xsl:if>
      <xsl:if test="pr:parameter">
        <div class="parameters">
          <h2>Parameters</h2>
          <dl>
            <xsl:apply-templates select="pr:parameter" mode="html"/>
          </dl>
        </div>
      </xsl:if>
    </body>
  </html>
</xsl:template>

<xsl:template match="pr:input" mode="html">
  <li xmlns="http://www.w3.org/1999/xhtml">
    <xsl:choose>
      <xsl:when test="@primary='true'">
        <strong>
          <xsl:value-of select="."/>
        </strong>
      </xsl:when>
      <xsl:otherwise>
        <span>
          <xsl:value-of select="."/>
        </span>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>: </xsl:text>
    <xsl:value-of select="@documents"/>
    <xsl:text> document(s) available</xsl:text>
  </li>
</xsl:template>

<xsl:template match="pr:output" mode="html">
  <li xmlns="http://www.w3.org/1999/xhtml">
    <xsl:choose>
      <xsl:when test="@primary='true'">
        <strong>
          <xsl:value-of select="."/>
        </strong>
      </xsl:when>
      <xsl:otherwise>
        <span>
          <xsl:value-of select="."/>
        </span>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>: </xsl:text>
    <xsl:value-of select="@documents"/>
    <xsl:text> document(s) remain</xsl:text>
  </li>
</xsl:template>

<xsl:template match="pr:option|pr:parameter" mode="html">
  <dt xmlns="http://www.w3.org/1999/xhtml">
    <xsl:choose>
      <xsl:when test="pr:value/@default='true'">
        <span>
          <xsl:value-of select="pr:name"/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <strong>
          <xsl:value-of select="pr:name"/>
        </strong>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="contains(pr:name, ':')">
      <xsl:text> (namespace = “</xsl:text>
      <xsl:value-of select="namespace-uri-for-prefix(substring-before(pr:name,':'), pr:name)"/>
      <xsl:text>”)</xsl:text>
    </xsl:if>
  </dt>
  <dd xmlns="http://www.w3.org/1999/xhtml">
    <xsl:choose>
      <xsl:when test="pr:value/@initialized='true'">
        <xsl:text>(initialized to general value)</xsl:text>
      </xsl:when>
      <xsl:when test="pr:value/@default='true'">
        <xsl:text>(no value provided; default will be used)</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <strong>
          <xsl:value-of select="pr:value"/>
        </strong>
      </xsl:otherwise>
    </xsl:choose>
  </dd>
</xsl:template>

<xsl:template match="pr:status" mode="html">
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Piperack status</title>
    </head>
    <body>
      <h1>Piperack status</h1>
      <dl>
        <dt>XML Calabash version</dt>
        <dd><xsl:value-of select="pr:version"/></dd>
        <dt>Saxon version</dt>
        <dd><xsl:value-of select="pr:saxon-version"/></dd>
        <dt>Saxon edition</dt>
        <dd><xsl:value-of select="pr:saxon-edition"/></dd>
        <dt>Copyright</dt>
        <dd><xsl:value-of select="pr:copyright"/></dd>
      </dl>
      <p>
        <xsl:apply-templates select="pr:message" mode="html"/>
      </p>
    </body>
  </html>
</xsl:template>

<xsl:template match="pr:message" mode="html">
  <xsl:value-of select="."/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:pipelines" mode="html">
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Piperack pipelines</title>
    </head>
    <body>
      <h1>Piperack pipelines</h1>
      <ul>
        <xsl:for-each select="pr:pipeline">
          <li>
            <code>
              <xsl:value-of select="pr:uri"/>
            </code>
            <xsl:text> (</xsl:text>
            <xsl:choose>
              <xsl:when test="pr:has-run = 'true'">
                <xsl:text>has run</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>has not run</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="pr:expires">
              <xsl:text>; expires: </xsl:text>
              <xsl:value-of select="pr:expires"/>
            </xsl:if>
            <xsl:text>)&#10;</xsl:text>
          </li>
        </xsl:for-each>
      </ul>
    </body>
  </html>
</xsl:template>

<xsl:template match="pr:help" mode="html">
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Piperack help</title>
    </head>
    <body>
      <h1>Piperack help</h1>
      <dl>
        <xsl:for-each select="pr:endpoint">
          <dt>
            <code>
              <xsl:value-of select="pr:uri"/>
            </code>
            <xsl:text> (</xsl:text>
            <xsl:value-of select="lower-case(pr:uri/@method)"/>
            <xsl:text>)</xsl:text>
          </dt>
          <dd><xsl:value-of select="pr:description"/></dd>
        </xsl:for-each>
      </dl>
    </body>
  </html>
</xsl:template>

<xsl:template match="pr:error" mode="html">
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Pipeline error</title>
    </head>
    <body>
      <h1>Error</h1>
      <dl>
        <dt>Code</dt>
        <dd><xsl:value-of select="pr:code"/></dd>
        <dt>Message</dt>
        <dd><xsl:value-of select="pr:message"/></dd>
      </dl>
    </body>
  </html>
</xsl:template>

<xsl:template match="pr:response" mode="html">
  <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
      <title>Pipeline response</title>
    </head>
    <body>
      <h1>Response</h1>
      <dl>
        <dt>Code</dt>
        <dd><xsl:value-of select="pr:code"/></dd>
        <dt>Message</dt>
        <dd><xsl:value-of select="pr:message"/></dd>
        <xsl:if test="pr:expires">
          <dt>Expires</dt>
          <dd><xsl:value-of select="pr:expires"/></dd>
        </xsl:if>
      </dl>
    </body>
  </html>
</xsl:template>

<xsl:template match="attribute()|comment()|processing-instruction()" mode="html">
  <!-- nop -->
</xsl:template>

<xsl:template match="text()" mode="html">
  <xsl:copy/>
</xsl:template>

<!-- == JSON ========================================================== -->

<xsl:template match="pr:pipeline" mode="json">
  <xsl:text>{</xsl:text>
  <xsl:value-of select="f:item('uri', pr:uri)"/>
  <xsl:text>, </xsl:text>
  <xsl:value-of select="f:item('has-run', pr:has-run)"/>

  <xsl:variable name="inputs" as="item()*">
    <xsl:if test="pr:input">
      <xsl:text>"inputs": {</xsl:text>
      <xsl:apply-templates select="pr:input" mode="json"/>
      <xsl:text>}&#10;</xsl:text>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="outputs" as="item()*">
    <xsl:if test="pr:output">
      <xsl:text>"outputs": {</xsl:text>
      <xsl:apply-templates select="pr:output" mode="json"/>
      <xsl:text>}&#10;</xsl:text>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="options" as="item()*">
    <xsl:if test="pr:option">
      <xsl:text>"options": {</xsl:text>
      <xsl:apply-templates select="pr:option" mode="json"/>
      <xsl:text>}&#10;</xsl:text>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="parameters" as="item()*">
    <xsl:if test="pr:parameter">
      <xsl:text>"parameters": {</xsl:text>
      <xsl:apply-templates select="pr:parameter" mode="json"/>
      <xsl:text>}&#10;</xsl:text>
    </xsl:if>
  </xsl:variable>

  <xsl:variable name="inputs"
                select="if (empty($inputs)) then () else string-join($inputs,'')"/>
  <xsl:variable name="outputs"
                select="if (empty($outputs)) then () else string-join($outputs,'')"/>
  <xsl:variable name="options"
                select="if (empty($options)) then () else string-join($options,'')"/>
  <xsl:variable name="parameters"
                select="if (empty($parameters)) then () else string-join($parameters,'')"/>

  <xsl:if test="exists(($inputs,$outputs,$options,$parameters))">,</xsl:if>

  <xsl:value-of select="string-join(($inputs,$outputs,$options,$parameters), ',')"/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:input" mode="json">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>": {</xsl:text>
  <xsl:value-of select="f:item('primary', (@primary,'false')[1])"/>
  <xsl:text>,</xsl:text>
  <xsl:value-of select="f:item('documents', @documents)"/>
  <xsl:text>}</xsl:text>
  <xsl:if test="following-sibling::pr:input">, </xsl:if>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:output" mode="json">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>": {</xsl:text>
  <xsl:value-of select="f:item('primary', (@primary, 'false')[1])"/>
  <xsl:text>,</xsl:text>
  <xsl:value-of select="f:item('documents', @documents)"/>
  <xsl:text>}</xsl:text>
  <xsl:if test="following-sibling::pr:output">, </xsl:if>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:option" mode="json">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="pr:name"/>
  <xsl:text>": {</xsl:text>

  <xsl:if test="contains(pr:name, ':')">
    <xsl:text>"namespace": "</xsl:text>
    <xsl:value-of select="namespace-uri-for-prefix(substring-before(pr:name,':'), pr:name)"/>
    <xsl:text>",</xsl:text>
  </xsl:if>

  <xsl:choose>
    <xsl:when test="pr:value/@initialized='true'">
      <xsl:value-of select="f:item('initialized', 'true')"/>
    </xsl:when>
    <xsl:when test="pr:value/@default='true'">
      <xsl:value-of select="f:item('default', 'true')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="f:item('value', pr:value)"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>}</xsl:text>
  <xsl:if test="following-sibling::pr:option">, </xsl:if>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:parameter" mode="json">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="pr:name"/>
  <xsl:text>": {</xsl:text>

  <xsl:if test="contains(pr:name, ':')">
    <xsl:text>"namespace": "</xsl:text>
    <xsl:value-of select="namespace-uri-for-prefix(substring-before(pr:name,':'), pr:name)"/>
    <xsl:text>",</xsl:text>
  </xsl:if>

  <xsl:choose>
    <xsl:when test="pr:value/@initialized='true'">
      <xsl:value-of select="f:item('initialized', 'true')"/>
    </xsl:when>
    <xsl:when test="pr:value/@default='true'">
      <xsl:value-of select="f:item('default', 'true')"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="f:item('value', pr:value)"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>}</xsl:text>
  <xsl:if test="following-sibling::pr:parameter">, </xsl:if>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="pr:status" mode="json">
  <xsl:text>{</xsl:text>
  <xsl:value-of select="f:item('version', pr:version)"/>
  <xsl:text>,</xsl:text>
  <xsl:value-of select="f:item('saxon-version', pr:saxon-version)"/>
  <xsl:text>,</xsl:text>
  <xsl:value-of select="f:item('saxon-edition', pr:saxon-edition)"/>
  <xsl:text>,</xsl:text>
  <xsl:value-of select="f:item('copyright', pr:copyright)"/>
  <xsl:text>,"message": [</xsl:text>
  <xsl:apply-templates select="pr:message" mode="json"/>
  <xsl:text>]}</xsl:text>
</xsl:template>

<xsl:template match="pr:message" mode="json">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>"</xsl:text>
  <xsl:if test="following-sibling::pr:message">
    <xsl:text>, </xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="pr:pipelines" mode="json">
  <xsl:text>[</xsl:text>

  <xsl:for-each select="pr:pipeline">
    <xsl:text>{ "uri": "</xsl:text>
    <xsl:value-of select="pr:uri"/>
    <xsl:text>",</xsl:text>
    <xsl:text>"has-run": </xsl:text>
    <xsl:value-of select="pr:has-run"/>
    <xsl:if test="pr:expires">
      <xsl:text>, "expires": "</xsl:text>
      <xsl:value-of select="pr:expires"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:text>}</xsl:text>
    <xsl:if test="following-sibling::pr:pipeline">,</xsl:if>
  </xsl:for-each>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="pr:uri" mode="json">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>"</xsl:text>
  <xsl:if test="following-sibling::pr:uri">
    <xsl:text>, </xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="pr:help" mode="json">
  <xsl:text>{</xsl:text>
  <xsl:for-each select="pr:endpoint">
    <xsl:text>"</xsl:text>
    <xsl:value-of select="pr:uri"/>
    <xsl:text>": {</xsl:text>
    <xsl:value-of select="f:item('method',lower-case(pr:uri/@method))"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="f:item('description',pr:description)"/>
    <xsl:text>}</xsl:text>
    <xsl:if test="following-sibling::pr:endpoint">, </xsl:if>
  </xsl:for-each>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="pr:error" mode="json">
  <xsl:text>{</xsl:text>
  <xsl:value-of select="f:item('code', pr:code)"/>
  <xsl:text>,</xsl:text>
  <xsl:value-of select="f:item('message', pr:message)"/>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="pr:response" mode="json">
  <xsl:text>{</xsl:text>
  <xsl:for-each select="*">
    <xsl:value-of select="f:item(local-name(.), .)"/>
    <xsl:if test="following-sibling::*">
      <xsl:text>,</xsl:text>
    </xsl:if>
  </xsl:for-each>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="attribute()|comment()|processing-instruction()" mode="json">
  <!-- nop -->
</xsl:template>

<xsl:template match="text()" mode="json">
  <xsl:copy/>
</xsl:template>

<!-- == XXXX ========================================================== -->

<xsl:function name="f:item">
  <xsl:param name="name" as="xs:string"/>
  <xsl:param name="value" as="xs:string"/>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="$name"/>
  <xsl:text>": </xsl:text>

  <xsl:choose>
    <xsl:when test="$value castable as xs:integer">
      <xsl:value-of select="$value"/>
    </xsl:when>
    <xsl:when test="$value castable as xs:boolean">
      <xsl:value-of select="$value"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>"</xsl:text>
      <xsl:value-of select="$value"/>
      <xsl:text>"</xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:text>&#10;</xsl:text>
</xsl:function>

</xsl:stylesheet>
