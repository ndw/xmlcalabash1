<p:declare-step xmlns:ex="http://example.com/ns/xproc-extensions" xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0">
      <p:output port="result"/>

      <p:choose>
	<p:when test="p:value-available('opt', false())">
	  <p:xpath-context>
	    <p:empty/>
	  </p:xpath-context>
	  <p:identity>
	    <p:input port="source">
	      <p:inline>
		<doc>Failure.</doc>
	      </p:inline>
	    </p:input>
	  </p:identity>
	</p:when>
	<p:otherwise>
	  <p:identity>
	    <p:input port="source">
	      <p:inline>
		<doc>Success.</doc>
	      </p:inline>
	    </p:input>
	  </p:identity>
	</p:otherwise>
      </p:choose>
    </p:declare-step>