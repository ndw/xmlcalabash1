<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

	  <p:identity>
	    <p:input port="source">
	      <p:empty/>
	      <p:inline>
	          <test/>
	      </p:inline>
	    </p:input>
	  </p:identity>

	</p:pipeline>