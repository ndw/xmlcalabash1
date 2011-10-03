<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

      <p:add-attribute xmlns:test="http://test.com" match="title" attribute-value="bar">
        <p:with-option name="attribute-name" select="'test:foo'"/>
      </p:add-attribute>

      <!-- After this step, only one (test2:foo) attribute should be set since
           test and test2 are bound to the same namespace URI -->  
      <p:add-attribute xmlns:test2="http://test.com" match="title" attribute-value="bar">
        <p:with-option name="attribute-name" select="'test2:foo'"/>
      </p:add-attribute>

    </p:pipeline>