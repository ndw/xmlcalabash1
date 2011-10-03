<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:choose name="choose">
  <p:when test="count(//*) mod 2 = 0">
    <p:output port="one" primary="true"/>
    <p:output port="two">
      <p:inline><doc/></p:inline>
    </p:output>
    <p:identity>
      <p:input port="source">
	<p:inline><p>There are an even number of elements in the document.</p></p:inline>
      </p:input>
    </p:identity>
  </p:when>
  <p:otherwise>
    <p:output port="one" primary="true"/>
    <p:output port="two">
      <p:inline><doc/></p:inline>
    </p:output>
    <p:identity>
      <p:input port="source">
	<p:inline><p>There are an odd number of elements in the document.</p></p:inline>
      </p:input>
    </p:identity>
  </p:otherwise>
</p:choose>

<p:wrap-sequence wrapper="wrapper">
  <p:input port="source">
    <p:pipe step="choose" port="one"/>
    <p:pipe step="choose" port="two"/>
  </p:input>
</p:wrap-sequence>

</p:pipeline>