<p:pipeline xmlns:t="http://xproc.org/ns/testsuite" xmlns:p="http://www.w3.org/ns/xproc" xmlns:c="http://www.w3.org/ns/xproc-step" xmlns:err="http://www.w3.org/ns/xproc-error" version="1.0" name="pipeline">
      
      <p:label-elements xmlns:test="http://test.com" label="concat(&#34;_foo_&#34;, $p:index, &#34;_bar_&#34;)" attribute="test:pid" match="p"/>

    </p:pipeline>