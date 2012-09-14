<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cxp="http://xmlcalabash.com/ns/extensions/xpcparser"
                xmlns:p="http://www.w3.org/ns/xproc"
                exclude-result-prefixes="cxp"
                version="2.0">

<xsl:output method="xml" encoding="utf-8" indent="no"
	    omit-xml-declaration="yes"/>

<xsl:strip-space elements="*"/>

<xsl:template match="document">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="TOKEN | QName | NCName | CName | quotedstr
                     | namespace | parExpression | from | EOF"/>

<xsl:template match="declareStep">
  <p:declare-step version="1.0">
    <xsl:apply-templates select="/document" mode="namespaces"/>
    <xsl:apply-templates select="inports|outports"/>
    <xsl:apply-templates select="node() except (inports|outports)"/>
  </p:declare-step>
</xsl:template>

<xsl:template match="pipeline">
  <p:pipeline version="1.0">
    <xsl:apply-templates select="/document" mode="namespaces"/>
    <xsl:apply-templates select="inports|outports"/>
    <xsl:apply-templates select="node() except (inports|outports)"/>
  </p:pipeline>
</xsl:template>

<xsl:template match="inports|outports">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="input">
  <p:input port="{NCName/NCName}">
    <xsl:if test="TOKEN = 'primary'">
      <xsl:attribute name="primary" select="'true'"/>
    </xsl:if>
  </p:input>
</xsl:template>

<xsl:template match="output">
  <p:output port="{NCName/NCName}">
    <xsl:if test="TOKEN = 'primary'">
      <xsl:attribute name="primary" select="'true'"/>
    </xsl:if>
  </p:output>
</xsl:template>

<xsl:template match="option">
  <p:option name="{QName}"/>
</xsl:template>

<xsl:template match="subpipeline">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="forEachStep">
  <p:for-each select="{parExpression/expr}">
    <xsl:apply-templates/>
  </p:for-each>
</xsl:template>

<xsl:template match="chooseStep">
  <p:choose>
    <p:when test="{parExpression}">
      <xsl:apply-templates select="from|subpipeline"/>
    </p:when>
    <xsl:apply-templates select="whenStep|otherwiseStep"/>
  </p:choose>
</xsl:template>

<xsl:template match="whenStep">
  <p:when test="{parExpression}">
    <xsl:apply-templates select="from|subpipeline"/>
  </p:when>
</xsl:template>

<xsl:template match="otherwiseStep">
  <p:otherwise>
    <xsl:apply-templates select="subpipeline"/>
  </p:otherwise>
</xsl:template>

<xsl:template match="atomicStep">
  <xsl:element name="{concat('p:', TOKEN[last()])}">
    <xsl:if test="NCName">
      <xsl:attribute name="name" select="NCName/NCName"/>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="element()">
  <xsl:copy>
    <xsl:apply-templates select="@*,node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()">
  <xsl:copy/>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="cxp:quotedstr">
  <xsl:param name="quotedstr" as="xs:string"
             xmlns:xs="http://www.w3.org/2001/XMLSchema"/>

  <xsl:variable name="strip1" select="substring($quotedstr,2)"/>
  <xsl:variable name="str" select="substring($strip1, 1, string-length($strip1)-1)"/>

  <xsl:value-of select="$str"/>
</xsl:function>

<xsl:template match="document" mode="namespaces">
  <xsl:namespace name="p" select="'http://www.w3.org/ns/xproc'"/>

  <xsl:for-each select="namespace">
    <xsl:namespace name="{prefix/NCName/NCName}" select="cxp:quotedstr(quotedstr)"/>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>

<!--
namespace c: "http://www.w3.org/ns/xproc-step"

step ( primary source, $opt ) {
  for-each (xyz) {
    identity
  }
  if ($opt = 3 or (false() != true())) from pipe stepname/result {
    o3 = identity
  } else if ($opt = 4) {
    o4 = identity
  } else {
    other = identity
  }
} => ( primary result )

