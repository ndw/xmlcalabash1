<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">

      <p:output port="result"/>

      <p:input port="source">
	<p:inline>
	  <doc>
	    <para id="x1">Some paragraph.</para>
	    <para id="x2">Some paragraph.</para>
	    <para id="x3">Some paragraph.</para>
	  </doc>
	</p:inline>
      </p:input>

      <p:for-each name="loop">
	<p:iteration-source select="/doc/para"/>

	<p:variable name="q" select="/para/@id">
	  <p:pipe step="loop" port="current"/>
	</p:variable>

	<p:choose>
	  <p:when test="$q = 'x2'">
	    <p:identity/>
	  </p:when>
	  <p:otherwise>
	    <p:identity>
	      <p:input port="source">
		<p:empty/>
	      </p:input>
	    </p:identity>
	  </p:otherwise>
	</p:choose>
      </p:for-each>

      <p:wrap-sequence wrapper="newdoc"/>

    </p:declare-step>