<!-- ============================================================= -->
<!--  MODULE:     suite2antunit.xsl                                -->
<!--  VERSION:    1                                                -->
<!--  DATE:       15 May 2012                                      -->
<!-- ============================================================= -->

<!-- ============================================================= -->
<!-- SYSTEM:      Calabash                                         -->
<!--                                                               -->
<!-- PURPOSE:     Generate a build file for use with antunit for   -->
<!--              running the CalabashTask Ant task on the XProc   -->
<!--              test suite.                                      -->
<!--                                                               -->
<!-- INPUT FILE:  Valid XProc test-suite XML                       -->
<!--                                                               -->
<!-- OUTPUT FILE: Ant build file.                                  -->
<!--                                                               -->
<!-- CREATED FOR: Calabash                                         -->
<!--                                                               -->
<!-- CREATED BY:  Mentea                                           -->
<!--              13 Kelly's Bay Beach                             -->
<!--              Skerries, Co. Dublin                             -->
<!--              Ireland                                          -->
<!--              http://www.mentea.net/                           -->
<!--              info@mentea.net                                  -->
<!--                                                               -->
<!-- ORIGINAL CREATION DATE:                                       -->
<!--              15 May 2012                                      -->
<!--                                                               -->
<!-- CREATED BY: Tony Graham (tkg)                                 -->
<!--                                                               -->
<!-- ============================================================= -->

<!-- ============================================================= -->
<!--               VERSION HISTORY                                 -->
<!-- ============================================================= -->
<!--
 1.  ORIGINAL VERSION                                 tkg 20120515
                                                                   -->

<!-- ============================================================= -->
<!--                    DESIGN CONSIDERATIONS                      -->
<!-- ============================================================= -->

<!-- ============================================================= -->
<!--                    XSL STYLESHEET INVOCATION                  -->
<!-- ============================================================= -->

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:t="http://xproc.org/ns/testsuite"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:err="http://www.w3.org/ns/xproc-error"
    xmlns:au="antlib:org.apache.ant.antunit"
    xmlns:m="http://www.mentea.net/resources"
    version="2.0"
    exclude-result-prefixes="xs t p c err au">

<xsl:strip-space elements="t:*" />

<xsl:param name="destdir"
	   select="'antunit'"
	   as="xs:string" />
<xsl:param name="calabash.home"
	   select="'/usr/local/src/xmlcalabash1'"
	   as="xs:string" />
<xsl:param name="antunit.home"
	   select="'/usr/local/src/apache-ant-antunit-1.2'"
	   as="xs:string" />
<xsl:param name="sourcedir"
	   select="m:dirname(/)"
	   as="xs:string" />

<xsl:template match="t:test-suite">
  <xsl:result-document
	href="{$destdir}/build-antunit.xml"
	omit-xml-declaration="yes"
	indent="yes">
    <project name="CalabashTask-antunit"
	     basedir="."
	     default="antunit"
	     xmlns:au="antlib:org.apache.ant.antunit">

      <dirname property="test.basedir" file="${{ant.file.CalabashTask-antunit}}"/>

      <property name="antunit.home" value="{$antunit.home}"/>

      <taskdef
	  uri="antlib:org.apache.ant.antunit"
	  resource="org/apache/ant/antunit/antlib.xml">
	<classpath>
	  <pathelement location="${{antunit.home}}/ant-antunit-1.2.jar"/>
	</classpath>
      </taskdef>

      <taskdef name="calabash"
	       classname="com.xmlcalabash.drivers.CalabashTask"
	       classpath="{$calabash.home}/calabash.jar"/>

      <target name="antunit">
	<au:antunit>
	  <file file="build-antunit.xml"/>
	  <au:plainlistener/>
	</au:antunit>
      </target>

      <xsl:apply-templates select="document(t:test/@href)/t:test" />
    </project>
  </xsl:result-document>
  <xsl:apply-templates
      select="document(t:test/@href)/t:test"
      mode="t:test" />
</xsl:template>

<xsl:template match="t:title" />

