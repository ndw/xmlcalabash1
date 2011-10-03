<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:identity xml:base="http://example.org/doc.xml">
	<p:input port="source">
          <p:inline>
            <doc/>
          </p:inline>
        </p:input>
      </p:identity>

      <p:string-replace match="result/text()">
	<p:with-option name="replace" select="concat('&#34;',p:base-uri(),'&#34;')"/>
        <p:input port="source">
          <p:inline>
            <result>base URI</result>
          </p:inline>
        </p:input>
      </p:string-replace>
    </p:declare-step>