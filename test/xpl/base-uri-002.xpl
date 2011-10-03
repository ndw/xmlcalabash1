<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <!-- This p:identity step makes sure that we grab the root element -->
      <!-- where the xml:base exists. Otherwise, we get the base uri -->
      <!-- of the input document itself, and that varies by test env. -->
      <p:identity>
	<p:input port="source" select="/doc"/>
      </p:identity>

      <p:string-replace match="para[@class]/text()">
	<p:with-option name="replace" select="concat('&#34;',p:base-uri(),'&#34;')"/>
      </p:string-replace>
    </p:pipeline>