<xsl:template match="t:test">
  <xsl:variable
      name="test-doc"
      select="/"
      as="document-node()"/>
  <xsl:variable
      name="testdir"
      select="m:basename(m:dirname(base-uri(.)))"
      as="xs:string" />
  <xsl:variable
      name="test-name"
      select="m:basename(base-uri(.), '.xml')"
      as="xs:string" />

  <target name="test_{$testdir}_{translate($test-name, '-', '_')}"
	  description="{t:title}">
    <echo message="{$testdir}/{$test-name}::{t:title}" />
    <xsl:choose>
      <xsl:when test="exists(@error)">
	<au:expectfailure expectedmessage="{substring-after(@error, 'err:')}">
	  <xsl:call-template name="calabash-task">
	    <xsl:with-param name="test-doc"
			    select="$test-doc"
			    as="document-node()"
			    tunnel="yes" />
	    <xsl:with-param name="testdir"
			    select="$testdir"
			    as="xs:string"
			    tunnel="yes" />
	    <xsl:with-param name="test-name"
			    select="$test-name"
			    as="xs:string"
			    tunnel="yes" />
	  </xsl:call-template>
	</au:expectfailure>
      </xsl:when>
      <xsl:otherwise>
	<xsl:call-template name="calabash-task">
	  <xsl:with-param name="test-doc"
			  select="$test-doc"
			  as="document-node()"
			  tunnel="yes" />
	  <xsl:with-param name="testdir"
			  select="$testdir"
			  as="xs:string"
			  tunnel="yes" />
	  <xsl:with-param name="test-name"
			  select="$test-name"
			  as="xs:string"
			  tunnel="yes" />
	</xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </target>
</xsl:template>

<xsl:template name="calabash-task">
  <xsl:param name="test-doc"
	     as="document-node()"
	     tunnel="yes"
	     required="yes" />
  <xsl:param name="testdir"
	     as="xs:string"
	     tunnel="yes"
	     required="yes" />
  <xsl:param name="test-name"
	     as="xs:string"
	     tunnel="yes"
	     required="yes" />

  <calabash useimplicitfileset="false">
    <pipeline>
      <xsl:choose>
	<xsl:when test="exists(t:pipeline/@href)">
	  <file file="{$testdir}/{t:pipeline/@href}" />
	</xsl:when>
	<xsl:otherwise>
	  <file file="{$testdir}/{$test-name}_pipeline.xpl" />
	</xsl:otherwise>
      </xsl:choose>
    </pipeline>
    <xsl:for-each select="t:input">
      <input port="{@port}">
	<xsl:choose>
	  <xsl:when test="exists(t:document)">
	    <xsl:variable name="input-position"
			  select="position()"
			  as="xs:integer" />
	    <xsl:for-each select="t:document">
	      <xsl:choose>
		<xsl:when test="exists(@href)">
		  <file file="{$testdir}/{@href}" />
		</xsl:when>
		<xsl:otherwise>
		  <file file="{$testdir}/{$test-name}_input{$input-position}-{position()}.xml" />
		</xsl:otherwise>
	      </xsl:choose>
	    </xsl:for-each>
	  </xsl:when>
	  <xsl:otherwise>
	    <file file="{$testdir}/{$test-name}_input{position()}.xml" />
	  </xsl:otherwise>
	</xsl:choose>
      </input>
    </xsl:for-each>
    <xsl:for-each select="t:output">
      <output port="{@port}">
	<xsl:choose>
	  <xsl:when test="exists(t:document)">
	    <xsl:for-each select="t:document">
	      <xsl:choose>
		<xsl:when test="exists(@href)">
		  <file file="{$testdir}/{@href}" />
		</xsl:when>
		<xsl:otherwise>
		  <file file="{$testdir}/{$test-name}_{@port}-{position()}.xml" />
		</xsl:otherwise>
	      </xsl:choose>
	    </xsl:for-each>
	  </xsl:when>
	  <xsl:otherwise>
	    <file file="{$testdir}/{$test-name}_{@port}.xml" />
	  </xsl:otherwise>
	</xsl:choose>
      </output>
    </xsl:for-each>
    <xsl:for-each select="t:option">
      <option name="{@name}" value="{@value}" />
    </xsl:for-each>
  </calabash>
