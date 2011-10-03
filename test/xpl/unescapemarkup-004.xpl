<p:declare-step xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
<p:input port="source"/>
<p:output port="result"/>

<p:unescape-markup namespace="http://www.w3.org/1999/xhtml" encoding="base64" content-type="text/html" charset="UTF-8"/>

</p:declare-step>