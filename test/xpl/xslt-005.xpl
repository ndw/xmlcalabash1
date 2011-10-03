<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

	<p:xslt initial-mode="first">
	     <p:input port="stylesheet">
             <p:inline>
	                  <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	    <xsl:template match="/" mode="first">
	            <xsl:copy-of select="."/>            
	    </xsl:template>
	  </xsl:stylesheet>
            </p:inline>    
	     </p:input>
	</p:xslt>

	</p:pipeline>