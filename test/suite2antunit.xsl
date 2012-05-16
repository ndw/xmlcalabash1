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
    xmlns:c="http://www.w3.org/ns/xproc-step"
    xmlns:err="http://www.w3.org/ns/xproc-error"
    version="2.0"
    exclude-result-prefixes="xs t c err">

<xsl:strip-space elements="t:*" />

<xsl:param name="destdir"
	   select="'antunit'"
	   as="xs:string" />
<xsl:param name="antunit.home"
	   select="'/usr/local/src/apache-ant-antunit-1.2'"
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
	       classpath="${{test.basedir}}/../../calabash.jar"/>

      <target name="antunit">
	<au:antunit>
	  <file file="build-antunit.xml"/>
	  <au:plainlistener/>
	</au:antunit>
      </target>

      <xsl:apply-templates select="t:test" />
    </project>
  </xsl:result-document>
  <xsl:apply-templates select="t:test" mode="t:test" />
</xsl:template>
<xsl:template match="t:title" />

<xsl:template match="t:test">
  <xsl:variable
      name="test-doc"
      select="document(@href, /)"
      as="document-node()?"/>
  <xsl:variable
      name="testdir"
      select="replace(@href, '\.xml$', '')"
      as="xs:string" />

  <target name="test_{translate(replace(@href, '\.xml$', ''), '/-', '__')}" description="{$test-doc/t:test/t:title}">
    <echo message="{replace(@href, '\.xml$', '')}::{$test-doc/t:test/t:title}" />
    <calabash pipeline="{$testdir}/pipeline.xpl">
      <sysproperty key="com.xmlcalabash.phonehome" value="false"/>
      <xsl:for-each select="$test-doc/t:test/t:input">
	<input port="{@port}">
	  <file file="{$testdir}/input{position()}.xml" />
	</input>
      </xsl:for-each>
      <xsl:for-each select="$test-doc/t:test/t:output">
	<output port="{@port}">
	  <file file="{$testdir}/{@port}.xml" />
	</output>
      </xsl:for-each>
    </calabash>
  </target>
</xsl:template>

<xsl:template match="t:test" mode="t:test">
  <xsl:variable
      name="test-doc"
      select="document(@href, /)"
      as="document-node()?"/>

  <xsl:message>
    <xsl:value-of select="@href" />
    <xsl:text>:</xsl:text>
    <xsl:value-of select="count($test-doc/t:test/t:input)" />
    <xsl:text>:</xsl:text>
    <xsl:value-of select="count($test-doc/t:test/t:output)" />
  </xsl:message>

  <xsl:variable
      name="testdir"
      select="concat($destdir, '/', replace(@href, '\.xml$', ''))"
      as="xs:string" />

  <xsl:for-each select="$test-doc/t:test/t:pipeline">
    <xsl:result-document
	href="{$testdir}/pipeline.xpl"
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

  <xsl:for-each select="$test-doc/t:test/t:input">
    <xsl:result-document
	href="{$testdir}/input{position()}.xml"
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

  <xsl:for-each select="$test-doc/t:test/t:output">
    <xsl:result-document
	href="{$testdir}/{@port}-ref.xml"
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
</xsl:template>

</xsl:stylesheet>