<p:declare-step xmlns:test="http://www.example.com" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main" type="test:test-step">
      
      <p:input port="parameters" kind="parameter"/>
      <p:output port="result"/>
      
      <!-- count the number of parameters passed to this pipeline -->
      <p:parameters name="params">
        <p:input port="parameters">
          <p:pipe step="main" port="parameters"/>
        </p:input>
      </p:parameters>
      
      <p:count>
        <p:input port="source" select="//c:param">
          <p:pipe step="params" port="result"/>
        </p:input>
      </p:count>
      
      <p:choose>
        <p:when test=". = '2'">
          <!-- 2 parameters: call this pipeline recursively; override + add some parameters -->
          <test:test-step>
            <p:input port="parameters">
              <p:pipe step="main" port="parameters"/>
            </p:input>
            <p:with-param name="param1" select="'valueX'"/>
            <p:with-param name="param3" select="'value3'"/>
          </test:test-step>
        </p:when>
        <p:otherwise>
          <!-- more than 2: generate the result document -->
          <p:xslt name="xslt">
            <!-- this parameter binding will be overriden by the values
                 read from the primary parameter input port of the
                 owner pipeline -->
            <p:with-param name="param2" select="'valueY'">
              <p:empty/>
            </p:with-param>
            <p:input port="source">
              <p:inline>
                <doc/>
              </p:inline>
            </p:input>
            <p:input port="stylesheet">
              <p:inline>
                <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
                  <xsl:param name="param1" select="'unset'"/>
                  <xsl:param name="param2" select="'unset'"/>
                  <xsl:param name="param3" select="'unset'"/>
                  
                  <xsl:template match="/">
                    <doc>
                      <param name="param1" value="{$param1}"/>
                      <param name="param2" value="{$param2}"/>
                      <param name="param3" value="{$param3}"/>
                    </doc>
                  </xsl:template>
                </xsl:stylesheet>
              </p:inline>
            </p:input>
          </p:xslt>
        </p:otherwise>
      </p:choose>
      
    </p:declare-step>