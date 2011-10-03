<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>
      <p:option name="path" required="true"/>

      <p:load>
        <p:with-option name="href" select="$path">
          <p:empty/>
        </p:with-option>
      </p:load>
      <p:identity/>

    </p:declare-step>