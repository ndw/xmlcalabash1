<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      
      <p:label-elements label="concat(&#34;http://foo.com/&#34;, $p:index)" attribute="xml:base" match="chap"/>

    </p:pipeline>