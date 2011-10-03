<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" xmlns:px="http://example.org/ns/pipelines" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:t="http://xproc.org/ns/testsuite" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="main">
<p:output port="result"/>
<p:option name="path" required="true"/>

<p:directory-list name="dirlist" include-filter=".*file" exclude-filter="a.*">
  <p:with-option name="path" select="$path">
    <p:empty/>
  </p:with-option>
</p:directory-list>

<p:identity name="testadir">
  <p:input port="source" select="/c:directory/c:directory[@name='adir']">
    <p:pipe step="dirlist" port="result"/>
  </p:input>
</p:identity>

<p:identity name="testbdir">
  <p:input port="source" select="/c:directory/c:directory[@name='adir']">
    <p:pipe step="dirlist" port="result"/>
  </p:input>
</p:identity>

<p:identity name="testafile">
  <p:input port="source" select="/c:directory/c:file[@name='afile']">
    <p:pipe step="dirlist" port="result"/>
  </p:input>
</p:identity>

<p:identity name="testbfile">
  <p:input port="source" select="/c:directory/c:file[@name='bfile']">
    <p:pipe step="dirlist" port="result"/>
  </p:input>
</p:identity>

<p:identity name="testcfile">
  <p:input port="source" select="/c:directory//c:file[@name='cfile']">
    <p:pipe step="dirlist" port="result"/>
  </p:input>
</p:identity>

<p:identity>
  <p:input port="source">
    <p:pipe step="testadir" port="result"/>
    <p:pipe step="testbdir" port="result"/>
    <p:pipe step="testafile" port="result"/>
    <p:pipe step="testbfile" port="result"/>
    <p:pipe step="testcfile" port="result"/>
  </p:input>
</p:identity>

</p:declare-step>