</xsl:template>

<xsl:template match="t:test" mode="t:test">
  <xsl:variable
      name="test-doc"
      select="/"
      as="document-node()"/>
  <xsl:variable
      name="testdir"
      select="concat($destdir, '/', m:basename(m:dirname(base-uri(.))))"
      as="xs:string" />
  <xsl:variable
      name="test-name"
      select="m:basename(base-uri(.), '.xml')"
      as="xs:string" />

<!--
  <xsl:message>
    <xsl:value-of select="$test-name" />
    <xsl:text>:</xsl:text>
    <xsl:value-of select="count(t:input)" />
    <xsl:text>:</xsl:text>
    <xsl:value-of select="count(t:output)" />
  </xsl:message>
-->

  <xsl:for-each select="t:pipeline">
    <xsl:choose>
      <!-- External document will be handled separately. -->
      <xsl:when test="exists(@href)" />
      <xsl:otherwise>
	<xsl:result-document
	    href="{$testdir}/{$test-name}_pipeline.xpl"
	    omit-xml-declaration="yes">
	  <xsl:copy-of select="*" />
	</xsl:result-document>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>

  <xsl:for-each select="t:input">
    <xsl:choose>
      <xsl:when test="exists(t:document)">
	<xsl:variable name="input-position"
		      select="position()"
		      as="xs:integer" />
	<xsl:for-each select="t:document">
	  <xsl:choose>
	    <!-- External document will be handled separately. -->
	    <xsl:when test="exists(@href)" />
	    <xsl:otherwise>
	      <xsl:result-document
		  href="{$testdir}/{$test-name}_input{$input-position}-{position()}.xml"
		  omit-xml-declaration="yes">
		<xsl:copy-of select="*" />
	      </xsl:result-document>
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
	<xsl:result-document
	    href="{$testdir}/{$test-name}_input{position()}.xml"
	    omit-xml-declaration="yes">
	  <xsl:copy-of select="*" />
	</xsl:result-document>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>

  <xsl:for-each select="t:output">
    <xsl:choose>
      <xsl:when test="exists(t:document)">
	<xsl:for-each select="t:document">
	  <xsl:result-document
	      href="{$testdir}/{$test-name}_{../@port}-{position()}-ref.xml"
	      omit-xml-declaration="yes">
	    <xsl:choose>
	      <xsl:when test="exists(@href)">
		<xsl:copy-of select="document(@href, .)" />
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:copy-of select="*" />
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:result-document>
	</xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
	<xsl:result-document
	    href="{$testdir}/{$test-name}_{@port}-ref.xml"
	    omit-xml-declaration="yes">
	  <xsl:copy-of select="*" />
	</xsl:result-document>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>

  <!-- Temporary solution for external files is to generate shell
       commands to copy them to correct place. -->
  <xsl:for-each select="t:pipeline/p:pipeline[@href] | t:pipeline//(p:import | p:document[@href]) | */t:document[@href]">
    <xsl:if test="not(contains(@href, '..') or
		      starts-with(@href, 'http') or
		      starts-with(@href, 'unsupported:') or
		      starts-with(@href, '#'))">
      <xsl:if test="contains(@href, '/')">
	<xsl:message>
	  <xsl:text>mkdir -p </xsl:text>
	  <xsl:value-of
	      select="concat($testdir, '/', m:dirname(@href))" />
	</xsl:message>
      </xsl:if>
      <xsl:value-of select="t:cp(concat(m:node-dirname(.), '/', @href),
			         concat($testdir, '/', @href))" />
    </xsl:if>
  </xsl:for-each>
</xsl:template>

<xsl:function name="t:cp" as="empty-sequence()">
  <xsl:param name="from" as="xs:string" />
  <xsl:param name="to" as="xs:string" />

    <xsl:message>
      <xsl:text>cp </xsl:text>
      <xsl:value-of select="$from" />
      <xsl:text> </xsl:text>
      <xsl:value-of select="$to" />
    </xsl:message>
