<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" version="1.0">
      <p:string-replace match="doc/@version">
	<p:with-option name="replace" select="concat('&#34;', number(/doc/@version)+1,'&#34;')"/>
      </p:string-replace>
    </p:pipeline>