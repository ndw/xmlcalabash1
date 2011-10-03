<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:string-replace match="para[@class]/text()">
	<p:with-option name="replace" select="concat('&#34;',p:base-uri(/doc/title),'&#34;')"/>
      </p:string-replace>
    </p:pipeline>