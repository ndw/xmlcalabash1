<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:variable name="count" select="count(//pfx:*)">
        <p:namespaces element="/*"/>
      </p:variable>

      <p:add-attribute match="/result" attribute-name="count">
        <p:with-option name="attribute-value" select="$count"/>
        <p:input port="source">
          <p:inline>
            <result/>
          </p:inline>
        </p:input>
      </p:add-attribute>
    </p:pipeline>