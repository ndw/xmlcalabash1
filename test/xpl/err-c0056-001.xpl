<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://xproc.dev.java.net/ns/extensions" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
	<p:input port="source"/>
	<p:input port="style"/>
	<p:input port="parameters" kind="parameter"/>
	<p:output port="result"/>

	<p:xslt version="1.0" initial-mode="non-existent-mode">
	  <p:input port="source">
	    <p:pipe step="pipeline" port="source"/>
	  </p:input>
	  <p:input port="stylesheet">
	    <p:pipe step="pipeline" port="style"/>
	  </p:input>
	</p:xslt>

	</p:declare-step>