</xsl:function>


<!-- ============================================================= -->
<!--                    FILENAME MUNGING FUNCTIONS                 -->
<!-- ============================================================= -->

<!-- Gets the last component of $uri. -->
<xsl:function name="m:node-basename" as="xs:string">
  <xsl:param name="node" as="node()" />

  <xsl:sequence select="m:basename(base-uri($node))" />
</xsl:function>

<!-- Gets the last component of $uri. -->
<xsl:function name="m:basename" as="xs:string">
  <xsl:param name="uri" as="xs:string" />

  <xsl:sequence select="tokenize($uri, '/')[last()]" />
</xsl:function>

<!-- Gets the last component of $uri. -->
<xsl:function name="m:basename" as="xs:string">
  <xsl:param name="uri" as="xs:string" />
  <xsl:param name="suffix" as="xs:string" />

  <xsl:variable name="suffix-regex"
		select="replace(concat(if (starts-with($suffix, '.')) then '' else '.', $suffix, '$'), '\.', '\\.')"
		as="xs:string" />

  <xsl:sequence select="replace(tokenize($uri, '/')[last()], $suffix-regex, '')" />
</xsl:function>

<xsl:function name="m:node-dirname" as="xs:string">
  <xsl:param name="node" as="node()" />

  <xsl:sequence select="m:dirname(replace(base-uri($node), '^file:', ''))" />
</xsl:function>

<!-- dirname
   - $uri: Pathname for which to get the directory name
   -
   - Gets the directory name (the part before the last $separator)
   - of $string.
   -->
<xsl:function name="m:dirname" as="xs:string">
  <xsl:param name="uri" as="xs:string" />

  <xsl:sequence select="m:dirname($uri, '/')" />
</xsl:function>

<!-- dirname
   - $uri: Pathname for which to get the directory name
   - $separator: String separating directory names.
   -
   - Gets the directory name (the part before the last $separator)
   - of $string.
   -->
<xsl:function name="m:dirname" as="xs:string">
  <xsl:param name="uri" as="xs:string" />
  <xsl:param name="separator" as="xs:string" />

  <xsl:sequence
      select="string-join(tokenize($uri, $separator)[position() &lt; last()],
	                  $separator)" />

</xsl:function>

<!-- merge-dirnames
     - $dirname1: Potential base for a relative directory name
     - $dirname2: directory name to merge
     - $separator: String to use when concatenating directories. Optional.
     -
     - Merge $dirname1 and $dirname2 such that if $dirname2 is
     - a relative path, the result is relative to $dirname1.
     -
     - Also handles if either name or both names are empty.
-->
<xsl:template name="merge-dirnames">
  <xsl:param name="dirname1"/>
  <xsl:param name="dirname2"/>
  <xsl:param name="separator" select="'/'"/>

  <xsl:variable name="merged-dirname">
    <xsl:choose>
      <xsl:when test="substring($dirname2, 1, 5) = 'http:'">
	<xsl:value-of select="$dirname2"/>
      </xsl:when>
      <xsl:when test="$dirname1 != ''">
	<xsl:choose>
	  <xsl:when test="$dirname2 != ''">
	    <xsl:choose>
	      <xsl:when test="substring($dirname2, 1, 1) = $separator">
		<xsl:if test="substring($dirname1, 1, 7) = 'http://'">
		  <xsl:value-of select="concat('http://',
					substring-before(substring($dirname1, 8), '/'))"/>
		</xsl:if>
		<xsl:value-of select="$dirname2"/>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:value-of select="concat($dirname1, $separator, $dirname2)"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:value-of select="$dirname1"/>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>
      <xsl:otherwise>
	<xsl:choose>
	  <xsl:when test="$dirname2">
	    <xsl:value-of select="$dirname2"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:text>.</xsl:text>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:value-of select="$merged-dirname"/>
</xsl:template>

</xsl:stylesheet>
