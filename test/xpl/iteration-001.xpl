<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:viewport match="para">
	<p:add-attribute match="para" attribute-name="count">
	  <p:with-option name="attribute-value" select="concat(p:iteration-position(), ' of ', p:iteration-size())"/>
	</p:add-attribute>
      </p:viewport>
    </p:pipeline>