<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

      <p:add-attribute xmlns:test="http://www.acme.com/test" match="title" attribute-value="bar">
        <p:with-option name="attribute-name" select="'xmlns'"/>
      </p:add-attribute>

    </p:pipeline>