<document>
   <namespace>
      <TOKEN>namespace</TOKEN>
      <prefix>
         <NCName>
            <NCName>c</NCName>
         </NCName>
         <TOKEN>:</TOKEN>
      </prefix>
      <quotedstr>"http://www.w3.org/ns/xproc-step"</quotedstr>
   </namespace>
   <declareStep>

      <TOKEN>step</TOKEN>
      <inports>
         <TOKEN>(</TOKEN>
         <input>
            <TOKEN>primary</TOKEN>
            <NCName>
               <NCName>source</NCName>
            </NCName>
         </input>
         <TOKEN>,</TOKEN>
         <option>
            <TOKEN>$</TOKEN>
            <QName>
               <NCName>
                  <NCName>opt</NCName>
               </NCName>
            </QName>
         </option>
         <TOKEN>)</TOKEN>
      </inports>
      <TOKEN>{</TOKEN>
      <subpipeline>
         <forEachStep>
            <TOKEN>for-each</TOKEN>
            <parExpression>
               <TOKEN>(</TOKEN>
               <expr>
                  <noParExpression>xyz</noParExpression>
               </expr>
               <TOKEN>)</TOKEN>
            </parExpression>
            <TOKEN>{</TOKEN>
            <subpipeline>
               <atomicStep>
                  <TOKEN>identity</TOKEN>
               </atomicStep>
            </subpipeline>
            <TOKEN>}</TOKEN>
         </forEachStep>
         <chooseStep>
            <TOKEN>if</TOKEN>
            <parExpression>
               <TOKEN>(</TOKEN>
               <expr>
                  <noParExpression>$opt = 3 or </noParExpression>
                  <parExpression>
                     <TOKEN>(</TOKEN>
                     <expr>
                        <noParExpression>false</noParExpression>
                        <parExpression>
                           <TOKEN>(</TOKEN>
                           <expr>
                              <noParExpression/>
                           </expr>
                           <TOKEN>)</TOKEN>
                        </parExpression>
                        <noParExpression> != true</noParExpression>
                        <parExpression>
                           <TOKEN>(</TOKEN>
                           <expr>
                              <noParExpression/>
                           </expr>
                           <TOKEN>)</TOKEN>
                        </parExpression>
                     </expr>
                     <TOKEN>)</TOKEN>
                  </parExpression>
               </expr>
               <TOKEN>)</TOKEN>
            </parExpression>
            <from>
               <fromPipe>
                  <TOKEN>from</TOKEN>
                  <TOKEN>pipe</TOKEN>
                  <NCName>
                     <NCName>stepname</NCName>
                  </NCName>
                  <TOKEN>/</TOKEN>
                  <NCName>
                     <NCName>result</NCName>
                  </NCName>
               </fromPipe>
            </from>
            <TOKEN>{</TOKEN>
            <subpipeline>
               <atomicStep>
                  <NCName>
                     <NCName>o3</NCName>
                  </NCName>
                  <TOKEN>=</TOKEN>
                  <TOKEN>identity</TOKEN>
               </atomicStep>
            </subpipeline>
            <TOKEN>}</TOKEN>
            <whenStep>
               <TOKEN>else</TOKEN>
               <TOKEN>if</TOKEN>
               <parExpression>
                  <TOKEN>(</TOKEN>
                  <expr>
                     <noParExpression>$opt = 4</noParExpression>
                  </expr>
                  <TOKEN>)</TOKEN>
               </parExpression>
               <TOKEN>{</TOKEN>
               <subpipeline>
                  <atomicStep>
                     <NCName>
                        <NCName>o4</NCName>
                     </NCName>
                     <TOKEN>=</TOKEN>
                     <TOKEN>identity</TOKEN>
                  </atomicStep>
               </subpipeline>
               <TOKEN>}</TOKEN>
            </whenStep>
            <otherwiseStep>
               <TOKEN>else</TOKEN>
               <TOKEN>{</TOKEN>
               <subpipeline>
                  <atomicStep>
                     <NCName>
                        <NCName>other</NCName>
                     </NCName>
                     <TOKEN>=</TOKEN>
                     <TOKEN>identity</TOKEN>
                  </atomicStep>
               </subpipeline>
               <TOKEN>}</TOKEN>
            </otherwiseStep>
         </chooseStep>
      </subpipeline>
      <TOKEN>}</TOKEN>
      <TOKEN>=&gt;</TOKEN>
      <outports>
         <TOKEN>(</TOKEN>
         <output>
            <TOKEN>primary</TOKEN>
            <NCName>
               <NCName>result</NCName>
            </NCName>
         </output>
         <TOKEN>)</TOKEN>
      </outports>
   </declareStep>
   <EOF/>
</document>
-->
