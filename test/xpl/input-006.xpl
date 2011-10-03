<p:pipeline xmlns:p="http://www.w3.org/ns/xproc" xmlns:t="http://xproc.org/ns/testsuite" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">

	      <p:identity name="step1">
	        <p:input port="source">
	            <p:inline>
	                <step1_test/>
	            </p:inline>
	        </p:input>
	      </p:identity>

	      <p:identity name="step2">
	        <p:input port="source">
	            <p:inline>
	                <step2_test/>
	            </p:inline>
	        </p:input>
	      </p:identity>

              <p:sink/>

	      <p:identity name="step3">
	          <p:input port="source">
	              <p:pipe step="step1" port="result"/>
	          </p:input>
	      </p:identity>

	</p:pipeline>