<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:option name="opt1" select="$opt2"/>
      <p:option name="opt2" select="'value'"/>

      <p:sink>
        <p:input port="source">
          <p:inline><doc/></p:inline>
        </p:input>
      </p:sink>
    </p:declare-step>