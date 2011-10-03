<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">

<p:identity name="identity">
  <p:input port="source">
    <p:pipe step="pipeline" port="source"/>
    <p:pipe step="pipeline" port="source"/>
    <p:pipe step="pipeline" port="source"/>
    <p:pipe step="pipeline" port="source"/>
  </p:input>
</p:identity>

<p:wrap-sequence name="splitseq" wrapper="p:pipe-sequence" group-adjacent="floor(position() div 2)"/>

<p:count/>
</p:pipeline>