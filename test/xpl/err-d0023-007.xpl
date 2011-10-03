<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:input port="source"/>
      <p:variable name="var1" select="$var2"/>
      <p:variable name="var2" select="'value'"/>

      <p:identity>
        <p:input port="source">
          <p:inline><doc/></p:inline>
        </p:input>
      </p:identity>

      <p:sink/>
    </p:declare-step>